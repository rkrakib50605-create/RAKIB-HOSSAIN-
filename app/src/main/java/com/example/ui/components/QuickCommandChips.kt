package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDeepBlue

data class QuickAction(
    val label: String,
    val command: String,
    val isInternetAction: Boolean = false
)

@Composable
fun QuickCommandChips(
    onCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickActions = listOf(
        QuickAction("🌐 Ask Internet Anything", "Tell me about James Webb Space Telescope", true),
        QuickAction("🌦️ Weather in Dhaka", "What is the weather in Dhaka?", true),
        QuickAction("🔬 What is Quantum Computing?", "What is Quantum Computing?", true),
        QuickAction("🌍 Who is Lionel Messi?", "Who is Lionel Messi?", true),
        QuickAction("🧮 125 * 48", "125 * 48", true),
        QuickAction("🇧🇩 বাংলাদেশের ইতিহাস বলো", "বাংলাদেশের ইতিহাস সংক্ষেপে বলো", true),
        QuickAction("⏰ What time is it?", "What time is it?"),
        QuickAction("📅 Today's Date", "What is today's date?"),
        QuickAction("▶️ Open YouTube", "open youtube"),
        QuickAction("🔍 Open Google", "open google"),
        QuickAction("🔋 Battery Status", "battery status"),
        QuickAction("📷 Open Camera", "open camera"),
        QuickAction("💡 Tell a Joke", "Tell me a funny joke")
    )

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        quickActions.forEach { action ->
            val borderColor = if (action.isInternetAction) JarvisAmber.copy(alpha = 0.6f) else JarvisBorder
            val textColor = if (action.isInternetAction) JarvisAmber else JarvisCyan

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(JarvisDeepBlue.copy(alpha = 0.5f))
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .clickable { onCommandClick(action.command) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("quick_action_${action.label.filter { it.isLetterOrDigit() }}")
            ) {
                Text(
                    text = action.label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
