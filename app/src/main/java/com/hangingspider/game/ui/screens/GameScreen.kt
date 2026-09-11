package com.hangingspider.game.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontStyle
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients
import com.hangingspider.game.viewmodel.CoinViewModel
import com.hangingspider.game.viewmodel.GameStatus
import com.hangingspider.game.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    coinVm: CoinViewModel,
    onExit: () -> Unit,
    onReplay: (proceed: () -> Unit) -> Unit = { it() },
    gameVm: GameViewModel = viewModel()
) {
    val state by gameVm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.status) {
        if (state.status == GameStatus.WON) coinVm.onGameFinished(won = true)
        if (state.status == GameStatus.LOST) coinVm.onGameFinished(won = false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradients.gameBackground)
    ) {
        // Danger vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppGradients.emberVignette)
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "THE HUNT",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AppColors.GoldBright
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = onExit) {
                            Text("‹ Back", color = AppColors.Lavender, style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { inner ->
            Column(
                Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SpiderStage(
                    wrong = state.wrong,
                    status = state.status,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                WordDisplay(masked = state.masked, wrong = state.wrong)

                Spacer(Modifier.height(6.dp))

                ClueLine(category = state.word.category, hint = state.word.hint)

                Spacer(Modifier.height(8.dp))

                if (state.status == GameStatus.PLAYING) {
                    val coins = coinVm.profile.collectAsStateWithLifecycle().value?.coins ?: 0L
                    val scope = rememberCoroutineScope()
                    HintButton(
                        coins = coins,
                        cost = GameViewModel.HINT_COST,
                        canReveal = gameVm.canReveal(),
                        onReveal = {
                            scope.launch {
                                if (coinVm.spendCoins(GameViewModel.HINT_COST)) {
                                    gameVm.revealHint()
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    Keyboard(guessed = state.guessed) { gameVm.guess(it) }
                    Spacer(Modifier.height(6.dp))
                } else {
                    ResultPanel(
                        won = state.status == GameStatus.WON,
                        word = state.word.text,
                        onReplay = { onReplay { gameVm.reset() } },
                        onExit = onExit
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SpiderStage(wrong: Int, status: GameStatus, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "gsway")
    val sway by transition.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gsway"
    )
    val eyePulse by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "eye"
    )

    Canvas(modifier = modifier.padding(vertical = 4.dp)) {
        val w = size.width
        val h = size.height
        val gold = AppColors.GoldBright
        val goldDim = AppColors.GoldAntique
        val webCol = AppColors.Lavender.copy(alpha = 0.35f)
        val body = AppColors.CharcoalDeep
        val bodyEdge = AppColors.EmberSoft
        val emberEye = if (status == GameStatus.LOST) AppColors.EmberSoft else
            AppColors.Ember.copy(alpha = 0.5f + eyePulse * 0.5f)

        // Ornamental gallows/bar
        val barY = h * 0.10f
        drawLine(gold.copy(alpha = 0.6f), Offset(w * 0.14f, barY), Offset(w * 0.86f, barY), 3f, StrokeCap.Round)
        drawLine(gold.copy(alpha = 0.3f), Offset(w * 0.14f, barY + 6f), Offset(w * 0.86f, barY + 6f), 1.4f)
        // Anchors
        drawCircle(gold, radius = 5f, center = Offset(w * 0.14f, barY))
        drawCircle(gold, radius = 5f, center = Offset(w * 0.86f, barY))

        // Silk strand grows longer with wrong guesses
        val progress = wrong.coerceAtMost(6) / 6f
        val strandBottomY = barY + (h * 0.72f - barY) * progress
        val swayX = w * 0.5f + sway * 8f * progress
        drawLine(webCol, Offset(w * 0.5f, barY), Offset(swayX, strandBottomY - 60f), 2f, StrokeCap.Round)

        // Body appears from wrong>=1
        if (wrong >= 1) {
            val cx = swayX
            val cy = strandBottomY

            // Legs (unfold as wrong increases: 2 -> 8 legs)
            val legPairs = ((wrong - 1) * 2).coerceAtMost(4)
            for (i in 0 until legPairs) {
                val yOff = (i - (legPairs - 1) / 2f) * 8f
                val angle = -0.35f + i * 0.24f
                val len = 60f + i * 6f
                // left
                drawLine(bodyEdge.copy(alpha = 0.85f),
                    start = Offset(cx - 12f, cy + yOff),
                    end = Offset(cx - 12f - len * 0.6f, cy + yOff + angle * 30f),
                    strokeWidth = 3.4f, cap = StrokeCap.Round)
                drawLine(bodyEdge.copy(alpha = 0.85f),
                    start = Offset(cx - 12f - len * 0.6f, cy + yOff + angle * 30f),
                    end = Offset(cx - 12f - len, cy + yOff + angle * 30f + 30f),
                    strokeWidth = 3.4f, cap = StrokeCap.Round)
                // right
                drawLine(bodyEdge.copy(alpha = 0.85f),
                    start = Offset(cx + 12f, cy + yOff),
                    end = Offset(cx + 12f + len * 0.6f, cy + yOff + angle * 30f),
                    strokeWidth = 3.4f, cap = StrokeCap.Round)
                drawLine(bodyEdge.copy(alpha = 0.85f),
                    start = Offset(cx + 12f + len * 0.6f, cy + yOff + angle * 30f),
                    end = Offset(cx + 12f + len, cy + yOff + angle * 30f + 30f),
                    strokeWidth = 3.4f, cap = StrokeCap.Round)
            }

            // Body abdomen
            drawCircle(body, radius = 30f, center = Offset(cx, cy))
            drawCircle(bodyEdge.copy(alpha = 0.4f), radius = 30f, center = Offset(cx, cy), style = Stroke(1.5f))
            drawCircle(gold.copy(alpha = 0.15f), radius = 34f, center = Offset(cx, cy), style = Stroke(1f))

            // Head
            if (wrong >= 2) {
                drawCircle(body, radius = 18f, center = Offset(cx, cy - 28f))
                drawCircle(bodyEdge.copy(alpha = 0.35f), radius = 18f, center = Offset(cx, cy - 28f), style = Stroke(1f))
            }

            // Fangs (wrong>=4)
            if (wrong >= 4) {
                drawLine(gold, Offset(cx - 4f, cy - 15f), Offset(cx - 6f, cy - 8f), 2f, StrokeCap.Round)
                drawLine(gold, Offset(cx + 4f, cy - 15f), Offset(cx + 6f, cy - 8f), 2f, StrokeCap.Round)
            }

            // Eyes glow (wrong>=3)
            if (wrong >= 3) {
                drawCircle(emberEye.copy(alpha = 0.30f), radius = 8f, center = Offset(cx - 6f, cy - 32f))
                drawCircle(emberEye.copy(alpha = 0.30f), radius = 8f, center = Offset(cx + 6f, cy - 32f))
                drawCircle(emberEye, radius = 3f, center = Offset(cx - 6f, cy - 32f))
                drawCircle(emberEye, radius = 3f, center = Offset(cx + 6f, cy - 32f))
            }
        }

        // Wrong markers along the top as gold tally
        for (i in 0 until 6) {
            val filled = i < wrong
            val cx = w * 0.5f + (i - 2.5f) * 18f
            val cy = h * 0.02f + 10f
            drawCircle(
                if (filled) AppColors.EmberSoft else gold.copy(alpha = 0.25f),
                radius = 4f,
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
private fun ClueLine(category: String, hint: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = AppColors.Signal.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                category.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                color = AppColors.Signal,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "“$hint”",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = AppColors.ChromeBright
        )
    }
}

@Composable
private fun HintButton(
    coins: Long,
    cost: Long,
    canReveal: Boolean,
    onReveal: () -> Unit
) {
    val enabled = canReveal && coins >= cost
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (enabled) AppGradients.violetCta
                else androidx.compose.ui.graphics.SolidColor(AppColors.CharcoalMid)
            )
    ) {
        TextButton(onClick = onReveal, enabled = enabled) {
            Text(
                "✦  Reveal a letter · $cost coins",
                color = if (enabled) AppColors.Ivory else AppColors.MutedText,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun WordDisplay(masked: String, wrong: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            masked,
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 30.sp,
                letterSpacing = 8.sp
            ),
            color = AppColors.GoldBright
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${GameViewModel.MAX_WRONG - wrong} strands remain",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.MutedText
        )
    }
}

@Composable
private fun Keyboard(guessed: Set<Char>, onGuess: (Char) -> Unit) {
    val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
    // Compute a key width that lets the WIDEST row (10 keys) fit the available
    // horizontal space with 2dp gaps. Same width used for every row so keys
    // align across rows like a standard mobile keyboard.
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val gap = 3.dp
        val keyWidth = ((maxWidth - gap * (10 - 1)) / 10).coerceAtLeast(24.dp)
        val keyHeight = keyWidth * 1.35f
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            rows.forEach { row ->
                Row(
                    Modifier.padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    row.forEach { c ->
                        val used = c in guessed
                        Key(
                            letter = c,
                            used = used,
                            width = keyWidth,
                            height = keyHeight,
                            onClick = { if (!used) onGuess(c) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Key(letter: Char, used: Boolean, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (used) AppColors.Aubergine.copy(alpha = 0.4f)
                else AppColors.Aubergine
            )
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(0.dp)) {
            Text(
                letter.toString(),
                color = if (used) AppColors.MutedText else AppColors.GoldBright,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp)
            )
        }
    }
}

@Composable
private fun ResultPanel(won: Boolean, word: String, onReplay: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (won) AppColors.Royal else AppColors.CharcoalMid)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (won) "V I C T O R Y" else "F A L L E N",
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
            color = if (won) AppColors.GoldBright else AppColors.EmberSoft
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (won) "You unravelled: $word" else "The word was: $word",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.IvoryDim
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppGradients.violetCta)
            ) {
                TextButton(onClick = onReplay) {
                    Text("Play again", color = AppColors.Ivory, style = MaterialTheme.typography.labelLarge)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.Aubergine)
            ) {
                TextButton(onClick = onExit) {
                    Text("Return home", color = AppColors.Lavender, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
