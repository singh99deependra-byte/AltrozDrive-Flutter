package com.altrozdrive.app

import android.app.Activity
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.graphics.Bitmap
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.key
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GasMeter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.math.absoluteValue
import kotlin.math.sin

internal fun dailyDateLabel(dayStart: Long): String =
    SimpleDateFormat("dd MMM", Locale.US).format(Date(dayStart))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.post {
            // Follow the display mode selected by Android. Compose/WebView are then
            // scheduled against the same display vsync (60/90/120 Hz as available).
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val refreshRate = display?.refreshRate ?: return@post
                window.attributes = window.attributes.apply { preferredRefreshRate = refreshRate }
            }
        }
        setContent { AltrozDriveApp() }
    }
}

internal object AltrozUiTokens {
    val pageHorizontal = 16.dp
    val pageVertical = 10.dp
    val pageSpacing = 9.dp
    val cardRadius = 18.dp
    val heroRadius = 25.dp
    val smallRadius = 16.dp
    val touchTarget = 44.dp
    val pressScale = 0.98f
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AltrozDriveApp() {
    var showLaunch by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        // Keep the branded launch screen visible long enough to feel intentional,
        // while keeping startup below two seconds. Bluetooth initialization may
        // continue behind it so the dashboard is ready when the animation ends.
        delay(900L)
        showLaunch = false
    }
    if (showLaunch) {
        AltrozDriveLaunchScreen()
        return
    }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    LaunchedEffect(Unit) { VehicleReminderScheduler.ensureChannel(context); VehicleReminderScheduler.scheduleAll(context) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var settings by remember { mutableStateOf(loadSettings(prefs)) }
    var vehicle by remember { mutableStateOf(loadVehicleProfile(prefs)) }
    var detailOriginTab by rememberSaveable { mutableIntStateOf(0) }
    val bluetoothManager = remember { BluetoothDiagnosticManager.getInstance(context.applicationContext) }
    var bluetoothState by remember { mutableStateOf(bluetoothManager.currentState()) }
    var gpsSpeedKph by remember { mutableFloatStateOf(0f) }
    var latestLocation by remember { mutableStateOf<Location?>(null) }
    var routeRecordingEnabled by remember { mutableStateOf(prefs.getBoolean("route_recording_enabled", false)) }
    val snapshotDb = remember { VehicleSnapshotDb(context.applicationContext) }
    DisposableEffect(snapshotDb) { onDispose { try { snapshotDb.close() } catch (_: Exception) { } } }
    var locationPermissionGranted by remember {
        mutableStateOf(hasAnyLocationPermission(context))
    }
    // GPS is a fallback for live speed only. We never request location permission
    // here, so the existing Bluetooth/startup permission flow remains unchanged.
    // If location permission was already granted, GPS speed can populate the
    // existing speed UI when no validated OEM speed signal is available.
    DisposableEffect(context, locationPermissionGranted) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val kph = if (location.hasSpeed()) location.speed * 3.6f else 0f
                if (kph.isFinite() && kph >= 0f && kph <= 300f) gpsSpeedKph = kph
                latestLocation = location

                // VehicleConnectionService is the single owner of route persistence.
                // Activity keeps only the latest location/speed for the visible UI,
                // avoiding a second GPS->SQLite pipeline while the app is foregrounded.
            }
        }
        val granted = hasAnyLocationPermission(context)
        if (locationManager != null && granted) {
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 5000L, 10f, listener
                    )
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let(listener::onLocationChanged)
                }
            } catch (_: SecurityException) {
                // Permission may have been revoked between the check and registration.
            }
        }
        onDispose {
            try { locationManager?.removeUpdates(listener) } catch (_: SecurityException) { }
        }
    }
    var cachedSnapshot by remember { mutableStateOf<VehicleSnapshotDb.Snapshot?>(null) }
    var telemetrySamples by remember { mutableStateOf<List<VehicleSnapshotDb.TelemetrySample>>(emptyList()) }
    var tripRecords by remember { mutableStateOf<List<VehicleSnapshotDb.TripRecord>>(emptyList()) }
    var historyLoaded by remember { mutableStateOf(false) }

    // DB is the offline source of truth. Keep the high-frequency refresh limited to
    // the small last-known snapshot. Historical telemetry/trips are refreshed less
    // often and with bounded result sets; this avoids repeatedly allocating thousands
    // of model objects and recomposing the entire dashboard every two seconds.
    LaunchedEffect(Unit) {
        while (true) {
            val snapshot = withContext(Dispatchers.IO) {
                snapshotDb.ensureSeeded()
                snapshotDb.loadMostRecent()
            }
            cachedSnapshot = snapshot
            delay(2000L)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val historical = withContext(Dispatchers.IO) {
                    val samples = snapshotDb.recentTelemetryDailyAny(
                        System.currentTimeMillis() - 180L * 24L * 60L * 60L * 1000L, 366
                    )
                    // The UI only needs a bounded recent window. History pages already
                    // cap their displayed groups; do not materialize the entire DB.
                    val trips = snapshotDb.recentTripsAny(2000)
                    samples to trips
                }
                telemetrySamples = historical.first
                tripRecords = historical.second
                historyLoaded = true
            } catch (_: Throwable) {
                // Keep the last successful history in place. On the very first failure,
                // historyLoaded remains false so data screens can show LOADING rather
                // than incorrectly claiming that the database is empty.
            }
            delay(10_000L)
        }
    }

    // LIVE means "a validated VIS frame arrived in the last few seconds", not merely
    // "the HTS socket exists". Tick once a second so the label drops to OFFLINE by itself.
    var liveFresh by remember { mutableStateOf(false) }
    LaunchedEffect(bluetoothState.htsConnected, bluetoothState.liveVisValidated, bluetoothState.liveDataAtMs) {
        if (!bluetoothState.htsConnected || !bluetoothState.liveVisValidated || bluetoothState.liveDataAtMs <= 0L) {
            liveFresh = false
            return@LaunchedEffect
        }
        liveFresh = true
        while (true) {
            val remaining = 5_000L - (System.currentTimeMillis() - bluetoothState.liveDataAtMs)
            if (remaining <= 0L) {
                liveFresh = false
                break
            }
            delay(remaining.coerceAtMost(1_000L))
        }
    }
    val useLiveVis = liveFresh
    val db = cachedSnapshot
    val decodedState = VehicleState(
        // Live X451 wins only when it is actually validated. Otherwise every VIS
        // field comes directly from the persistent SQLite snapshot.
        odo = if (useLiveVis) (bluetoothState.liveOdo ?: db?.odo ?: 0) else (db?.odo ?: bluetoothState.cachedOdo ?: 0),
        fuel = if (useLiveVis) (bluetoothState.liveFuelPercent ?: db?.fuelPercent ?: 0).toFloat() else (db?.fuelPercent ?: bluetoothState.cachedFuelPercent ?: 0).toFloat(),
        dte = if (useLiveVis) (bluetoothState.liveDteKm ?: db?.dteKm ?: 0) else (db?.dteKm ?: bluetoothState.cachedDteKm ?: 0),
        speed = if (useLiveVis) (bluetoothState.liveSpeedKph?.toFloat() ?: gpsSpeedKph) else 0f,
        rpm = bluetoothState.liveRpm ?: 0,
        battery = (bluetoothState.liveBatteryVoltage ?: 0.0).toFloat(),
        coolant = bluetoothState.liveCoolantTempC ?: 0,
        ignition = bluetoothState.liveIgnitionOn ?: false,
        connected = liveFresh || bluetoothState.htsConnected || bluetoothState.selectedDevice?.connected == true,
        dataLive = liveFresh,
        connectionStatus = when {
            liveFresh -> "LIVE"
            bluetoothState.htsConnected -> "CONNECTED • WAITING FOR VEHICLE DATA"
            bluetoothState.cnInternalRecovery -> "CONNECTED • RECOVERING CONNECTNEXT"
            bluetoothState.status.contains("reconnect", ignoreCase = true) -> "RECONNECTING"
            bluetoothState.selectedDevice?.connected == true -> "CONNECTING • CONNECTNEXT SESSION STARTING"
            else -> "OFFLINE"
        },
        tripA = if (useLiveVis) (bluetoothState.liveTripA ?: db?.tripA ?: 0f) else (db?.tripA ?: bluetoothState.cachedTripA ?: 0f),
        tripB = if (useLiveVis) (bluetoothState.liveTripB ?: db?.tripB ?: 0f) else (db?.tripB ?: bluetoothState.cachedTripB ?: 0f),
        avg = if (useLiveVis) (bluetoothState.liveAvgFuelEcoTripA ?: db?.avgA ?: 0f) else (db?.avgA ?: bluetoothState.cachedAvgFuelEcoTripA ?: 0f),
        avgB = if (useLiveVis) (bluetoothState.liveAvgFuelEcoTripB ?: db?.avgB ?: 0f) else (db?.avgB ?: bluetoothState.cachedAvgFuelEcoTripB ?: 0f)
    )
    var showFirstLaunchSetup by remember {
        mutableStateOf(!prefs.getBoolean(KEY_FIRST_LAUNCH_SETUP_COMPLETE, false))
    }
    var setupStep by remember { mutableIntStateOf(0) }
    fun finishFirstLaunchSetup() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_SETUP_COMPLETE, true).apply()
        showFirstLaunchSetup = false
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_PROMPTED, true).apply()
        if (showFirstLaunchSetup && setupStep == 3) {
            finishFirstLaunchSetup()
        }
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        bluetoothManager.refresh()
        setupStep = 1
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationPermissionGranted = granted
        if (granted) {
            routeRecordingEnabled = true
            prefs.edit().putBoolean("route_recording_enabled", true).apply()
            try {
                val routeIntent = Intent(context, VehicleConnectionService::class.java).apply {
                    action = VehicleConnectionService.ACTION_ROUTE_RECORDING_CHANGED
                    putExtra(VehicleConnectionService.EXTRA_ROUTE_RECORDING_ENABLED, true)
                }
                ContextCompat.startForegroundService(context, routeIntent)
            } catch (_: Exception) { }
        }
    }
    DisposableEffect(bluetoothManager) {
        bluetoothManager.setListeners(
            onState = { next -> bluetoothState = next }
        )
        // VehicleConnectionService owns the initial refresh/auto-connect lifecycle.
        // Keeping Activity from refreshing here prevents a second ConnectNext attempt.
        if (bluetoothManager.hasRequiredPermissions()) {
            try {
                val intent = Intent(context, VehicleConnectionService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) { }
        }
        // Detach only the Activity callback. The singleton manager, Bluetooth
        // receiver, sockets and vehicle service remain alive exactly as before.
        onDispose { bluetoothManager.clearStateListener() }
    }
    var media by remember { mutableStateOf(MediaState()) }
    // Automatic appearance follows the Android system theme.
    // Do not use the ambient light sensor: cabin/room lighting must not
    // cause the dashboard to flip between light and dark.
    val systemDark = isSystemInDarkTheme()
    val effectiveAppearance = if (settings.appearance == "auto") {
        if (systemDark) "graphite" else "daylight"
    } else settings.appearance
    val theme = themePalette(effectiveAppearance)
    val dark = theme.dark

    // Theme/accent changes are deliberately animated. The target palette is still
    // selected synchronously, but every visible color interpolates over 300 ms so
    // switching appearance or accent never produces an instant visual jump.
    val animatedBackground = AltrozAnimatedColor(theme.background, "theme_background")
    val animatedSurface = AltrozAnimatedColor(theme.surface, "theme_surface")
    val animatedSurfaceVariant = AltrozAnimatedColor(theme.surfaceVariant, "theme_surface_variant")
    val animatedOnBackground = AltrozAnimatedColor(theme.onBackground, "theme_on_background")
    val animatedOnSurface = AltrozAnimatedColor(theme.onSurface, "theme_on_surface")
    val animatedOnSurfaceVariant = AltrozAnimatedColor(theme.onSurfaceVariant, "theme_on_surface_variant")
    val animatedSecondary = AltrozAnimatedColor(theme.secondary, "theme_secondary")
    val animatedTertiary = AltrozAnimatedColor(theme.tertiary, "theme_tertiary")
    val animatedAccent = AltrozAnimatedColor(accentColor(settings.accent), "theme_accent")
    val colors = if (dark) {
        darkColorScheme(
            primary = animatedAccent,
            onPrimary = accentOnPrimary(animatedAccent),
            secondary = animatedSecondary,
            tertiary = animatedTertiary,
            background = animatedBackground,
            surface = animatedSurface,
            surfaceVariant = animatedSurfaceVariant,
            onBackground = animatedOnBackground,
            onSurface = animatedOnSurface,
            onSurfaceVariant = animatedOnSurfaceVariant
        )
    } else {
        lightColorScheme(
            primary = animatedAccent,
            onPrimary = accentOnPrimary(animatedAccent),
            secondary = animatedSecondary,
            tertiary = animatedTertiary,
            background = animatedBackground,
            surface = animatedSurface,
            surfaceVariant = animatedSurfaceVariant,
            onBackground = animatedOnBackground,
            onSurface = animatedOnSurface,
            onSurfaceVariant = animatedOnSurfaceVariant
        )
    }

    LaunchedEffect(Unit) {
        // Active Android media sessions can change without AltrozDrive being resumed.
        // Polling also handles players such as YouTube/Spotify that replace their session
        // when a track changes. The notification-listener permission is required.
        while (true) {
            media = readMediaState(context)
            delay(2000L)
        }
    }
    val view = LocalView.current
    SideEffect {
        applySystemBarColors((view.context as Activity).window, animatedBackground.toArgb(), dark)
    }
    fun save(next: AppSettings) {
        settings = next
        prefs.edit()
            .putString(KEY_APPEARANCE, next.appearance)
            .putString(KEY_DISTANCE, next.distance)
            .putString(KEY_ECONOMY, next.economy)
            .putString(KEY_TEMP, next.temperature)
            .putString(KEY_ACCENT, next.accent)
            .apply()
    }

    LaunchedEffect(showFirstLaunchSetup, setupStep) {
        if (!showFirstLaunchSetup) return@LaunchedEffect
        if (setupStep == 1) {
            while (showFirstLaunchSetup && !isMediaAccessGranted(context)) delay(500L)
            if (showFirstLaunchSetup && isMediaAccessGranted(context)) setupStep = 2
        }
    }

    LaunchedEffect(showFirstLaunchSetup) {
        if (showFirstLaunchSetup) return@LaunchedEffect
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_PROMPTED, false)
        ) {
            prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_PROMPTED, true).apply()
            delay(350L)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    MaterialTheme(colorScheme = colors) {
        if (showFirstLaunchSetup) {
            FirstLaunchSetup(
                step = setupStep,
                bluetoothReady = bluetoothManager.hasRequiredPermissions(),
                musicReady = isMediaAccessGranted(context),
                locationReady = hasAnyLocationPermission(context),
                notificationReady = notificationPermissionGranted(context),
                onContinue = {
                    when (setupStep) {
                        0 -> if (bluetoothManager.hasRequiredPermissions()) setupStep = 1
                             else bluetoothPermissionLauncher.launch(bluetoothManager.requiredPermissions())
                        1 -> if (isMediaAccessGranted(context)) setupStep = 2
                             else openMediaAccess(context)
                        2 -> if (hasAnyLocationPermission(context)) setupStep = 3
                             else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        3 -> if (notificationPermissionGranted(context)) finishFirstLaunchSetup()
                             else if (android.os.Build.VERSION.SDK_INT >= 33) {
                                 prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_PROMPTED, true).apply()
                                 notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                             } else finishFirstLaunchSetup()
                        else -> finishFirstLaunchSetup()
                    }
                },
            )
            return@MaterialTheme
        }

        val pagerState = rememberPagerState(initialPage = tab, pageCount = { 5 })
        val pagerScope = rememberCoroutineScope()

        // Navigation is intentionally single-source-of-truth: the pager owns the
        // top-level tab, while `detail` owns the More/detail stack. This prevents
        // Android back gestures from changing tabs while a detail screen is open.
        LaunchedEffect(pagerState.currentPage) {
            tab = pagerState.currentPage
        }

        fun goToTab(target: Int, clearDetail: Boolean = true) {
            if (clearDetail) detail = null
            tab = target
            if (target != pagerState.currentPage) {
                pagerScope.launch {
                    pagerState.animateScrollToPage(
                        page = target,
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    )
                }
            }
        }

        val detailRoutes = remember { setOf(
            "profile", "media", "data", "settings", "about", "navigation", "reminders", "driving_insights", "fuel_intelligence", "vehicle_trends", "since_last_drive"
        ) }

        fun openDetail(page: String, origin: Int = pagerState.currentPage) {
            // Every More/quick-action destination must resolve to a real detail route.
            // Unknown routes are rejected instead of silently falling through to Settings.
            if (page !in detailRoutes) return
            // Detail is a real, top-level navigation layer. Set the route first,
            // then move the pager only when the detail originated outside More.
            // This avoids the old More -> detail -> pager race that could leave
            // Settings/Music/Navigation visually stuck on the More screen.
            detailOriginTab = origin.coerceIn(0, 4)
            detail = page
            if (origin.coerceIn(0, 4) != 4 && pagerState.currentPage != 4) {
                pagerScope.launch {
                    pagerState.animateScrollToPage(
                        page = 4,
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    )
                }
            }
        }

        fun closeDetail() {
            val origin = detailOriginTab.coerceIn(0, 4)
            detail = null
            if (origin != pagerState.currentPage) goToTab(origin, clearDetail = false)
        }

        // Android system back, including edge-swipe back/predictive-back on
        // supported devices, follows a normal navigation stack:
        // Detail -> its origin tab -> Home -> system exit.
        BackHandler(enabled = detail != null || pagerState.currentPage != 0) {
            if (detail != null) closeDetail() else goToTab(0)
        }

        Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = { BottomBar(if (detail != null) detailOriginTab.coerceIn(0, 4) else pagerState.currentPage) { target ->
            goToTab(target)
        } }) { padding ->
            if (detail != null) {
                // Detail pages are a real navigation layer, not another state hidden
                // inside the More tab. This prevents More/Settings/Music/Navigation
                // from appearing behind a detail page or being revisited accidentally.
                Box(Modifier.fillMaxSize().padding(padding)) {
                    key(detail) {
                    Detail(
                        page = detail!!,
                        settings = settings,
                        vehicle = vehicle,
                        media = media,
                        bluetoothState = bluetoothState,
                        tripRecords = tripRecords,
                        cachedSnapshot = cachedSnapshot,
                        telemetrySamples = telemetrySamples,
                        routeRecordingEnabled = routeRecordingEnabled,
                        locationPermissionGranted = locationPermissionGranted,
                        latestLocation = latestLocation,
                        historyLoaded = historyLoaded,
                        onRouteRecordingChange = { enabled ->
                            routeRecordingEnabled = enabled
                            prefs.edit().putBoolean("route_recording_enabled", enabled).apply()
                            try {
                                val routeIntent = Intent(context, VehicleConnectionService::class.java).apply {
                                    action = VehicleConnectionService.ACTION_ROUTE_RECORDING_CHANGED
                                    putExtra(VehicleConnectionService.EXTRA_ROUTE_RECORDING_ENABLED, enabled)
                                }
                                ContextCompat.startForegroundService(context, routeIntent)
                            } catch (_: Exception) { }
                        },
                        onRequestLocationPermission = { locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                        onSettings = ::save,
                        onVehicle = { next ->
                            vehicle = next
                            prefs.edit()
                                .putString(KEY_VEHICLE_MODEL, next.model)
                                .putString(KEY_VEHICLE_TRIM, next.trim)
                                .putString(KEY_VEHICLE_FUEL, next.fuel)
                                .putString(KEY_VEHICLE_TRANSMISSION, next.transmission)
                                .apply()
                        },
                        back = ::closeDetail
                    )
                    }
                    }
            } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(padding),
                userScrollEnabled = true,
                key = { it }
            ) { page ->
                // Subtle depth/fade during the horizontal transition keeps the motion
                // premium without making a 120 Hz swipe feel heavy.
                val pageDistance = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).absoluteValue
                val pageProgress = pageDistance.coerceIn(0f, 1f)
                val pageScale = 1f - (pageProgress * 0.012f)
                val pageAlpha = 1f - (pageProgress * 0.08f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = pageAlpha
                            scaleX = pageScale
                            scaleY = pageScale
                        }
                ) {
                when (page) {
                    0 -> HomeScreen(decodedState, settings, media, vehicle, cachedSnapshot?.updatedAt ?: 0L, tripRecords, historyLoaded) { action -> when(action){
                        "Trips"->{goToTab(2)};
                        "Fuel"->{goToTab(3)};
                        "Navigate"->{openDetail("navigation", origin = 0)};
                        "Music"->{openDetail("media", origin = 0)}
                    } }
                    1 -> DriveScreen(decodedState, settings, tripRecords, historyLoaded) { action -> when(action){
                        "Navigate"->{openDetail("navigation", origin = 1)};
                        "Music"->{openDetail("media", origin = 1)}
                    } }
                    2 -> TripScreen(settings, tripRecords, decodedState.dataLive, historyLoaded)
                    3 -> FuelScreen(decodedState, settings, telemetrySamples, historyLoaded)
                    else -> More(
                        vehicle = vehicle,
                        bluetoothState = bluetoothState,
                        onDetail = { openDetail(it, origin = 4) }
                    )
                }
                }
            }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun applySystemBarColors(window: android.view.Window, color: Int, dark: Boolean) {
    window.statusBarColor = color
    window.navigationBarColor = color
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.isAppearanceLightStatusBars = !dark
    controller.isAppearanceLightNavigationBars = !dark
}

@Composable
private fun FirstLaunchSetup(
    step: Int,
    bluetoothReady: Boolean,
    musicReady: Boolean,
    locationReady: Boolean,
    notificationReady: Boolean,
    onContinue: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val items = listOf(
        Triple("Bluetooth", "Connect to ConnectNext and receive vehicle data", bluetoothReady),
        Triple("Music", "Control and display your phone's active media session", musicReady),
        Triple("Location", "Allow GPS access so trips and routes can be recorded", locationReady),
        Triple("Notifications", "Allow expiry reminders to appear in the Android notification panel", notificationReady)
    )
    val current = items[step.coerceIn(0, items.lastIndex)]
    Surface(Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(horizontal=24.dp,vertical=32.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Text("ALTROZDRIVE",fontSize=12.sp,fontWeight=FontWeight.Bold,color=accent,letterSpacing=2.sp)
                Text("Let's get your drive ready",fontSize=30.sp,fontWeight=FontWeight.Bold)
                Text("A one-time setup for the features you already use. Permissions are requested here, not from the More menu.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=13.sp,lineHeight=19.sp)
            }
            Surface(Modifier.fillMaxWidth(),RoundedCornerShape(28.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Surface(Modifier.size(56.dp),RoundedCornerShape(18.dp),color=accent.copy(alpha=.12f)){Box(contentAlignment=Alignment.Center){Icon(if(step==0)Icons.Default.Bluetooth else if(step==1)Icons.Default.MusicNote else Icons.Default.Navigation,null,tint=accent,modifier=Modifier.size(28.dp))}};Text(current.first,fontSize=22.sp,fontWeight=FontWeight.Bold);Text(current.second,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=13.sp,lineHeight=19.sp);Text("Step ${step+1} of ${items.size}",fontSize=10.sp,fontWeight=FontWeight.Bold,color=accent,letterSpacing=1.sp)}}
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Button(onClick=onContinue,modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp)){Text(if(current.third)"Continue" else "Allow access",fontWeight=FontWeight.Bold)};Text("You can change permissions later in Android Settings.",modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }
    }
}

@Composable
private fun AboutDetail(){
    // Detail() already owns the single scrolling Page container. Keeping About as
    // plain content here avoids nesting a second LazyColumn and the Android 16
    // infinite-height crash when the page is opened.
    Column(Modifier.fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Surface(Modifier.fillMaxWidth(),RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface){
            Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)){
                Surface(Modifier.size(58.dp),CircleShape,color=MaterialTheme.colorScheme.primary.copy(alpha=.12f)){Box(contentAlignment=Alignment.Center){Icon(Icons.Default.DirectionsCar,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(30.dp))}}
                Text("AltrozDrive",fontSize=22.sp,fontWeight=FontWeight.Bold)
                Text("Connected vehicle companion",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Version 1.1.18",fontSize=10.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
            }
        }
        Surface(Modifier.fillMaxWidth(),RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){
            Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                Text("Verified vehicle data",fontSize=14.sp,fontWeight=FontWeight.Bold)
                Text("ODO • Fuel • Range • Trip A • Trip B • Average Economy • RPM",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,lineHeight=17.sp)
                Text("Trip and route history are shown only when persisted data exists.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,lineHeight=15.sp)
            }
        }
        Surface(Modifier.fillMaxWidth(),RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){
            Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Text("Design & data",fontSize=14.sp,fontWeight=FontWeight.Bold)
                Text("Themes and accent colors are supported without changing the vehicle connection or decoder layer.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,lineHeight=15.sp)
            }
        }
    }
}

fun loadVehicleProfile(prefs: android.content.SharedPreferences) = VehicleProfile(
    prefs.getString(KEY_VEHICLE_MODEL,"Altroz") ?: "Altroz",
    prefs.getString(KEY_VEHICLE_TRIM,"XZA+S") ?: "XZA+S",
    prefs.getString(KEY_VEHICLE_FUEL,"Petrol") ?: "Petrol",
    prefs.getString(KEY_VEHICLE_TRANSMISSION,"DCA") ?: "DCA"
)

fun loadSettings(prefs: android.content.SharedPreferences): AppSettings {
    val storedTheme = prefs.getString(KEY_APPEARANCE, "daylight") ?: "daylight"
    val theme = when (storedTheme) {
        "auto", "amoled", "daylight", "pearl", "sandstone", "graphite", "obsidian", "titanium", "carbon" -> storedTheme
        // Migrate themes removed from the selector to visually related production themes.
        "midnight" -> "graphite"
        "arctic" -> "pearl"
        "light", "system" -> "daylight"
        "dark", "ocean", "forest", "sunset", "lavender", "rose" -> "graphite"
        else -> "auto"
    }
    val storedAccent = prefs.getString(KEY_ACCENT, "gold") ?: "gold"
    val accent = when (storedAccent) {
        "gold", "cyan", "emerald", "amber", "turquoise", "jade", "olive_gold" -> storedAccent
        // Migrate removed accent names without changing any other setting.
        "violet", "purple", "magenta", "pink" -> "jade"
        "blue" -> "cyan"
        "orange", "red" -> "amber"
        "ice" -> "turquoise"
        "green", "teal", "lime" -> "emerald"
        else -> "cyan"
    }
    return AppSettings(
        theme,
        prefs.getString(KEY_DISTANCE,"km") ?: "km",
        prefs.getString(KEY_ECONOMY,"km/l") ?: "km/l",
        prefs.getString(KEY_TEMP,"°C") ?: "°C",
        accent
    )
}
data class ThemePalette(
    val dark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val secondary: Color,
    val tertiary: Color
)

private val ThemeTextDark = Color(0xFFF5F7FA)

fun accentColor(a:String)=when(a){
    "gold"->Color(0xFFF2C14E)
    "cyan"->Color(0xFF22D3EE)
    "emerald"->Color(0xFF10B981)
    "amber"->Color(0xFFD99A2B)
    "turquoise"->Color(0xFF20A89A)
    "jade"->Color(0xFF3A9B72)
    "olive_gold"->Color(0xFF9A8B32)
    else->Color(0xFF22D3EE)
}

@Composable
internal fun navigationAccent(): Color {
    // Navigation follows the user-selected accent in BOTH light and dark themes.
    // Do not force dark themes to blue; the selected accent is already supplied
    // to MaterialTheme.colorScheme.primary by AltrozDriveApp().
    return MaterialTheme.colorScheme.primary
}

private fun accentOnPrimary(accent: Color): Color =
    if (accent.luminance() > 0.48f) Color(0xFF101317) else Color.White

fun themePalette(name:String):ThemePalette = when(name) {
    // Auto follows the Android system. These are only the palette definitions;
    // no connection, decoder, database, navigation or telemetry behavior depends on them.
    "auto" -> ThemePalette(true, Color.Black, Color(0xFF090A0D), Color(0xFF181A20), ThemeTextDark, ThemeTextDark, Color(0xFFB7BEC9), Gold, Gold)
    "amoled" -> ThemePalette(true, Color.Black, Color(0xFF090A0D), Color(0xFF181A20), ThemeTextDark, ThemeTextDark, Color(0xFFB7BEC9), Gold, Gold)
    "daylight" -> ThemePalette(false, Color(0xFFF7F4EE), Color.White, Color(0xFFEAE5DC), Color(0xFF181817), Color(0xFF181817), Color(0xFF5B5A55), Gold, Gold)
    "pearl" -> ThemePalette(false, Color(0xFFF3F5F7), Color.White, Color(0xFFE2E7EB), Color(0xFF172027), Color(0xFF172027), Color(0xFF5C6871), Color(0xFF91A1AF), Color(0xFFB8C4CE))
    "sandstone" -> ThemePalette(false, Color(0xFFF5EDE1), Color(0xFFFFFAF2), Color(0xFFE8DCCB), Color(0xFF261F18), Color(0xFF261F18), Color(0xFF6D6257), Color(0xFFC49A67), Color(0xFFD8B17B))
    "graphite" -> ThemePalette(true, Color(0xFF0D0F12), Color(0xFF191C21), Color(0xFF282C33), ThemeTextDark, ThemeTextDark, Color(0xFFB8BEC8), Color(0xFFD5D8DE), Color(0xFF8E97A5))
    "obsidian" -> ThemePalette(true, Color(0xFF07100F), Color(0xFF0C1917), Color(0xFF15302C), ThemeTextDark, ThemeTextDark, Color(0xFFA9BBB7), Color(0xFF1D9C88), Color(0xFF43D6BD))
    "titanium" -> ThemePalette(false, Color(0xFFE9EDF0), Color(0xFFF7F8F9), Color(0xFFD9E0E5), Color(0xFF172027), Color(0xFF172027), Color(0xFF5B6670), Color(0xFF7C8B96), Color(0xFFB3BEC6))
    "carbon" -> ThemePalette(true, Color(0xFF17110B), Color(0xFF24170D), Color(0xFF3A2616), ThemeTextDark, ThemeTextDark, Color(0xFFC4B6A7), Color(0xFFC56A25), Color(0xFFF0A04A))
    else -> ThemePalette(false, Color(0xFFF7F4EE), Color.White, Color(0xFFEAE5DC), Color(0xFF181817), Color(0xFF181817), Color(0xFF5B5A55), Gold, Gold)
}
fun distanceValue(km:Float,s:AppSettings,decimals:Int=1):String {
    val safeKm = if (km.isFinite()) km else 0f
    val value = if (s.distance == "mi") safeKm * 0.621371f else safeKm
    val unit = if (s.distance == "mi") "mi" else "km"
    return if (decimals <= 0) {
        "${value.roundToInt()}\u00A0$unit"
    } else {
        String.format(Locale.US, "%.${decimals}f\u00A0%s", value, unit)
    }
}
fun speedValue(kmh:Float,s:AppSettings)="${(if(s.distance=="mi")kmh*.621371f else kmh).roundToInt()}\u00A0${if(s.distance=="mi")"mph" else "km/h"}"
fun economyValue(k:Float,s:AppSettings):String {
    val safe = k.coerceAtLeast(0.1f)
    return when (s.economy) {
        "l/100 km" -> String.format(Locale.US, "%.1f\u00A0L/100\u00A0km", 100f / safe)
        "mpg" -> String.format(Locale.US, "%.1f\u00A0mpg", safe * 2.35215f)
        else -> String.format(Locale.US, "%.1f\u00A0km/L", safe)
    }
}

fun averageTripSpeed(distanceKm: Float, durationSec: Long, settings: AppSettings): String {
    if (distanceKm <= 0f || durationSec <= 0L) return "—"
    val kmh = distanceKm / (durationSec / 3600f)
    return speedValue(kmh, settings)
}

fun formatTripDuration(seconds: Long): String {
    val m = (seconds.coerceAtLeast(0L) / 60L)
    val h = m / 60L
    val min = m % 60L
    return if (h > 0) "${h}h ${min}m" else "${min}m"
}

fun formatDisplayedTripDuration(trips: List<VehicleSnapshotDb.TripRecord>): String {
    // Match the per-trip display, which intentionally uses whole elapsed minutes.
    val displayedMinutes = trips.sumOf { it.durationSec.coerceAtLeast(0L) / 60L }
    val hours = displayedMinutes / 60L
    val minutes = displayedMinutes % 60L
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

internal fun stableValueSize(value: String, base: Float = 16f): androidx.compose.ui.unit.TextUnit = when {
    value.length <= 7 -> base.sp
    value.length <= 9 -> (base - 1f).sp
    value.length <= 11 -> (base - 2f).sp
    value.length <= 13 -> (base - 3f).sp
    else -> (base - 4f).coerceAtLeast(10f).sp
}

internal fun stableLargeValueSize(value: String, base: Float = 20f): androidx.compose.ui.unit.TextUnit = when {
    value.length <= 7 -> base.sp
    value.length <= 9 -> (base - 1.5f).sp
    value.length <= 11 -> (base - 3f).sp
    value.length <= 13 -> (base - 4f).sp
    else -> (base - 5f).coerceAtLeast(11f).sp
}

@Composable
fun BottomBar(selected:Int,onSelect:(Int)->Unit){
    val items=listOf(Icons.Default.Home to "Home",Icons.Default.Speed to "Drive",Icons.Default.Route to "Trip",Icons.Default.GasMeter to "Fuel",Icons.Default.MoreHoriz to "More")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val active = index == selected
                val pillWidth by animateDpAsState(
                    targetValue = if (active) 72.dp else 52.dp,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    label = "nav_pill_width_$index"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (active) 1.06f else 1f,
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    label = "nav_icon_scale_$index"
                )
                Column(
                    Modifier.weight(1f).heightIn(min = AltrozUiTokens.touchTarget).clickable { if (!active) onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Surface(
                        Modifier.height(34.dp).width(pillWidth),
                        shape = RoundedCornerShape(18.dp),
                        color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                item.first,
                                item.second,
                                modifier = Modifier.size(20.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                                tint = if (active) accentOnPrimary(MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        item.second,
                        fontSize = 9.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AltrozDriveLaunchScreen() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80L)
        entered = true
    }

    val accent = MaterialTheme.colorScheme.primary
    // Splash/opening animation also follows the selected accent in dark themes.
    val launchAccent = accent
    val carAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "launchCarAlpha"
    )
    val carScale by animateFloatAsState(
        targetValue = if (entered) 1f else .90f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "launchCarScale"
    )
    val carOffset by animateFloatAsState(
        targetValue = if (entered) 0f else 34f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "launchCarOffset"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(420, delayMillis = 260, easing = FastOutSlowInEasing),
        label = "launchTitleAlpha"
    )
    val progressAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(350, delayMillis = 420),
        label = "launchProgressAlpha"
    )
    val arcSweep by animateFloatAsState(
        targetValue = if (entered) 230f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "launchArcSweep"
    )
    val loadProgress by animateFloatAsState(
        targetValue = if (entered) .82f else 0f,
        animationSpec = tween(900, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "launchLoadProgress"
    )
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progressAlpha * .34f }
                .background(
                    Brush.radialGradient(
                        colors = listOf(launchAccent.copy(alpha = .14f), Color.Transparent),
                        radius = 430f
                    )
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(horizontal = 22.dp)) {
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = progressAlpha }) {
                    val arcRect = androidx.compose.ui.geometry.Rect(
                        left = size.width * .17f,
                        top = size.height * .03f,
                        right = size.width * .83f,
                        bottom = size.height * .82f
                    )
                    drawArc(
                        color = launchAccent.copy(alpha = .42f),
                        startAngle = 206f,
                        sweepAngle = arcSweep,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(arcRect.left, arcRect.top),
                        size = androidx.compose.ui.geometry.Size(arcRect.width, arcRect.height),
                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Canvas(
                    Modifier
                        .fillMaxWidth(.72f)
                        .height(34.dp)
                        .align(Alignment.BottomCenter)
                        .graphicsLayer { alpha = carAlpha * .82f }
                ) {
                    drawOval(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                launchAccent.copy(alpha = .28f),
                                Color.Transparent
                            )
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * .32f),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height * .36f)
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.altroz_cutout),
                    contentDescription = "Tata Altroz",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(.92f)
                        .height(210.dp)
                        .graphicsLayer {
                            alpha = carAlpha
                            scaleX = carScale
                            scaleY = carScale
                            translationY = carOffset
                        }
                        .then(AltrozCarIdleModifier())
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { alpha = titleAlpha }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Altroz", fontSize = 31.sp, fontWeight = FontWeight.Bold)
                    Text("Drive", fontSize = 31.sp, fontWeight = FontWeight.Bold, color = launchAccent)
                }
                Spacer(Modifier.height(2.dp))
                Text("Your vehicle at a glance", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }

            Box(Modifier.width(138.dp).height(5.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.outline.copy(alpha = .18f)).graphicsLayer { alpha = progressAlpha }) {
                Box(Modifier.fillMaxWidth(loadProgress).fillMaxHeight().background(launchAccent, RoundedCornerShape(50)))
            }
        }
    }
}

@Composable
fun Page(title: String, subtitle: String? = null, showHeader: Boolean = true, content: @Composable () -> Unit) {
    // Use one ordinary scroll container for every page.  Detail screens can contain
    // arbitrary content without ever creating a LazyColumn-inside-LazyColumn measurement
    // chain.  This avoids the Android 16 infinite-height crash seen when opening About.
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AltrozUiTokens.pageHorizontal, vertical = AltrozUiTokens.pageVertical),
        verticalArrangement = Arrangement.spacedBy(AltrozUiTokens.pageSpacing)
    ) {
        if (showHeader) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontSize = 23.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.25).sp)
                if (!subtitle.isNullOrBlank()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        Column(AltrozRevealModifier("page_content_$title", 35).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
fun CardBox(content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AltrozUiTokens.cardRadius),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) { Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(AltrozUiTokens.pageSpacing)) { content() } }
}

@Composable
internal fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    val live = text.equals("LIVE", ignoreCase = true)
    Surface(modifier = modifier.heightIn(min = 34.dp), color = color.copy(alpha = .12f), shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            AltrozLiveDot(live, color, Modifier.size(7.dp))
            Spacer(Modifier.width(7.dp))
            Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = 12.sp)
        }
    }
}


@Composable
fun SummaryTile(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(modifier.heightIn(min = 92.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f), shape = RoundedCornerShape(AltrozUiTokens.smallRadius)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = if (icon == Icons.Default.Eco) LeafGreen else MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, lineHeight = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .25.sp, maxLines = 2, textAlign = TextAlign.Center)
            AltrozAnimatedText(value) { animatedValue ->
                Text(animatedValue, fontSize = stableLargeValueSize(animatedValue, 15.5f), lineHeight = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun EmptyDataState(text: String, icon: ImageVector, detail: String? = null) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .32f), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(34.dp), CircleShape, color = MaterialTheme.colorScheme.surface) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Muted, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                if (!detail.isNullOrBlank()) Text(detail, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
fun Detail(
    page: String,
    settings: AppSettings,
    vehicle: VehicleProfile,
    media: MediaState,
    bluetoothState: BluetoothDiagnosticState,
    tripRecords: List<VehicleSnapshotDb.TripRecord>,
    onSettings: (AppSettings) -> Unit,
    onVehicle: (VehicleProfile) -> Unit,
    cachedSnapshot: VehicleSnapshotDb.Snapshot?,
    telemetrySamples: List<VehicleSnapshotDb.TelemetrySample>,
    routeRecordingEnabled: Boolean,
    locationPermissionGranted: Boolean,
    latestLocation: Location?,
    historyLoaded: Boolean,
    onRouteRecordingChange: (Boolean) -> Unit,
    onRequestLocationPermission: () -> Unit,
    back: () -> Unit
) {
    var detailSwipeState by remember(page) { mutableFloatStateOf(0f) }
    val title = when (page) {
        "navigation" -> "Navigation"
        "media" -> "Music"
        "data" -> "Data & History"
        "profile" -> "Vehicle Profile"
        "settings" -> "Settings"
        "about" -> "About"
        "reminders" -> "Vehicle reminders"
        "driving_insights" -> "Driving insights"
        "fuel_intelligence" -> "Fuel intelligence"
        "vehicle_trends" -> "Vehicle trends"
        "since_last_drive" -> "Since last drive"
        else -> "More"
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(page) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount -> detailSwipeState += dragAmount },
                    onDragEnd = {
                        if (detailSwipeState > 150f) back()
                        detailSwipeState = 0f
                    },
                    onDragCancel = { detailSwipeState = 0f }
                )
            }
    ) {
        val featurePage = page in setOf(
            "reminders",
            "driving_insights",
            "fuel_intelligence",
            "vehicle_trends",
            "since_last_drive"
        )

        val detailContent: @Composable () -> Unit = {
            when (page) {
                "media" -> MediaDetail(media)
                "navigation" -> NavigationDetail()
                "data" -> DataHistoryDetail(
                    cachedSnapshot, tripRecords, telemetrySamples, settings, bluetoothState,
                    routeRecordingEnabled, locationPermissionGranted, latestLocation,
                    onRouteRecordingChange, onRequestLocationPermission, historyLoaded
                )
                "reminders" -> VehicleRemindersScreen(back)
                "driving_insights" -> DrivingInsightsScreen(tripRecords, settings, back, historyLoaded)
                "fuel_intelligence" -> FuelIntelligenceScreen(cachedSnapshot, tripRecords, settings, back, historyLoaded)
                "vehicle_trends" -> VehicleTrendsScreen(telemetrySamples, settings, back, historyLoaded)
                "since_last_drive" -> SinceLastDriveScreen(tripRecords, settings, back, historyLoaded)
                "profile" -> VehicleProfileDetail(vehicle, onVehicle)
                "settings" -> Settings(settings, onSettings)
                "about" -> AboutDetail()
            }
        }

        if (featurePage) {
            // Feature screens own the only vertical scroll container.
            detailContent()
        } else {
            Page(title) {
                FeatureBackButton(back)
                detailContent()
            }
        }
    }
}

private fun openMediaAccess(context: Context) {
    try {
        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    } catch (_: Exception) {
        // Settings activity may not be available on every OEM build.
    }
}

@Composable
fun MediaDetail(media: MediaState) {
    val context = LocalContext.current
    val accent = navigationAccent()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Box(Modifier.fillMaxWidth().height(390.dp)) {
                MediaArtwork(media.artwork, media.artworkUri, circular = false)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.18f), Color.Black.copy(alpha=.32f), Color.Black.copy(alpha=.92f)))))
                Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape=RoundedCornerShape(50), color=Color.Black.copy(alpha=.42f), border=androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha=.12f))) {
                            Row(Modifier.padding(horizontal=10.dp, vertical=6.dp), verticalAlignment=Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).background(if(media.playing) Green else Color.White.copy(alpha=.55f), CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Text(if(media.playing) "NOW PLAYING" else "PAUSED", color=Color.White, fontSize=9.sp, fontWeight=FontWeight.Bold, letterSpacing=1.sp)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text("MUSIC", color=Color.White.copy(alpha=.72f), fontSize=9.sp, fontWeight=FontWeight.Bold, letterSpacing=1.sp)
                    }
                    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text(media.title.ifBlank { "No active media" }, color=Color.White, fontSize=23.sp, fontWeight=FontWeight.Bold, maxLines=2, softWrap=true)
                        Text(media.artist.ifBlank { "Choose music on your phone" }, color=Color.White.copy(alpha=.78f), fontSize=13.sp, maxLines=2, softWrap=true)
                        if(media.album.isNotBlank()) Text(media.album, color=Color.White.copy(alpha=.58f), fontSize=11.sp, maxLines=2, softWrap=true)
                        MusicWaveform(media.positionMs, media.durationMs, media.playing, onSeek={ fraction ->
                            try {
                                if(media.durationMs > 0L) media.controller?.transportControls?.seekTo((media.durationMs * fraction).roundToInt().toLong())
                            } catch (_: Exception) { }
                        }, dark=true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.Center, verticalAlignment=Alignment.CenterVertically) {
                            MediaControlButton(Icons.Default.SkipPrevious, media.controller != null, 50.dp) { try { media.controller?.transportControls?.skipToPrevious() } catch (_: Exception) { } }
                            Spacer(Modifier.width(20.dp))
                            Surface(Modifier.size(66.dp).clickable(enabled=media.controller != null) { try { if(media.playing) media.controller?.transportControls?.pause() else media.controller?.transportControls?.play() } catch (_: Exception) { } }, shape=CircleShape, color=accent, shadowElevation=10.dp) {
                                Box(contentAlignment=Alignment.Center) { Icon(if(media.playing) Icons.Default.Pause else Icons.Default.PlayArrow, if(media.playing) "Pause" else "Play", tint=MaterialTheme.colorScheme.onPrimary, modifier=Modifier.size(31.dp)) }
                            }
                            Spacer(Modifier.width(20.dp))
                            MediaControlButton(Icons.Default.SkipNext, media.controller != null, 50.dp) { try { media.controller?.transportControls?.skipToNext() } catch (_: Exception) { } }
                        }
                    }
                }
            }
        }

        if(!media.accessGranted) {
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.32f), border=androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=.18f))) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment=Alignment.CenterVertically) {
                    Surface(Modifier.size(40.dp), CircleShape, color=accent.copy(alpha=.10f)) { Box(contentAlignment=Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint=accent, modifier=Modifier.size(20.dp)) } }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Music access", fontWeight=FontWeight.SemiBold, fontSize=14.sp)
                        Text("Allow notification-listener media access to control the active music app.", color=MaterialTheme.colorScheme.onSurfaceVariant, fontSize=12.sp, lineHeight=16.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick={openMediaAccess(context)}) { Text("Allow") }
                }
            }
        }
    }
}

@Composable
private fun MediaControlButton(icon: ImageVector, enabled: Boolean, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val description = when (icon) {
        Icons.Default.SkipPrevious -> "Previous track"
        Icons.Default.SkipNext -> "Next track"
        else -> "Media control"
    }
    Surface(
        Modifier.size(size).clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = Color.Black.copy(alpha = .34f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .16f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = if(enabled) Color.White else Color.White.copy(alpha=.35f), modifier=Modifier.size(22.dp))
        }
    }
}

@Composable
private fun MusicWaveform(positionMs: Long, durationMs: Long, playing: Boolean, onSeek: (Float) -> Unit = {}, dark: Boolean = false) {
    val accent=navigationAccent()
    val inactive=if(dark) Color.White.copy(alpha=.28f) else MaterialTheme.colorScheme.outline.copy(alpha=.30f)
    val active=if(dark) accent else accent
    val progress=if(durationMs>0L) (positionMs.toFloat()/durationMs.toFloat()).coerceIn(0f,1f) else 0f
    val seekCenterHole = if(dark) Color.Black.copy(alpha=.72f) else MaterialTheme.colorScheme.surface
    val barCount=48
    var waveformWidth by remember { mutableIntStateOf(0) }
    val phase = if (playing) {
        val infinite = rememberInfiniteTransition(label = "music_waveform")
        val value by infinite.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2.0).toFloat(),
            animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Restart),
            label = "music_wave_phase"
        )
        value
    } else 0f
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Box(Modifier.fillMaxWidth().height(58.dp).onSizeChanged { waveformWidth = it.width }.pointerInput(durationMs, playing) {
            detectTapGestures(onTap = { offset -> onSeek(if(waveformWidth>0) (offset.x/waveformWidth.toFloat()).coerceIn(0f,1f) else 0f) })
        }) {
            Canvas(Modifier.fillMaxSize()) {
                val centerY=size.height/2f
                val slot=size.width/barCount
                val barWidth=(slot*.52f).coerceAtLeast(2.dp.toPx())
                for(i in 0 until barCount) {
                    val n=i.toFloat()/(barCount-1).coerceAtLeast(1)
                    val shape=(.24f + sin(i*1.71f).toFloat().absoluteValue*.52f + sin(i*.43f).toFloat().absoluteValue*.22f).coerceIn(.18f,1f)
                    val envelope=(.72f + .28f*sin((n*Math.PI).toFloat()).absoluteValue)
                    val isPlayed=durationMs>0L && n<=progress
                    val pulse = 0.5f + 0.5f * sin(phase * 1.35f + i * 0.55f)
                    val motion = if (playing && isPlayed) (1f + .26f * pulse) else if (!playing) (1f + .035f * pulse) else 1f
                    val barHeight=(shape*envelope*size.height*.92f*motion).coerceIn(5.dp.toPx(), size.height-4.dp.toPx())
                    val x=i*slot+(slot-barWidth)/2f
                    drawRoundRect(if(isPlayed) active else inactive, androidx.compose.ui.geometry.Offset(x, centerY-barHeight/2f), androidx.compose.ui.geometry.Size(barWidth, barHeight), androidx.compose.ui.geometry.CornerRadius(barWidth/2f, barWidth/2f))
                }
                if(durationMs>0L) {
                    val x=size.width*progress
                    if(playing) {
                        val glow = 0.22f + 0.18f * (0.5f + 0.5f * sin(phase * 1.7f))
                        drawCircle(active.copy(alpha=glow), 14.dp.toPx(), androidx.compose.ui.geometry.Offset(x, centerY))
                        drawCircle(active.copy(alpha=.34f), 8.dp.toPx(), androidx.compose.ui.geometry.Offset(x, centerY))
                    }
                    drawCircle(active, 5.dp.toPx(), androidx.compose.ui.geometry.Offset(x, centerY))
                    drawCircle(seekCenterHole, 2.dp.toPx(), androidx.compose.ui.geometry.Offset(x, centerY))
                    if (playing && progress > 0f) {
                        val shimmerX = (progress * size.width - 18.dp.toPx() + ((phase / (kotlin.math.PI.toFloat() * 2f)) * 36.dp.toPx()))
                            .coerceIn(0f, size.width)
                        drawCircle(active.copy(alpha = .24f), 3.dp.toPx(), androidx.compose.ui.geometry.Offset(shimmerX, centerY))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
            Text(formatMediaTime(positionMs), color=if(dark) Color.White.copy(alpha=.78f) else MaterialTheme.colorScheme.onSurfaceVariant, fontSize=10.sp)
            Text(if(durationMs>0L) "-${formatMediaTime((durationMs-positionMs).coerceAtLeast(0L))}" else if(playing) "LIVE" else "", color=if(dark) Color.White.copy(alpha=.78f) else MaterialTheme.colorScheme.onSurfaceVariant, fontSize=10.sp)
        }
    }
}

private fun formatMediaTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

@Composable
internal fun MediaArtwork(metadataBitmap: Bitmap?, artworkUri: String?, rotation: Float = 0f, circular: Boolean = false) {
    val context = LocalContext.current
    val uriBitmap by produceState<Bitmap?>(initialValue = null, artworkUri) {
        value = if (metadataBitmap != null || artworkUri.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(android.net.Uri.parse(artworkUri))?.use {
                    android.graphics.BitmapFactory.decodeStream(it)
                }
            } catch (_: Exception) {
                null
            }
        }
    }
    val bitmap = metadataBitmap ?: uriBitmap
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = .12f),
        shape = if (circular) CircleShape else RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Album artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
            }
        }
    }
}


private val mediaArtworkCache = object : java.util.LinkedHashMap<String, Bitmap>(2, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > 2
}

private fun mediaArtworkCacheGet(key: String): Bitmap? = synchronized(mediaArtworkCache) { mediaArtworkCache[key] }

private fun mediaArtworkCachePut(key: String, bitmap: Bitmap) {
    synchronized(mediaArtworkCache) { mediaArtworkCache[key] = bitmap }
}

private fun mediaText(metadata: android.media.MediaMetadata?, key: String): String =
    metadata?.getString(key)?.trim().orEmpty()

fun notificationPermissionGranted(context: Context): Boolean =
    android.os.Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

fun hasAnyLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun isMediaAccessGranted(context: Context): Boolean {
    return try {
        val enabled = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        enabled.split(":").any {
            try { ComponentName.unflattenFromString(it)?.packageName == context.packageName }
            catch (_: Exception) { false }
        }
    } catch (_: Exception) { false }
}

fun readMediaState(context: Context): MediaState {
    val accessGranted = isMediaAccessGranted(context)
    return try {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(context, MediaNotificationListener::class.java)
        val sessions = manager.getActiveSessions(component)
        if (sessions.isEmpty()) return MediaState(accessGranted = accessGranted)

        // Prefer a currently playing session. If none is playing, retain the most
        // recently relevant session so paused media can still be resumed.
        val controller = sessions.firstOrNull {
            it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
        } ?: sessions.firstOrNull { it.metadata != null } ?: sessions.firstOrNull()

        if (controller == null) return MediaState(accessGranted = accessGranted)

        val metadata = controller.metadata
        val title = listOf(
            mediaText(metadata, android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
            mediaText(metadata, android.media.MediaMetadata.METADATA_KEY_TITLE)
        ).firstOrNull { it.isNotBlank() } ?: "No media title"

        val artist = listOf(
            mediaText(metadata, android.media.MediaMetadata.METADATA_KEY_ARTIST),
            mediaText(metadata, android.media.MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        ).firstOrNull { it.isNotBlank() }.orEmpty()

        val album = mediaText(metadata, android.media.MediaMetadata.METADATA_KEY_ALBUM)
        val artworkUri = listOf(
            mediaText(metadata, android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI),
            mediaText(metadata, android.media.MediaMetadata.METADATA_KEY_ART_URI)
        ).firstOrNull { it.isNotBlank() }
        val artworkKey = listOf(
            artworkUri.orEmpty(),
            controller.packageName.orEmpty(),
            title,
            artist,
            album
        ).joinToString("|")
        val artwork = try {
            mediaArtworkCacheGet(artworkKey) ?: run {
                val raw = metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
                raw?.let {
                    // Media sessions sometimes expose very large artwork. Keep the UI
                    // responsive and reduce repeated bitmap allocations by caching the
                    // bounded result for the current/previous track.
                    val maxEdge = 1200
                    val edge = maxOf(it.width, it.height)
                    val scaled = if (edge > maxEdge) {
                        val scale = maxEdge.toFloat() / edge.toFloat()
                        Bitmap.createScaledBitmap(
                            it,
                            (it.width * scale).roundToInt().coerceAtLeast(1),
                            (it.height * scale).roundToInt().coerceAtLeast(1),
                            true
                        )
                    } else it
                    mediaArtworkCachePut(artworkKey, scaled)
                    scaled
                }
            }
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: Exception) {
            null
        }
        MediaState(
            accessGranted = accessGranted,
            available = true,
            title = title,
            artist = artist,
            album = album,
            artwork = artwork,
            artworkUri = artworkUri,
            packageName = controller.packageName,
            playing = controller.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING,
            positionMs = controller.playbackState?.position?.coerceAtLeast(0L) ?: 0L,
            durationMs = metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L,
            controller = controller
        )
    } catch (_: SecurityException) {
        MediaState(accessGranted = accessGranted)
    } catch (_: Exception) {
        MediaState(accessGranted = accessGranted)
    }
}

@Composable
private fun NavigationDetail() {
    val context = LocalContext.current
    val navAccent = navigationAccent()
    val darkNav = MaterialTheme.colorScheme.background.luminance() < .18f
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var destination by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var homePlace by remember { mutableStateOf(prefs.getString("nav_home", "") ?: "") }
    var workPlace by remember { mutableStateOf(prefs.getString("nav_work", "") ?: "") }
    var recentPlaces by remember { mutableStateOf((prefs.getString("nav_recent_csv", "") ?: "").split("\u001F").filter { it.isNotBlank() }.take(5)) }

    fun saveRecent(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        val next = (listOf(q) + recentPlaces).distinct().take(5)
        recentPlaces = next
        prefs.edit().putString("nav_recent_csv", next.joinToString("\u001F")).apply()
    }

    fun launchNavigation(query: String = destination) {
        val q = query.trim()
        if (q.isBlank()) {
            message = "Enter a destination first."
            return
        }
        destination = q
        saveRecent(q)
        val uri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(q)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { addCategory(Intent.CATEGORY_DEFAULT) }
        try {
            context.startActivity(Intent.createChooser(intent, "Choose navigation app"))
            message = "Opening your navigation app…"
        } catch (_: Exception) {
            message = "No compatible navigation app is installed."
        }
    }

    val quickColors = if (darkNav) {
        // Respect the selected accent in dark mode as well.
        listOf(navAccent, navAccent, navAccent, navAccent)
    } else {
        listOf(Color(0xFF20B8CC), Color(0xFF168C62), Color(0xFF2E72D2), Color(0xFF7656C7))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, navAccent.copy(alpha = .14f))
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(46.dp), CircleShape, color = navAccent.copy(alpha = .11f)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Navigation, null, tint = navAccent, modifier = Modifier.size(24.dp)) }
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Navigation", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Search, save places and hand off to your map app.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it; message = "" },
                    label = { Text("Search location") },
                    placeholder = { Text("Place, address or landmark") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = navAccent) },
                    trailingIcon = if (destination.isNotBlank()) {
                        { IconButton(onClick = { destination = ""; message = "" }) { Icon(Icons.Default.Close, "Clear destination") } }
                    } else null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    QuickNavigationAction("Search", Icons.Default.Search, quickColors[0], Modifier.weight(1f)) { launchNavigation() }
                    QuickNavigationAction("Home", Icons.Default.Home, quickColors[1], Modifier.weight(1f)) {
                        if (homePlace.isBlank()) message = "No Home place saved yet." else launchNavigation(homePlace)
                    }
                    QuickNavigationAction("Work", Icons.Default.DirectionsCar, quickColors[2], Modifier.weight(1f)) {
                        if (workPlace.isBlank()) message = "No Work place saved yet." else launchNavigation(workPlace)
                    }
                    QuickNavigationAction("Recent", Icons.Default.Schedule, quickColors[3], Modifier.weight(1f)) {
                        if (recentPlaces.isNotEmpty()) launchNavigation(recentPlaces.first()) else message = "No recent places yet."
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (destination.isBlank()) message = "Search a place first, then save it as Home."
                            else { homePlace = destination.trim(); prefs.edit().putString("nav_home", homePlace).apply(); message = "Saved Home: $homePlace" }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = AltrozUiTokens.touchTarget), shape = RoundedCornerShape(15.dp)
                    ) { Icon(Icons.Default.Home, null, tint = quickColors[1], modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text(if (homePlace.isBlank()) "Save Home" else "Update Home", fontSize = 11.sp) }
                    OutlinedButton(
                        onClick = {
                            if (destination.isBlank()) message = "Search a place first, then save it as Work."
                            else { workPlace = destination.trim(); prefs.edit().putString("nav_work", workPlace).apply(); message = "Saved Work: $workPlace" }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = AltrozUiTokens.touchTarget), shape = RoundedCornerShape(15.dp)
                    ) { Icon(Icons.Default.DirectionsCar, null, tint = quickColors[2], modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text(if (workPlace.isBlank()) "Save Work" else "Update Work", fontSize = 11.sp) }
                }

                Button(
                    enabled = destination.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    onClick = { launchNavigation() },
                    shape = RoundedCornerShape(17.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = navAccent, contentColor = accentOnPrimary(navAccent))
                ) {
                    Icon(Icons.Default.Navigation, null); Spacer(Modifier.width(8.dp)); Text("Open in LibreMap / Maps", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (message.isNotBlank()) {
                    Text(message, color = navAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().height(210.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f),
            border = androidx.compose.foundation.BorderStroke(1.dp, navAccent.copy(alpha = .10f))
        ) {
            Box(Modifier.fillMaxSize()) {
                // Resolve Compose theme colors in the composable scope before entering DrawScope.
                // MaterialTheme is @Composable and cannot be read from inside Canvas' draw lambda.
                val mapGridColor = MaterialTheme.colorScheme.outline.copy(alpha = .10f)
                Canvas(Modifier.fillMaxSize()) {
                    val grid = mapGridColor
                    for (x in 0..10) drawLine(grid, androidx.compose.ui.geometry.Offset(size.width * x / 10f, 0f), androidx.compose.ui.geometry.Offset(size.width * x / 10f, size.height))
                    for (y in 0..6) drawLine(grid, androidx.compose.ui.geometry.Offset(0f, size.height * y / 6f), androidx.compose.ui.geometry.Offset(size.width, size.height * y / 6f))
                }
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(Modifier.size(54.dp), CircleShape, color = navAccent.copy(alpha = .10f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = navAccent, modifier = Modifier.size(28.dp))
                        }
                    }
                    Text(
                        if (destination.isBlank()) "Map preview" else "Ready to navigate",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(Modifier.align(Alignment.TopStart).padding(13.dp), RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = .90f)) {
                    Text(if (destination.isBlank()) "Map preview" else destination, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = 13.sp)
                }
                Surface(Modifier.align(Alignment.BottomStart).padding(13.dp), RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = .90f)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Navigation, null, tint = navAccent, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text("Map preview", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (recentPlaces.isNotEmpty()) {
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Recent places", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    recentPlaces.take(4).forEach { place ->
                        Row(Modifier.fillMaxWidth().clickable { destination = place; message = "Selected $place" }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(34.dp), CircleShape, color = navAccent.copy(alpha = .10f)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Schedule, null, tint = navAccent, modifier = Modifier.size(17.dp)) } }
                            Spacer(Modifier.width(10.dp))
                            Text(place, Modifier.weight(1f), fontSize = 12.sp, maxLines = 2, softWrap = true, lineHeight = 15.sp)
                            Icon(Icons.Default.ArrowForward, null, tint = navAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = navAccent.copy(alpha = .06f), border = androidx.compose.foundation.BorderStroke(1.dp, navAccent.copy(alpha = .10f))) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.OpenInNew, null, tint = navAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Your map app handles the journey", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("GPS, traffic, ETA, rerouting and voice guidance stay in the installed map app.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DataHistoryDetail(
    latestSnapshot: VehicleSnapshotDb.Snapshot?,
    trips: List<VehicleSnapshotDb.TripRecord>,
    telemetrySamples: List<VehicleSnapshotDb.TelemetrySample>,
    settings: AppSettings,
    bluetoothState: BluetoothDiagnosticState,
    routeRecordingEnabled: Boolean,
    locationPermissionGranted: Boolean,
    latestLocation: Location?,
    onRouteRecordingChange: (Boolean) -> Unit,
    onRequestLocationPermission: () -> Unit,
    historyLoaded: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val detailRouteDb = remember { VehicleSnapshotDb(context.applicationContext) }
    DisposableEffect(detailRouteDb) { onDispose { try { detailRouteDb.close() } catch (_: Exception) { } } }
    val completed = trips.filter { !it.active }
    val active = trips.firstOrNull { it.active }
    var selectedTripId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedTrip = selectedTripId?.let { id -> trips.firstOrNull { it.id == id } }
    var backupInfo by remember { mutableStateOf(VehicleDbBackup.backupInfo(context)) }
    var backupRunning by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf("") }
    var selectedRouteTripId by rememberSaveable { mutableStateOf<Long?>(null) }
    var routePoints by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    var routeLoading by remember { mutableStateOf(false) }
    var routeLoadError by remember { mutableStateOf(false) }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { VehicleDbBackup.restoreFromUri(context, uri) }
                backupMessage = if (ok) "Restore complete" else "Restore unavailable"
                if (ok) backupInfo = VehicleDbBackup.backupInfo(context)
            }
        }
    }
    LaunchedEffect(historyLoaded, completed.firstOrNull()?.id, selectedRouteTripId) {
        if (!historyLoaded) {
            routeLoading = false
            return@LaunchedEffect
        }
        if (selectedRouteTripId == null) {
            selectedRouteTripId = completed.firstOrNull()?.id
        }
        val id = selectedRouteTripId ?: completed.firstOrNull()?.id
        routeLoading = id != null
        routeLoadError = false
        try {
            routePoints = if (id != null) withContext(Dispatchers.IO) { detailRouteDb.routePoints(id) } else emptyList()
        } catch (_: Throwable) {
            routePoints = emptyList()
            routeLoadError = true
        } finally {
            routeLoading = false
        }
    }
    val parkingPrefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var parkingLat by remember { mutableStateOf(if (parkingPrefs.contains("parking_lat")) parkingPrefs.getFloat("parking_lat", 0f).toDouble() else null) }
    var parkingLon by remember { mutableStateOf(if (parkingPrefs.contains("parking_lon")) parkingPrefs.getFloat("parking_lon", 0f).toDouble() else null) }
    var parkingAt by remember { mutableStateOf(parkingPrefs.getLong("parking_at", 0L)) }

    data class RefuelEvent(val timestamp: Long, val from: Int, val to: Int)
    val refuels = buildList {
        var previous: VehicleSnapshotDb.TelemetrySample? = null
        telemetrySamples.sortedBy { it.timestamp }.forEach { sample ->
            val before = previous?.fuelPercent
            val after = sample.fuelPercent
            if (before != null && after != null && after - before >= 5) add(RefuelEvent(sample.timestamp, before, after))
            previous = sample
        }
    }.takeLast(10).reversed()

    val calendarMonthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val monthYear = calendarMonthStart.get(Calendar.YEAR)
    val monthIndex = calendarMonthStart.get(Calendar.MONTH)
    val firstDayOffset = (calendarMonthStart.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    val daysInMonth = calendarMonthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
    val activeDays = completed.map {
        Calendar.getInstance().apply { timeInMillis = it.startAt }.let { c ->
            Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
        }
    }.toSet()
    var selectedCalendarDay by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedDayTrips = selectedCalendarDay?.let { day ->
        completed.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.startAt }
            c.get(Calendar.YEAR) == monthYear && c.get(Calendar.MONTH) == monthIndex && c.get(Calendar.DAY_OF_MONTH) == day
        }
    } ?: emptyList()

    CardBox {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DATA & HISTORY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("Saved vehicle data", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Pill(completed.size.toString(), MaterialTheme.colorScheme.primary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile("LATEST ODO", latestSnapshot?.odo?.let { "%,d\u00A0km".format(Locale.US, it) } ?: "—", Icons.Default.Speed, Modifier.weight(1f))
            SummaryTile("LATEST FUEL", latestSnapshot?.fuelPercent?.let { "$it%" } ?: "—", Icons.Default.GasMeter, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile("RANGE", latestSnapshot?.dteKm?.let { distanceValue(it.toFloat(), settings, 0) } ?: "—", Icons.Default.Navigation, Modifier.weight(1f))
            SummaryTile("DRIVES", completed.size.toString(), Icons.Default.Route, Modifier.weight(1f))
        }
    }

    CardBox {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("DATA AVAILABILITY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
            val dataStatusLive = bluetoothState.liveVisValidated
            val dataStatusSaved = latestSnapshot != null
            Text(when { dataStatusLive -> "LIVE"; dataStatusSaved -> "SAVED"; bluetoothState.htsConnected -> "CONNECTED"; else -> "OFFLINE" }, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (dataStatusLive) Green else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DataQualityChip("ODO", latestSnapshot?.odo != null, Modifier.weight(1f))
            DataQualityChip("FUEL", latestSnapshot?.fuelPercent != null, Modifier.weight(1f))
            DataQualityChip("DTE", latestSnapshot?.dteKm != null, Modifier.weight(1f))
            DataQualityChip("TRIPS", completed.isNotEmpty(), Modifier.weight(1f))
        }
    }

    // Analytics are intentionally kept on their dedicated screens.
    // Data & History is reserved for stored data, route/drive history and backup.

    CardBox {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("DRIVING CALENDAR", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
            Text(SimpleDateFormat("MMMM yyyy", Locale.US).format(calendarMonthStart.time), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("M","T","W","T","F","S","S").forEach { Text(it, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center) }
        }
        val cells = List(firstDayOffset) { 0 } + (1..daysInMonth).toList()
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                week.forEach { day ->
                    if (day == 0) Spacer(Modifier.size(44.dp)) else {
                        val hasDrive = activeDays.contains(Triple(monthYear, monthIndex, day))
                        Surface(
                            Modifier.size(44.dp).clickable { selectedCalendarDay = day },
                            shape = CircleShape,

                            color = when {
                                selectedCalendarDay == day -> MaterialTheme.colorScheme.primary.copy(alpha = .20f)
                                hasDrive -> MaterialTheme.colorScheme.primary.copy(alpha = .10f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .22f)
                            }
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Surface(Modifier.size(34.dp), CircleShape, color = Color.Transparent) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(day.toString(), fontSize = 9.sp, fontWeight = if (hasDrive) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.size(44.dp)) }
            }
        }
        if (selectedCalendarDay != null) {
            Text("$selectedCalendarDay ${SimpleDateFormat("MMM", Locale.US).format(calendarMonthStart.time)} • ${selectedDayTrips.size} trip${if (selectedDayTrips.size == 1) "" else "s"}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            if (selectedDayTrips.isNotEmpty()) Text("${distanceValue(selectedDayTrips.sumOf { it.distanceKm.toDouble() }.toFloat(), settings)} • ${formatDisplayedTripDuration(selectedDayTrips)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    CardBox {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ROUTE HISTORY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
            Switch(checked = routeRecordingEnabled, onCheckedChange = { enabled ->
                if (enabled && !locationPermissionGranted) onRequestLocationPermission()
                else onRouteRecordingChange(enabled)
            })
        }
        Text(if (routeRecordingEnabled) "GPS route recording ON" else "GPS route recording OFF", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (completed.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                completed.take(4).forEach { trip ->
                    val selected = selectedRouteTripId == trip.id
                    Surface(Modifier.weight(1f).heightIn(min = 44.dp).clickable { selectedRouteTripId = trip.id }, shape = RoundedCornerShape(11.dp), color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)) {
                        Box(contentAlignment = Alignment.Center) { Text(SimpleDateFormat("dd MMM", Locale.US).format(Date(trip.startAt)), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                    }
                }
            }
        }
        when {
            routeLoading -> EmptyDataState("LOADING ROUTE…", Icons.Default.Navigation)
            routeLoadError -> EmptyDataState("ROUTE UNAVAILABLE", Icons.Default.Navigation, "Saved route data could not be loaded right now.")
            routePoints.size >= 2 -> OpenRouteMap(routePoints, MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth().height(210.dp))
            else -> EmptyDataState("ROUTE NOT AVAILABLE", Icons.Default.Navigation, "No route points were saved for the selected trip.")
        }
    }

    CardBox {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("LAST PARKED", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Navigation, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        if (parkingLat != null && parkingLon != null) {
            Text(SimpleDateFormat("dd MMM • HH:mm", Locale.US).format(Date(parkingAt)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = {
                val uri = Uri.parse("geo:${parkingLat},${parkingLon}?q=${parkingLat},${parkingLon}")
                try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { }
            }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Navigation, null); Spacer(Modifier.width(7.dp)); Text("OPEN PARKING LOCATION")
            }
        } else {
            Text("No parking location saved yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(
            enabled = latestLocation != null,
            onClick = {
                latestLocation?.let { loc ->
                    parkingLat = loc.latitude; parkingLon = loc.longitude; parkingAt = System.currentTimeMillis()
                    parkingPrefs.edit().putFloat("parking_lat", loc.latitude.toFloat()).putFloat("parking_lon", loc.longitude.toFloat()).putLong("parking_at", parkingAt).apply()
                }
            }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp)
        ) { Text("SAVE CURRENT LOCATION") }
    }

    if (refuels.isNotEmpty()) {
        CardBox {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("REFUEL HISTORY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
                Text(refuels.size.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            refuels.forEach { event ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(SimpleDateFormat("dd MMM • HH:mm", Locale.US).format(Date(event.timestamp)), fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("${event.from}% → ${event.to}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }

    CardBox {
        Text("BACKUP & EXPORT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                backupRunning = true; backupMessage = ""
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { VehicleDbBackup.backupNow(context) }
                    backupInfo = VehicleDbBackup.backupInfo(context); backupRunning = false
                    backupMessage = if (ok) "Backup ready" else "Backup could not be created"
                }
            }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(14.dp), enabled = !backupRunning) {
                Icon(Icons.Default.UploadFile, null); Spacer(Modifier.width(5.dp)); Text("Back up")
            }
            OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/octet-stream", "application/x-sqlite3", "*/*")) }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Download, null); Spacer(Modifier.width(5.dp)); Text("Restore")
            }
        }
        Button(onClick = { shareVehicleDataJson(context, completed, telemetrySamples) }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.Download, null); Spacer(Modifier.width(7.dp)); Text("Export JSON")
        }
        Text(
            if (backupInfo.available) "Backup • ${SimpleDateFormat("dd MMM • HH:mm", Locale.US).format(Date(backupInfo.createdAt ?: 0L))} • ${backupInfo.snapshotRows} snapshots • ${backupInfo.telemetryRows} samples" else "No shared backup available yet",
            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp
        )
        if (backupMessage.isNotBlank()) Text(backupMessage, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }

    if (active != null) {
        CardBox {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("CURRENT DRIVE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
                Pill(if (bluetoothState.liveVisValidated) "LIVE" else "TRIP ACTIVE", if (bluetoothState.liveVisValidated) Green else Muted)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryTile("DISTANCE", distanceValue(active.distanceKm, settings), Icons.Default.Route, Modifier.weight(1f))
                SummaryTile("TIME", formatTripDuration(active.durationSec), Icons.Default.Speed, Modifier.weight(1f))
            }
        }
    }

    CardBox {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("TRIP HISTORY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
            if (completed.isNotEmpty()) {
                TextButton(onClick = { shareTripCsv(context, completed) }) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("Export")
                }
            }
        }
        if (!historyLoaded) EmptyDataState("LOADING TRIP HISTORY…", Icons.Default.Route)
        else if (completed.isEmpty()) EmptyDataState("NO COMPLETED DRIVES", Icons.Default.Route, "Completed trips will appear here after a drive is finalized.")
        else completed.take(20).forEach { trip ->
            TripHistoryRow(trip, settings, Modifier.clickable { selectedTripId = trip.id })
            if (trip != completed.take(20).last()) androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = .18f))
        }
    }

    selectedTrip?.let { trip ->
        TripDetailDialog(trip, settings) { selectedTripId = null }
    }
}

@Composable
private fun ComparisonTile(label: String, distanceKm: Float, economy: Float?, settings: AppSettings, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .34f)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(distanceValue(distanceKm, settings), fontSize = stableValueSize(distanceValue(distanceKm, settings), 14f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, softWrap = false)
            Text(economy?.let { economyValue(it, settings) } ?: "—", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun DataQualityChip(label: String, available: Boolean, modifier: Modifier) {
    Surface(
        modifier.height(34.dp),
        shape = RoundedCornerShape(11.dp),
        color = if (available) Green.copy(alpha = .10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (available) Green else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun shareVehicleDataJson(context: Context, trips: List<VehicleSnapshotDb.TripRecord>, samples: List<VehicleSnapshotDb.TelemetrySample>) {
    fun esc(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    val json = buildString {
        appendLine("{")
        appendLine("  \"app\": \"AltrozDrive\",")
        appendLine("  \"exported_at\": ${System.currentTimeMillis()},")
        appendLine("  \"trips\": [")
        trips.forEachIndexed { index, t ->
            append("    {\"id\":${t.id},\"start_at\":${t.startAt},\"end_at\":${t.endAt ?: "null"},\"distance_km\":${"%.1f".format(Locale.US, t.distanceKm)},\"duration_sec\":${t.durationSec},\"avg_economy\":${t.avgEconomy?.let { "%.2f".format(Locale.US, it) } ?: "null"},\"start_odo\":${t.startOdo ?: "null"},\"end_odo\":${t.endOdo ?: "null"},\"start_fuel\":${t.startFuel ?: "null"},\"end_fuel\":${t.endFuel ?: "null"}}")
            if (index < trips.lastIndex) append(",")
            appendLine()
        }
        appendLine("  ],")
        appendLine("  \"telemetry_samples\": [")
        samples.forEachIndexed { index, t ->
            append("    {\"timestamp\":${t.timestamp},\"vehicle_key\":\"${esc(t.vehicleKey)}\",\"odo\":${t.odo ?: "null"},\"fuel_percent\":${t.fuelPercent ?: "null"},\"dte_km\":${t.dteKm ?: "null"},\"trip_a\":${t.tripA?.let { "%.2f".format(Locale.US, it) } ?: "null"},\"trip_b\":${t.tripB?.let { "%.2f".format(Locale.US, it) } ?: "null"},\"avg_a\":${t.avgA?.let { "%.2f".format(Locale.US, it) } ?: "null"},\"avg_b\":${t.avgB?.let { "%.2f".format(Locale.US, it) } ?: "null"}}")
            if (index < samples.lastIndex) append(",")
            appendLine()
        }
        appendLine("  ]")
        appendLine("}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_SUBJECT, "AltrozDrive vehicle data export")
        putExtra(Intent.EXTRA_TEXT, json)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try { context.startActivity(Intent.createChooser(intent, "Export AltrozDrive data")) } catch (_: Exception) { }
}

@Composable
private fun TripHistoryRow(trip: VehicleSnapshotDb.TripRecord, settings: AppSettings, modifier: Modifier = Modifier) {
    val date = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(trip.startAt))
    val time = SimpleDateFormat("HH:mm", Locale.US).format(Date(trip.startAt))
    Row(
        modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
        ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Route, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) } }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("$date • $time", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${distanceValue(trip.distanceKm, settings)} • ${formatTripDuration(trip.durationSec)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(trip.avgEconomy?.let { economyValue(it, settings) } ?: "—", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("DETAILS", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
        }
    }
}

@Composable
private fun TripDetailDialog(trip: VehicleSnapshotDb.TripRecord, settings: AppSettings, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trip details", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.US).format(Date(trip.startAt)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                trip.endAt?.let { Text("Ended ${SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.US).format(Date(it))}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                TripDetailLine("Distance", distanceValue(trip.distanceKm, settings))
                TripDetailLine("Duration", formatTripDuration(trip.durationSec))
                TripDetailLine("Start ODO", trip.startOdo?.let { "%,d\u00A0km".format(Locale.US, it) } ?: "—")
                TripDetailLine("End ODO", trip.endOdo?.let { "%,d\u00A0km".format(Locale.US, it) } ?: "—")
                TripDetailLine("Economy", trip.avgEconomy?.let { economyValue(it, settings) } ?: "—")
                TripDetailLine("Trip A", trip.startTripA?.let { "%.1f".format(Locale.US, it) }?.plus(" → ")?.plus(trip.endTripA?.let { "%.1f\u00A0km".format(Locale.US, it) } ?: "—") ?: "—")
                TripDetailLine("Trip B", trip.startTripB?.let { "%.1f".format(Locale.US, it) }?.plus(" → ")?.plus(trip.endTripB?.let { "%.1f\u00A0km".format(Locale.US, it) } ?: "—") ?: "—")
                TripDetailLine("Fuel", trip.startFuel?.let { "$it%" }?.plus(" → ")?.plus(trip.endFuel?.let { "$it%" } ?: "—") ?: "—")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
internal fun TripDetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = stableValueSize(value, 12f), fontFamily = FontFamily.Monospace, maxLines = 2, softWrap = true, textAlign = TextAlign.End, lineHeight = 14.sp)
        }
    }
}

private fun shareTripCsv(context: Context, trips: List<VehicleSnapshotDb.TripRecord>) {
    val csv = buildString {
        appendLine("trip_id,vehicle_key,start_time,end_time,start_odo_km,end_odo_km,distance_km,duration_sec,avg_economy,start_trip_a_km,end_trip_a_km,start_trip_b_km,end_trip_b_km,start_fuel_percent,end_fuel_percent")
        trips.forEach { t ->
            appendLine(listOf(
                t.id,
                csvCell(t.vehicleKey),
                t.startAt,
                t.endAt ?: "",
                t.startOdo ?: "",
                t.endOdo ?: "",
                "%.1f".format(Locale.US, t.distanceKm),
                t.durationSec,
                t.avgEconomy?.let { "%.1f".format(Locale.US, it) } ?: "",
                t.startTripA?.let { "%.1f".format(Locale.US, it) } ?: "",
                t.endTripA?.let { "%.1f".format(Locale.US, it) } ?: "",
                t.startTripB?.let { "%.1f".format(Locale.US, it) } ?: "",
                t.endTripB?.let { "%.1f".format(Locale.US, it) } ?: "",
                t.startFuel ?: "",
                t.endFuel ?: ""
            ).joinToString(","))
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "AltrozDrive trip history")
        putExtra(Intent.EXTRA_TEXT, csv)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try { context.startActivity(Intent.createChooser(intent, "Export trip history")) } catch (_: Exception) { }
}

private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""

@Composable
fun VehicleProfileDetail(vehicle:VehicleProfile,onVehicle:(VehicleProfile)->Unit){
    val option = tataIceVehicles.firstOrNull { it.model == vehicle.model } ?: tataIceVehicles.first()
    var selectedModel by remember(vehicle.model) { mutableStateOf(vehicle.model) }
    var selectedTrim by remember(vehicle.trim) { mutableStateOf(vehicle.trim) }
    var selectedFuel by remember(vehicle.fuel) { mutableStateOf(vehicle.fuel) }
    var selectedTransmission by remember(vehicle.transmission) { mutableStateOf(vehicle.transmission) }
    val current = tataIceVehicles.firstOrNull { it.model == selectedModel } ?: option
    CardBox {
        Text("My Tata vehicle",fontSize=20.sp,fontWeight=FontWeight.Bold)
        Text("Select the Tata ICE model, trim, fuel and transmission used by this profile.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=13.sp)
        TataVehicleImage(selectedModel)
        Text("Current model image",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=11.sp)
        SettingsDropdown("Model",selectedModel,tataIceVehicles.map{it.model to it.model}, onSelect = {
            selectedModel=it; val v=tataIceVehicles.firstOrNull{it.model==selectedModel}; if (v != null) { selectedTrim=v.trims.firstOrNull().orEmpty(); selectedFuel=v.fuels.firstOrNull().orEmpty(); selectedTransmission=v.transmissions.firstOrNull().orEmpty() }
        })
        SettingsDropdown("Variant / trim",selectedTrim,current.trims.map{it to it}, onSelect = { selectedTrim=it })
        SettingsDropdown("Fuel",selectedFuel,current.fuels.map{it to it}, onSelect = { selectedFuel=it })
        SettingsDropdown("Transmission",selectedTransmission,current.transmissions.map{it to it}, onSelect = { selectedTransmission=it })
        Button(
            onClick = { onVehicle(VehicleProfile(selectedModel,selectedTrim,selectedFuel,selectedTransmission)) },
            modifier = Modifier.heightIn(min = AltrozUiTokens.touchTarget)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Use this vehicle")
        }
    }
}

@Composable
fun TataCarIcon(model: String, modifier: Modifier = Modifier, idleAnimation: Boolean = false) {
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    Canvas(modifier = modifier.then(AltrozCarIdleModifier(idleAnimation))) {
        val w = size.width
        val h = size.height
        val isSuv = model in setOf("Punch", "Nexon", "Curvv", "Harrier", "Safari", "Sierra")
        val isLarge = model in setOf("Harrier", "Safari", "Sierra")
        val bodyLeft = w * if (isLarge) .10f else .12f
        val bodyRight = w * if (isLarge) .90f else .88f
        val bodyTop = h * if (isSuv) .35f else .40f
        val bodyBottom = h * .72f
        val roofTop = h * if (isLarge) .18f else if (isSuv) .23f else .27f
        val roofLeft = w * if (isLarge) .28f else if (isSuv) .30f else .33f
        val roofRight = w * if (isLarge) .72f else if (isSuv) .70f else .67f

        // Solid monochrome vehicle silhouette. No external image/white background.
        val body = androidx.compose.ui.graphics.Path().apply {
            moveTo(bodyLeft, bodyBottom)
            lineTo(bodyLeft + w*.025f, bodyTop)
            quadraticBezierTo(w*.18f, h*.33f, roofLeft, roofTop)
            quadraticBezierTo(w*.50f, h*(if (isLarge) .13f else .20f), roofRight, roofTop)
            quadraticBezierTo(w*(if (isLarge) .82f else .80f), h*.31f, bodyRight-w*.025f, bodyTop)
            lineTo(bodyRight, bodyBottom)
            quadraticBezierTo(bodyRight, h*.77f, bodyRight-w*.08f, h*.78f)
            lineTo(bodyLeft+w*.08f, h*.78f)
            quadraticBezierTo(bodyLeft, h*.77f, bodyLeft, bodyBottom)
            close()
        }
        drawPath(body, color=accent)

        // Windows cut out visually with the app surface colour.
        val window = androidx.compose.ui.graphics.Path().apply {
            moveTo(w*.30f, h*.37f)
            lineTo(roofLeft+w*.035f, roofTop+h*.025f)
            lineTo(roofRight-w*.035f, roofTop+h*.025f)
            lineTo(w*.70f, h*.37f)
            close()
        }
        drawPath(window, color=surface)
        drawLine(surface, androidx.compose.ui.geometry.Offset(w*.50f,h*.25f), androidx.compose.ui.geometry.Offset(w*.50f,h*.37f), strokeWidth=w*.018f)

        // Headlamps / grille detail remain accent-only for a clean icon.
        drawRoundRect(accent, androidx.compose.ui.geometry.Offset(w*.17f,h*.51f), androidx.compose.ui.geometry.Size(w*.13f,h*.055f), cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.02f,w*.02f))
        drawRoundRect(accent, androidx.compose.ui.geometry.Offset(w*.70f,h*.51f), androidx.compose.ui.geometry.Size(w*.13f,h*.055f), cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.02f,w*.02f))
        drawRoundRect(surface, androidx.compose.ui.geometry.Offset(w*.38f,h*.58f), androidx.compose.ui.geometry.Size(w*.24f,h*.045f), cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.02f,w*.02f))

        // Wheels.
        drawCircle(surface, radius=w*.075f, center=androidx.compose.ui.geometry.Offset(w*.27f,h*.76f))
        drawCircle(surface, radius=w*.075f, center=androidx.compose.ui.geometry.Offset(w*.73f,h*.76f))
    }
}


@Composable
fun AltrozCutoutImage(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    accentStrength: Float = 0.20f,
    idleAnimation: Boolean = false
) {
    // The PNG is genuinely transparent: the original vehicle stays photographic while
    // a low-alpha accent layer preserves body detail and gives the car the selected theme color.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Accent glow is deliberately kept below the car rather than tinting the whole card.
        Box(
            Modifier
                .fillMaxWidth(0.78f)
                .height(18.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, accent.copy(alpha = (0.34f * (accentStrength / 0.20f).coerceIn(0f, 1f))), Color.Transparent)
                    ),
                    RoundedCornerShape(50)
                )
                .graphicsLayer { alpha = 0.72f }
        )
        Image(
            painter = painterResource(id = R.drawable.altroz_cutout),
            contentDescription = "Tata Altroz",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().then(AltrozCarIdleModifier(idleAnimation))
        )
        // Do NOT tint the whole bitmap. A full-image color filter makes the bitmap
        // bounds visually read as a rectangle on light themes. Keep the photographic
        // vehicle untouched and use only the separate accent ground glow below it.
        // This keeps the car naturally integrated into the card surface.
    }
}


@Composable
fun TataVehicleImage(model: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .32f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if (model.equals("Altroz", ignoreCase = true)) {
                    AltrozCutoutImage(Modifier.size(width=250.dp,height=128.dp), MaterialTheme.colorScheme.primary, 0.22f, idleAnimation = false)
                } else {
                    TataCarIcon(model, Modifier.size(190.dp, 120.dp))
                }
                Text(model, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Current model", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MiniBarChart(
    values: List<Float>,
    label: String,
    rangeLabel: String = "Recorded history",
    chartColor: Color = MaterialTheme.colorScheme.primary,
    unit: String = ""
) {
    // Use a small, fixed number of wide bars so the history is unambiguously a bar chart.
    // The visible window is fixed, so adding larger values never changes the card height.
    val visible = values.takeLast(8)
    val maxValue = visible.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f)
    var selectedIndex by remember(visible) { mutableIntStateOf(visible.lastIndex.coerceAtLeast(0)) }
    val selected = visible.getOrNull(selectedIndex)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                selected?.let {
                    val selectedText = if (unit.isBlank()) String.format(Locale.US, "%.1f", it) else String.format(Locale.US, "%.1f\u00A0%s", it, unit)
                    Text(selectedText, color = chartColor, fontSize = stableLargeValueSize(selectedText,22f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, softWrap = false)
                }
            }
            if (rangeLabel.isNotBlank()) Text(rangeLabel, color = muted, fontSize = 10.sp, maxLines = 2, lineHeight = 13.sp)
        }

        Surface(
            modifier = Modifier.fillMaxWidth().height(132.dp),
            shape = RoundedCornerShape(16.dp),
            color = surface
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                visible.forEachIndexed { index, value ->
                    val fraction = (value / maxValue).coerceIn(0f, 1f)
                    val isSelected = index == selectedIndex
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable { selectedIndex = index },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.BottomCenter) {
                            if (isSelected) {
                                val text = if (unit.isBlank()) String.format(Locale.US, "%.0f", value) else String.format(Locale.US, "%.0f\u00A0%s", value, unit)
                                Text(text, color = chartColor, fontSize = stableValueSize(text, 8.5f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, softWrap = false)
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Box(
                            Modifier.fillMaxWidth(.72f)
                                .height((92.dp * AltrozAnimatedFraction(fraction, index * 55)).coerceAtLeast(8.dp))
                                .background(if (isSelected) chartColor else chartColor.copy(alpha = .62f), RoundedCornerShape(7.dp))
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Oldest", color = muted, fontSize = 10.sp)
            Text("Latest", color = muted, fontSize = 10.sp)
        }
    }
}

