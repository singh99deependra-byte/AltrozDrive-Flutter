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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
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

@Composable
fun FuelScreen(s: VehicleState, settings: AppSettings, samples: List<VehicleSnapshotDb.TelemetrySample>, historyLoaded: Boolean = true) {
    var windowDays by rememberSaveable{mutableIntStateOf(7)}
    val cutoff=System.currentTimeMillis()-windowDays.toLong()*24L*60L*60L*1000L
    val dailyFuel=remember(samples,windowDays){dailyLastTelemetry(samples.filter{it.timestamp>=cutoff}){it.fuelPercent?.takeIf{v->v in 0..100}?.toFloat()}}
    val dailyRange=remember(samples,windowDays){dailyLastTelemetry(samples.filter{it.timestamp>=cutoff}){it.dteKm?.takeIf{v->v>=0}?.toFloat()}}
    val primary=MaterialTheme.colorScheme.primary
    val fuel=s.fuel.takeIf{it>0f}; val range=s.dte.takeIf{it>0}; val eco=s.avg.takeIf{it>0f}
    val rangeColor=if(MaterialTheme.colorScheme.background.luminance()<.18f) Color(0xFF35B9FF) else primary
    Page("Fuel", showHeader = false) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){Text("FUEL & EFFICIENCY",fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.1.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("Know your next stop",fontSize=20.sp,fontWeight=FontWeight.Bold)}
            val hasSaved = s.fuel > 0f || s.dte > 0 || s.avg > 0f
            Pill(when { s.dataLive -> "LIVE"; hasSaved -> "SAVED"; else -> "OFFLINE" }, if (s.dataLive) LeafGreen else Muted)
        }
        Surface(Modifier.fillMaxWidth(),RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.52f),border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.68f))){
            Column(Modifier.fillMaxWidth().padding(13.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("FUEL LEVEL",fontSize=8.sp,fontWeight=FontWeight.Bold,letterSpacing=.8.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(fuel?.let{"${it.toInt()}%"}?:"—",fontSize=25.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface)
                    }
                    Column(horizontalAlignment=Alignment.End){
                        Text("RANGE",fontSize=8.sp,fontWeight=FontWeight.Bold,letterSpacing=.8.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(range?.let{distanceValue(it.toFloat(),settings,0)}?:"—",fontSize=18.sp,fontWeight=FontWeight.Bold,color=rangeColor)
                    }
                }
                if(fuel!=null){
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)){
                        Box(Modifier.fillMaxWidth(AltrozAnimatedFractionSlow((fuel/100f).coerceIn(0f,1f))).fillMaxHeight().background(Amber,RoundedCornerShape(10.dp)))
                    }
                }
            }
        }
        Surface(Modifier.fillMaxWidth(),RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.52f),border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.68f))){Column(Modifier.fillMaxWidth().padding(13.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text("AVG. ECONOMY",fontSize=8.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurfaceVariant,letterSpacing=.8.sp);Text(eco?.let{economyValue(it,settings)}?:"—",fontSize=stableLargeValueSize(eco?.let{economyValue(it,settings)}?:"—",25f),fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold)}}
        Surface(Modifier.fillMaxWidth(),RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.52f),border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.68f))){Column(Modifier.fillMaxWidth().padding(11.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            Text("FUEL HISTORY",fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=.9.sp)
            TimeChips(windowDays){windowDays=it}
            when {
                !historyLoaded -> EmptyDataState("LOADING FUEL HISTORY…", Icons.Default.GasMeter, "Loading recorded daily fuel values.")
                dailyFuel.isNotEmpty() -> DailyHistoryBars(dailyFuel,Amber,"%",windowDays)
                else -> EmptyDataState("NO FUEL HISTORY YET",Icons.Default.GasMeter)
            }
        }}
        Surface(Modifier.fillMaxWidth(),RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.52f),border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.68f))){Column(Modifier.fillMaxWidth().padding(11.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            Text("RANGE HISTORY",fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=.9.sp)
            when {
                !historyLoaded -> EmptyDataState("LOADING RANGE HISTORY…", Icons.Default.Navigation, "Loading recorded distance-to-empty values.")
                dailyRange.isNotEmpty() -> {
                    val displayRange = if (settings.distance == "mi") dailyRange.map { it.copy(value = it.value * 0.621371f) } else dailyRange
                    DailyHistoryBars(displayRange, rangeColor, if (settings.distance == "mi") "mi" else "km", windowDays)
                }
                else -> EmptyDataState("NO RANGE HISTORY YET",Icons.Default.Navigation)
            }
        }}
    }
}

@Composable
private fun DailyHistoryBars(points: List<DailyTelemetryPoint>, color: Color, unit: String, windowDays: Int = 7) {
    val visible = if (windowDays <= 7) points.takeLast(7) else points
    val max = visible.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
    var selectedIndex by remember(visible) { mutableIntStateOf(visible.lastIndex.coerceAtLeast(0)) }
    val selected = visible.getOrNull(selectedIndex)
    val chartScroll = rememberScrollState()
    LaunchedEffect(visible.size, windowDays) {
        if (windowDays > 7) chartScroll.scrollTo(chartScroll.maxValue)
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        selected?.let {
            Text(
                "${dailyDateLabel(it.dayStart)} • ${if(unit=="km") it.value.roundToInt().toString() else String.format(Locale.US,"%.0f",it.value)} $unit",
                color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }
        Row(Modifier.fillMaxWidth().height(142.dp).horizontalScroll(chartScroll),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(5.dp)){
            visible.forEachIndexed{idx,point->
                val fraction=(point.value/max).coerceIn(0f,1f); val selectedBar=idx==selectedIndex
                Column(Modifier.then(if (windowDays <= 7) Modifier.weight(1f) else Modifier.width(34.dp)).fillMaxHeight().clickable{selectedIndex=idx},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom){
                    Text(if(unit=="km")point.value.roundToInt().toString() else String.format(Locale.US,"%.0f",point.value),fontSize=7.sp,fontWeight=FontWeight.Bold,color=if(selectedBar)color else MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,softWrap=false)
                    Spacer(Modifier.height(3.dp));Box(Modifier.fillMaxWidth(.70f).height((88.dp*AltrozAnimatedFraction(fraction, idx * 55)).coerceAtLeast(8.dp)).background(if(selectedBar)color else color.copy(alpha=.58f),RoundedCornerShape(topStart=6.dp,topEnd=6.dp)))
                    Spacer(Modifier.height(4.dp));Text(SimpleDateFormat("dd",Locale.US).format(Date(point.dayStart)),fontSize=7.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("One entry per day",fontSize=8.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(if(windowDays <= 7) "7 days" else "Scroll through selected days",fontSize=8.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}

@Composable
internal fun AltrozSegmentedSelector(
    options: List<Pair<String, Int>>,
    selectedValue: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
    ) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (label, value) ->
                val active = selectedValue == value
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 38.dp).clickable { onSelect(value) },
                    shape = RoundedCornerShape(13.dp),
                    color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                    border = if (active) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .35f)) else null
                ) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
fun TimeChips(selectedDays: Int, onSelect: (Int) -> Unit) {
    AltrozSegmentedSelector(
        options = listOf("7D" to 7, "30D" to 30, "3M" to 90, "6M" to 180),
        selectedValue = selectedDays,
        onSelect = onSelect
    )
}

