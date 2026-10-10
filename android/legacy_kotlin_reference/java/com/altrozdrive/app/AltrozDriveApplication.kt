package com.altrozdrive.app

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Last-resort crash recorder. Writes uncaught exceptions to the user's normal
 * Downloads folder so a crash can be diagnosed even when the app cannot reopen.
 */
class AltrozDriveApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try { MapLibreBridge.initialize(this) } catch (_: Throwable) { }
        val restored = try { VehicleDbBackup.restoreIfNeeded(this) } catch (_: Throwable) { false }
        try { val db = VehicleSnapshotDb(this); try { db.finalizeStaleActiveTrips() } finally { db.close() } } catch (_: Throwable) { }
        try { TripFinalizationWatchdog.start(this) } catch (_: Throwable) { }
        try { VehicleDbBackup.scheduleNightly(this) } catch (_: Throwable) { }
        try { VehicleDbBackup.scheduleStartupCatchUp(this) } catch (_: Throwable) { }
        try { AppFileLogger.log(this, "APP_START version=${BuildConfig.VERSION_NAME} restored=$restored") } catch (_: Throwable) { }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try { AppFileLogger.log(this, "UNCAUGHT_EXCEPTION thread=${thread.name} type=${throwable.javaClass.name}") } catch (_: Throwable) { }
            try { CrashFileLogger.write(this, thread, throwable) } catch (_: Throwable) { }
            try { previous?.uncaughtException(thread, throwable) } catch (_: Throwable) { }
        }
    }
}

/**
 * Process-lifetime trip finalization watchdog. It is intentionally owned by the
 * Application rather than BluetoothDiagnosticManager so closing/disconnecting
 * RFCOMM/HTS cannot cancel physical-trip inactivity finalization.
 */
private object TripFinalizationWatchdog {
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    @Volatile private var started = false

    private val check = object : Runnable {
        override fun run() {
            try {
                val app = application ?: return
                val db = VehicleSnapshotDb(app)
                try {
                    val finalized = db.finalizeStaleActiveTrips(
                        System.currentTimeMillis(),
                        VehicleSnapshotDb.TRIP_IDLE_TIMEOUT_MS
                    )
                    if (finalized > 0) {
                        AppFileLogger.log(
                            app,
                            "TRIP AUTO-FINALIZED inactivity=${VehicleSnapshotDb.TRIP_IDLE_TIMEOUT_MS / 60000L}m count=$finalized"
                        )
                    }
                } finally {
                    db.close()
                }
            } catch (t: Throwable) {
                application?.let {
                    try {
                        AppFileLogger.log(it, "TRIP AUTO-FINALIZE CHECK FAILED ${t.javaClass.simpleName}: ${t.message ?: "unknown"}")
                    } catch (_: Throwable) { }
                }
            } finally {
                handler.postDelayed(this, 30_000L)
            }
        }
    }

    @Volatile private var application: Application? = null

    fun start(app: Application) {
        if (started) return
        application = app.applicationContext as Application
        started = true
        handler.removeCallbacks(check)
        handler.postDelayed(check, 30_000L)
    }
}

private object AppFileLogger {
    private const val RELATIVE_PATH = "Download/AltrozDrive/Logs/"

    fun log(app: Application, message: String) {
        val name = "app_runtime.log"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = app.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_PATH)
            }
            // Find the existing runtime log; create it only once.
            var uri: android.net.Uri? = null
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.RELATIVE_PATH}=? AND ${MediaStore.Downloads.DISPLAY_NAME}=?",
                arrayOf(RELATIVE_PATH, name), null
            )?.use { c -> if (c.moveToFirst()) uri = android.content.ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0)) }
            if (uri == null) uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let { resolver.openOutputStream(it, "wa")?.use { out ->
                val line = "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}  $message\n"
                out.write(line.toByteArray(Charsets.UTF_8))
            } }
        } else {
            @Suppress("DEPRECATION")
            val dir = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AltrozDrive/Logs")
            dir.mkdirs()
            java.io.File(dir, name).appendText("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}  $message\n", Charsets.UTF_8)
        }
    }
}

private object CrashFileLogger {
    private const val RELATIVE_PATH = "Download/AltrozDrive/Logs/"

    fun write(app: Application, thread: Thread, throwable: Throwable) {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val name = "crash_${stamp}.txt"
        val text = buildString {
            appendLine("AltrozDrive crash report")
            appendLine("Time: ${Date()}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Thread: ${thread.name}")
            appendLine()
            appendLine("Exception:")
            appendLine(android.util.Log.getStackTraceString(throwable))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_PATH)
            }
            val uri = app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
            app.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        } else {
            @Suppress("DEPRECATION")
            val dir = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AltrozDrive/Logs")
            dir.mkdirs()
            java.io.File(dir, name).writeText(text, Charsets.UTF_8)
        }
    }
}
