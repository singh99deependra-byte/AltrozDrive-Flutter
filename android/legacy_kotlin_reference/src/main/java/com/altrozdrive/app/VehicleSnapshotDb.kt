package com.altrozdrive.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.concurrent.TimeUnit
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.*

/**
 * Local last-known VIS cache and real telemetry history.
 *
 * The Bluetooth/HTS/X451 layer remains the source of validated values. This
 * class only persists those validated values and never invents vehicle data.
 */
class VehicleSnapshotDb(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DB_NAME,
    null,
    DB_VERSION
) {
    private val appContext = context.applicationContext

    data class TelemetrySample(
        val timestamp: Long,
        val vehicleKey: String,
        val odo: Int?,
        val fuelPercent: Int?,
        val dteKm: Int?,
        val tripA: Float?,
        val tripB: Float?,
        val avgA: Float?,
        val avgB: Float?
    )

    data class TripRecord(
        val id: Long,
        val vehicleKey: String,
        val startAt: Long,
        val endAt: Long?,
        val startOdo: Int?,
        val endOdo: Int?,
        val distanceKm: Float,
        val durationSec: Long,
        val avgEconomy: Float?,
        val startTripA: Float?,
        val endTripA: Float?,
        val startTripB: Float?,
        val endTripB: Float?,
        val startFuel: Int?,
        val endFuel: Int?,
        val active: Boolean
    )

    data class Snapshot(
        val vehicleKey: String,
        val uid: String? = null,
        val odo: Int? = null,
        val fuelPercent: Int? = null,
        val dteKm: Int? = null,
        val tripA: Float? = null,
        val tripB: Float? = null,
        val avgA: Float? = null,
        val avgB: Float? = null,
        val updatedAt: Long = 0L,
        val source: String = "LOCAL_DB"
    )

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // WAL keeps the high-frequency telemetry writer from unnecessarily
        // blocking Compose read queries. Backup checkpointing is coordinated by
        // VehicleDbBackup.DB_IO_LOCK, so no backup race is introduced here.
        try { db.enableWriteAheadLogging() } catch (_: Throwable) { }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE vehicle_snapshot (
                vehicle_key TEXT PRIMARY KEY,
                uid TEXT,
                last_known_odometer INTEGER,
                last_known_fuel_level REAL,
                last_known_dte REAL,
                last_known_tripA REAL,
                last_known_tripB REAL,
                last_known_avgA REAL,
                last_known_avgB REAL,
                updated_at INTEGER NOT NULL,
                source TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_vehicle_snapshot_updated ON vehicle_snapshot(updated_at DESC)")
        db.execSQL(
            "CREATE TABLE telemetry_sample (timestamp INTEGER NOT NULL, vehicle_key TEXT NOT NULL, " +
                "odo INTEGER, fuel_percent REAL, dte_km REAL, trip_a REAL, trip_b REAL, " +
                "avg_a REAL, avg_b REAL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_telemetry_sample_vehicle_time ON telemetry_sample(vehicle_key, timestamp DESC)")
        db.execSQL("CREATE TABLE drive_trip (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_key TEXT NOT NULL, start_at INTEGER NOT NULL, end_at INTEGER, start_odo INTEGER, end_odo INTEGER, distance_km REAL NOT NULL DEFAULT 0, duration_sec INTEGER NOT NULL DEFAULT 0, avg_economy REAL, start_trip_a REAL, end_trip_a REAL, start_trip_b REAL, end_trip_b REAL, start_fuel INTEGER, end_fuel INTEGER, last_movement_at INTEGER NOT NULL, active INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE INDEX idx_drive_trip_vehicle_time ON drive_trip(vehicle_key, start_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_drive_trip_active_vehicle ON drive_trip(vehicle_key, active, start_at DESC)")
        db.execSQL("CREATE TABLE route_point (id INTEGER PRIMARY KEY AUTOINCREMENT, trip_id INTEGER NOT NULL, timestamp INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL)")
        db.execSQL("CREATE INDEX idx_route_point_trip_time ON route_point(trip_id, timestamp ASC)")
        db.execSQL("CREATE TABLE IF NOT EXISTS pending_route_point (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_key TEXT NOT NULL, timestamp INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pending_route_vehicle_time ON pending_route_point(vehicle_key, timestamp ASC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS telemetry_sample (timestamp INTEGER NOT NULL, vehicle_key TEXT NOT NULL, " +
                    "odo INTEGER, fuel_percent REAL, dte_km REAL, trip_a REAL, trip_b REAL, " +
                    "avg_a REAL, avg_b REAL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_telemetry_sample_vehicle_time ON telemetry_sample(vehicle_key, timestamp DESC)")
        }
        if (oldVersion < 4) {
            // Remove only the synthetic 5.9.8.24 baseline. Genuine snapshots/history remain.
            db.delete("vehicle_snapshot", "vehicle_key=? OR source=?", arrayOf(BASELINE_VEHICLE_KEY, BASELINE_SOURCE))
            db.delete("telemetry_sample", "vehicle_key=?", arrayOf(BASELINE_VEHICLE_KEY))
        }
        if (oldVersion < 5) {
            // 1.0.0 production persistence release. No schema change required.
        }
        if (oldVersion < 6) {
            db.execSQL("CREATE TABLE IF NOT EXISTS drive_trip (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_key TEXT NOT NULL, start_at INTEGER NOT NULL, end_at INTEGER, start_odo INTEGER, end_odo INTEGER, distance_km REAL NOT NULL DEFAULT 0, duration_sec INTEGER NOT NULL DEFAULT 0, avg_economy REAL, start_trip_a REAL, end_trip_a REAL, start_trip_b REAL, end_trip_b REAL, start_fuel INTEGER, end_fuel INTEGER, active INTEGER NOT NULL DEFAULT 1)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_drive_trip_vehicle_time ON drive_trip(vehicle_key, start_at DESC)")
        }
        if (oldVersion < 8) {
            db.execSQL("ALTER TABLE drive_trip ADD COLUMN last_movement_at INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE drive_trip SET last_movement_at=COALESCE(end_at,start_at) WHERE last_movement_at=0")
        }
        if (oldVersion < 7) {
            db.execSQL("CREATE TABLE IF NOT EXISTS route_point (id INTEGER PRIMARY KEY AUTOINCREMENT, trip_id INTEGER NOT NULL, timestamp INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_route_point_trip_time ON route_point(trip_id, timestamp ASC)")
        }
        if (oldVersion < 9) {
            // DB 8 -> 9: add only the buffered GPS table. Existing telemetry,
            // trips and route points are intentionally preserved.
            db.execSQL("CREATE TABLE IF NOT EXISTS pending_route_point (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_key TEXT NOT NULL, timestamp INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_pending_route_vehicle_time ON pending_route_point(vehicle_key, timestamp ASC)")
        }
        if (oldVersion < 10) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_drive_trip_active_vehicle ON drive_trip(vehicle_key, active, start_at DESC)")
        }
    }

    /** Compatibility no-op retained so older callers cannot reintroduce fake baseline data. */
    fun ensureSeeded() = Unit

    fun load(vehicleKey: String): Snapshot? =
        VehicleDbBackup.DB_IO_LOCK.read {
            readableDatabase.query(
                "vehicle_snapshot",
                null,
                "vehicle_key=?",
                arrayOf(vehicleKey),
                null,
                null,
                null
            ).use { cursor -> if (cursor.moveToFirst()) cursor.toSnapshot() else null }
        }

    fun loadMostRecent(): Snapshot? =
        VehicleDbBackup.DB_IO_LOCK.read {
            readableDatabase.query(
                "vehicle_snapshot",
                null,
                "vehicle_key<>? AND source<>?",
                arrayOf(BASELINE_VEHICLE_KEY, BASELINE_SOURCE),
                null,
                null,
                "updated_at DESC",
                "1"
            ).use { cursor -> if (cursor.moveToFirst()) cursor.toSnapshot() else null }
        }

    /**
     * Atomically merges a validated VIS update into the last-known row.
     * A failed SQLite insert throws instead of being reported as a successful save.
     */
    fun mergeAndSave(
        vehicleKey: String,
        uid: String?,
        odo: Int?,
        fuelPercent: Int?,
        dteKm: Int?,
        tripA: Float?,
        tripB: Float?,
        avgA: Float?,
        avgB: Float?,
        source: String
    ): Snapshot {
        require(vehicleKey.isNotBlank()) { "vehicleKey must not be blank" }
        require(source.isNotBlank()) { "source must not be blank" }
        require(vehicleKey != BASELINE_VEHICLE_KEY) { "reserved baseline vehicle key is not writable" }
        require(source != BASELINE_SOURCE) { "reserved baseline source is not writable" }

        val merged = VehicleDbBackup.DB_IO_LOCK.write {
            val db = writableDatabase
            val old = querySnapshot(db, vehicleKey)
            val result = Snapshot(
                vehicleKey = vehicleKey,
                uid = uid ?: old?.uid,
                odo = odo ?: old?.odo,
                fuelPercent = fuelPercent ?: old?.fuelPercent,
                dteKm = dteKm ?: old?.dteKm,
                tripA = tripA ?: old?.tripA,
                tripB = tripB ?: old?.tripB,
                avgA = avgA ?: old?.avgA,
                avgB = avgB ?: old?.avgB,
                updatedAt = System.currentTimeMillis(),
                source = source
            )
            val values = ContentValues().apply {
                put("vehicle_key", result.vehicleKey)
                putOrNull("uid", result.uid)
                putOrNull("last_known_odometer", result.odo)
                putOrNull("last_known_fuel_level", result.fuelPercent)
                putOrNull("last_known_dte", result.dteKm)
                putOrNull("last_known_tripA", result.tripA)
                putOrNull("last_known_tripB", result.tripB)
                putOrNull("last_known_avgA", result.avgA)
                putOrNull("last_known_avgB", result.avgB)
                put("updated_at", result.updatedAt)
                put("source", result.source)
            }
            db.beginTransaction()
            try {
                // vehicle_key is the primary key. Replacing the existing snapshot row
                // is intentional: this is a last-known-value cache, not append-only data.
                db.insertWithOnConflict(
                    "vehicle_snapshot",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            result
        }

        VehicleDbBackup.requestBackup(appContext)
        return merged
    }

    /**
     * Stores at most one historical telemetry checkpoint per vehicle/day.
     * Live VIS frames remain live-only; historical storage is intentionally coarse.
     */
    fun appendTelemetrySampleIfDayMissing(sample: TelemetrySample) {
        require(sample.vehicleKey.isNotBlank()) { "vehicleKey must not be blank" }
        require(sample.timestamp > 0L) { "timestamp must be positive" }
        require(sample.vehicleKey != BASELINE_VEHICLE_KEY) { "reserved baseline vehicle key is not writable" }
        val dayStart = java.util.Calendar.getInstance().apply {
            timeInMillis = sample.timestamp
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val nextDay = dayStart + TimeUnit.DAYS.toMillis(1)
        val inserted = VehicleDbBackup.DB_IO_LOCK.write {
            val db = writableDatabase
            val exists = db.query(
                "telemetry_sample", arrayOf("timestamp"),
                "vehicle_key=? AND timestamp>=? AND timestamp<?",
                arrayOf(sample.vehicleKey, dayStart.toString(), nextDay.toString()),
                null, null, "timestamp DESC", "1"
            ).use { it.moveToFirst() }
            if (exists) false else {
                val values = ContentValues().apply {
                    put("timestamp", sample.timestamp); put("vehicle_key", sample.vehicleKey)
                    putOrNull("odo", sample.odo); putOrNull("fuel_percent", sample.fuelPercent)
                    putOrNull("dte_km", sample.dteKm); putOrNull("trip_a", sample.tripA)
                    putOrNull("trip_b", sample.tripB); putOrNull("avg_a", sample.avgA); putOrNull("avg_b", sample.avgB)
                }
                db.insertOrThrow("telemetry_sample", null, values)
                true
            }
        }
        if (inserted) VehicleDbBackup.requestBackup(appContext)
    }

    /** Updates today's single historical checkpoint when a trip/session ends. */
    fun upsertDailyTelemetrySample(sample: TelemetrySample) {
        require(sample.vehicleKey.isNotBlank()) { "vehicleKey must not be blank" }
        require(sample.timestamp > 0L) { "timestamp must be positive" }
        if (sample.vehicleKey == BASELINE_VEHICLE_KEY) return
        val dayStart = java.util.Calendar.getInstance().apply {
            timeInMillis = sample.timestamp
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val nextDay = dayStart + TimeUnit.DAYS.toMillis(1)
        val changed = VehicleDbBackup.DB_IO_LOCK.write {
            val db = writableDatabase
            val rowId = db.query(
                "telemetry_sample", arrayOf("rowid"),
                "vehicle_key=? AND timestamp>=? AND timestamp<?",
                arrayOf(sample.vehicleKey, dayStart.toString(), nextDay.toString()),
                null, null, "timestamp DESC", "1"
            ).use { c -> if (c.moveToFirst()) c.getLong(0) else -1L }
            val values = ContentValues().apply {
                put("timestamp", sample.timestamp); put("vehicle_key", sample.vehicleKey)
                putOrNull("odo", sample.odo); putOrNull("fuel_percent", sample.fuelPercent)
                putOrNull("dte_km", sample.dteKm); putOrNull("trip_a", sample.tripA)
                putOrNull("trip_b", sample.tripB); putOrNull("avg_a", sample.avgA); putOrNull("avg_b", sample.avgB)
            }
            if (rowId > 0L) db.update("telemetry_sample", values, "rowid=?", arrayOf(rowId.toString())) > 0
            else db.insertOrThrow("telemetry_sample", null, values) > 0L
        }
        if (changed) VehicleDbBackup.requestBackup(appContext)
    }

    /** Returns one latest real telemetry checkpoint per local calendar day. */
    fun recentTelemetryDailyAny(since: Long = 0L, limitDays: Int = 366): List<TelemetrySample> {
        val safeLimit = limitDays.coerceIn(1, 366)
        return VehicleDbBackup.DB_IO_LOCK.read {
            readableDatabase.rawQuery(
                """
                SELECT t.* FROM telemetry_sample t
                INNER JOIN (
                    SELECT vehicle_key, date(timestamp / 1000, 'unixepoch', 'localtime') AS day_key, MAX(timestamp) AS max_ts
                    FROM telemetry_sample
                    WHERE vehicle_key<>? AND timestamp>=?
                    GROUP BY vehicle_key, day_key
                ) d ON d.vehicle_key=t.vehicle_key AND d.max_ts=t.timestamp
                ORDER BY t.timestamp ASC LIMIT ?
                """.trimIndent(),
                arrayOf(BASELINE_VEHICLE_KEY, since.toString(), safeLimit.toString())
            ).use { readSamples(it) }
        }
    }

    fun recentTelemetryAny(since: Long = 0L, limit: Int = 240): List<TelemetrySample> {
        val safeLimit = limit.coerceIn(1, 5000)
        return VehicleDbBackup.DB_IO_LOCK.read {
            readableDatabase.query(
                "telemetry_sample", null, "vehicle_key<>? AND timestamp>=?",
                arrayOf(BASELINE_VEHICLE_KEY, since.toString()),
                null, null, "timestamp DESC", safeLimit.toString()
            ).use { cursor -> readSamples(cursor).asReversed() }
        }
    }

    fun recentTelemetry(vehicleKey: String, since: Long = 0L, limit: Int = 240): List<TelemetrySample> {
        val safeLimit = limit.coerceIn(1, 5000)
        return VehicleDbBackup.DB_IO_LOCK.read {
            readableDatabase.query(
                "telemetry_sample", null, "vehicle_key=? AND vehicle_key<>? AND timestamp>=?",
                arrayOf(vehicleKey, BASELINE_VEHICLE_KEY, since.toString()), null, null,
                "timestamp ASC", safeLimit.toString()
            ).use { cursor -> readSamples(cursor) }
        }
    }

    fun recentTripsAny(limit: Int = 50): List<TripRecord> {
        val safeLimit = limit.coerceIn(1, 5000)
        // Defense-in-depth: the UI/history refresh is also a safe finalization boundary.
        // This prevents a stale active trip from remaining visible as LIVE if the
        // process-lifetime watchdog was delayed, paused, or missed by the current process.
        // It does not end a trip early: finalizeStaleActiveTrips() requires a full
        // TRIP_IDLE_TIMEOUT_MS since the last physical movement.
        finalizeStaleActiveTrips(System.currentTimeMillis(), TRIP_IDLE_TIMEOUT_MS)
        return VehicleDbBackup.DB_IO_LOCK.read {
            readableDatabase.query("drive_trip", null, "vehicle_key<>?", arrayOf(BASELINE_VEHICLE_KEY), null, null, "start_at DESC", safeLimit.toString()).use { readTrips(it) }
        }
    }

    /** Returns completed real-drive sessions only, newest first. */
    fun completedTrips(limit: Int = 1000): List<TripRecord> {
        val safeLimit = limit.coerceIn(1, 5000)
        return VehicleDbBackup.DB_IO_LOCK.read {
            readableDatabase.query(
                "drive_trip", null, "vehicle_key<>? AND active=0",
                arrayOf(BASELINE_VEHICLE_KEY), null, null, "start_at DESC", safeLimit.toString()
            ).use { readTrips(it) }
        }
    }

    fun findTrip(id: Long): TripRecord? = VehicleDbBackup.DB_IO_LOCK.read {
        readableDatabase.query(
            "drive_trip", null, "id=? AND vehicle_key<>?",
            arrayOf(id.toString(), BASELINE_VEHICLE_KEY), null, null, null, "1"
        ).use { if (it.moveToFirst()) it.toTripRecord() else null }
    }

    fun recordDriveSample(
        vehicleKey: String, timestamp: Long, odo: Int?, tripA: Float?, tripB: Float?,
        avgEconomy: Float?, fuel: Int?, movementConfirmed: Boolean? = null
    ) {
        if (vehicleKey.isBlank() || vehicleKey == BASELINE_VEHICLE_KEY) return
        VehicleDbBackup.DB_IO_LOCK.write {
            val db = writableDatabase
            val active = db.query("drive_trip", null, "vehicle_key=? AND active=1", arrayOf(vehicleKey), null, null, "start_at DESC", "1").use { c -> if (c.moveToFirst()) c.toTripRecord() else null }
            if (active == null) return@write
            val previousOdo = active.endOdo ?: active.startOdo
            val previousA = active.endTripA ?: active.startTripA
            val previousB = active.endTripB ?: active.startTripB
            // Movement decisions are supplied by the live decoder when it knows which
            // values came from a fresh X451 frame. Older callers retain this conservative
            // fallback, but both ODO and fractional Trip A/B deltas are considered.
            val moved = movementConfirmed ?: (
                (odo != null && previousOdo != null && odo > previousOdo) ||
                    (tripA != null && previousA != null && tripA - previousA >= 0.05f) ||
                    (tripB != null && previousB != null && tripB - previousB >= 0.05f)
                )
            // X451 can create the trip from Trip A/B movement before the ODO-bearing
            // 234-byte snapshot arrives. If that happens, backfill start_odo from the
            // first valid ODO received immediately after trip creation. This is deliberately
            // time-bounded so a much-later ODO sample can never become a false trip start.
            val backfillStartOdo = active.startOdo == null &&
                odo != null &&
                timestamp >= active.startAt &&
                timestamp - active.startAt <= START_ODO_BACKFILL_WINDOW_MS
            val effectiveStartOdo = if (backfillStartOdo) odo else active.startOdo
            val odoDistance = if (effectiveStartOdo != null && odo != null && odo >= effectiveStartOdo)
                (odo - effectiveStartOdo).toFloat() else null
            val tripADistance = if (active.startTripA != null && tripA != null && tripA >= active.startTripA)
                tripA - active.startTripA else null
            val tripBDistance = if (active.startTripB != null && tripB != null && tripB >= active.startTripB)
                tripB - active.startTripB else null
            // Trip A/B carry 0.1-km precision while ODO is integer-only. Use the
            // trip counters when available so a 100–900 m drive is not flattened to
            // 0 km or rounded to a full kilometre; fall back to ODO after a counter reset.
            val computedDistance = tripADistance ?: tripBDistance ?: odoDistance ?: active.distanceKm
            // A transiently stale/reset trip counter must never make the live distance move
            // backwards; retain the furthest validated distance observed for this trip.
            val distance = maxOf(active.distanceKm, computedDistance, 0f)
            if (moved || timestamp - active.startAt > 0L || backfillStartOdo) {
                val duration = ((timestamp - active.startAt) / 1000L).coerceAtLeast(0L)
                val values = ContentValues().apply {
                    if (backfillStartOdo) put("start_odo", odo)
                    put("end_at", timestamp)
                    putOrNull("end_odo", odo ?: active.endOdo)
                    put("distance_km", distance)
                    put("duration_sec", duration)
                    putOrNull("avg_economy", avgEconomy ?: active.avgEconomy)
                    putOrNull("end_trip_a", tripA ?: active.endTripA)
                    putOrNull("end_trip_b", tripB ?: active.endTripB)
                    putOrNull("end_fuel", fuel ?: active.endFuel)
                    if (moved) put("last_movement_at", timestamp)
                }
                db.update("drive_trip", values, "id=?", arrayOf(active.id.toString()))
            }
        }
    }

    fun startDriveIfNeeded(vehicleKey: String, timestamp: Long, odo: Int?, tripA: Float?, tripB: Float?, fuel: Int?) {
        if (vehicleKey.isBlank() || vehicleKey == BASELINE_VEHICLE_KEY) return
        VehicleDbBackup.DB_IO_LOCK.write {
            val db = writableDatabase
            val activeRow = db.query("drive_trip", arrayOf("id", "last_movement_at"), "vehicle_key=? AND active=1", arrayOf(vehicleKey), null, null, "start_at DESC", "1").use {
                if (it.moveToFirst()) it.getLong(0) to it.getLong(1) else null
            }
            if (activeRow != null) {
                val stale = timestamp - activeRow.second >= TRIP_IDLE_TIMEOUT_MS
                if (!stale) return@write
                db.update("drive_trip", ContentValues().apply {
                    put("active", 0); put("end_at", activeRow.second);
                    put("duration_sec", ((activeRow.second - db.query("drive_trip", arrayOf("start_at"), "id=?", arrayOf(activeRow.first.toString()), null, null, null, "1").use { if (it.moveToFirst()) it.getLong(0) else timestamp }) / 1000L).coerceAtLeast(0L))
                }, "id=?", arrayOf(activeRow.first.toString()))
            }
            val values = ContentValues().apply {
                put("vehicle_key", vehicleKey); put("start_at", timestamp); put("start_odo", odo); put("start_trip_a", tripA); put("start_trip_b", tripB); put("start_fuel", fuel); put("last_movement_at", timestamp); put("active", 1); put("distance_km", 0f); put("duration_sec", 0L)
            }
            val tripId = db.insertOrThrow("drive_trip", null, values)
            // Retain pending points for up to 48 hours in storage, but only attach a
            // short lead-in to a newly detected trip. Attaching the full retention window
            // mixed yesterday's routes/parking points into today's current-trip polyline.
            val cutoff = timestamp - ROUTE_PRESTART_ATTACH_WINDOW_MS
            db.query(
                "pending_route_point", arrayOf("timestamp", "latitude", "longitude"),
                "vehicle_key=? AND timestamp>=? AND timestamp<=?",
                arrayOf(vehicleKey, cutoff.toString(), timestamp.toString()), null, null, "timestamp ASC"
            ).use { c ->
                while (c.moveToNext()) {
                    val routeValues = ContentValues().apply {
                        put("trip_id", tripId); put("timestamp", c.getLong(0)); put("latitude", c.getDouble(1)); put("longitude", c.getDouble(2))
                    }
                    db.insertOrThrow("route_point", null, routeValues)
                }
            }
            db.delete("pending_route_point", "vehicle_key=? AND timestamp<=?", arrayOf(vehicleKey, timestamp.toString()))
        }
        VehicleDbBackup.requestBackup(appContext)
    }

    fun recordRoutePointForVehicle(vehicleKey: String, timestamp: Long, latitude: Double, longitude: Double) {
        if (vehicleKey.isBlank() || vehicleKey == BASELINE_VEHICLE_KEY) return
        if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return
        VehicleDbBackup.DB_IO_LOCK.write {
            val db = writableDatabase
            // Prefer the exact vehicle key, but if the Bluetooth selected-device state
            // changed during a reconnect, bind GPS to the single current active trip.
            // Route recording must follow the trip lifecycle rather than depend on a
            // transient Bluetooth UI state. This does not change Bluetooth/decoder logic.
            var tripId = db.query("drive_trip", arrayOf("id"), "vehicle_key=? AND active=1", arrayOf(vehicleKey), null, null, "start_at DESC", "1").use { c -> if (c.moveToFirst()) c.getLong(0) else -1L }
            if (tripId <= 0L) {
                tripId = db.query("drive_trip", arrayOf("id"), "active=1", null, null, null, "start_at DESC", "1").use { c -> if (c.moveToFirst()) c.getLong(0) else -1L }
            }
            if (tripId <= 0L) {
                val pendingLast = db.query(
                    "pending_route_point", arrayOf("latitude", "longitude", "timestamp"),
                    "vehicle_key=?", arrayOf(vehicleKey), null, null, "timestamp DESC", "1"
                ).use { c -> if (c.moveToFirst()) Triple(c.getDouble(0), c.getDouble(1), c.getLong(2)) else null }
                if (pendingLast != null) {
                    if (timestamp <= pendingLast.third) return@write
                    val distanceMeters = routeDistanceMeters(pendingLast.first, pendingLast.second, latitude, longitude)
                    val elapsedSeconds = (timestamp - pendingLast.third) / 1000.0
                    if (elapsedSeconds > 0.0 && distanceMeters > ROUTE_MAX_SPEED_MPS * elapsedSeconds + ROUTE_JUMP_TOLERANCE_METERS) return@write
                    if (distanceMeters < 15.0 && timestamp - pendingLast.third < 5000L) {
                        db.delete("pending_route_point", "vehicle_key=? AND timestamp<?", arrayOf(vehicleKey, (timestamp - ROUTE_PENDING_RETENTION_MS).toString()))
                        return@write
                    }
                }
                val pending = ContentValues().apply { put("vehicle_key", vehicleKey); put("timestamp", timestamp); put("latitude", latitude); put("longitude", longitude) }
                db.insertOrThrow("pending_route_point", null, pending)
                db.delete("pending_route_point", "vehicle_key=? AND timestamp<?", arrayOf(vehicleKey, (timestamp - ROUTE_PENDING_RETENTION_MS).toString()))
                return@write
            }
            val last = db.query("route_point", arrayOf("latitude", "longitude", "timestamp"), "trip_id=?", arrayOf(tripId.toString()), null, null, "timestamp DESC", "1").use { c ->
                if (c.moveToFirst()) Triple(c.getDouble(0), c.getDouble(1), c.getLong(2)) else null
            }
            if (last != null) {
                if (timestamp <= last.third) return@write
                val distanceMeters = routeDistanceMeters(last.first, last.second, latitude, longitude)
                val elapsedSeconds = (timestamp - last.third) / 1000.0
                if (elapsedSeconds > 0.0 && distanceMeters > ROUTE_MAX_SPEED_MPS * elapsedSeconds + ROUTE_JUMP_TOLERANCE_METERS) return@write
                if (distanceMeters < 15.0 && timestamp - last.third < 5000L) return@write
            }
            val values = ContentValues().apply { put("trip_id", tripId); put("timestamp", timestamp); put("latitude", latitude); put("longitude", longitude) }
            db.insertOrThrow("route_point", null, values)
        }
        VehicleDbBackup.requestBackup(appContext)
    }

    fun routePoints(tripId: Long, limit: Int = 5000): List<Pair<Double, Double>> {
        val safeLimit = limit.coerceIn(1, 10000)
        return VehicleDbBackup.DB_IO_LOCK.read {
            val db = readableDatabase
            val tripStartAt = db.query(
                "drive_trip", arrayOf("start_at"), "id=?", arrayOf(tripId.toString()),
                null, null, null, "1"
            ).use { c -> if (c.moveToFirst()) c.getLong(0) else null }
            val minRouteTimestamp = tripStartAt?.minus(ROUTE_PRESTART_ATTACH_WINDOW_MS)
            db.query("route_point", arrayOf("timestamp", "latitude", "longitude"), "trip_id=?", arrayOf(tripId.toString()), null, null, "timestamp ASC", safeLimit.toString()).use { c ->
                val out = ArrayList<Pair<Double, Double>>()
                var lastLat: Double? = null
                var lastLon: Double? = null
                var lastTimestamp: Long? = null
                while (c.moveToNext()) {
                    val timestamp = c.getLong(0)
                    // Apply the same trip-boundary rule when rendering data recorded by
                    // older app versions. This makes contaminated existing trip polylines
                    // clean up immediately without deleting or rewriting stored history.
                    if (minRouteTimestamp != null && timestamp < minRouteTimestamp) continue
                    val lat = c.getDouble(1)
                    val lon = c.getDouble(2)
                    if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) continue
                    val pLat = lastLat
                    val pLon = lastLon
                    val pTime = lastTimestamp
                    if (pLat != null && pLon != null && pTime != null) {
                        if (timestamp <= pTime) continue
                        val distance = routeDistanceMeters(pLat, pLon, lat, lon)
                        val elapsedSeconds = (timestamp - pTime) / 1000.0
                        // Defensive display filter for routes recorded by previous versions,
                        // which stamped stale last-known coordinates with the current time.
                        if (elapsedSeconds > 0.0 && distance > ROUTE_MAX_SPEED_MPS * elapsedSeconds + ROUTE_JUMP_TOLERANCE_METERS) continue
                    }
                    out += lat to lon
                    lastLat = lat
                    lastLon = lon
                    lastTimestamp = timestamp
                }
                out
            }
        }
    }

    private fun routeDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2.0).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2.0).pow(2)
        return 6_371_000.0 * 2.0 * atan2(sqrt(a.coerceIn(0.0, 1.0)), sqrt((1.0 - a).coerceIn(0.0, 1.0)))
    }

    fun routePointCount(tripId: Long): Int = VehicleDbBackup.DB_IO_LOCK.read {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM route_point WHERE trip_id=?", arrayOf(tripId.toString())).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    fun finalizeStaleActiveTrips(timestamp: Long = System.currentTimeMillis(), idleTimeoutMs: Long = TRIP_IDLE_TIMEOUT_MS): Int {
        val finalized = VehicleDbBackup.DB_IO_LOCK.write {
            val db = writableDatabase
            // IMPORTANT: SQLite selectionArgs are bound as TEXT. The previous
            // "?-last_movement_at>=?" predicate therefore failed to match stale rows
            // reliably. Compute the numeric cutoff first and compare it to the INTEGER
            // last_movement_at column. This is the process-independent finalization path
            // used after Bluetooth disconnect and by the history refresh.
            val cutoff = timestamp - idleTimeoutMs
            val count = db.query(
                "drive_trip", arrayOf("id"),
                "active=1 AND last_movement_at<=?",
                arrayOf(cutoff.toString()),
                null, null, null
            ).use { c -> c.count }
            if (count > 0) {
                db.execSQL(
                    "UPDATE drive_trip SET active=0, end_at=last_movement_at, " +
                        "duration_sec=MAX(0, (last_movement_at-start_at)/1000) " +
                        "WHERE active=1 AND last_movement_at<=?",
                    arrayOf(cutoff)
                )
            }
            count
        }
        if (finalized > 0) VehicleDbBackup.requestBackup(appContext)
        return finalized
    }

    fun finalizeActiveTrip(vehicleKey: String, timestamp: Long = System.currentTimeMillis()) {
        if (vehicleKey.isBlank() || vehicleKey == BASELINE_VEHICLE_KEY) return
        VehicleDbBackup.DB_IO_LOCK.write {
            val db = writableDatabase
            db.query("drive_trip", null, "vehicle_key=? AND active=1", arrayOf(vehicleKey), null, null, "start_at DESC", "1").use { c ->
                if (!c.moveToFirst()) return@use
                val i = { name: String -> c.getColumnIndexOrThrow(name) }
                fun intOrNull(name: String): Int? = if (c.isNull(i(name))) null else c.getInt(i(name))
                fun floatOrNull(name: String): Float? = if (c.isNull(i(name))) null else c.getFloat(i(name))
                val startAt = c.getLong(i("start_at"))
                val lastMovementAt = c.getLong(i("last_movement_at"))
                // A Bluetooth/socket close is not proof that the physical drive ended.
                // Only finalize here when the trip has already been inactive long enough.
                if (timestamp - lastMovementAt < TRIP_IDLE_TIMEOUT_MS) return@use
                val endAt = lastMovementAt
                val values = ContentValues().apply {
                    put("active", 0)
                    put("end_at", endAt)
                    put("end_odo", intOrNull("end_odo"))
                    put("end_trip_a", floatOrNull("end_trip_a"))
                    put("end_trip_b", floatOrNull("end_trip_b"))
                    put("end_fuel", intOrNull("end_fuel"))
                    put("duration_sec", ((endAt - startAt) / 1000L).coerceAtLeast(0L))
                }
                db.update("drive_trip", values, "id=?", arrayOf(c.getLong(i("id")).toString()))
            }
        }
        VehicleDbBackup.requestBackup(appContext)
    }

    fun maybeFinalizeInactiveTrip(
        vehicleKey: String,
        timestamp: Long,
        odo: Int?,
        tripA: Float?,
        tripB: Float?,
        idleTimeoutMs: Long = TRIP_IDLE_TIMEOUT_MS
    ): Boolean {
        if (vehicleKey.isBlank() || vehicleKey == BASELINE_VEHICLE_KEY) return false
        val shouldFinalize = VehicleDbBackup.DB_IO_LOCK.read {
            readableDatabase.query("drive_trip", arrayOf("id", "last_movement_at", "end_odo", "end_trip_a", "end_trip_b"), "vehicle_key=? AND active=1", arrayOf(vehicleKey), null, null, "start_at DESC", "1").use { c ->
                if (!c.moveToFirst()) return@use false
                val sameOdo = odo == null || c.isNull(2) || odo == c.getInt(2)
                // If ODO is known, Trip A/B drift is irrelevant to physical inactivity.
                // Only use Trip A/B equality when no ODO endpoint is available at all.
                val sameTripCounters = if (!c.isNull(2) || odo != null) {
                    true
                } else {
                    val sameA = tripA == null || c.isNull(3) || kotlin.math.abs(tripA - c.getFloat(3)) <= 0.05f
                    val sameB = tripB == null || c.isNull(4) || kotlin.math.abs(tripB - c.getFloat(4)) <= 0.05f
                    sameA && sameB
                }
                timestamp - c.getLong(1) >= idleTimeoutMs && sameOdo && sameTripCounters
            }
        }
        if (shouldFinalize) {
            finalizeActiveTrip(vehicleKey, timestamp)
            return true
        }
        return false
    }

    fun purgeTelemetryOlderThan(cutoff: Long) {
        require(cutoff > 0L) { "cutoff must be positive" }
        val deleted = VehicleDbBackup.DB_IO_LOCK.write {
            writableDatabase.delete("telemetry_sample", "timestamp<?", arrayOf(cutoff.toString()))
        }
        if (deleted > 0) VehicleDbBackup.requestBackup(appContext)
    }

    /** Purges historical telemetry, trips and route points beyond the requested retention window. */
    fun purgeHistoryOlderThanDays(days: Long = 180L) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.coerceAtLeast(1L))
        val changed = VehicleDbBackup.DB_IO_LOCK.write {
            val db = writableDatabase
            db.beginTransaction()
            try {
                // Never purge an active trip, even if a malformed/very-long session
                // somehow spans the retention boundary. Only completed history is aged out.
                db.delete("route_point", "trip_id IN (SELECT id FROM drive_trip WHERE start_at<? AND active=0)", arrayOf(cutoff.toString()))
                db.delete("drive_trip", "start_at<? AND active=0", arrayOf(cutoff.toString()))
                db.delete("telemetry_sample", "timestamp<?", arrayOf(cutoff.toString()))
                db.delete("pending_route_point", "timestamp<?", arrayOf((System.currentTimeMillis() - ROUTE_PENDING_RETENTION_MS).toString()))
                db.setTransactionSuccessful()
                true
            } finally { db.endTransaction() }
        }
        if (changed) VehicleDbBackup.requestBackup(appContext)
    }

    /** Performs bounded cleanup without touching the live Bluetooth path. */
    fun purgeTelemetryOlderThanDays(days: Long) {
        purgeHistoryOlderThanDays(days)
    }

    private fun querySnapshot(db: SQLiteDatabase, vehicleKey: String): Snapshot? =
        db.query(
            "vehicle_snapshot", null, "vehicle_key=?", arrayOf(vehicleKey), null, null, null
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toSnapshot() else null }

    private fun readTrips(cursor: android.database.Cursor): List<TripRecord> {
        val out = ArrayList<TripRecord>()
        while (cursor.moveToNext()) out += cursor.toTripRecord()
        return out
    }

    private fun android.database.Cursor.toTripRecord(): TripRecord {
        fun i(name:String): Int? { val x=getColumnIndex(name); return if(x>=0 && !isNull(x)) getInt(x) else null }
        fun l(name:String): Long? { val x=getColumnIndex(name); return if(x>=0 && !isNull(x)) getLong(x) else null }
        fun f(name:String): Float? { val x=getColumnIndex(name); return if(x>=0 && !isNull(x)) getFloat(x) else null }
        return TripRecord(getLong(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("vehicle_key")), getLong(getColumnIndexOrThrow("start_at")), l("end_at"), i("start_odo"), i("end_odo"), f("distance_km") ?: 0f, getLong(getColumnIndexOrThrow("duration_sec")), f("avg_economy"), f("start_trip_a"), f("end_trip_a"), f("start_trip_b"), f("end_trip_b"), i("start_fuel"), i("end_fuel"), getInt(getColumnIndexOrThrow("active")) == 1)
    }

    private fun readSamples(cursor: android.database.Cursor): List<TelemetrySample> {
        val out = ArrayList<TelemetrySample>(cursor.count.coerceAtMost(cursor.columnCount * 8))
        val timeIdx = cursor.getColumnIndexOrThrow("timestamp")
        val vehicleIdx = cursor.getColumnIndexOrThrow("vehicle_key")
        while (cursor.moveToNext()) {
            fun intN(name: String): Int? {
                val i = cursor.getColumnIndex(name)
                return if (i >= 0 && !cursor.isNull(i)) cursor.getInt(i) else null
            }
            fun floatN(name: String): Float? {
                val i = cursor.getColumnIndex(name)
                return if (i >= 0 && !cursor.isNull(i)) cursor.getFloat(i) else null
            }
            out += TelemetrySample(
                timestamp = cursor.getLong(timeIdx),
                vehicleKey = cursor.getString(vehicleIdx),
                odo = intN("odo"),
                fuelPercent = intN("fuel_percent"),
                dteKm = intN("dte_km"),
                tripA = floatN("trip_a"),
                tripB = floatN("trip_b"),
                avgA = floatN("avg_a"),
                avgB = floatN("avg_b")
            )
        }
        return out
    }

    private fun ContentValues.putOrNull(key: String, value: Any?) {
        when (value) {
            null -> putNull(key)
            is String -> put(key, value)
            is Int -> put(key, value)
            is Float -> put(key, value)
            is Double -> put(key, value)
            is Long -> put(key, value)
            else -> put(key, value.toString())
        }
    }

    private fun android.database.Cursor.toSnapshot(): Snapshot {
        fun idx(name: String): Int = getColumnIndex(name)
        fun stringOrNull(name: String): String? {
            val i = idx(name); return if (i >= 0 && !isNull(i)) getString(i) else null
        }
        fun intOrNull(name: String): Int? {
            val i = idx(name); return if (i >= 0 && !isNull(i)) getInt(i) else null
        }
        fun floatOrNull(name: String): Float? {
            val i = idx(name); return if (i >= 0 && !isNull(i)) getFloat(i) else null
        }
        return Snapshot(
            vehicleKey = stringOrNull("vehicle_key") ?: "",
            uid = stringOrNull("uid"),
            odo = intOrNull("last_known_odometer"),
            fuelPercent = floatOrNull("last_known_fuel_level")?.let(Float::toInt),
            dteKm = floatOrNull("last_known_dte")?.let(Float::toInt),
            tripA = floatOrNull("last_known_tripA"),
            tripB = floatOrNull("last_known_tripB"),
            avgA = floatOrNull("last_known_avgA"),
            avgB = floatOrNull("last_known_avgB"),
            updatedAt = getLong(idx("updated_at")),
            source = stringOrNull("source") ?: "LOCAL_DB"
        )
    }

    companion object {
        const val DB_NAME = "altrozdrive_vehicle.db"
        const val DB_VERSION = 10
        // Physical stop threshold. Bluetooth disconnect is not itself a trip boundary.
        const val TRIP_IDLE_TIMEOUT_MS: Long = 5L * 60L * 1000L
        private const val ROUTE_MAX_SPEED_MPS = 45.0
        private const val ROUTE_JUMP_TOLERANCE_METERS = 25.0
        const val BASELINE_VEHICLE_KEY = "ALTROZ_XZA+S_EVIDENCE"
        const val BASELINE_UID = "544216271207"
        const val BASELINE_SOURCE = "OFFLINE_BASELINE_2026-09-19"
        private const val ROUTE_PENDING_RETENTION_MS = 48L * 60L * 60L * 1000L
        // Route detection now uses fractional Trip A/B movement; five minutes is a
        // bounded pre-start cushion, not a license to attach earlier trips.
        private const val ROUTE_PRESTART_ATTACH_WINDOW_MS = 5L * 60L * 1000L
        private const val START_ODO_BACKFILL_WINDOW_MS = 30_000L
    }
}
