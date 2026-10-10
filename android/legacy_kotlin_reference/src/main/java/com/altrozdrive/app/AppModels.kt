package com.altrozdrive.app

import android.graphics.Bitmap
import android.media.session.MediaController

data class VehicleState(
    val speed: Float = 0f, val rpm: Int = 0, val fuel: Float = 0f, val dte: Int = 0,
    val odo: Int = 0, val tripA: Float = 0f, val tripB: Float = 0f,
    val avg: Float = 0f, val avgB: Float = 0f, val instant: Float = 0f, val battery: Float = 0f,
    val coolant: Int = 0, val ignition: Boolean = false, val connected: Boolean = false,
    val dataLive: Boolean = false, val connectionStatus: String = "OFFLINE"
)

data class VehicleProfile(
    val model: String = "Altroz",
    val trim: String = "XZA+S",
    val fuel: String = "Petrol",
    val transmission: String = "DCA"
)

data class VehicleOption(
    val model: String,
    val trims: List<String>,
    val fuels: List<String>,
    val transmissions: List<String>
)

data class AppSettings(
    val appearance: String, val distance: String, val economy: String, val temperature: String,
    val accent: String
)

data class MediaState(
    val accessGranted: Boolean = false,
    val available: Boolean = false,
    val title: String = "No media playing",
    val artist: String = "",
    val album: String = "",
    val artwork: Bitmap? = null,
    val artworkUri: String? = null,
    val packageName: String = "",
    val playing: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val controller: MediaController? = null
)

data class DailyTripGroup(
    val dayStart: Long,
    val trips: List<VehicleSnapshotDb.TripRecord>,
    val distanceKm: Float,
    val durationSec: Long,
    val averageEconomy: Float?,
    val startAt: Long
)

data class DailyTelemetryPoint(val dayStart: Long, val value: Float)

internal fun dayStartMillis(timestamp: Long): Long = java.util.Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(java.util.Calendar.HOUR_OF_DAY, 0)
    set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0)
    set(java.util.Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun groupTripsByDay(trips: List<VehicleSnapshotDb.TripRecord>): List<DailyTripGroup> =
    trips.filter { !it.active }.groupBy { dayStartMillis(it.startAt) }.map { (day, raw) ->
        val dayTrips = raw.sortedBy { it.startAt }
        val distance = dayTrips.sumOf { it.distanceKm.toDouble() }.toFloat()
        val weightedEconomy = dayTrips.mapNotNull { trip ->
            val eco = trip.avgEconomy
            if (eco != null && eco > 0f && trip.distanceKm > 0f) trip.distanceKm to eco else null
        }.let { pairs ->
            if (pairs.isEmpty()) null else {
                val totalDistance = pairs.sumOf { it.first.toDouble() }
                val weighted = pairs.sumOf { (it.first * it.second).toDouble() }
                (weighted / totalDistance).toFloat()
            }
        }
        DailyTripGroup(day, dayTrips, distance, dayTrips.sumOf { it.durationSec.coerceAtLeast(0L) }, weightedEconomy, dayTrips.firstOrNull()?.startAt ?: day)
    }.sortedByDescending { it.dayStart }

internal fun dailyLastTelemetry(
    samples: List<VehicleSnapshotDb.TelemetrySample>,
    selector: (VehicleSnapshotDb.TelemetrySample) -> Float?
): List<DailyTelemetryPoint> = samples.asSequence()
    .filter { selector(it) != null }
    .groupBy { dayStartMillis(it.timestamp) }
    .mapNotNull { (day, rows) ->
        val latest = rows.maxByOrNull { it.timestamp } ?: return@mapNotNull null
        selector(latest)?.let { DailyTelemetryPoint(day, it) }
    }
    .sortedBy { it.dayStart }
