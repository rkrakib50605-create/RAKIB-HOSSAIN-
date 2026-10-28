package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.JarvisSettings
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisDeepBlue
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun SettingsDialog(
    currentSettings: JarvisSettings,
    onSave: (JarvisSettings) -> Unit,
    onTestVoice: () -> Unit,
    onDismiss: () -> Unit
) {
    var ownerName by remember { mutableStateOf(currentSettings.ownerName) }
    var speechRate by remember { mutableFloatStateOf(currentSettings.speechRate) }
    var speechPitch by remember { mutableFloatStateOf(currentSettings.speechPitch) }
    var isMuted by remember { mutableStateOf(currentSettings.isMuted) }
    var selectedLanguage by remember { mutableStateOf(currentSettings.preferredLanguage) }
    var customApiKey by remember { mutableStateOf(currentSettings.customApiKey) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = JarvisCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "JARVIS CONFIGURATION",
                            color = JarvisCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("btn_close_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = JarvisTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Owner Name Input
                Text(
                    text = "COMMANDER NAME",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedContainerColor = JarvisCardBg,
                        unfocusedContainerColor = JarvisCardBg
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_owner_name")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Language Mode
                Text(
                    text = "VOICE LANGUAGE RECOGNITION",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("AUTO" to "Auto-Detect", "EN" to "English", "BN" to "বাংলা (Bangla)").forEach { (code, label) ->
                        FilterChip(
                            selected = selectedLanguage == code,
                            onClick = { selectedLanguage = code },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisDeepBlue,
                                selectedLabelColor = JarvisCyan,
                                containerColor = JarvisCardBg,
                                labelColor = JarvisTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedLanguage == code,
                                borderColor = if (selectedLanguage == code) JarvisCyan else JarvisBorder
                            ),
                            modifier = Modifier.testTag("chip_lang_$code")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Speech Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "VOICE SPEED",
                        color = JarvisTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "%.2fx (%.0f wpm)".format(speechRate, speechRate * 160f),
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = speechRate,
                    onValueChange = { speechRate = it },
                    valueRange = 0.6f..1.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisCyan,
                        activeTrackColor = JarvisCyan,
                        inactiveTrackColor = JarvisDeepBlue
                    ),
                    modifier = Modifier.testTag("slider_speech_rate")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Speech Pitch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "VOICE PITCH",
                        color = JarvisTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "%.2fx".format(speechPitch),
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = speechPitch,
                    onValueChange = { speechPitch = it },
                    valueRange = 0.6f..1.4f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisCyan,
                        activeTrackColor = JarvisCyan,
                        inactiveTrackColor = JarvisDeepBlue
                    ),
                    modifier = Modifier.testTag("slider_speech_pitch")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mute Voice Audio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MUTE AUDIO OUTPUT",
                            color = JarvisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Replies will appear in text only",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isMuted,
                        onCheckedChange = { isMuted = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisAmber,
                            checkedTrackColor = JarvisDeepBlue,
                            uncheckedThumbColor = JarvisTextSecondary,
                            uncheckedTrackColor = JarvisCardBg
                        ),
                        modifier = Modifier.testTag("switch_mute_voice")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom API Key field
                Text(
                    text = "CUSTOM API KEY (GEMINI / OPENAI)",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = customApiKey,
                    onValueChange = { customApiKey = it },
                    singleLine = true,
                    placeholder = { Text("Paste key (or configured via Secrets)", color = JarvisTextSecondary.copy(alpha = 0.5f), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedContainerColor = JarvisCardBg,
                        unfocusedContainerColor = JarvisCardBg
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_api_key")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Test voice button
                OutlinedButton(
                    onClick = onTestVoice,
                    modifier = Modifier.fillMaxWidth().testTag("btn_test_voice"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = JarvisCyanLight
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Test Voice",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Voice Synthesis", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save button
                Button(
                    onClick = {
                        onSave(
                            currentSettings.copy(
                                ownerName = ownerName.ifBlank { "Rakib" },
                                speechRate = speechRate,
                                speechPitch = speechPitch,
                                isMuted = isMuted,
                                preferredLanguage = selectedLanguage,
                                customApiKey = customApiKey
                            )
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisCyan,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("btn_save_settings")
                ) {
                    Text("SAVE CONFIGURATION", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}
