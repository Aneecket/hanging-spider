package com.hangingspider.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hangingspider.game.data.model.LeaderboardEntry
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(vm: LeaderboardViewModelHolder) {
    val entries by vm.instance.entries.collectAsStateWithLifecycle()

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
                        Text(
                            "HALL OF WEAVERS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AppColors.GoldBright
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { inner ->
            LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    Text(
                        "The greatest word-binders in the web",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = AppColors.Lavender,
                        modifier = Modifier.padding(bottom = 14.dp, start = 4.dp)
                    )
                }
                if (entries.size >= 3) {
                    item { Podium(entries.take(3)) }
                    item { Spacer(Modifier.height(20.dp)) }
                    item {
                        Text(
                            "OTHER SEEKERS",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.MutedText,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                    }
                    itemsIndexed(entries.drop(3)) { i, e -> LeaderRow(rank = i + 4, entry = e) }
                } else {
                    items(entries) { entry ->
                        LeaderRow(rank = entries.indexOf(entry) + 1, entry = entry)
                    }
                }
            }
        }
    }
}

class LeaderboardViewModelHolder(val instance: com.hangingspider.game.viewmodel.LeaderboardViewModel)

@Composable
fun rememberLeaderboardHolder(): LeaderboardViewModelHolder {
    val vm: com.hangingspider.game.viewmodel.LeaderboardViewModel = viewModel()
    return remember { LeaderboardViewModelHolder(vm) }
}

@Composable
private fun Podium(top3: List<LeaderboardEntry>) {
    // Positions: 2nd (left), 1st (center, taller), 3rd (right)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        if (top3.size > 1) PodiumTile(rank = 2, entry = top3[1], height = 132.dp)
        PodiumTile(rank = 1, entry = top3[0], height = 168.dp, highlighted = true)
        if (top3.size > 2) PodiumTile(rank = 3, entry = top3[2], height = 108.dp)
    }
}

@Composable
private fun PodiumTile(rank: Int, entry: LeaderboardEntry, height: androidx.compose.ui.unit.Dp, highlighted: Boolean = false) {
    val accent = when (rank) {
        1 -> AppColors.GoldBright
        2 -> AppColors.Lavender
        else -> AppColors.GoldAntique
    }
    Column(
        modifier = Modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(if (highlighted) 68.dp else 56.dp)
                .shadow(if (highlighted) 16.dp else 6.dp, CircleShape, spotColor = accent)
                .background(accent.copy(alpha = 0.15f), CircleShape)
                .then(Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                entry.name.take(1).uppercase(),
                style = MaterialTheme.typography.headlineLarge,
                color = accent
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            entry.name,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            color = AppColors.Ivory,
            maxLines = 1
        )
        if (entry.isBot) BotBadge()
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(if (highlighted) 90.dp else 78.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(
                    if (highlighted) AppGradients.goldHero
                    else androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(AppColors.Aubergine, AppColors.PlumMid)
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(
                    "#$rank",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                    color = if (highlighted) AppColors.PlumDeep else accent
                )
                Text(
                    entry.coins.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (highlighted) AppColors.PlumDeep else AppColors.Ivory
                )
                Text(
                    "coins",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                    color = if (highlighted) AppColors.PlumDeep.copy(alpha = 0.7f) else AppColors.MutedText
                )
            }
        }
    }
}

@Composable
private fun LeaderRow(rank: Int, entry: LeaderboardEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Aubergine.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "#$rank",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 16.sp),
            color = AppColors.GoldAntique,
            modifier = Modifier.width(42.dp)
        )
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(AppColors.Mystic.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                entry.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Lavender
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.name,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Ivory
            )
            if (entry.isBot) BotBadge()
        }
        Text(
            entry.coins.toString(),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp),
            color = AppColors.GoldBright
        )
    }
}

@Composable
private fun BotBadge() {
    Surface(
        color = AppColors.Mystic.copy(alpha = 0.2f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            "AI BOT",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 8.sp, letterSpacing = 1.sp),
            color = AppColors.Lavender,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}
