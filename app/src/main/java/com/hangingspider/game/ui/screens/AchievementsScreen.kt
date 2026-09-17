package com.hangingspider.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hangingspider.game.game.Achievements
import com.hangingspider.game.game.AppDay
import com.hangingspider.game.game.PlayerProgress
import com.hangingspider.game.game.StreakState
import com.hangingspider.game.game.Streaks
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients
import com.hangingspider.game.viewmodel.CoinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(coinVm: CoinViewModel, onExit: () -> Unit) {
    val profile = coinVm.profile.collectAsStateWithLifecycle().value
    val level = coinVm.level.collectAsStateWithLifecycle().value
    val streak = StreakState(profile?.streakCount ?: 0, profile?.streakLastDay ?: -10)
    val progress = PlayerProgress(
        stats = profile?.stats.orEmpty(),
        stars = profile?.stars.orEmpty(),
        level = level,
        streak = maxOf(Streaks.current(streak, AppDay.today()), 0)
    )
    val earned = profile?.achievements.orEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradients.homeBackground)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("ACHIEVEMENTS", style = MaterialTheme.typography.headlineMedium, color = AppColors.GoldBright) },
                    navigationIcon = {
                        TextButton(onClick = onExit) {
                            Text("‹ Back", color = AppColors.Lavender, style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { inner ->
            LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "${earned.size} of ${Achievements.all.size} earned · +${Achievements.REWARD} points each",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = AppColors.Lavender,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                items(Achievements.all) { a ->
                    val done = a.id in earned
                    val value = a.progress(progress).coerceAtMost(a.target)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.CharcoalMid.copy(alpha = if (done) 0.95f else 0.55f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (done) "🏆" else "🔒", fontSize = 26.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.title, style = MaterialTheme.typography.titleMedium, color = if (done) AppColors.GoldBright else AppColors.Ivory)
                            Text(a.description, style = MaterialTheme.typography.bodySmall, color = AppColors.IvoryDim)
                            if (!done && a.target > 1) {
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { value.toFloat() / a.target },
                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                                    color = AppColors.Signal,
                                    trackColor = AppColors.Coal
                                )
                                Text("$value / ${a.target}", style = MaterialTheme.typography.labelMedium, color = AppColors.MutedText)
                            }
                        }
                    }
                }
            }
        }
    }
}
