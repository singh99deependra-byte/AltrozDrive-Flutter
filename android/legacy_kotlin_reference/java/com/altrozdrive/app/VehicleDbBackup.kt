package com.altrozdrive.app

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.concurrent.read

/**
 * Production vehicle-history persistence/recovery layer.
 *
 * Design goals:
 *  - Live SQLite remains the single source of truth for the running app.
 *  - Bluetooth/HTS/X451 code only calls requestBackup() after a successful DB write;
 *    requestBackup only marks the DB dirty and keeps the next nightly backup armed.
 *  - Exactly one dated backup file is maintained per calendar day.
 *  - A shared-storage backup is written under Android/media/com.altrozdrive.app/AltrozDriveBackup.
 *  - Android Auto Backup is also enabled for the private database as a second recovery path.
 *  - Backup creation is serialized and validated before a recovery point is published.
 *  - An empty/corrupt database can never replace a known-good recovery point.
 *
 * Important Android storage note:
 * A normal app cannot silently regain access to arbitrary files from an older
 * installation on Android 11+ after uninstall. Therefore the shared copy is a
 * user-visible recovery/export copy, while automatic reinstall recovery relies
 * on Android's own Auto Backup when that device/account has a backup available.
 */
object VehicleDbBackup {
    private const val TAG = "AltrozDrive"
    const val DB_NAME = "altrozdrive_vehicle.db"
    const val SHARED_BACKUP_DIR = "Android/media/com.altrozdrive.app/AltrozDriveBackup/"
    private const val SHARED_PREFIX = "altrozdrive_vehicle_backup_"
    private const val SHARED_INFO_NAME = "backup_info.txt"
    private const val SHARED_SUFFIX = ".db"
        private const val MIN_BACKUP_INTERVAL_MS = 60_000L
    private const val COALESCE_DELAY_MS = 750L
    private const val RETRY_DELAY_MS = 15L * 60L * 1000L
    private const val NIGHTLY_HOUR = 1
    private const val NIGHTLY_MINUTE = 30

    /** All application DB mutations and physical DB snapshots share this lock. */
    internal val DB_IO_LOCK = ReentrantReadWriteLock()

    /** Prevents the nightly receiver and the asynchronous worker from sharing one temp file. */
    private val BACKUP_EXECUTION_LOCK = ReentrantReadWriteLock()

    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "AltrozDrive-DB-Backup").apply { isDaemon = true }
    }
    private val backupRunning = AtomicBoolean(false)
    private val backupDirty = AtomicBoolean(false)
    private val backupScheduled = AtomicBoolean(false)
    private val lastBackupStartedAt = AtomicLong(0L)

    data class BackupInfo(
        val available: Boolean,
        val createdAt: Long? = null,
        val sizeBytes: Long = 0L,
        val snapshotRows: Int = 0,
        val telemetryRows: Int = 0,
        val location: String = SHARED_BACKUP_DIR
    )

    /**
     * Called after a genuine DB write. This does not perform I/O immediately; the
     * database is backed up automatically during the nightly backup window.
     */
    fun requestBackup(context: Context) {
        // DB writes only mark the database dirty. Automatic backups are deliberately
        // performed once during the nightly window, never once per telemetry write.
        backupDirty.set(true)
        scheduleNightly(context.applicationContext)
    }

    private fun scheduleWorker(context: Context, delayMs: Long = COALESCE_DELAY_MS) {
        if (!backupScheduled.compareAndSet(false, true)) return
        executor.schedule({
            backupScheduled.set(false)
            try {
                if (backupRunning.compareAndSet(false, true)) {
                    runCoalescedBackups(context)
                } else {
                    backupScheduled.set(false)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "DB_BACKUP_WORKER_FAILED", t)
                backupRunning.set(false)
                backupScheduled.set(false)
                backupDirty.set(true)
                scheduleWorker(context, RETRY_DELAY_MS)
            }
        }, delayMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
    }

    private fun runCoalescedBackups(context: Context) {
        try {
            var attempts = 0
            while (attempts < 2) {
                if (!backupDirty.getAndSet(false)) return

                // Do not sleep on the executor thread while honoring the one-minute
                // write-to-backup throttle. Re-schedule the worker instead.
                val now = android.os.SystemClock.elapsedRealtime()
                val previous = lastBackupStartedAt.get()
                val wait = (MIN_BACKUP_INTERVAL_MS - (now - previous)).coerceAtLeast(0L)
                if (wait > 0L) {
                    backupDirty.set(true)
                    scheduleWorker(context, wait)
                    return
                }

                val success = backupNow(context)
                attempts++
                if (!success) {
                    backupDirty.set(true)
                    scheduleWorker(context, RETRY_DELAY_MS)
                    return
                }
            }
        } finally {
            backupRunning.set(false)
            if (backupDirty.get()) scheduleWorker(context)
        }
    }

    /** Performs the actual validated backup. Nightly scheduling is the automatic trigger. */
    fun backupNow(context: Context): Boolean {
        val app = context.applicationContext
        return BACKUP_EXECUTION_LOCK.write {
            val temp = File(app.cacheDir, "altrozdrive_db_backup_candidate.db")
            lastBackupStartedAt.set(android.os.SystemClock.elapsedRealtime())
            DB_IO_LOCK.write {
                try {
                    val dbFile = app.getDatabasePath(DB_NAME)
                    if (!dbFile.exists() || dbFile.length() == 0L) {
                        false
                    } else {
                        val counts: Pair<Int, Int>? = openAndCheckpoint(dbFile).use { db ->
                            val result = genuineRowCounts(db)
                            if (result.first == 0 && result.second == 0) null
                            else {
                                requireSqliteHealthy(db)
                                result
                            }
                        }

                        if (counts == null) {
                            false
                        } else {
                            copyAndSync(dbFile, temp)
                            validateBackup(temp)

                            val sharedSaved = writeSharedBackup(app, temp, counts)
                            if (!sharedSaved) {
                                // The validated private DB remains intact. Treat failure to
                                // publish the shared recovery point as a retryable failure.
                                backupDirty.set(true)
                                Log.w(TAG, "DB_SHARED_BACKUP_UNAVAILABLE")
                                scheduleWorker(app, RETRY_DELAY_MS)
                                false
                            } else {
                                // Safe because DB_IO_LOCK is still held: another genuine DB
                                // write cannot finish until after this backup point is published.
                                backupDirty.set(false)
                                true
                            }
                        }
                    }
                } catch (t: Throwable) {
                    backupDirty.set(true)
                    Log.e(TAG, "DB_BACKUP_FAILED", t)
                    scheduleWorker(app, RETRY_DELAY_MS)
                    false
                } finally {
                    temp.delete()
                }
            }
        }
    }

    private fun openAndCheckpoint(dbFile: File): SQLiteDatabase {
        val db = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        try {
            var success = false
            for (attempt in 0 until 3) {
                try {
                    var busy = 0
                    db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor ->
                        if (cursor.moveToFirst() && !cursor.isNull(0)) busy = cursor.getInt(0)
                    }
                    if (busy != 0) throw IOException("SQLite WAL checkpoint is busy")
                    success = true
                    break
                } catch (t: Throwable) {
                    if (attempt == 2) throw t
                    try { Thread.sleep(50L * (attempt + 1)) } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Interrupted while checkpointing database", t)
                    }
                }
            }
            if (!success) throw IOException("SQLite checkpoint failed")
            return db
        } catch (t: Throwable) {
            try { db.close() } catch (_: Throwable) { }
            throw t
        }
    }

    private fun copyAndSync(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        FileInputStream(source).use { input ->
            FileOutputStream(destination, false).use { output ->
                input.copyTo(output, 128 * 1024)
                output.fd.sync()
            }
        }
    }

    private fun writeSharedBackup(context: Context, source: File, counts: Pair<Int, Int>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            val root = File(Environment.getExternalStorageDirectory(), "Android/media/com.altrozdrive.app/AltrozDriveBackup")
            if (!root.exists() && !root.mkdirs()) return false
            val destination = File(root, dailyBackupName())
            return try {
                copyAndSync(source, destination)
                validateBackup(destination)
                writeLegacyInfo(root, destination, counts)
                true
            } catch (t: Throwable) {
                destination.delete()
                Log.w(TAG, "DB_SHARED_LEGACY_BACKUP_FAILED", t)
                false
            }
        }

        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val name = dailyBackupName()
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3")
            put(MediaStore.MediaColumns.RELATIVE_PATH, SHARED_BACKUP_DIR)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = try {
            findMediaStoreUri(resolver, collection, name) ?: resolver.insert(collection, values)
        } catch (t: Throwable) {
            Log.w(TAG, "DB_SHARED_BACKUP_INSERT_FAILED", t)
            null
        } ?: return false

        try {
            // Reuse today's item rather than creating a second backup for the same day.
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 1) }, null, null)

            resolver.openOutputStream(uri, "wt")?.use { output ->
                FileInputStream(source).use { input -> input.copyTo(output, 128 * 1024) }
            } ?: throw IOException("MediaStore backup output unavailable")

            val publishValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, publishValues, null, null)
            val reportedSize = querySize(resolver, uri)
            require(reportedSize == source.length()) { "shared backup size mismatch" }
            writeMediaStoreInfo(resolver, collection, counts, source.length())
            return true
        } catch (t: Throwable) {
            try { resolver.delete(uri, null, null) } catch (_: Throwable) { }
            Log.w(TAG, "DB_SHARED_BACKUP_WRITE_FAILED", t)
            return false
        }
    }

    private fun querySize(resolver: ContentResolver, uri: Uri): Long {
        resolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) return c.getLong(0)
        }
        return -1L
    }

    private fun writeMediaStoreInfo(
        resolver: ContentResolver,
        collection: Uri,
        counts: Pair<Int, Int>,
        sizeBytes: Long
    ) {
        val name = SHARED_INFO_NAME
        val existing = findMediaStoreUri(resolver, collection, name)
        val uri = existing ?: resolver.insert(
            collection,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, SHARED_BACKUP_DIR)
            }
        ) ?: return
        resolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(
                buildString {
                    appendLine("AltrozDrive production DB backup")
                    appendLine("created_at=${nowText()}")
                    appendLine("db_version=${DB_VERSION}")
                    appendLine("vehicle_snapshot_rows=${counts.first}")
                    appendLine("telemetry_sample_rows=${counts.second}")
                    appendLine("size_bytes=$sizeBytes")
                    appendLine("folder=$SHARED_BACKUP_DIR")
                }.toByteArray(Charsets.UTF_8)
            )
        }
    }

    private fun findMediaStoreUri(resolver: ContentResolver, collection: Uri, displayName: String): Uri? {
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            selection,
            arrayOf(displayName, SHARED_BACKUP_DIR),
            null
        )?.use { c ->
            if (c.moveToFirst()) return Uri.withAppendedPath(collection, c.getLong(0).toString())
        }
        return null
    }

    private fun writeLegacyInfo(root: File, destination: File, counts: Pair<Int, Int>) {
        File(root, "backup_info.txt").writeText(
            buildString {
                appendLine("AltrozDrive production DB backup")
                appendLine("created_at=${nowText()}")
                appendLine("db_version=${DB_VERSION}")
                appendLine("vehicle_snapshot_rows=${counts.first}")
                appendLine("telemetry_sample_rows=${counts.second}")
                appendLine("size_bytes=${destination.length()}")
                appendLine("file=${destination.name}")
            }, Charsets.UTF_8
        )
    }

    /**
     * Restores the app-private DB only when it is missing/empty. This is safe to call
     * before VehicleSnapshotDb is constructed on application start.
     */
    fun restoreIfNeeded(context: Context): Boolean {
        val app = context.applicationContext
        val current = app.getDatabasePath(DB_NAME)
        return DB_IO_LOCK.write {
            try {
                if (current.exists() && hasGenuineRows(current)) return@write false

                // First support any legacy app-specific recovery file from the previous
                // release while that installation still has it. This path is not relied
                // upon for uninstall survival on Android 11+.
                val legacy = legacyAppSpecificBackup(app)
                if (legacy != null && legacy.exists() && legacy.length() > 0L && validateBackupQuiet(legacy)) {
                    val restored = restoreFileIntoDatabase(legacy, current)
                    if (restored) return@write true
                }

                // On the same installation, MediaStore may still expose the shared
                // recovery file that this app created. Try it before falling back to
                // Android's automatic cloud/device restore.
                if (restoreFromCurrentInstallSharedBackup(app, current)) {
                    return@write true
                }

                // If Android Auto Backup restored the database itself, the genuine-row
                // check above would have returned earlier. After a normal uninstall on
                // Android 11+, arbitrary old-install shared files may require SAF access;
                // restoreFromUri() is the safe user-selected fallback for that case.
                false
            } catch (t: Throwable) {
                Log.e(TAG, "DB_RESTORE_STARTUP_FAILED", t)
                false
            }
        }
    }

    /**
     * Schedules a backup after process restart when the live database appears newer than
     * the newest shared backup. This closes the gap where a process dies after a DB write
     * but before the asynchronous backup worker runs.
     */
    fun scheduleStartupCatchUp(context: Context) {
        val app = context.applicationContext
        try {
            val db = app.getDatabasePath(DB_NAME)
            if (!db.exists() || !hasGenuineRows(db)) return
            val dbChangedAt = maxOf(
                db.lastModified(),
                File(db.parentFile, "${db.name}-wal").takeIf { it.exists() }?.lastModified() ?: 0L,
                File(db.parentFile, "${db.name}-shm").takeIf { it.exists() }?.lastModified() ?: 0L
            )
            val latestSharedAt = newestSharedBackup(app)?.second ?: 0L
            if (latestSharedAt == 0L || dbChangedAt > latestSharedAt + 1000L) {
                backupDirty.set(true)
                scheduleNightly(app)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "DB_STARTUP_CATCHUP_FAILED", t)
        }
    }

    private fun restoreFromCurrentInstallSharedBackup(app: Context, current: File): Boolean {
        val incoming = File(app.cacheDir, "altrozdrive_restore_shared_candidate.db")
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                val root = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "AltrozDriveBackup"
                )
                val source = root.listFiles { f ->
                    f.isFile && f.name.startsWith(SHARED_PREFIX) && f.name.endsWith(SHARED_SUFFIX)
                }?.maxByOrNull { it.lastModified() } ?: return false
                if (!validateBackupQuiet(source)) return false
                return restoreFileIntoDatabase(source, current)
            }

            val uri = newestSharedBackupUri(app) ?: return false
            app.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(incoming, false).use { output ->
                    input.copyTo(output, 128 * 1024)
                    output.fd.sync()
                }
            } ?: return false
            if (!validateBackupQuiet(incoming)) return false
            restoreFileIntoDatabase(incoming, current)
        } catch (t: Throwable) {
            Log.w(TAG, "DB_RESTORE_SHARED_STARTUP_UNAVAILABLE", t)
            false
        } finally {
            incoming.delete()
        }
    }

    /** Restores a user-selected shared backup through Storage Access Framework. */
    fun restoreFromUri(context: Context, uri: Uri): Boolean {
        val app = context.applicationContext
        val incoming = File(app.cacheDir, "altrozdrive_restore_candidate.db")
        return DB_IO_LOCK.write {
            try {
                if (app.getDatabasePath(DB_NAME).exists() && hasGenuineRows(app.getDatabasePath(DB_NAME))) {
                    return@write false
                }
                app.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(incoming, false).use { output ->
                        input.copyTo(output, 128 * 1024)
                        output.fd.sync()
                    }
                } ?: return@write false
                if (!validateBackupQuiet(incoming)) return@write false
                val restored = restoreFileIntoDatabase(incoming, app.getDatabasePath(DB_NAME))
                incoming.delete()
                restored
            } catch (t: Throwable) {
                Log.e(TAG, "DB_RESTORE_URI_FAILED", t)
                incoming.delete()
                false
            }
        }
    }

    fun scheduleNightly(context: Context) {
        VehicleDbBackupScheduler.schedule(context.applicationContext, NIGHTLY_HOUR, NIGHTLY_MINUTE)
    }

    fun backupInfo(context: Context): BackupInfo {
        val app = context.applicationContext
        return try {
            val shared = newestSharedBackup(app)
            if (shared != null) {
                val (size, modified) = shared
                val db = File(app.getDatabasePath(DB_NAME).absolutePath)
                val counts = if (db.exists()) DB_IO_LOCK.read { openReadOnly(db).use { genuineRowCounts(it) } } else 0 to 0
                return BackupInfo(true, modified, size, counts.first, counts.second, SHARED_BACKUP_DIR)
            }
            BackupInfo(false)
        } catch (_: Throwable) {
            BackupInfo(false)
        }
    }

    private fun newestSharedBackupUri(context: Context): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        return try {
            resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_MODIFIED),
                "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
                arrayOf(SHARED_BACKUP_DIR, "$SHARED_PREFIX%$SHARED_SUFFIX"),
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { c ->
                if (c.moveToFirst()) Uri.withAppendedPath(collection, c.getLong(0).toString()) else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun newestSharedBackup(context: Context): Pair<Long, Long>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            val root = File(Environment.getExternalStorageDirectory(), "Android/media/com.altrozdrive.app/AltrozDriveBackup")
            val file = root.listFiles { f -> f.isFile && f.name.startsWith(SHARED_PREFIX) && f.name.endsWith(SHARED_SUFFIX) }
                ?.maxByOrNull { it.lastModified() } ?: return null
            return file.length() to file.lastModified()
        }
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED),
            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf(SHARED_BACKUP_DIR, "$SHARED_PREFIX%$SHARED_SUFFIX"),
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { c ->
            if (c.moveToFirst()) {
                return (if (!c.isNull(0)) c.getLong(0) else 0L) to (if (!c.isNull(1)) c.getLong(1) * 1000L else 0L)
            }
        }
        return null
    }

    private fun restoreFileIntoDatabase(source: File, current: File): Boolean {
        validateBackup(source)
        val parent = current.parentFile ?: return false
        if (!parent.exists() && !parent.mkdirs()) return false
        val temp = File(parent, "${current.name}.restore.tmp")
        return try {
            copyAndSync(source, temp)
            validateBackup(temp)
            deleteDatabaseSidecars(current)
            replaceAtomically(temp, current)
            true
        } catch (t: Throwable) {
            temp.delete()
            Log.e(TAG, "DB_RESTORE_COPY_FAILED", t)
            false
        }
    }

    private fun replaceAtomically(temp: File, destination: File) {
        try {
            Files.move(
                temp.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Throwable) {
            // Some OEM/filesystem combinations do not support ATOMIC_MOVE. Preserve
            // the temp file until a validated non-atomic replacement succeeds.
            if (!temp.renameTo(destination)) {
                if (destination.exists()) destination.delete()
                require(temp.renameTo(destination)) { "unable to replace restored DB" }
            }
        }
    }

    private fun deleteDatabaseSidecars(db: File) {
        File(db.parentFile, "${db.name}-journal").delete()
        File(db.parentFile, "${db.name}-wal").delete()
        File(db.parentFile, "${db.name}-shm").delete()
    }

    @Suppress("DEPRECATION")
    private fun legacyAppSpecificBackup(context: Context): File? {
        return try {
            val mediaRoot = context.getExternalMediaDirs().firstOrNull() ?: return null
            // The previous release used this location. Keep one-way migration support
            // for upgrades, but do not rely on it for uninstall recovery.
            File(mediaRoot, "AltrozDriveBackup/altrozdrive_vehicle_backup.db")
        } catch (_: Throwable) { null }
    }

    private fun openReadOnly(file: File): SQLiteDatabase =
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

    private fun hasGenuineRows(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        return try {
            openReadOnly(file).use { db ->
                genuineRowCounts(db).let { it.first > 0 || it.second > 0 }
            }
        } catch (_: Throwable) { false }
    }

    private fun genuineRowCounts(db: SQLiteDatabase): Pair<Int, Int> {
        val snapshots = try {
            db.rawQuery(
                "SELECT COUNT(*) FROM vehicle_snapshot WHERE vehicle_key<>? AND source<>?",
                arrayOf(VehicleSnapshotDb.BASELINE_VEHICLE_KEY, VehicleSnapshotDb.BASELINE_SOURCE)
            ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        } catch (_: Throwable) { 0 }
        val telemetry = try {
            db.rawQuery(
                "SELECT COUNT(*) FROM telemetry_sample WHERE vehicle_key<>?",
                arrayOf(VehicleSnapshotDb.BASELINE_VEHICLE_KEY)
            ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        } catch (_: Throwable) { 0 }
        return snapshots to telemetry
    }

    private fun validateBackup(file: File) {
        require(file.exists() && file.length() > 0L) { "backup is empty" }
        openReadOnly(file).use { db ->
            requireSqliteHealthy(db)
            val counts = genuineRowCounts(db)
            require(counts.first > 0 || counts.second > 0) { "backup contains no genuine vehicle data" }
            require(dbVersionOf(db) >= 5) { "unsupported backup schema" }
        }
    }

    private fun validateBackupQuiet(file: File): Boolean = try { validateBackup(file); true } catch (_: Throwable) { false }

    private fun requireSqliteHealthy(db: SQLiteDatabase) {
        val result = db.rawQuery("PRAGMA quick_check", null).use { c ->
            if (c.moveToFirst()) c.getString(0) else ""
        }
        require(result.equals("ok", ignoreCase = true)) { "sqlite quick_check failed: $result" }
        // Verify required tables are present before any copy is published.
        val tables = mutableSetOf<String>()
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
            while (c.moveToNext()) tables += c.getString(0)
        }
        require("vehicle_snapshot" in tables && "telemetry_sample" in tables) { "required DB tables missing" }
    }

    private fun dbVersionOf(db: SQLiteDatabase): Int =
        db.rawQuery("PRAGMA user_version", null).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    private fun dailyBackupName(): String = "${SHARED_PREFIX}${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}$SHARED_SUFFIX"
    private fun timestampForFile(): String = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    private fun nowText(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())

    // Documented schema version for backup validation. VehicleSnapshotDb owns the source of truth.
    private const val DB_VERSION = 5
}
