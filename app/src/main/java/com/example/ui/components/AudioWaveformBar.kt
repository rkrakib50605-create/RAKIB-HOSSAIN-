package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import kotlin.math.sin

@Composable
fun AudioWaveformBar(
    isActive: Boolean,
    audioLevel: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "WaveAnimation")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val barCount = 32
        val spacing = size.width / (barCount * 1.5f)
        val barWidth = spacing * 0.7f
        val centerY = size.height / 2f
        val maxBarHeight = size.height * 0.85f

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            val sinVal = sin(progress * 6.283f + wavePhase).toFloat()
            val baseHeight = if (isActive) {
                // Dynamic height modulated by audioLevel and sine wave
                val dynamicFactor = 0.25f + (audioLevel * 0.75f)
                (maxBarHeight * (0.3f + 0.7f * kotlin.math.abs(sinVal)) * dynamicFactor)
                    .coerceIn(4.dp.toPx(), maxBarHeight)
            } else {
                3.dp.toPx()
            }

            val x = (i * spacing * 1.5f) + (spacing / 2)
            val topY = centerY - (baseHeight / 2)

            val barAlpha = if (isActive) 0.6f + 0.4f * kotlin.math.abs(sinVal) else 0.25f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        JarvisCyan.copy(alpha = barAlpha),
                        JarvisElectricBlue.copy(alpha = barAlpha)
                    )
                ),
                topLeft = Offset(x, topY),
                size = Size(barWidth, baseHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
