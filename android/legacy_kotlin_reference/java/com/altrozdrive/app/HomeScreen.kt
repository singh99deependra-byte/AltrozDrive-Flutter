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
fun HomeScreen(
    s: VehicleState,
    settings: AppSettings,
    media: MediaState,
    vehicle: VehicleProfile,
    latestUpdatedAt: Long,
    trips: List<VehicleSnapshotDb.TripRecord>,
    historyLoaded: Boolean = true,
    onAction: (String) -> Unit
) {
    val live = s.dataLive
    val connected = s.connected
    val fuel = s.fuel.takeIf { it > 0f }
    val range = s.dte.takeIf { it > 0 }
    val economy = s.avg.takeIf { it > 0f }
    val dailyGroups = remember(trips) { groupTripsByDay(trips) }
    val today = dailyGroups.firstOrNull { it.dayStart == dayStartMillis(System.currentTimeMillis()) }
    // Resolve the complete Today's Trip presentation state before entering Page's
    // @Composable lambda. Do not capture DailyTripGroup itself in the lambda: on
    // Android 16 this can trigger a Compose-generated Function0<Void> cast.
    val hasTodayTrip = today != null
    val todayDistance = today?.let { distanceValue(it.distanceKm, settings) } ?: ""
    val todayTrips = today?.let { it.trips.size.toString() } ?: ""
    val todayTime = today?.let { formatTripDuration(it.durationSec) } ?: ""
    val todayEconomy = today?.averageEconomy?.let { economyValue(it, settings) } ?: "—"
    val savedLabel = latestUpdatedAt.takeIf { it > 0L }?.let {
        SimpleDateFormat("dd MMM • HH:mm", Locale.US).format(Date(it))
    } ?: "No saved data"
    val primary = MaterialTheme.colorScheme.primary
    val mapAccent = primary

    Page("Home", showHeader = false) {
        // Reference Home header: compact, quiet and unchanged in structure.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${vehicle.model} ${vehicle.trim}", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AltrozLiveDot(live, if (live) LeafGreen else Muted)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (live) "Connected • Live" else if (connected) s.connectionStatus else "Offline • Last saved $savedLabel",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (live) LeafGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DirectionsCar, null, tint = primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(vehicle.model, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // SINGLE HERO SURFACE. The accent wash belongs to this entire card; the
        // transparent Altroz image is never placed inside a secondary rectangle.
        Card(
            AltrozRevealModifier("home_hero")
                .fillMaxWidth(),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    primary.copy(alpha = .13f),
                                    primary.copy(alpha = .075f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = .98f)
                                )
                            )
                        )
                ) {
                    // Soft diagonal accent atmosphere, matching the reference image.
                    Box(
                        Modifier
                            .fillMaxWidth(.62f)
                            .height(82.dp)
                            .align(Alignment.TopEnd)
                            .rotate(-25f)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, primary.copy(alpha = .075f))
                                ),
                                RoundedCornerShape(60.dp)
                            )
                    )

                    Column(
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 15.dp, end = 152.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            "YOUR ALTROZ",
                            color = primary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                        Text("${vehicle.model} ${vehicle.trim}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${vehicle.fuel} • ${vehicle.transmission}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }

                    AltrozCutoutImage(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 3.dp, bottom = 4.dp)
                            .size(width = 190.dp, height = 108.dp),
                        accent = primary,
                        accentStrength = .22f,
                        idleAnimation = live
                    )
                }

                Row(
                    Modifier.fillMaxWidth().padding(9.dp),
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
                        fuel?.let { "${it.toInt()}%" } ?: "—",
                        Amber,
                        Modifier.weight(1f)
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 9.dp).padding(bottom = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModernMetric(
                        Icons.Default.Navigation,
                        "RANGE",
                        range?.let { distanceValue(it.toFloat(), settings, 0) } ?: "—",
                        mapAccent,
                        Modifier.weight(1f)
                    )
                    ModernMetric(
                        Icons.Default.Eco,
                        "AVG. ECONOMY",
                        economy?.let { economyValue(it, settings) } ?: "—",
                        LeafGreen,
                        Modifier.weight(1f)
                    )
                }
            }
        }

        // Only show Today's Trip when there is an actual trip for the current day.
        // Do not manufacture a summary from yesterday's history just to fill the space.
        if (hasTodayTrip) {
            ModernSectionHeader("TODAY'S TRIP", "View", onClick = { onAction("Trips") })
            Surface(
                AltrozRevealModifier("today_trip", 180).fillMaxWidth(),
                RoundedCornerShape(17.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .68f))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(11.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ModernTripStat("DISTANCE", todayDistance, Icons.Default.Route, Modifier.weight(1f))
                    ModernTripStat("TRIPS", todayTrips, Icons.Default.CalendarMonth, Modifier.weight(1f))
                    ModernTripStat("TIME", todayTime, Icons.Default.Schedule, Modifier.weight(1f))
                    ModernTripStat("AVG ECO", todayEconomy, Icons.Default.Eco, Modifier.weight(1f))
                }
            }
        }

        if (!historyLoaded) {
            EmptyDataState("LOADING HISTORY…", Icons.Default.Route, "Loading saved trips and route history.")
        } else if (dailyGroups.isNotEmpty()) {
            ModernSectionHeader("RECENT TRIP DAYS", "View all", onClick = { onAction("Trips") })
            // ONE full-width card instead of multiple tiny cards. This uses the empty
            // right side to show the useful daily summary without changing the data model.
            dailyGroups.take(5).forEach { day ->
                RecentTripDayCard(day, settings, primary) { onAction("Trips") }
            }
        }

        if (media.available || media.title.isNotBlank()) MusicMiniCard(media) { onAction("Music") }
    }
}

@Composable
internal fun QuickNavigationAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = AltrozUiTokens.touchTarget).height(48.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = tint.copy(alpha = .07f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = .11f))
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
            Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, softWrap = true, textAlign = TextAlign.Center, lineHeight = 9.sp)
        }
    }
}

@Composable
private fun RecentTripDayCard(
    day: DailyTripGroup,
    settings: AppSettings,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = AltrozRevealModifier("recent_trip_${day.dayStart}", 120).fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .68f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(day.dayStart)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${day.trips.size} trip${if (day.trips.size == 1) "" else "s"}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatTripDuration(day.durationSec),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                Modifier
                    .width(1.dp)
                    .height(44.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
            )

            Spacer(Modifier.width(18.dp))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Route, null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(distanceValue(day.distanceKm, settings), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Eco, null, tint = LeafGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        day.averageEconomy?.let { economyValue(it, settings) } ?: "—",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Icon(Icons.Default.ArrowForward, null, tint = accent, modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
internal fun ModernMetric(icon:ImageVector,label:String,value:String,tint:Color,modifier:Modifier){
    Surface(
        modifier.heightIn(min = 70.dp),
        RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .68f))
    ){
        Row(Modifier.fillMaxWidth().padding(horizontal=11.dp),verticalAlignment=Alignment.CenterVertically){
            Icon(icon,null,tint=tint,modifier=Modifier.size(19.dp));
            Spacer(Modifier.width(9.dp));
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)){
                Text(label,fontSize=8.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurfaceVariant,letterSpacing=.7.sp,maxLines=2,lineHeight=9.sp)
                AltrozAnimatedText(value) { animatedValue ->
                    Text(animatedValue,fontSize=stableValueSize(animatedValue,16f),fontWeight=FontWeight.Bold,fontFamily=FontFamily.Monospace,maxLines=1,softWrap=false)
                }
            }
        }
    }
}

@Composable
internal fun ModernSectionHeader(title:String,action:String,onClick:(()->Unit)?){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(title,fontSize=12.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp,modifier=Modifier.weight(1f));if(onClick!=null)TextButton(onClick=onClick){Text(action,fontSize=11.sp,fontWeight=FontWeight.Bold)}}
}

@Composable
internal fun ModernTripStat(label:String,value:String,icon:ImageVector,modifier:Modifier){
    Column(modifier, horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.spacedBy(3.dp)){
        Icon(icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(18.dp));
        Text(value,fontSize=stableValueSize(value,12f),fontWeight=FontWeight.Bold,fontFamily=FontFamily.Monospace,maxLines=1,softWrap=false,textAlign=TextAlign.Center);
        Text(label,fontSize=7.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,fontWeight=FontWeight.Bold,maxLines=2,softWrap=true,textAlign=TextAlign.Center,lineHeight=8.sp)
    }
}

@Composable
private fun MusicMiniCard(media: MediaState, onOpen: () -> Unit) {
    val accent = navigationAccent()
    Surface(
        modifier = AltrozRevealModifier("music_mini").fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = .08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .14f))
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(48.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                Box(contentAlignment = Alignment.Center) {
                    MediaArtwork(media.artwork, media.artworkUri, circular = false)
                    if (media.artwork == null && media.artworkUri == null) {
                        Icon(Icons.Default.MusicNote, null, tint = accent, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (media.playing) "NOW PLAYING" else "MUSIC", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(media.title.ifBlank { "No active media" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, softWrap = true)
                Text(media.artist.ifBlank { "Tap to open music" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 2, softWrap = true)
            }
            Icon(Icons.Default.ArrowForward, null, tint = accent, modifier = Modifier.size(18.dp))
        }
    }
}








internal fun formatFuelPair(start: Int?, end: Int?): String =
    if (start != null && end != null) "$start% → $end%" else "—"

