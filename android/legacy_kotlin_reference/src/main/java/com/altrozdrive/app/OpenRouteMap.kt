package com.altrozdrive.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun OpenRouteMap(points: List<Pair<Double, Double>>, accent: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val darkMap = MaterialTheme.colorScheme.background.luminance() < 0.18f
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember(darkMap) {
        runCatching { MapLibreBridge.RouteView(context, darkMap) }.getOrNull()
    }
    if (mapView == null) {
        EmptyDataState("MAP UNAVAILABLE", Icons.Default.Navigation, "Trip data is still saved; the map renderer could not be opened.")
        return
    }
    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStartMap()
                Lifecycle.Event.ON_RESUME -> mapView.onResumeMap()
                Lifecycle.Event.ON_PAUSE -> mapView.onPauseMap()
                Lifecycle.Event.ON_STOP -> mapView.onStopMap()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStartMap()
        }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResumeMap()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPauseMap()
            mapView.onStopMap()
            mapView.onDestroyMap()
        }
    }
    val display = RouteDisplayProcessor.simplify(points)
    val lat = remember(display) { display.map { it.first }.toDoubleArray() }
    val lon = remember(display) { display.map { it.second }.toDoubleArray() }
    Box(modifier.clip(RoundedCornerShape(22.dp))) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView }, update = { it.setRoute(lat, lon) })
        Surface(modifier = Modifier.align(Alignment.TopEnd), shape = CircleShape, color = Color.White.copy(alpha = .94f), shadowElevation = 4.dp) {
            IconButton(onClick = { mapView.fitRoute() }) {
                Icon(Icons.Default.MyLocation, contentDescription = "Fit route", tint = accent)
            }
        }
    }
}

object RouteDisplayProcessor {
    fun simplify(points: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
        if (points.size <= 2) return points
        val cleaned = ArrayList<Pair<Double, Double>>(min(points.size, 10000))
        var last: Pair<Double, Double>? = null
        points.forEach { p ->
            if (!p.first.isFinite() || !p.second.isFinite()) return@forEach
            if (p.first !in -90.0..90.0 || p.second !in -180.0..180.0) return@forEach
            if (last == null || distanceMeters(last!!, p) >= 2.0) {
                cleaned += p
                last = p
            }
        }
        if (cleaned.size <= 2) return cleaned

        val tolerance = when {
            cleaned.size > 8000 -> 12.0
            cleaned.size > 4000 -> 9.0
            cleaned.size > 2000 -> 7.0
            cleaned.size > 800 -> 5.0
            else -> 3.0
        }
        val simplified = douglasPeucker(cleaned, tolerance)
        return if (simplified.size > 1800) uniformResample(simplified, 1800) else simplified
    }

    private fun douglasPeucker(points: List<Pair<Double, Double>>, toleranceMeters: Double): List<Pair<Double, Double>> {
        if (points.size <= 2) return points
        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true
        simplifyRange(points, 0, points.lastIndex, toleranceMeters, keep)
        return points.indices.filter { keep[it] }.map { points[it] }
    }

    private fun simplifyRange(points: List<Pair<Double, Double>>, start: Int, end: Int, tolerance: Double, keep: BooleanArray) {
        if (end <= start + 1) return
        var maxDistance = tolerance
        var index = -1
        val a = points[start]
        val b = points[end]
        for (i in start + 1 until end) {
            val distance = perpendicularDistanceMeters(points[i], a, b)
            if (distance > maxDistance) {
                maxDistance = distance
                index = i
            }
        }
        if (index >= 0) {
            keep[index] = true
            simplifyRange(points, start, index, tolerance, keep)
            simplifyRange(points, index, end, tolerance, keep)
        }
    }

    private fun perpendicularDistanceMeters(p: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val lat = Math.toRadians((a.first + b.first + p.first) / 3.0)
        val cosLat = cos(lat).coerceAtLeast(0.01)
        val radius = 6_371_000.0
        val x1 = Math.toRadians(a.second) * radius * cosLat
        val y1 = Math.toRadians(a.first) * radius
        val x2 = Math.toRadians(b.second) * radius * cosLat
        val y2 = Math.toRadians(b.first) * radius
        val x = Math.toRadians(p.second) * radius * cosLat
        val y = Math.toRadians(p.first) * radius
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0.0 && dy == 0.0) return sqrt((x - x1).pow(2) + (y - y1).pow(2))
        val t = (((x - x1) * dx) + ((y - y1) * dy)) / (dx * dx + dy * dy)
        val clamped = t.coerceIn(0.0, 1.0)
        val px = x1 + clamped * dx
        val py = y1 + clamped * dy
        return sqrt((x - px).pow(2) + (y - py).pow(2))
    }

    private fun uniformResample(points: List<Pair<Double, Double>>, maxPoints: Int): List<Pair<Double, Double>> {
        if (points.size <= maxPoints) return points
        val out = ArrayList<Pair<Double, Double>>(maxPoints)
        val step = (points.size - 1).toDouble() / (maxPoints - 1)
        for (i in 0 until maxPoints) out += points[(i * step).toInt().coerceIn(0, points.lastIndex)]
        return out.distinct()
    }

    private fun distanceMeters(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val lat1 = Math.toRadians(a.first)
        val lat2 = Math.toRadians(b.first)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.second - a.second)
        val sinLat = kotlin.math.sin(dLat / 2.0)
        val sinLon = kotlin.math.sin(dLon / 2.0)
        val h = sinLat * sinLat + kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * sinLon * sinLon
        return 6_371_000.0 * 2.0 * kotlin.math.asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
}
