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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
private fun TripHistorySelector(selected: Int, recentCount: Int, onSelect: (Int) -> Unit) {
    AltrozSegmentedSelector(
        options = listOf("Recent ($recentCount)" to 0, "History" to 1),
        selectedValue = selected,
        onSelect = onSelect
    )
}

@Composable
fun TripScreen(settings: AppSettings, trips: List<VehicleSnapshotDb.TripRecord>, dataLive: Boolean = false, historyLoaded: Boolean = true) {
    val context=LocalContext.current
    val routeDb=remember { VehicleSnapshotDb(context.applicationContext) }
    DisposableEffect(routeDb) { onDispose { try { routeDb.close() } catch (_: Exception) { } } }
    val completed=remember(trips){trips.filter{!it.active}.sortedByDescending{it.startAt}}
    val dailyGroups=remember(completed){groupTripsByDay(completed)}
    val active=trips.firstOrNull{it.active}
    var selectedTripId by rememberSaveable{mutableStateOf<Long?>(null)}
    var expandedDay by rememberSaveable{mutableStateOf<Long?>(null)}
    var selectedTripSection by rememberSaveable { mutableIntStateOf(0) }
    var routePoints by remember{mutableStateOf<List<Pair<Double,Double>>>(emptyList())}
    var routeLoading by remember { mutableStateOf(false) }
    var routeLoadError by remember { mutableStateOf(false) }
    LaunchedEffect(selectedTripId,active?.id){
        val id=selectedTripId?:active?.id
        routeLoading = id != null
        routeLoadError = false
        try {
            routePoints = if(id!=null) withContext(Dispatchers.IO){routeDb.routePoints(id)} else emptyList()
        } catch (_: Throwable) {
            routePoints = emptyList()
            routeLoadError = true
        } finally {
            routeLoading = false
        }
    }
    val primary=MaterialTheme.colorScheme.primary
    val mapAccent=primary
    Page("Trip", showHeader = false) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Trips", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (active != null) Pill(if (dataLive) "LIVE" else "TRIP ACTIVE", if (dataLive) LeafGreen else primary)
        }
        if (historyLoaded && active != null) {
            Surface(Modifier.fillMaxWidth(),RoundedCornerShape(19.dp),color=if (dataLive) LeafGreen.copy(alpha=.055f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.52f),border=if (dataLive) androidx.compose.foundation.BorderStroke(1.dp,LeafGreen.copy(alpha=.22f)) else androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.68f))){Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("CURRENT TRIP",fontSize=8.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurfaceVariant,letterSpacing=.9.sp);Text(if (routePoints.size >= 2) "Route recorded" else "Route data pending",fontSize=15.sp,fontWeight=FontWeight.Bold)};Pill(if (dataLive) "LIVE" else "TRIP ACTIVE", if (dataLive) LeafGreen else Muted)}
                when {
                    routeLoading -> EmptyDataState("LOADING ROUTE…", Icons.Default.Navigation)
                    routeLoadError -> EmptyDataState("ROUTE UNAVAILABLE", Icons.Default.Navigation, "Saved route data could not be loaded right now.")
                    routePoints.size>=2 -> OpenRouteMap(routePoints,mapAccent,Modifier.fillMaxWidth().height(190.dp))
                    else -> EmptyDataState("ROUTE NOT AVAILABLE YET", Icons.Default.Navigation, "No route points are available for this trip yet.")
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){ModernTripStat("DISTANCE",distanceValue(active.distanceKm,settings),Icons.Default.Route,Modifier.weight(1f));ModernTripStat("DURATION",formatTripDuration(active.durationSec),Icons.Default.Schedule,Modifier.weight(1f));ModernTripStat("AVG SPEED",averageTripSpeed(active.distanceKm,active.durationSec,settings),Icons.Default.Speed,Modifier.weight(1f));ModernTripStat("AVG ECO",active.avgEconomy?.let{economyValue(it,settings)}?:"—",Icons.Default.Eco,Modifier.weight(1f))}
            }}
        }

        // Recent trips and full history are separate views, using the same compact
        // segmented selector pattern as Fuel's time-range controls.
        TripHistorySelector(
            selected = selectedTripSection,
            recentCount = completed.size.coerceAtMost(5),
            onSelect = { selectedTripSection = it }
        )
        if(!historyLoaded) {
            EmptyDataState("LOADING TRIP HISTORY…", Icons.Default.Route, "Loading saved trips and route information.")
        } else if (selectedTripSection == 0) {
            val recentTrips = completed.take(5)
            if (recentTrips.isEmpty()) {
                EmptyDataState("NO RECENT TRIPS", Icons.Default.Route, "Completed trips will appear here after a drive.")
            } else {
                recentTrips.forEach { trip ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { selectedTripId = trip.id },
                        shape = RoundedCornerShape(17.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .68f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(Modifier.size(36.dp), RoundedCornerShape(12.dp), color = primary.copy(alpha = .12f)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Route, null, tint = primary, modifier = Modifier.size(19.dp))
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(Date(trip.startAt)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${distanceValue(trip.distanceKm, settings)} • ${formatTripDuration(trip.durationSec)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(trip.avgEconomy?.let { economyValue(it, settings) } ?: "—", fontSize = 10.sp, color = LeafGreen, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(Icons.Default.ArrowForward, null, tint = primary, modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }
        } else {
            ModernSectionHeader("TRIP HISTORY", if(historyLoaded) "${dailyGroups.size} days" else "", null)
            if(dailyGroups.isEmpty()) EmptyDataState("NO TRIP HISTORY",Icons.Default.Route, "Your daily trip summaries will appear here.") else dailyGroups.take(30).forEach { day ->
            val expanded=expandedDay==day.dayStart
            Surface(Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(100)),RoundedCornerShape(17.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=if(expanded) .66f else .52f),border=androidx.compose.foundation.BorderStroke(1.dp,if(expanded) primary.copy(alpha=.24f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha=.68f))){
                Column(Modifier.fillMaxWidth().clickable{expandedDay=if(expanded)null else day.dayStart}.padding(11.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Surface(Modifier.size(38.dp),RoundedCornerShape(12.dp),color=primary.copy(alpha=.10f)){Box(contentAlignment=Alignment.Center){Icon(Icons.Default.Route,null,tint=primary,modifier=Modifier.size(19.dp))}}
                        Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f)){Text(SimpleDateFormat("dd MMM yyyy",Locale.US).format(Date(day.dayStart)),fontSize=13.sp,fontWeight=FontWeight.Bold);Text("${day.trips.size} trip${if(day.trips.size==1)"" else "s"} • ${formatTripDuration(day.durationSec)}",fontSize=9.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                        Column(horizontalAlignment=Alignment.End){Text(distanceValue(day.distanceKm,settings),fontSize=12.sp,fontWeight=FontWeight.Bold);Text(day.averageEconomy?.let{economyValue(it,settings)}?:"—",fontSize=9.sp,color=LeafGreen,fontWeight=FontWeight.SemiBold)}
                        Spacer(Modifier.width(6.dp));Icon(if(expanded)Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,null,tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(18.dp))
                    }
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(
                            animationSpec = tween(100),
                            expandFrom = Alignment.Top
                        ) + fadeIn(animationSpec = tween(70)),
                        exit = shrinkVertically(
                            animationSpec = tween(100),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(animationSpec = tween(70))
                    ){
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)){
                        day.trips.forEach { trip ->
                            Surface(Modifier.fillMaxWidth().clickable{selectedTripId=trip.id},RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.48f)){
                                Row(Modifier.fillMaxWidth().padding(horizontal=9.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
                                    Text(SimpleDateFormat("HH:mm",Locale.US).format(Date(trip.startAt)),fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.width(46.dp))
                                    Column(Modifier.weight(1f)){Text("${distanceValue(trip.distanceKm,settings)} • ${formatTripDuration(trip.durationSec)}",fontSize=10.sp,fontWeight=FontWeight.SemiBold);Text(trip.avgEconomy?.let{economyValue(it,settings)}?:"—",fontSize=8.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                                    Icon(Icons.Default.ArrowForward,null,tint=primary,modifier=Modifier.size(15.dp))
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
        }

        selectedTripId?.let { id ->
            val trip = completed.firstOrNull { it.id == id }
            if (trip != null) {
                Dialog(
                    onDismissRequest = { selectedTripId = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        Modifier.fillMaxWidth(.96f).heightIn(max = 760.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("TRIP DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(SimpleDateFormat("dd MMM yyyy • HH:mm",Locale.US).format(Date(trip.startAt)),fontSize=18.sp,fontWeight=FontWeight.Bold)
                                }
                                IconButton(onClick = { selectedTripId = null }) { Icon(Icons.Default.Close, contentDescription = "Close") }
                            }
                            when {
                                routeLoading -> EmptyDataState("LOADING ROUTE…", Icons.Default.Navigation)
                                routeLoadError -> EmptyDataState("ROUTE UNAVAILABLE", Icons.Default.Navigation, "Saved route data could not be loaded right now.")
                                routePoints.size>=2 -> OpenRouteMap(routePoints,mapAccent,Modifier.fillMaxWidth().height(230.dp))
                                else -> EmptyDataState("ROUTE NOT AVAILABLE", Icons.Default.Navigation, "No route points were saved for this trip.")
                            }
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                ModernTripStat("DISTANCE",distanceValue(trip.distanceKm,settings),Icons.Default.Route,Modifier.weight(1f))
                                ModernTripStat("DURATION",formatTripDuration(trip.durationSec),Icons.Default.Schedule,Modifier.weight(1f))
                                ModernTripStat("AVG SPEED",averageTripSpeed(trip.distanceKm,trip.durationSec,settings),Icons.Default.Speed,Modifier.weight(1f))
                                ModernTripStat("AVG ECO",trip.avgEconomy?.let{economyValue(it,settings)}?:"—",Icons.Default.Eco,Modifier.weight(1f))
                            }
                            TripDetailLine("Start time",SimpleDateFormat("HH:mm",Locale.US).format(Date(trip.startAt)))
                            TripDetailLine("End time",trip.endAt?.let{SimpleDateFormat("HH:mm",Locale.US).format(Date(it))} ?: "—")
                            TripDetailLine("Start ODO",trip.startOdo?.let{"%,d km".format(Locale.US,it)}?:"—")
                            TripDetailLine("End ODO",trip.endOdo?.let{"%,d km".format(Locale.US,it)}?:"—")
                        }
                    }
                }
            }
        }
    }
}

