package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisMode
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisDeepBlue
import com.example.ui.theme.JarvisElectricBlue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
    mode: JarvisMode,
    audioLevel: Float,
    statusText: String,
    onCoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorRotation")

    // Slow clockwise rotation for outer tech ring
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRing"
    )

    // Faster counter-clockwise rotation for inner segmented ring
    val innerAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRing"
    )

    // Pulse animation for core breathing
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (mode == JarvisMode.LISTENING) 500 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Wave ripple for listening/speaking
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    val (primaryGlowColor, coreColor) = when (mode) {
        JarvisMode.LISTENING -> JarvisCyan to JarvisCyanLight
        JarvisMode.PROCESSING -> JarvisAmber to JarvisAmber
        JarvisMode.SPEAKING -> JarvisElectricBlue to JarvisCyanLight
        JarvisMode.ERROR -> JarvisCrimson to JarvisCrimson
        JarvisMode.IDLE -> JarvisCyan to JarvisCyan
    }

    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(170.dp)
                .testTag("arc_reactor_core")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCoreClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Ripple ring when active
            if (mode == JarvisMode.LISTENING || mode == JarvisMode.SPEAKING) {
                Box(
                    modifier = Modifier
                        .size(160.dp * rippleScale)
                        .border(
                            width = 1.5.dp,
                            color = primaryGlowColor.copy(alpha = (1f - (rippleScale - 1f) / 0.35f).coerceIn(0f, 1f)),
                            shape = CircleShape
                        )
                )
            }

            // Canvas drawing high-tech holographic arcs, ticks, and energy rings
            Canvas(modifier = Modifier.size(170.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.width / 2

                // Outer tech ring background
                drawCircle(
                    color = JarvisDeepBlue.copy(alpha = 0.35f),
                    radius = maxRadius - 6.dp.toPx(),
                    center = center
                )

                // Outer decorative dashed orbit
                drawCircle(
                    color = primaryGlowColor.copy(alpha = 0.4f),
                    radius = maxRadius - 8.dp.toPx(),
                    center = center,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), outerAngle * 0.5f)
                    )
                )

                // Intermediate segmented arc ticks
                val tickCount = 24
                val tickRadiusOuter = maxRadius - 16.dp.toPx()
                val tickRadiusInner = maxRadius - 22.dp.toPx()
                for (i in 0 until tickCount) {
                    val angleDeg = (i * (360f / tickCount) + innerAngle) % 360f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val start = Offset(
                        (center.x + tickRadiusInner * cos(angleRad)).toFloat(),
                        (center.y + tickRadiusInner * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        (center.x + tickRadiusOuter * cos(angleRad)).toFloat(),
                        (center.y + tickRadiusOuter * sin(angleRad)).toFloat()
                    )
                    val tickAlpha = if (i % 3 == 0) 0.85f else 0.3f
                    drawLine(
                        color = primaryGlowColor.copy(alpha = tickAlpha),
                        start = start,
                        end = end,
                        strokeWidth = if (i % 3 == 0) 2.5.dp.toPx() else 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Inner energy ring with dynamic audio reactive thickness
                val effectiveAudioBoost = audioLevel * 14.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlowColor.copy(alpha = 0.7f),
                            JarvisDeepBlue.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = 48.dp.toPx() + effectiveAudioBoost
                    ),
                    radius = 48.dp.toPx() + effectiveAudioBoost,
                    center = center
                )

                // Cyan core glow border
                drawCircle(
                    color = primaryGlowColor,
                    radius = 38.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Center glowing core button
            Box(
                modifier = Modifier
                    .size((68 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                coreColor.copy(alpha = 0.95f),
                                primaryGlowColor.copy(alpha = 0.8f),
                                JarvisDeepBlue
                            )
                        )
                    )
                    .border(2.dp, primaryGlowColor, CircleShape)
                    .shadow(16.dp, CircleShape, spotColor = primaryGlowColor),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (mode) {
                    JarvisMode.LISTENING -> Icons.Default.Mic
                    JarvisMode.SPEAKING -> Icons.AutoMirrored.Filled.VolumeUp
                    JarvisMode.PROCESSING -> Icons.Default.Mic
                    JarvisMode.ERROR -> Icons.Default.MicOff
                    JarvisMode.IDLE -> Icons.Default.Mic
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "JARVIS Core",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Status badge
        val modeLabel = when (mode) {
            JarvisMode.LISTENING -> "● LISTENING TO RAKIB"
            JarvisMode.PROCESSING -> "▲ PROCESSING COMMAND"
            JarvisMode.SPEAKING -> "▶ JARVIS SPEAKING"
            JarvisMode.ERROR -> "✕ SYSTEM ALERT"
            JarvisMode.IDLE -> "■ SYSTEM STANDBY"
        }

        Text(
            text = modeLabel,
            color = primaryGlowColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = statusText,
            color = JarvisCyanLight.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
