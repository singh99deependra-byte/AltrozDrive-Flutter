package com.altrozdrive.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Settings UI is isolated from the Activity so the Activity remains an orchestration layer. */
@Composable
fun Settings(s: AppSettings, on: (AppSettings) -> Unit) {
    CardBox {
        Text("Appearance", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text("Theme", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        val themes = listOf(
            "auto" to "Auto • System",
            "amoled" to "AMOLED",
            "daylight" to "Daylight",
            "pearl" to "Pearl",
            "sandstone" to "Sandstone",
            "graphite" to "Graphite",
            "obsidian" to "Obsidian",
            "titanium" to "Titanium",
            "carbon" to "Carbon"
        )
        themes.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (key, label) ->
                    ThemeChoice(key, label, s.appearance == key, Modifier.weight(1f)) { on(s.copy(appearance = key)) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
    CardBox {
        Text("Accent", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        val accents = listOf(
            "gold" to "Gold", "cyan" to "Cyan", "emerald" to "Emerald",
            "amber" to "Amber", "turquoise" to "Turquoise", "jade" to "Jade",
            "olive_gold" to "Olive Gold"
        )
        accents.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (key, label) ->
                    AccentChoice(key, label, s.accent == key, Modifier.weight(1f)) { on(s.copy(accent = key)) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    CardBox {
        Text("Units", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        SettingDropdownRow(Icons.Default.Speed, "Distance & speed", s.distance, listOf("km" to "Kilometres (km)", "mi" to "Miles (mi)")) { on(s.copy(distance = it)) }
        SettingDropdownRow(Icons.Default.Eco, "Fuel economy", s.economy, listOf("km/l" to "km/L", "l/100 km" to "L/100 km", "mpg" to "MPG")) { on(s.copy(economy = it)) }
        SettingDropdownRow(Icons.Default.Thermostat, "Temperature", s.temperature, listOf("°C" to "Celsius (°C)", "°F" to "Fahrenheit (°F)")) { on(s.copy(temperature = it)) }
    }
}

@Composable
private fun ThemeChoice(key: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val palette = themePalette(key)
    Card(
        modifier = modifier.heightIn(min = AltrozUiTokens.touchTarget).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else androidx.compose.foundation.BorderStroke(1.dp, palette.surfaceVariant)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(palette.background)) {
                    Box(Modifier.fillMaxWidth().height(6.dp).align(Alignment.BottomCenter).background(palette.secondary))
                    Box(Modifier.size(8.dp).align(Alignment.Center).clip(CircleShape).background(palette.tertiary))
                }
                Box(Modifier.height(30.dp), contentAlignment = Alignment.CenterStart) {
                    Text(label, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = palette.onSurface, maxLines = 2, lineHeight = 14.sp)
                }
            }
            Text(if (selected) "Selected" else "Tap to use", fontSize = 9.sp, color = if (selected) MaterialTheme.colorScheme.primary else palette.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccentChoice(key: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = accentColor(key)
    Column(modifier.heightIn(min = AltrozUiTokens.touchTarget).clickable(onClick = onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(if (selected) 30.dp else 26.dp).clip(CircleShape).background(color).then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier))
        Text(label, fontSize = 9.sp, fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium, color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, lineHeight = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    val tint = if (icon == Icons.Default.Eco) LeafGreen else MaterialTheme.colorScheme.primary
    Surface(color = tint.copy(alpha = .12f), shape = RoundedCornerShape(14.dp), modifier = Modifier.size(42.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun SettingDropdownRow(icon: ImageVector, label: String, selected: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingsDropdown(label, selected, options, onSelect, compact = true)
        }
    }
}

/** Shared by vehicle profile and settings because both use the same production dropdown. */
@Composable
fun SettingsDropdown(label: String, selected: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit, compact: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: selected
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, tween(140, easing = FastOutSlowInEasing), label = "dropdownArrow")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        if (!compact) Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Box(Modifier.fillMaxWidth()) {
            Surface(
                onClick = { expanded = !expanded },
                shape = RoundedCornerShape(15.dp),
                color = if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = .07f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = .55f) else MaterialTheme.colorScheme.outline.copy(alpha = .45f)),
                modifier = Modifier.fillMaxWidth().height(if (compact) 48.dp else 52.dp)
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        if (compact) Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(selectedLabel, fontSize = if (compact) 14.sp else 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 2, lineHeight = if (compact) 16.sp else 17.sp)
                    }
                    Text("⌄", fontSize = 19.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.graphicsLayer { rotationZ = arrowRotation })
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.widthIn(min = 220.dp, max = 340.dp)) {
                options.forEach { (value, labelText) ->
                    val isSelected = value == selected
                    DropdownMenuItem(
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(labelText, fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal)
                                if (isSelected) Text("Currently selected", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        leadingIcon = {
                            Surface(color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = .14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f), shape = CircleShape, modifier = Modifier.size(30.dp)) {
                                Box(contentAlignment = Alignment.Center) { if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp)) }
                            }
                        },
                        trailingIcon = if (isSelected) ({ Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }) else null,
                        onClick = { expanded = false; onSelect(value) },
                        modifier = Modifier.padding(horizontal = 5.dp).background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = .06f) else Color.Transparent, RoundedCornerShape(13.dp))
                    )
                }
            }
        }
    }
}
