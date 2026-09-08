package com.hangingspider.game.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import com.hangingspider.game.ui.theme.AppColors

data class BottomTab(val route: String, val label: String, val glyph: String)

// Market tab intentionally hidden until Blaze + crypto policy decisions are made.
// Re-add `BottomTab(Routes.BUY, "Market", "◉")` once Coinbase Commerce is live.
private val Tabs = listOf(
    BottomTab(Routes.HOME, "Home", "◆"),
    BottomTab(Routes.LEADERBOARD, "Leaders", "♛"),
    BottomTab(Routes.MESSAGES, "Missives", "✉")
)

@Composable
fun AppBottomBar(current: String, onSelect: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.CharcoalDeep.copy(alpha = 0.98f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Tabs.forEach { tab ->
                val active = current == tab.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) AppColors.Aubergine.copy(alpha = 0.7f) else Color.Transparent)
                        .pointerInput(tab.route) { detectTapGestures { onSelect(tab.route) } }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        tab.glyph,
                        color = if (active) AppColors.GoldBright else AppColors.MutedText,
                        fontSize = 22.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                        color = if (active) AppColors.Ivory else AppColors.MutedText
                    )
                }
            }
        }
    }
}
