package com.altrozdrive.app

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
private fun featureCard(content: @Composable () -> Unit) {
    CardBox { content() }
}

@Composable
internal fun FeatureBackButton(onBack: () -> Unit) {
    OutlinedButton(
        onClick = onBack,
        modifier = Modifier.heightIn(min = 44.dp).widthIn(min = 108.dp),
        shape = RoundedCornerShape(13.dp),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        Icon(Icons.Default.ArrowBack, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Back")
    }
}

@Composable
fun VehicleRemindersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(VehicleReminderScheduler.PREFS, Context.MODE_PRIVATE) }
    var pucExpiry by remember { mutableLongStateOf(prefs.getLong(VehicleReminderScheduler.KEY_PUC_EXPIRY, 0L)) }
    var insuranceExpiry by remember { mutableLongStateOf(prefs.getLong(VehicleReminderScheduler.KEY_INSURANCE_EXPIRY, 0L)) }
    var pucNumber by remember { mutableStateOf(prefs.getString(VehicleReminderScheduler.KEY_PUC_NUMBER, "") ?: "") }
    var insuranceNumber by remember { mutableStateOf(prefs.getString(VehicleReminderScheduler.KEY_INSURANCE_NUMBER, "") ?: "") }
    var insurer by remember { mutableStateOf(prefs.getString(VehicleReminderScheduler.KEY_INSURER, "") ?: "") }
    var pucReminderDays by remember { mutableIntStateOf(prefs.getInt(VehicleReminderScheduler.KEY_PUC_REMINDER_DAYS, VehicleReminderScheduler.DEFAULT_REMINDER_DAYS)) }
    var insuranceReminderDays by remember { mutableIntStateOf(prefs.getInt(VehicleReminderScheduler.KEY_INSURANCE_REMINDER_DAYS, VehicleReminderScheduler.DEFAULT_REMINDER_DAYS)) }
    val initialMonth = remember { Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) } }
    var monthCursorYear by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(initialMonth.get(Calendar.YEAR)) }
    var monthCursorMonth by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(initialMonth.get(Calendar.MONTH)) }
    val monthCursor = remember(monthCursorYear, monthCursorMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, monthCursorYear)
            set(Calendar.MONTH, monthCursorMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun shiftMonth(delta: Int) {
        val next = (monthCursor.clone() as Calendar).apply { add(Calendar.MONTH, delta) }
        monthCursorYear = next.get(Calendar.YEAR)
        monthCursorMonth = next.get(Calendar.MONTH)
    }

    fun pickDate(type: String) {
        val current = if (type == "PUC") pucExpiry else insuranceExpiry
        val base = Calendar.getInstance().apply { if (current > 0L) timeInMillis = current }
        DatePickerDialog(context, { _, year, month, day ->
            val selected = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            if (type == "PUC") {
                pucExpiry = selected
                VehicleReminderScheduler.save(context, "PUC", selected, pucNumber, reminderDays = pucReminderDays)
            } else {
                insuranceExpiry = selected
                VehicleReminderScheduler.save(context, "INSURANCE", selected, insuranceNumber, insurer, insuranceReminderDays)
            }
        }, base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun dateText(value: Long): String = if (value <= 0L) "Not set" else SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(value))
    fun daysLeft(value: Long): Long = if (value <= 0L) Long.MIN_VALUE else ((value - System.currentTimeMillis()) / 86_400_000L)

    Page("Vehicle reminders") {
        FeatureBackButton(onBack)
        Text("DOCUMENTS & EXPIRY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.2.sp)

        featureCard {
            ReminderDocumentCard("PUC certificate", "PUC", pucExpiry, pucNumber, "Certificate number", { pucNumber = it; prefs.edit().putString(VehicleReminderScheduler.KEY_PUC_NUMBER, it).apply(); if (pucExpiry > 0) VehicleReminderScheduler.save(context, "PUC", pucExpiry, it, reminderDays = pucReminderDays) }, ::pickDate, ::dateText, ::daysLeft, reminderDays = pucReminderDays, onReminderDays = { pucReminderDays = it; prefs.edit().putInt(VehicleReminderScheduler.KEY_PUC_REMINDER_DAYS, it).apply(); if (pucExpiry > 0) VehicleReminderScheduler.save(context, "PUC", pucExpiry, pucNumber, reminderDays = it) }, onClear = {
                VehicleReminderScheduler.clear(context, "PUC"); pucExpiry = 0L; pucNumber = ""
            })
        }
        featureCard {
            ReminderDocumentCard("Motor insurance", "INSURANCE", insuranceExpiry, insuranceNumber, "Policy number", { insuranceNumber = it; prefs.edit().putString(VehicleReminderScheduler.KEY_INSURANCE_NUMBER, it).apply(); if (insuranceExpiry > 0) VehicleReminderScheduler.save(context, "INSURANCE", insuranceExpiry, it, insurer, insuranceReminderDays) }, ::pickDate, ::dateText, ::daysLeft, reminderDays = insuranceReminderDays, onReminderDays = { insuranceReminderDays = it; prefs.edit().putInt(VehicleReminderScheduler.KEY_INSURANCE_REMINDER_DAYS, it).apply(); if (insuranceExpiry > 0) VehicleReminderScheduler.save(context, "INSURANCE", insuranceExpiry, insuranceNumber, insurer, it) }, extraLabel = "Insurer", extraValue = insurer, onExtraChange = { insurer = it; prefs.edit().putString(VehicleReminderScheduler.KEY_INSURER, it).apply(); if (insuranceExpiry > 0) VehicleReminderScheduler.save(context, "INSURANCE", insuranceExpiry, insuranceNumber, it, insuranceReminderDays) }, onClear = {
                VehicleReminderScheduler.clear(context, "INSURANCE"); insuranceExpiry = 0L; insuranceNumber = ""; insurer = ""
            })
        }

        featureCard {
            Text("REMINDER SCHEDULE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.2.sp)
            Text("Choose how many days before expiry the daily notification should start. One notification is sent each day from that date through the expiry date.", fontSize = 12.sp, lineHeight = 17.sp)
            Text("Calendar view", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { shiftMonth(-1) }) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month") }
                Text(SimpleDateFormat("MMMM yyyy", Locale.US).format(monthCursor.time), Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                IconButton(onClick = { shiftMonth(1) }) { Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month") }
            }
            val firstOffset = (monthCursor.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            val days = monthCursor.getActualMaximum(Calendar.DAY_OF_MONTH)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { listOf("M","T","W","T","F","S","S").forEach { Text(it, Modifier.width(44.dp), textAlign = TextAlign.Center, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            (List(firstOffset) { 0 } + (1..days).toList()).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    week.forEach { day ->
                        if (day == 0) Spacer(Modifier.size(44.dp)) else {
                            val dayMillis = monthCursor.clone().let { (it as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }.timeInMillis }
                            val hasReminder = sameDay(dayMillis, pucExpiry) || sameDay(dayMillis, insuranceExpiry)
                            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                Surface(Modifier.size(34.dp), CircleShape, color = if (hasReminder) MaterialTheme.colorScheme.primary.copy(alpha = .16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .20f)) {
                                    Box(contentAlignment = Alignment.Center) { Text(day.toString(), fontSize = 10.sp, fontWeight = if (hasReminder) FontWeight.Bold else FontWeight.Normal) }
                                }
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.size(44.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ReminderDocumentCard(
    title: String, type: String, expiry: Long, number: String, numberLabel: String,
    onNumber: (String) -> Unit, onDate: (String) -> Unit, dateText: (Long) -> String, daysLeft: (Long) -> Long,
    reminderDays: Int, onReminderDays: (Int) -> Unit,
    extraLabel: String? = null, extraValue: String = "", onExtraChange: ((String) -> Unit)? = null, onClear: () -> Unit
) {
    val days = daysLeft(expiry)
    Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (type == "PUC") Icons.Default.CheckCircle else Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(if (expiry > 0L) dateText(expiry) else "Expiry date not set", fontSize = 13.sp)
            if (expiry > 0L) {
                val statusColor = when { days < 0L -> Color(0xFFD95C5C); days <= 7L -> Color(0xFFD18A27); else -> MaterialTheme.colorScheme.onSurfaceVariant }
                val statusText = when { days < 0L -> "Expired"; days == 0L -> "Expires today"; else -> "$days days remaining" }
                Text(statusText, fontSize = 11.sp, color = statusColor, fontWeight = if (days <= 7L) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
        Button(onClick = { onDate(type) }, modifier = Modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(12.dp)) { Text(if (expiry > 0L) "Update" else "Set date") }
    }
    OutlinedTextField(value = number, onValueChange = onNumber, modifier = Modifier.fillMaxWidth(), label = { Text(numberLabel) }, singleLine = true)
    if (extraLabel != null && onExtraChange != null) OutlinedTextField(value = extraValue, onValueChange = onExtraChange, modifier = Modifier.fillMaxWidth(), label = { Text(extraLabel) }, singleLine = true)
    var reminderText by remember(reminderDays) { mutableStateOf(reminderDays.toString()) }
    OutlinedTextField(
        value = reminderText,
        onValueChange = { raw ->
            val filtered = raw.filter(Char::isDigit).take(3)
            reminderText = filtered
            filtered.toIntOrNull()?.coerceIn(1, 365)?.let(onReminderDays)
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Remind me before (days)") },
        supportingText = { Text("Daily notifications start ${reminderDays} days before expiry") },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
    )
    if (expiry > 0L) TextButton(onClick = onClear, modifier = Modifier.heightIn(min = 44.dp)) { Text("Clear reminder") }
}

private fun sameDay(a: Long, b: Long): Boolean {
    if (a <= 0L || b <= 0L) return false
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun DrivingInsightsScreen(
    trips: List<VehicleSnapshotDb.TripRecord>,
    settings: AppSettings,
    onBack: () -> Unit,
    historyLoaded: Boolean = true
) {
    val completed = remember(trips) { trips.filter { !it.active } }
    val eco = remember(completed) { completed.mapNotNull { it.avgEconomy }.filter { it > 0f } }
    val avgEco = eco.average().takeIf { eco.isNotEmpty() }?.toFloat()
    val best = eco.maxOrNull()
    val distance = completed.sumOf { it.distanceKm.toDouble() }.toFloat()
    val score = if (avgEco != null && best != null && best > 0f) {
        (70f + 30f * (avgEco / best).coerceIn(0f, 1f)).roundToInt()
    } else null
    var showMethodology by remember { mutableStateOf(false) }

    Page("Driving insights") {
        FeatureBackButton(onBack)
        if (!historyLoaded) {
            EmptyDataState(
                "LOADING DRIVING DATA…",
                Icons.Default.Route,
                "Loading completed trips and recorded economy."
            )
        } else {
            val scoreTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f)
            val scoreColor = MaterialTheme.colorScheme.primary
            val scoreFraction by animateFloatAsState(
                targetValue = ((score ?: 0) / 100f).coerceIn(0f, 1f),
                animationSpec = tween(450, easing = FastOutSlowInEasing),
                label = "score_ring"
            )
            featureCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "EFFICIENCY SCORE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showMethodology = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Score information", tint = scoreColor)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            val stroke = 9.dp.toPx()
                            drawArc(
                                scoreTrackColor,
                                -90f,
                                360f,
                                false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
                            )
                            drawArc(
                                scoreColor,
                                -90f,
                                360f * scoreFraction,
                                false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(score?.toString() ?: "—", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                            if (score != null) Text("/ 100", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            if (score != null) "Recorded economy score" else "No score yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Based on recorded trip economy only.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryTile("TRIPS", completed.size.toString(), Icons.Default.Route, Modifier.weight(1f))
                SummaryTile("DISTANCE", distanceValue(distance, settings), Icons.Default.Speed, Modifier.weight(1f))
                SummaryTile("AVG ECO", avgEco?.let { economyValue(it, settings) } ?: "—", Icons.Default.Eco, Modifier.weight(1f))
            }
            featureCard {
                Text(
                    "ECONOMY HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp
                )
                if (eco.isEmpty()) {
                    EmptyDataState(
                        "NO RECORDED TRIP ECONOMY",
                        Icons.Default.Eco,
                        "Recorded economy will appear after a completed trip contains an economy value."
                    )
                } else {
                    MiniBarChart(eco, "Trip economy", "Recorded trips", MaterialTheme.colorScheme.primary, settings.economy)
                }
            }
        }
    }

    if (showMethodology) {
        AlertDialog(
            onDismissRequest = { showMethodology = false },
            title = { Text("Efficiency score") },
            text = {
                Text(
                    "Based only on recorded trip economy. No unverified braking, gear or driving-behaviour signals are inferred.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = { TextButton(onClick = { showMethodology = false }) { Text("Close") } }
        )
    }
}

@Composable
fun FuelIntelligenceScreen(
    snapshot: VehicleSnapshotDb.Snapshot?,
    trips: List<VehicleSnapshotDb.TripRecord>,
    settings: AppSettings,
    onBack: () -> Unit,
    historyLoaded: Boolean = true
) {
    val completed = remember(trips) { trips.filter { !it.active } }
    val recent = remember(completed) { completed.take(10) }
    val avgDailyDistance = if (recent.isNotEmpty()) {
        recent.groupBy { dayStartMillis(it.startAt) }
            .values
            .map { day -> day.sumOf { it.distanceKm.toDouble() }.toFloat() }
            .average()
            .toFloat()
    } else 0f
    val dte = snapshot?.dteKm
    val days = if (dte != null && avgDailyDistance > 0f) dte / avgDailyDistance else null
    val ecoValues = recent.mapNotNull { it.avgEconomy }.filter { it > 0f }
    val eco = ecoValues.takeIf { it.isNotEmpty() }?.average()?.toFloat()
    var showEstimateInfo by remember { mutableStateOf(false) }

    Page("Fuel intelligence") {
        FeatureBackButton(onBack)
        if (!historyLoaded) {
            EmptyDataState("LOADING FUEL INTELLIGENCE…", Icons.Default.GasMeter, "Loading recorded trip history.")
        } else {
            featureCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "RANGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showEstimateInfo = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Range information", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                val rangeText = dte?.let { distanceValue(it.toFloat(), settings, 0) } ?: "—"
                AltrozAnimatedText(rangeText) { animated ->
                    Text(
                        animated,
                        fontSize = stableLargeValueSize(animated, 38f),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Text("Last recorded from the vehicle", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryTile("FUEL", snapshot?.fuelPercent?.let { "$it%" } ?: "—", Icons.Default.GasMeter, Modifier.weight(1f))
                SummaryTile("AVG ECO", eco?.let { economyValue(it, settings) } ?: "—", Icons.Default.Eco, Modifier.weight(1f))
                SummaryTile("DAYS*", days?.let { String.format(Locale.US, "%.1f", it) } ?: "—", Icons.Default.Schedule, Modifier.weight(1f))
            }
        }
    }

    if (showEstimateInfo) {
        AlertDialog(
            onDismissRequest = { showEstimateInfo = false },
            title = { Text("Fuel intelligence") },
            text = {
                Text(
                    "Days estimate uses your recorded daily distance and current DTE; it is a planning estimate, not a vehicle forecast.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = { TextButton(onClick = { showEstimateInfo = false }) { Text("Close") } }
        )
    }
}

@Composable
fun VehicleTrendsScreen(
    samples: List<VehicleSnapshotDb.TelemetrySample>,
    settings: AppSettings,
    onBack: () -> Unit,
    historyLoaded: Boolean = true
) {
    val fuel = remember(samples) { dailyLastTelemetry(samples) { it.fuelPercent?.toFloat() } }
    val dte = remember(samples) { dailyLastTelemetry(samples) { it.dteKm?.toFloat() } }
    val odo = remember(samples) { dailyLastTelemetry(samples) { it.odo?.toFloat() } }
    Page("Vehicle trends") {
        FeatureBackButton(onBack)
        if (!historyLoaded) {
            EmptyDataState("LOADING VEHICLE TRENDS…", Icons.Default.Speed, "Loading recorded daily vehicle values.")
        } else {
            featureCard {
                Text("FUEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (fuel.isEmpty()) EmptyDataState("NO FUEL HISTORY", Icons.Default.GasMeter, "Fuel history appears after valid fuel values are recorded.")
                else MiniBarChart(fuel.takeLast(8).map { it.value }, "Daily fuel", "", MaterialTheme.colorScheme.primary, "%")
            }
            featureCard {
                Text("DISTANCE TO EMPTY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (dte.isEmpty()) EmptyDataState("NO DTE HISTORY", Icons.Default.Route, "Distance-to-empty history appears after valid DTE values are recorded.")
                else {
                    val display = if (settings.distance == "mi") dte.takeLast(8).map { it.value * 0.621371f } else dte.takeLast(8).map { it.value }
                    MiniBarChart(display, "Daily DTE", "", MaterialTheme.colorScheme.primary, if (settings.distance == "mi") "mi" else "km")
                }
            }
            featureCard {
                Text("ODOMETER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (odo.isEmpty()) EmptyDataState("NO ODOMETER HISTORY", Icons.Default.Speed, "Odometer history appears after valid odometer values are recorded.")
                else {
                    val display = if (settings.distance == "mi") odo.takeLast(8).map { it.value * 0.621371f } else odo.takeLast(8).map { it.value }
                    MiniBarChart(display, "Daily odometer", "", MaterialTheme.colorScheme.primary, if (settings.distance == "mi") "mi" else "km")
                }
            }
        }
    }
}

@Composable
fun SinceLastDriveScreen(
    trips: List<VehicleSnapshotDb.TripRecord>,
    settings: AppSettings,
    onBack: () -> Unit,
    historyLoaded: Boolean = true
) {
    val completed = remember(trips) { trips.filter { !it.active }.sortedByDescending { it.endAt ?: it.startAt } }
    val last = completed.firstOrNull()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(last?.id) {
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000L)
        }
    }
    val elapsed = last?.let { ((nowMs - (it.endAt ?: it.startAt)).coerceAtLeast(0L) / 3_600_000L) }
    Page("Since last drive") {
        FeatureBackButton(onBack)
        if (!historyLoaded) {
            EmptyDataState("LOADING LAST DRIVE…", Icons.Default.Schedule, "Loading your most recent completed trip.")
        } else {
            featureCard {
                Text("LAST DRIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.2.sp)
                Text(if (last == null) "No completed trip yet" else "${elapsed ?: 0}h ago", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (last != null) {
                    Text("${distanceValue(last.distanceKm, settings)} • ${last.avgEconomy?.let { economyValue(it, settings) } ?: "—"}", fontSize = 13.sp)
                    Text(SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.US).format(Date(last.endAt ?: last.startAt)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryTile("TOTAL TRIPS", completed.size.toString(), Icons.Default.Route, Modifier.weight(1f))
                SummaryTile("TOTAL DIST", distanceValue(completed.sumOf { it.distanceKm.toDouble() }.toFloat(), settings), Icons.Default.Speed, Modifier.weight(1f))
            }
        }
    }
}
