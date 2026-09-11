package com.hangingspider.game.ui.screens

import androidx.compose.animation.core.*
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
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients
import com.hangingspider.game.viewmodel.AuthViewModel
import com.hangingspider.game.viewmodel.CoinEvent
import com.hangingspider.game.viewmodel.CoinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    coinVm: CoinViewModel,
    authVm: AuthViewModel,
    onPlay: () -> Unit,
    onWatchAdForDoubler: () -> Unit,
    onCashout: () -> Unit,
    onSettings: () -> Unit
) {
    val profile by coinVm.profile.collectAsStateWithLifecycle()
    val event by coinVm.events.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(event) {
        val e = event ?: return@LaunchedEffect
        val msg = when (e) {
            is CoinEvent.DailyClaimed -> "Blessing received: +${e.amount} coins"
            CoinEvent.DailyOnCooldown -> "The moon rests. Return later for your daily blessing."
            is CoinEvent.DoublerActivated -> "The oracle grants you doubled bounty for 10 minutes"
            is CoinEvent.GameResult -> if (e.won) "Victory! +${e.awarded} coins" else "The spider claims this round"
        }
        snackbar.showSnackbar(msg)
        coinVm.consumeEvent()
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
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(4.dp))

                TopStrip(
                    coins = profile?.coins,
                    doublerUntilMs = doublerUntilMs,
                    onSettings = onSettings
                )

                Spacer(Modifier.height(8.dp))

                HeroSpiderStage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                TitleBlock()

                Spacer(Modifier.height(20.dp))

                PlayButton(onClick = onPlay)

                Spacer(Modifier.height(14.dp))

                ActionChips(
                    onDaily = { coinVm.claimDaily() },
                    onBounty = onWatchAdForDoubler,
                    onCashout = onCashout
                )

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun TopStrip(
    coins: Long?,
    doublerUntilMs: Long,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoinChip(coins = coins, doublerUntilMs = doublerUntilMs)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(AppColors.CharcoalMid.copy(alpha = 0.7f))
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension * 0.30f
                val col = AppColors.Silver
                // Gear teeth
                for (i in 0 until 8) {
                    val a = (i * 45f) * (Math.PI.toFloat() / 180f)
                    val x1 = cx + kotlin.math.cos(a) * r * 1.05f
                    val y1 = cy + kotlin.math.sin(a) * r * 1.05f
                    val x2 = cx + kotlin.math.cos(a) * r * 1.45f
                    val y2 = cy + kotlin.math.sin(a) * r * 1.45f
                    drawLine(col, androidx.compose.ui.geometry.Offset(x1, y1), androidx.compose.ui.geometry.Offset(x2, y2), 2.5f)
                }
                drawCircle(col, radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f))
                drawCircle(col, radius = r * 0.35f)
            }
        }
    }
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
        "bind the beast · earn the bounty",
        style = MaterialTheme.typography.bodySmall.copy(
            fontStyle = FontStyle.Italic,
            letterSpacing = 1.sp
        ),
        color = AppColors.Signal.copy(alpha = 0.9f)
    )
}

@Composable
private fun PlayButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
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
    onDaily: () -> Unit,
    onBounty: () -> Unit,
    onCashout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        ActionChip(label = "Daily rewards", accent = AppColors.GoldBright, icon = "☀",  onClick = onDaily)
        ActionChip(label = "2x rewards",    accent = AppColors.Signal,     icon = "✦",  onClick = onBounty)
        ActionChip(label = "Cash out",      accent = AppColors.GoldBright, icon = "◈",  onClick = onCashout)
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
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            maxLines = 1
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
