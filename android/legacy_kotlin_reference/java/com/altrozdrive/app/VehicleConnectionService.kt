package com.altrozdrive.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Keeps the live vehicle connection eligible to continue while AltrozDrive is backgrounded.
 * When the user has enabled route recording and already granted location permission, this
 * service also owns GPS collection so route points continue while MainActivity is backgrounded.
 */
class VehicleConnectionService : Service() {
    companion object {
        const val ACTION_ROUTE_RECORDING_CHANGED = "com.altrozdrive.app.ROUTE_RECORDING_CHANGED"
        const val EXTRA_ROUTE_RECORDING_ENABLED = "route_recording_enabled"
        private const val MAX_FIX_AGE_MS = 15_000L
        private const val MAX_FUTURE_FIX_SKEW_MS = 5_000L
        private const val GPS_PREFERENCE_WINDOW_MS = 20_000L
        private const val MAX_GPS_ACCURACY_METERS = 45f
        private const val MAX_NETWORK_ACCURACY_METERS = 30f
    }

    private val channelId = "vehicle_connection"
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    @Volatile private var lastAcceptedGpsAt: Long = 0L
    @Volatile private var lastAcceptedLocationTime: Long = 0L
    // Reuse one helper for the lifetime of the service instead of constructing a
    // new SQLiteOpenHelper for every GPS callback. This does not change route logic.
    private lateinit var snapshotDb: VehicleSnapshotDb

    override fun onCreate() {
        super.onCreate()
        snapshotDb = VehicleSnapshotDb(applicationContext)
        createChannel()
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("AltrozDrive vehicle connection")
            .setContentText("Automatically watching for the paired vehicle")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            val hasLocationPermission = hasLocationPermission()
            val foregroundTypes = if (hasLocationPermission) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            }
            startForeground(7001, notification, foregroundTypes)
        } else {
            startForeground(7001, notification)
        }
        BluetoothDiagnosticManager.getInstance(applicationContext).refresh()
        startRouteRecordingIfEnabled()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ROUTE_RECORDING_CHANGED) {
            if (intent.getBooleanExtra(EXTRA_ROUTE_RECORDING_ENABLED, false)) startRouteRecordingIfEnabled()
            else stopRouteRecording()
        } else {
            // Re-check after Activity starts the service or after a permission grant.
            startRouteRecordingIfEnabled()
        }
        return START_STICKY
    }

    private fun startRouteRecordingIfEnabled() {
        val prefs = getSharedPreferences("altrozdrive", MODE_PRIVATE)
        if (!prefs.getBoolean("route_recording_enabled", false)) {
            stopRouteRecording()
            return
        }
        if (!hasLocationPermission()) return
        val lm = getSystemService(LOCATION_SERVICE) as? LocationManager ?: return
        locationManager = lm
        if (locationListener != null) return
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val now = System.currentTimeMillis()
                if (!location.isUsable(now)) return
                val isGps = location.provider == LocationManager.GPS_PROVIDER
                // GPS and network fixes must not be interleaved: their positions can differ
                // by tens or hundreds of metres and make the displayed route zig-zag.
                // Network is retained only as a bounded fallback while no good GPS fix arrives.
                if (!isGps && now - lastAcceptedGpsAt < GPS_PREFERENCE_WINDOW_MS) return
                if (location.time <= lastAcceptedLocationTime) return
                val manager = BluetoothDiagnosticManager.getInstance(applicationContext)
                val stateKey = manager.currentState().selectedDevice?.address
                    ?.uppercase(java.util.Locale.US)?.takeIf { it.isNotBlank() }
                val savedKey = try {
                    getSharedPreferences("altrozdrive", MODE_PRIVATE)
                        .getString("auto_vehicle_bt_address", null)
                        ?.uppercase(java.util.Locale.US)
                        ?.takeIf { it.isNotBlank() }
                } catch (_: Throwable) { null }
                val vehicleKey = stateKey ?: savedKey ?: return
                val fixTime = location.time
                try {
                    snapshotDb.recordRoutePointForVehicle(
                        vehicleKey, fixTime, location.latitude, location.longitude
                    )
                    lastAcceptedLocationTime = fixTime
                    if (isGps) lastAcceptedGpsAt = now
                    getSharedPreferences("altrozdrive_prefs", MODE_PRIVATE).edit()
                        .putString("last_route_lat", location.latitude.toString())
                        .putString("last_route_lon", location.longitude.toString())
                        .putLong("last_route_location_at", fixTime)
                        .apply()
                } catch (t: Throwable) {
                    android.util.Log.e("AltrozDriveRoute", "route point save failed", t)
                }
            }
        }
        try {
            // Keep both providers registered for fallback, but onLocationChanged filters
            // stale/inaccurate fixes and suppresses network positions while GPS is fresh.
            // This avoids alternating two coordinate sources on one polyline.
            var registered = false
            val providers = lm.getProviders(true)
            if (LocationManager.GPS_PROVIDER in providers) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 5f, listener)
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let(listener::onLocationChanged)
                registered = true
            }
            if (LocationManager.NETWORK_PROVIDER in providers) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 5f, listener)
                lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let(listener::onLocationChanged)
                registered = true
            }
            if (registered) {
                locationListener = listener
                android.util.Log.i("AltrozDriveRoute", "route recording started")
            }
        } catch (_: SecurityException) {
            // Permission may have changed between the check and registration.
        } catch (_: IllegalArgumentException) {
            // A provider can disappear while the system location setting changes.
        }
    }

    private fun stopRouteRecording() {
        val listener = locationListener ?: return
        try { locationManager?.removeUpdates(listener) } catch (_: SecurityException) { }
        locationListener = null
    }

    override fun onDestroy() {
        stopRouteRecording()
        try { snapshotDb.close() } catch (_: Exception) { }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun Location.isUsable(now: Long): Boolean {
        if (!latitude.isFinite() || !longitude.isFinite() ||
            latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
        // Last-known fixes can be hours old. Never stamp them with the callback time,
        // otherwise old coordinates become apparent teleportation points in a live route.
        if (time <= 0L || now - time > MAX_FIX_AGE_MS || time - now > MAX_FUTURE_FIX_SKEW_MS) return false
        if (!accuracy.isFinite() || accuracy <= 0f) return false
        val isGps = provider == LocationManager.GPS_PROVIDER
        val maxAccuracy = if (isGps) MAX_GPS_ACCURACY_METERS else MAX_NETWORK_ACCURACY_METERS
        if (accuracy > maxAccuracy) return false
        return true
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(channelId, "Vehicle connection", NotificationManager.IMPORTANCE_LOW))
        }
    }
}
