package com.hangingspider.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hangingspider.game.game.GameType
import com.hangingspider.game.game.Levels
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients
import com.hangingspider.game.viewmodel.CoinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelsScreen(coinVm: CoinViewModel, onPlay: (level: Int) -> Unit) {
    val unlocked by coinVm.level.collectAsStateWithLifecycle()
    val points = coinVm.profile.collectAsStateWithLifecycle().value?.coins ?: 0L

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradients.homeBackground)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("LEVELS", style = MaterialTheme.typography.headlineMedium, color = AppColors.GoldBright)
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
                        "$unlocked of ${Levels.MAX} unlocked · ${"%,d".format(points)} points",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = AppColors.Lavender,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }
                items(GameType.entries) { type ->
                    val level = type.ordinal + 1
                    LevelRow(
                        level = level,
                        type = type,
                        isUnlocked = level <= unlocked,
                        isCurrent = level == unlocked,
                        isNext = level == unlocked + 1,
                        onClick = { if (level <= unlocked) onPlay(level) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelRow(
    level: Int,
    type: GameType,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    isNext: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.CharcoalMid.copy(alpha = if (isUnlocked) 0.9f else 0.45f))
            .then(if (isCurrent) Modifier.border(1.dp, AppColors.Signal, RoundedCornerShape(18.dp)) else Modifier)
            .clickable(enabled = isUnlocked, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isUnlocked) AppColors.Crimson else AppColors.Charcoal),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isUnlocked) "$level" else "🔒",
                color = if (isUnlocked) AppColors.Ivory else AppColors.MutedText,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = if (isUnlocked) 16.sp else 14.sp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                type.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isUnlocked) AppColors.Ivory else AppColors.MutedText
            )
            Text(
                type.howToPlay,
                style = MaterialTheme.typography.bodySmall,
                color = if (isUnlocked) AppColors.IvoryDim else AppColors.MutedText,
                maxLines = 3
            )
            if (!isUnlocked) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Level $level · unlocks at ${"%,d".format(Levels.pointsToUnlock(level))} points" +
                        if (isNext) "" else " · unlock Level ${level - 1} first",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    color = if (isNext) AppColors.GoldBright else AppColors.MutedText
                )
            }
        }
        if (isUnlocked) {
            Spacer(Modifier.width(8.dp))
            Text("PLAY ›", style = MaterialTheme.typography.labelLarge, color = AppColors.Signal)
        }
    }
}
