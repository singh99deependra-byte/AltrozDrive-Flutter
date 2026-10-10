package com.altrozdrive.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun AltrozAnimatedColor(target: Color, label: String): Color {
    return animateColorAsState(
        targetValue = target,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = label
    ).value
}

/** UI-only motion. No vehicle, Bluetooth, database, GPS or telemetry state is owned here. */
@Composable
fun AltrozRevealModifier(key: Any, delayMillis: Int = 0): Modifier {
    var shown by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        if (delayMillis > 0) kotlinx.coroutines.delay(delayMillis.toLong())
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "altroz_reveal"
    )
    return Modifier.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 6.dp.toPx()
        scaleX = .99f + progress * .01f
        scaleY = .99f + progress * .01f
    }
}

@Composable
fun AltrozCarIdleModifier(live: Boolean = true): Modifier {
    if (!live) return Modifier
    val transition = rememberInfiniteTransition(label = "altroz_car_idle")
    val lift by transition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "altroz_car_lift"
    )
    val scale by transition.animateFloat(
        initialValue = .998f,
        targetValue = 1.002f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "altroz_car_scale"
    )
    return Modifier.graphicsLayer {
        translationY = lift.dp.toPx()
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Numeric-first transition for telemetry metric strings. The numeric part is
 * interpolated while its existing suffix/unit is preserved. Non-numeric values
 * such as "—" fall back to a short cross-fade.
 */
@Composable
fun AltrozAnimatedText(value: String, modifier: Modifier = Modifier, content: @Composable (String) -> Unit) {
    val match = remember(value) {
        Regex("^\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)(.*)$").matchEntire(value)
    }
    if (match == null) {
        androidx.compose.animation.Crossfade(
            targetState = value,
            animationSpec = tween(160, easing = FastOutSlowInEasing),
            modifier = modifier,
            label = "altroz_text_change"
        ) { current -> content(current) }
        return
    }

    val raw = match.groupValues[1]
    val suffix = match.groupValues[2]
    val target = raw.replace(",", "").toFloatOrNull()
    if (target == null) {
        content(value)
        return
    }
    val decimals = raw.substringAfter('.', "").length
    val initial = remember { target }
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = when {
                suffix.contains("RPM", true) -> 220
                suffix.contains("km/h", true) || suffix.contains("mph", true) -> 220
                else -> 420
            },
            easing = FastOutSlowInEasing
        ),
        label = "altroz_numeric_change"
    )
    val formatted = if (decimals == 0) {
        String.format(Locale.US, "%,d", animated.toInt())
    } else {
        String.format(Locale.US, "%,.${decimals}f", animated)
    }
    content(formatted + suffix)
}

@Composable
fun AltrozAnimatedFraction(target: Float, delayMillis: Int = 0): Float {
    val boundedTarget = target.coerceIn(0f, 1f)
    val previousTarget = remember { boundedTarget }
    var animationTarget by remember { mutableStateOf(previousTarget) }
    LaunchedEffect(boundedTarget, delayMillis) {
        if (delayMillis > 0) kotlinx.coroutines.delay(delayMillis.toLong())
        animationTarget = boundedTarget
    }
    return animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "altroz_chart_fraction"
    ).value
}

@Composable
fun AltrozAnimatedFractionSlow(target: Float, delayMillis: Int = 0): Float {
    val boundedTarget = target.coerceIn(0f, 1f)
    val previousTarget = remember { boundedTarget }
    var animationTarget by remember { mutableStateOf(previousTarget) }
    LaunchedEffect(boundedTarget, delayMillis) {
        if (delayMillis > 0) kotlinx.coroutines.delay(delayMillis.toLong())
        animationTarget = boundedTarget
    }
    return animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "altroz_slow_fraction"
    ).value
}

@Composable
fun AltrozLiveDot(live: Boolean, color: Color, modifier: Modifier = Modifier) {
    if (!live) {
        Box(modifier.background(color.copy(alpha = .45f), CircleShape))
        return
    }
    val transition = rememberInfiniteTransition(label = "altroz_live_dot")
    val pulse by transition.animateFloat(
        initialValue = .72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "altroz_live_pulse"
    )
    Box(modifier.background(color.copy(alpha = pulse), CircleShape))
}
