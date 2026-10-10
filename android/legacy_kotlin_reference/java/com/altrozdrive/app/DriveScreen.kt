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
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
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

@Composable
fun DriveScreen(s: VehicleState, settings: AppSettings, trips: List<VehicleSnapshotDb.TripRecord>, historyLoaded: Boolean = true, onAction: (String) -> Unit) {
    val tripA = s.tripA.takeIf { it > 0f }
    val tripB = s.tripB.takeIf { it > 0f }
    val avgA = s.avg.takeIf { it > 0f }
    val avgB = s.avgB.takeIf { it > 0f }
    val primary = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background.luminance() < .18f
    val rangeColor = if (isDark) Color(0xFF35B9FF) else primary

    Page("Drive", showHeader = false) {
        // Compact live header
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "LIVE DASHBOARD",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Drive", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            val hasSaved = s.odo > 0 || s.fuel > 0f || s.dte > 0 || s.tripA > 0f || s.tripB > 0f
            Pill(
                when {
                    s.dataLive -> "LIVE"
                    s.connected -> "CONNECTED"
                    hasSaved -> "SAVED"
                    else -> "OFFLINE"
                },
                when {
                    s.dataLive -> LeafGreen
                    else -> Muted
                }
            )
        }

        // Final compact Drive instrument card: RPM and Speed are two independent
        // gauges in one shared card. Equal width, equal outer padding and a fixed
        // center divider keep the two meters visually separate without wasting
        // the vertical space of the old full-height RPM card.
        Surface(
            Modifier.fillMaxWidth(),
            RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .68f))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactDriveGauge(
                    label = "RPM",
                    value = s.rpm.toFloat(),
                    max = 8000f,
                    centerUnit = "RPM",
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .height(166.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = .12f))
                )
                val speedInDisplayUnits = if (settings.distance == "mi") s.speed * .621371f else s.speed
                CompactDriveGauge(
                    label = "SPEED",
                    value = speedInDisplayUnits,
                    max = if (settings.distance == "mi") 100f else 160f,
                    centerUnit = if (settings.distance == "mi") "mph" else "km/h",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        ModernSectionHeader("TRIP METERS", "", null)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModernMetric(
                Icons.Default.Route,
                "TRIP A",
                tripA?.let { "%.1f km".format(Locale.US, it) } ?: "—",
                rangeColor,
                Modifier.weight(1f)
            )
            ModernMetric(
                Icons.Default.Route,
                "TRIP B",
                tripB?.let { "%.1f km".format(Locale.US, it) } ?: "—",
                rangeColor,
                Modifier.weight(1f)
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModernMetric(
                Icons.Default.Eco,
                "AVG ECONOMY A",
                avgA?.let { economyValue(it, settings) } ?: "—",
                LeafGreen,
                Modifier.weight(1f)
            )
            ModernMetric(
                Icons.Default.Eco,
                "AVG ECONOMY B",
                avgB?.let { economyValue(it, settings) } ?: "—",
                LeafGreen,
                Modifier.weight(1f)
            )
        }

        ModernSectionHeader("LIVE DATA", "", null)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModernMetric(
                Icons.Default.Speed,
                "ODOMETER",
                if (s.odo > 0) "%,d km".format(Locale.US, s.odo) else "—",
                primary,
                Modifier.weight(1f)
            )
            ModernMetric(
                Icons.Default.GasMeter,
                "FUEL",
                if (s.fuel > 0f) "${s.fuel.roundToInt()}%" else "—",
                Amber,
                Modifier.weight(1f)
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModernMetric(
                Icons.Default.Navigation,
                "RANGE",
                if (s.dte > 0) distanceValue(s.dte.toFloat(), settings, 0) else "—",
                rangeColor,
                Modifier.weight(1f)
            )
            ModernMetric(
                Icons.Default.Eco,
                "AVG. ECONOMY",
                avgA?.let { economyValue(it, settings) } ?: "—",
                LeafGreen,
                Modifier.weight(1f)
            )
        }

        // Do not repeat RPM, speed and fuel in a second snapshot card.
        // Those live values are already represented by the gauge and LIVE DATA above.

        val history = remember(trips) { groupTripsByDay(trips).take(7).reversed() }
        if (!historyLoaded) {
            EmptyDataState("LOADING HISTORY…", Icons.Default.Route, "Loading saved trips for the economy view.")
        } else if (history.isNotEmpty()) {
            ModernSectionHeader("TRIP ECONOMY", "7 days", null)
            Card(
                Modifier.fillMaxWidth(),
                RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    val max = history.mapNotNull { it.averageEconomy }.maxOrNull()?.coerceAtLeast(.1f) ?: 1f
                    history.forEach { day ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                SimpleDateFormat("dd", Locale.US).format(Date(day.dayStart)),
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(28.dp)
                            )
                            Box(
                                Modifier.weight(1f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f))
                            ) {
                                val p = ((day.averageEconomy ?: 0f) / max).coerceIn(0f, 1f)
                                Box(
                                    Modifier.fillMaxWidth(p)
                                        .fillMaxHeight()
                                        .background(LeafGreen, RoundedCornerShape(8.dp))
                                )
                            }
                            Spacer(Modifier.width(7.dp))
                            Text(
                                day.averageEconomy?.let { economyValue(it, settings) } ?: "—",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(58.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        if (historyLoaded) trips.firstOrNull { it.active }?.let { trip ->
            ModernSectionHeader("CURRENT APP TRIP", "Open", onClick = { onAction("Trips") })
            Surface(
                Modifier.fillMaxWidth().clickable { onAction("Trips") },
                RoundedCornerShape(18.dp),
                color = primary.copy(alpha = .08f)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(11.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModernTripStat("DISTANCE", distanceValue(trip.distanceKm, settings), Icons.Default.Route, Modifier.weight(1f))
                    ModernTripStat("TIME", formatTripDuration(trip.durationSec), Icons.Default.Schedule, Modifier.weight(1f))
                    ModernTripStat("AVG SPEED", averageTripSpeed(trip.distanceKm, trip.durationSec, settings), Icons.Default.Speed, Modifier.weight(1f))
                    ModernTripStat("AVG ECO", trip.avgEconomy?.let { economyValue(it, settings) } ?: "—", Icons.Default.Eco, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SnapshotValue(label: String, value: String, tint: Color, modifier: Modifier) {
    Surface(
        modifier.height(58.dp),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .68f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = stableValueSize(value, 13f), fontWeight = FontWeight.Bold, color = tint, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
fun DriveStatChip(label:String,value:String,modifier:Modifier){
    Surface(modifier,shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f)){
        Column(Modifier.padding(horizontal=12.dp,vertical=11.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
            Text(label,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=.8.sp)
            Text(value,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=1)
        }
    }
}


private fun formatGaugeValue(value: Float): String {
    if (!value.isFinite()) return "0"
    val rounded = value.roundToInt()
    return if (kotlin.math.abs(value - rounded) < 0.001f) {
        String.format(Locale.US, "%,d", rounded)
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

@Composable
private fun CompactDriveGauge(
    label: String,
    value: Float,
    max: Float,
    centerUnit: String,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = value.coerceIn(0f, max),
        animationSpec = tween(
            durationMillis = 100,
            easing = FastOutSlowInEasing
        ),
        label = "capsule_${label.lowercase(Locale.US)}_meter"
    )
    val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)
    val outline = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .12f)
    val text = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .80f)
    val ratio = (animated / max.coerceAtLeast(1f)).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .9.sp,
                color = muted,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatGaugeValue(animated),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = text,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.width(4.dp))
            Text(
                centerUnit,
                fontSize = 9.sp,
                color = muted,
                letterSpacing = .5.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // Clean multicolor capsule: no scale numbers, just the live position.
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    brush = if (ratio <= 0.001f) {
                        SolidColor(track)
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF1597FF),
                                Color(0xFF20C96B),
                                Color(0xFFE6D51B),
                                Color(0xFFFF9F1C),
                                Color(0xFFFF3030)
                            )
                        )
                    }
                )
                .border(1.dp, outline, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            val indicatorSize = 18.dp
            val travel = (maxWidth - indicatorSize - 6.dp).coerceAtLeast(0.dp)
            Box(
                Modifier
                    .padding(start = 3.dp)
                    .offset(x = travel * ratio)
                    .size(indicatorSize)
                    .background(if (ratio <= 0.001f) Muted.copy(alpha = .75f) else Color.White, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = .82f), CircleShape)
            )
        }

    }
}

