package com.altrozdrive.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun More(
    vehicle: VehicleProfile,
    bluetoothState: BluetoothDiagnosticState,
    onDetail: (String) -> Unit
) {
    Page("More", showHeader = false) {
        Surface(
            Modifier.fillMaxWidth(),
            RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ){
            // Keep the three visual zones independent: status pill, car, and vehicle text.
            // The pill is overlaid in the upper-left, the car has its own bounded area,
            // and the text gets the remaining width. This prevents Altroz XZA+S from
            // being squeezed or clipped and keeps the floating car away from the pill.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(104.dp)
            ){
                // Status stays inside the card and does not consume the vehicle-text width.
                val isLive = bluetoothState.htsConnected && bluetoothState.liveVisValidated
                val hasSaved = (bluetoothState.cachedOdo ?: 0) > 0 || (bluetoothState.cachedFuelPercent ?: 0) > 0 || (bluetoothState.cachedDteKm ?: 0) > 0
                MoreStatusPill(
                    text = when { isLive -> "LIVE"; hasSaved -> "SAVED"; else -> "OFFLINE" },
                    live = isLive,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 10.dp)
                )

                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, end = 14.dp, top = 18.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    // Dedicated car zone. The image is shifted slightly right and
                    // vertically separated from the pill so the two never fuse.
                    Box(
                        Modifier
                            .width(174.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ){
                        AltrozCutoutImage(
                            modifier = Modifier
                                .fillMaxWidth(.88f)
                                .fillMaxHeight(.86f),
                            accent = MaterialTheme.colorScheme.primary,
                            accentStrength = 0.16f,
                            idleAnimation = isLive
                        )
                    }
                    Spacer(Modifier.width(8.dp))

                    // Dedicated text zone. No status pill is allowed to reduce this width.
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.Start
                    ){
                        Text(
                            "${vehicle.model} ${vehicle.trim}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            softWrap = true,
                            lineHeight = 17.sp
                        )
                        Text(
                            "${vehicle.fuel} • ${vehicle.transmission}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            softWrap = true,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
        MoreSectionHeader("VEHICLE")
        MoreModernItem("Vehicle profile","Model, trim and drivetrain",Icons.Default.DirectionsCar,{onDetail("profile")},Color(0xFF1597FF))

        MoreSectionHeader("DRIVING")
        MoreModernItem("Driving insights","Efficiency score and recorded-trip analysis",Icons.Default.Eco,{onDetail("driving_insights")},LeafGreen)
        MoreModernItem("Since last drive","Last-drive summary and ownership snapshot",Icons.Default.Schedule,{onDetail("since_last_drive")},Color(0xFF35B9FF))

        MoreSectionHeader("VEHICLE DATA")
        MoreModernItem("Fuel intelligence","DTE, fuel and history-based range planning",Icons.Default.GasMeter,{onDetail("fuel_intelligence")},Amber)
        MoreModernItem("Vehicle trends","Fuel, DTE and odometer history",Icons.Default.Speed,{onDetail("vehicle_trends")},Color(0xFF8E6CFF))

        MoreSectionHeader("REMINDERS & UPKEEP")
        MoreModernItem("Vehicle reminders","PUC and insurance expiry calendar",Icons.Default.Notifications,{onDetail("reminders")},Amber)

        MoreSectionHeader("DATA")
        MoreModernItem("Data & history","Backup, restore, routes and driving calendar",Icons.Default.CalendarMonth,{onDetail("data")},Color(0xFF8E6CFF))

        MoreSectionHeader("APP")
        MoreModernItem("Settings","Theme, units and permissions",Icons.Default.Settings,{onDetail("settings")},MaterialTheme.colorScheme.primary)
        MoreModernItem("About","Version and application information",Icons.Default.Info,{onDetail("about")},Muted)
    }
}

@Composable
private fun MoreStatusPill(
    text: String,
    live: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (live) Color(0xFF35C759) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun MoreSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun MoreModernItem(title:String,sub:String,icon:ImageVector,onClick:()->Unit,tint:Color){
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = AltrozUiTokens.touchTarget),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .68f))
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), RoundedCornerShape(13.dp), color = tint.copy(alpha = .10f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(sub, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, softWrap = true, lineHeight = 14.sp)
            }
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
        }
    }
}
