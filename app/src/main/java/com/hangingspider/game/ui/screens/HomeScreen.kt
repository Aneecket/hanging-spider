package com.hangingspider.game.ui.screens

import androidx.compose.animation.core.*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.hangingspider.game.ads.rememberRewardedAd
import com.hangingspider.game.data.model.DailyRecord
import com.hangingspider.game.game.GameType
import com.hangingspider.game.game.StreakState
import com.hangingspider.game.game.Streaks
import com.hangingspider.game.reminders.EngagementPrefs
import com.hangingspider.game.reminders.Reminders
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hangingspider.game.game.Levels
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients
import com.hangingspider.game.viewmodel.CoinEvent
import com.hangingspider.game.viewmodel.CoinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    coinVm: CoinViewModel,
    onPlay: () -> Unit,
    onDaily: (GameType) -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val profile by coinVm.profile.collectAsStateWithLifecycle()
    val level by coinVm.level.collectAsStateWithLifecycle()
    val event by coinVm.events.collectAsStateWithLifecycle()
    val today by coinVm.today.collectAsStateWithLifecycle()
    val daily by coinVm.daily.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val showRewarded = rememberRewardedAd()
    var claimed by remember { mutableStateOf<Long?>(null) }

    val streak = StreakState(profile?.streakCount ?: 0, profile?.streakLastDay ?: -10)
    val liveStreak = Streaks.current(streak, today)
    val claimedToday = profile?.lastDailyClaimDay == today

    LaunchedEffect(event) {
        val e = event ?: return@LaunchedEffect
        coinVm.consumeEvent()
        when (e) {
            is CoinEvent.DailyClaimed -> claimed = e.amount
            CoinEvent.DailyOnCooldown -> snackbar.showSnackbar("Already claimed today. Come back tomorrow for a bigger reward.")
            is CoinEvent.DoublerActivated -> snackbar.showSnackbar("Double points for 10 minutes")
            is CoinEvent.BonusAdded -> snackbar.showSnackbar("Bonus: +${e.amount} points")
            is CoinEvent.StreakSaved -> snackbar.showSnackbar("Streak saved: ${e.days} days")
        }
    }

    val doublerUntilMs = profile?.doublerUntil ?: 0L

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Coal)
    ) {
        // Red radial glow behind the hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to AppColors.Signal.copy(alpha = 0.28f),
                            0.4f to AppColors.Crimson.copy(alpha = 0.12f),
                            1.0f to Color.Transparent
                        ),
                        center = Offset(540f, 500f),
                        radius = 900f
                    )
                )
        )
        // Hex honeycomb overlay
        HexOverlay(alpha = 0.05f, modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(snackbar) { data ->
                    Snackbar(
                        containerColor = AppColors.CharcoalMid,
                        contentColor = AppColors.Ivory,
                        shape = RoundedCornerShape(14.dp),
                        snackbarData = data
                    )
                }
            }
        ) { inner ->
            Column(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(4.dp))

                TopStrip(
                    coins = profile?.coins,
                    doublerUntilMs = doublerUntilMs,
                    streak = liveStreak,
                    onAchievements = onAchievements,
                    onSettings = onSettings
                )

                HeroSpiderStage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                )

                TitleBlock()

                if (Streaks.canRepair(streak, today)) {
                    Spacer(Modifier.height(12.dp))
                    StreakRepairCard(days = streak.count, onSave = { showRewarded({ coinVm.repairStreak() }, {}) })
                }

                Spacer(Modifier.height(14.dp))

                LevelCard(level = level, points = profile?.coins)

                Spacer(Modifier.height(12.dp))

                // Wait for the profile so PLAY opens the player's real unlocked level.
                PlayButton(onClick = { if (profile != null) onPlay() })

                Spacer(Modifier.height(12.dp))

                DailyPuzzles(
                    done = daily,
                    onOpen = onDaily
                )

                Spacer(Modifier.height(12.dp))

                ActionChips(
                    dailyLabel = if (claimedToday) "Reward\nclaimed" else "Daily\n+${Streaks.dailyReward(streak, today)}",
                    onDaily = { coinVm.claimDaily() },
                    onBounty = { showRewarded({ coinVm.activateDoubler() }, {}) }
                )

                Spacer(Modifier.height(20.dp))
            }
        }
    }

    claimed?.let { amount ->
        AlertDialog(
            onDismissRequest = { claimed = null },
            containerColor = AppColors.CharcoalMid,
            titleContentColor = AppColors.GoldBright,
            textContentColor = AppColors.Ivory,
            title = { Text("+$amount points", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp)) },
            text = {
                Text(
                    "Day ${Streaks.rewardDay(streak, today)} reward. Rewards grow each day you play in a row, up to 500 points on day 7.\n\nWatch a short ad to double today's reward?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    claimed = null
                    showRewarded({ coinVm.addBonus(amount) }, {})
                }) { Text("▶ Double it", color = AppColors.GoldBright) }
            },
            dismissButton = {
                TextButton(onClick = { claimed = null }) { Text("No thanks", color = AppColors.Lavender) }
            }
        )
    }

    ReminderOffer(gamesPlayed = profile?.gamesPlayed ?: 0)
}

/** Asks once, after the first finished round, whether the player wants a daily reminder. */
@Composable
private fun ReminderOffer(gamesPlayed: Int) {
    val context = LocalContext.current
    val prefs = remember { EngagementPrefs(context) }
    var show by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Reminders.setEnabled(context, granted)
    }
    LaunchedEffect(gamesPlayed) {
        if (gamesPlayed >= 1 && !prefs.reminderAsked) show = true
    }
    if (!show) return
    AlertDialog(
        onDismissRequest = { show = false; prefs.reminderAsked = true },
        containerColor = AppColors.CharcoalMid,
        titleContentColor = AppColors.GoldBright,
        textContentColor = AppColors.Ivory,
        title = { Text("Daily reminder?", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp)) },
        text = {
            Text(
                "One notification a day at 7 PM when a new puzzle is ready or your streak is about to end. You can turn it off in Settings.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = {
                show = false
                prefs.reminderAsked = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else Reminders.setEnabled(context, true)
            }) { Text("Remind me", color = AppColors.GoldBright) }
        },
        dismissButton = {
            TextButton(onClick = { show = false; prefs.reminderAsked = true }) { Text("No thanks", color = AppColors.Lavender) }
        }
    )
}

@Composable
private fun StreakRepairCard(days: Int, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Bloodstone)
            .clickable(onClick = onSave)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Your $days-day streak broke yesterday", style = MaterialTheme.typography.titleMedium, color = AppColors.Ivory)
            Text("Watch a short ad to keep it", style = MaterialTheme.typography.bodySmall, color = AppColors.IvoryDim)
        }
        Text("▶ Save", style = MaterialTheme.typography.labelLarge, color = AppColors.GoldBright)
    }
}

@Composable
private fun DailyPuzzles(done: Map<String, DailyRecord>, onOpen: (GameType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(GameType.FIVE_LETTER, GameType.GROUPS).forEach { type ->
            val record = done[type.name]
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.CharcoalMid.copy(alpha = 0.9f))
                    .clickable { onOpen(type) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text("DAILY PUZZLE", style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, letterSpacing = 1.sp), color = AppColors.Signal)
                Text(type.title, style = MaterialTheme.typography.titleMedium, color = AppColors.Ivory, maxLines = 1)
                Text(
                    when {
                        record == null -> "▶ Play today's"
                        record.won -> "✓ Solved"
                        else -> "✓ Done"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (record == null) AppColors.GoldBright else AppColors.MutedText
                )
            }
        }
    }
}

@Composable
private fun TopStrip(
    coins: Long?,
    doublerUntilMs: Long,
    streak: Int,
    onAchievements: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoinChip(coins = coins, doublerUntilMs = doublerUntilMs)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(AppColors.CharcoalMid.copy(alpha = 0.8f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("🔥 $streak", color = if (streak > 0) AppColors.GoldBright else AppColors.MutedText, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.weight(1f))
        RoundIconButton(onClick = onAchievements) { Text("🏆", fontSize = 18.sp) }
        Spacer(Modifier.width(8.dp))
        RoundIconButton(onClick = onSettings) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension * 0.30f
                val col = AppColors.Silver
                for (i in 0 until 8) {
                    val a = (i * 45f) * (Math.PI.toFloat() / 180f)
                    val x1 = cx + kotlin.math.cos(a) * r * 1.05f
                    val y1 = cy + kotlin.math.sin(a) * r * 1.05f
                    val x2 = cx + kotlin.math.cos(a) * r * 1.45f
                    val y2 = cy + kotlin.math.sin(a) * r * 1.45f
                    drawLine(col, Offset(x1, y1), Offset(x2, y2), 2.5f)
                }
                drawCircle(col, radius = r, style = Stroke(width = 2.2f))
                drawCircle(col, radius = r * 0.35f)
            }
        }
    }
}

@Composable
private fun RoundIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(AppColors.CharcoalMid.copy(alpha = 0.7f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun CoinChip(coins: Long?, doublerUntilMs: Long) {
    // Ticking "now" — refreshes every second while the doubler is running,
    // stops as soon as it expires so we don't churn frames forever.
    val now by produceState(System.currentTimeMillis(), doublerUntilMs) {
        while (doublerUntilMs > value) {
            value = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    val remainingMs = (doublerUntilMs - now).coerceAtLeast(0)
    val active = remainingMs > 0

    Row(
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(50), spotColor = AppColors.GoldBright.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    listOf(AppColors.GoldAntique, AppColors.GoldBright, AppColors.Gold)
                )
            )
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(AppColors.Coal, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(AppColors.GoldBright, CircleShape)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = coins?.let { formatCoins(it) } ?: "—",
            color = AppColors.Coal,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = com.hangingspider.game.ui.theme.Cinzel,
                fontSize = 15.sp
            )
        )
        if (active) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AppColors.Coal)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "2× · ${formatMmSs(remainingMs)}",
                    color = AppColors.Signal,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, letterSpacing = 0.5.sp)
                )
            }
        }
    }
}

@Composable
private fun HeroSpiderStage(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "hero")
    val pulse by transition.animateFloat(
        0.75f, 1f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val sway by transition.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f + sway * 4f
            val cy = h * 0.55f + sway * 2f
            val body = AppColors.Coal
            val red = AppColors.Signal
            val redDim = AppColors.Crimson
            val chrome = AppColors.ChromeBright
            val chromeDim = AppColors.Silver

            // Silk strand
            drawLine(chromeDim.copy(alpha = 0.5f), Offset(cx, 0f), Offset(cx, cy - h * 0.20f), 1.4f, StrokeCap.Round)

            // Radial red glow behind spider
            for (i in 0..4) {
                val r = w * (0.30f + i * 0.06f)
                drawCircle(red.copy(alpha = (0.10f - i * 0.017f).coerceAtLeast(0f) * pulse),
                    radius = r, center = Offset(cx, cy))
            }

            // 8 legs — 4 pairs, angled outward and down
            val legLen = w * 0.28f
            val angles = listOf(-0.9f to -0.35f, -0.55f to -0.05f, -0.20f to 0.20f, 0.15f to 0.45f)
            angles.forEach { (a, drop) ->
                val bendX = kotlin.math.cos(a) * legLen * 0.55f
                val bendY = kotlin.math.sin(a) * legLen * 0.55f + drop * legLen
                val endX = kotlin.math.cos(a + 0.3f) * legLen * 1.15f
                val endY = kotlin.math.sin(a + 0.3f) * legLen * 1.15f + drop * legLen + 20f
                drawLine(redDim, Offset(cx - w * 0.05f, cy), Offset(cx - bendX, cy + bendY), 4.5f, StrokeCap.Round)
                drawLine(redDim, Offset(cx - bendX, cy + bendY), Offset(cx - endX, cy + endY), 4.5f, StrokeCap.Round)
                drawLine(redDim, Offset(cx + w * 0.05f, cy), Offset(cx + bendX, cy + bendY), 4.5f, StrokeCap.Round)
                drawLine(redDim, Offset(cx + bendX, cy + bendY), Offset(cx + endX, cy + endY), 4.5f, StrokeCap.Round)
            }

            // Abdomen
            val abd = w * 0.11f
            drawCircle(body, radius = abd, center = Offset(cx, cy + abd * 0.15f))
            drawCircle(redDim.copy(alpha = 0.7f), radius = abd, center = Offset(cx, cy + abd * 0.15f), style = Stroke(2.2f))

            // Head
            val head = w * 0.07f
            drawCircle(body, radius = head, center = Offset(cx, cy - head * 1.3f))
            drawCircle(redDim.copy(alpha = 0.7f), radius = head, center = Offset(cx, cy - head * 1.3f), style = Stroke(1.8f))

            // Chrome specular on head
            drawCircle(chrome.copy(alpha = 0.22f), radius = head * 0.32f, center = Offset(cx - head * 0.25f, cy - head * 1.5f))

            // Fangs
            drawLine(chromeDim, Offset(cx - 5f, cy - head * 0.35f), Offset(cx - 7f, cy - head * 0.05f), 2.2f, StrokeCap.Round)
            drawLine(chromeDim, Offset(cx + 5f, cy - head * 0.35f), Offset(cx + 7f, cy - head * 0.05f), 2.2f, StrokeCap.Round)

            // Red eyes with halo + pulse
            val eyeY = cy - head * 1.45f
            listOf(-6f, 6f).forEach { dx ->
                drawCircle(red.copy(alpha = 0.30f * pulse), radius = 8f, center = Offset(cx + dx, eyeY))
                drawCircle(red, radius = 3.2f, center = Offset(cx + dx, eyeY))
            }
        }
    }
}

@Composable
private fun TitleBlock() {
    Text(
        "HUNT WORDS",
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = 22.sp,
            letterSpacing = 5.sp
        ),
        color = AppColors.ChromeBright
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "guess words · earn points · unlock levels",
        style = MaterialTheme.typography.bodySmall.copy(
            fontStyle = FontStyle.Italic,
            letterSpacing = 1.sp
        ),
        color = AppColors.Signal.copy(alpha = 0.9f)
    )
}

@Composable
private fun LevelCard(level: Int, points: Long?) {
    val have = points ?: 0L
    val maxed = level >= Levels.MAX
    val next = Levels.pointsToUnlock(level + 1)
    val progress = if (maxed) 1f else (have.toFloat() / next).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CharcoalMid.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "LEVEL $level · ${Levels.gameFor(level).title.uppercase()}",
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.sp),
                color = AppColors.GoldBright,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                when {
                    points == null -> "—"
                    maxed -> "${formatCoins(have)} points"
                    else -> "${formatCoins(have)} / ${formatCoins(next)}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.IvoryDim
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = AppColors.Signal,
            trackColor = AppColors.Coal
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                maxed -> "All ${Levels.MAX} levels unlocked"
                have >= next -> "Finish any round to unlock Level ${level + 1} · ${Levels.gameFor(level + 1).title}"
                else -> "Reach ${formatCoins(next)} points to unlock Level ${level + 1} · ${Levels.gameFor(level + 1).title}"
            },
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = AppColors.MutedText
        )
    }
}

@Composable
private fun PlayButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = AppColors.Signal)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(AppColors.Ember, AppColors.Crimson, AppColors.Signal)
                )
            )
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Text(
                "PLAY",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 22.sp,
                    letterSpacing = 8.sp
                ),
                color = AppColors.Ivory
            )
        }
    }
}

@Composable
private fun ActionChips(
    dailyLabel: String,
    onDaily: () -> Unit,
    onBounty: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        ActionChip(label = dailyLabel,        accent = AppColors.GoldBright, icon = "☀",  onClick = onDaily)
        ActionChip(label = "2x\nrewards",    accent = AppColors.Signal,     icon = "✦",  onClick = onBounty)
    }
}

@Composable
private fun ActionChip(
    label: String,
    accent: Color,
    icon: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AppColors.CharcoalMid.copy(alpha = 0.85f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = accent, fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = AppColors.Ivory,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, lineHeight = 13.sp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun HexOverlay(alpha: Float, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = 22f
        val hexH = r * kotlin.math.sqrt(3f)
        val stepX = r * 1.5f
        var row = 0
        var y = -hexH
        while (y < h + hexH) {
            val xOff = if (row % 2 == 0) 0f else stepX * 0.5f
            var x = -stepX + xOff - stepX
            while (x < w + stepX) {
                val path = androidx.compose.ui.graphics.Path()
                for (i in 0..5) {
                    val ang = (i * 60f) * (Math.PI.toFloat() / 180f)
                    val px = x + r * kotlin.math.cos(ang)
                    val py = y + r * kotlin.math.sin(ang)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, AppColors.Crimson.copy(alpha = alpha), style = Stroke(width = 0.8f))
                x += stepX * 2f
            }
            y += hexH * 0.5f
            row++
        }
    }
}

private fun formatCoins(n: Long): String =
    if (n >= 1_000) "%,d".format(n) else n.toString()

private fun formatMmSs(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%02d:%02d".format(m, s)
}
