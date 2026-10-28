package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.JarvisSettings
import com.example.data.model.SystemStats
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDeepBlue
import com.example.ui.theme.JarvisSuccess
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun DiagnosticsSheet(
    settings: JarvisSettings,
    stats: SystemStats,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .testTag("diagnostics_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(JarvisSuccess)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYSTEM TELEMETRY",
                            color = JarvisCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("btn_close_diagnostics")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = JarvisTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DiagnosticRow(
                    icon = Icons.Default.Security,
                    title = "SYSTEM ARCHITECTURE",
                    value = "RAKIBX9 JARVIS v2.0",
                    status = "ONLINE"
                )

                DiagnosticRow(
                    icon = Icons.Default.Memory,
                    title = "COMMANDER IDENTITY",
                    value = settings.ownerName,
                    status = "AUTHORIZED"
                )

                DiagnosticRow(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "VOICE ENGINE",
                    value = "TTS + SpeechRecognizer",
                    status = if (settings.isMuted) "MUTED" else "ACTIVE"
                )

                DiagnosticRow(
                    icon = Icons.Default.Wifi,
                    title = "INTERNET KNOWLEDGE ENGINE",
                    value = "Live Wikipedia + DuckDuckGo + Weather + Search Grounding",
                    status = "ONLINE"
                )

                DiagnosticRow(
                    icon = Icons.Default.Security,
                    title = "AI NEURAL LINK",
                    value = if (settings.customApiKey.isNotBlank()) "CUSTOM KEY CONNECTED" else "ONLINE / BUILT-IN",
                    status = "READY"
                )

                DiagnosticRow(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "POWER SOURCE",
                    value = "Device Internal Cell",
                    status = "OPTIMAL"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisDeepBlue.copy(alpha = 0.5f))
                        .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "All neural sub-routines, language parsers (English & Bangla), and device execution subsystems are functioning within nominal parameters.",
                        color = JarvisTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    icon: ImageVector,
    title: String,
    value: String,
    status: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisCardBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = JarvisCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    color = JarvisTextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    color = JarvisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Text(
            text = status,
            color = if (status == "MUTED") JarvisAmber else JarvisSuccess,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
