package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.JarvisMode
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.DiagnosticsSheet
import com.example.ui.components.QuickCommandChips
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisBorderHighlight
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.JarvisDeepBlue
import com.example.ui.theme.JarvisSuccess
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    viewModel: JarvisViewModel,
    onLaunchIntent: (android.content.Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val jarvisMode by viewModel.jarvisMode.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val systemStats by viewModel.systemStats.collectAsState()
    val audioRmsLevel by viewModel.audioRmsLevel.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDiagnosticsSheet by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Listen for outgoing intent actions (YouTube, Google, Camera)
    LaunchedEffect(Unit) {
        viewModel.actionIntent.collect { intent ->
            try {
                onLaunchIntent(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open action: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Scroll to latest message whenever message list updates
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Live digital clock string
    var liveClock by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            liveClock = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(Date())
            delay(1000)
        }
    }

    // Permission launcher for microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required for voice commands, ${settings.ownerName}.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestVoiceCommand() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.toggleListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisDarkBg),
        containerColor = JarvisDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ================= 1. TOP HUD HEADER =================
            TopHudHeader(
                ownerName = settings.ownerName,
                clockTime = liveClock,
                isMuted = settings.isMuted,
                onToggleMute = { viewModel.toggleMute() },
                onDiagnosticsClick = { showDiagnosticsSheet = true },
                onSettingsClick = { showSettingsDialog = true },
                onClearChat = { viewModel.clearChat() }
            )

            // ================= 2. ARC REACTOR CORE VISUALIZER =================
            ArcReactorVisualizer(
                mode = jarvisMode,
                audioLevel = audioRmsLevel,
                statusText = statusText,
                onCoreClick = { requestVoiceCommand() },
                modifier = Modifier.fillMaxWidth()
            )

            // ================= 3. AUDIO WAVEFORM BARS =================
            AudioWaveformBar(
                isActive = isListening || isSpeaking,
                audioLevel = audioRmsLevel,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            // ================= 4. QUICK COMMAND CHIPS =================
            QuickCommandChips(
                onCommandClick = { cmd ->
                    viewModel.handleUserCommand(cmd)
                }
            )

            // ================= 5. CONVERSATION TELEMETRY FEED =================
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageItem(
                        message = message,
                        ownerName = settings.ownerName,
                        onReplayAudio = { viewModel.replayMessage(it) },
                        onActionClick = { url ->
                            try {
                                val browserIntent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(url)
                                )
                                onLaunchIntent(browserIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open: $url", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // ================= 6. BOTTOM COMMAND DOCK =================
            BottomControlDock(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.handleUserCommand(inputText)
                        inputText = ""
                    }
                },
                isListening = isListening,
                isSpeaking = isSpeaking,
                onMicClick = { requestVoiceCommand() },
                onStopSpeaking = { viewModel.stopSpeaking() }
            )
        }
    }

    // Settings Modal
    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            onSave = { updated ->
                viewModel.updateSettings(updated)
            },
            onTestVoice = { viewModel.testSpeech() },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Diagnostics Modal
    if (showDiagnosticsSheet) {
        DiagnosticsSheet(
            settings = settings,
            stats = systemStats,
            onDismiss = { showDiagnosticsSheet = false }
        )
    }
}

@Composable
private fun TopHudHeader(
    ownerName: String,
    clockTime: String,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onDiagnosticsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onClearChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Branding and Online Dot
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(JarvisSuccess)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "RAKIBX9 JARVIS",
                    color = JarvisCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "COMMANDER: ${ownerName.uppercase()}",
                    color = JarvisAmber,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center: Digital Clock + Internet Live badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisDeepBlue.copy(alpha = 0.5f))
                    .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = clockTime,
                    color = JarvisCyanLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisDeepBlue.copy(alpha = 0.5f))
                    .border(1.dp, JarvisSuccess.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(JarvisSuccess)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "WEB LIVE",
                        color = JarvisSuccess,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Right Action Buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier.size(36.dp).testTag("btn_mute_toggle")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) JarvisAmber else JarvisCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDiagnosticsClick,
                modifier = Modifier.size(36.dp).testTag("btn_telemetry")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Diagnostics",
                    tint = JarvisCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onClearChat,
                modifier = Modifier.size(36.dp).testTag("btn_clear_chat")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear History",
                    tint = JarvisTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(36.dp).testTag("btn_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = JarvisCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomControlDock(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isListening: Boolean,
    isSpeaking: Boolean,
    onMicClick: () -> Unit,
    onStopSpeaking: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MicPulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text Input Field
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputTextChange,
            placeholder = {
                Text(
                    text = if (isListening) "Listening to you…" else "Type command or ask JARVIS…",
                    color = JarvisTextSecondary.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary,
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = JarvisBorder,
                focusedContainerColor = JarvisCardBg,
                unfocusedContainerColor = JarvisCardBg
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("command_input_field")
        )

        Spacer(modifier = Modifier.width(8.dp))

        // If user typed something, show Send button; otherwise show Mic button
        if (inputText.isNotBlank()) {
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(JarvisCyan)
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else if (isSpeaking) {
            // Stop speaking button
            IconButton(
                onClick = onStopSpeaking,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(JarvisAmber)
                    .testTag("btn_stop_speaking")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop Speech",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            // Prominent Voice Mic FAB
            FloatingActionButton(
                onClick = onMicClick,
                shape = CircleShape,
                containerColor = if (isListening) JarvisAmber else JarvisCyan,
                contentColor = Color.Black,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .size(52.dp)
                    .scale(micScale)
                    .testTag("mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop Listening" else "Speak to JARVIS",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
