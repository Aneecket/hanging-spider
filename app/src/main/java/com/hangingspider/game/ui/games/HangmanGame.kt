package com.hangingspider.game.ui.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hangingspider.game.game.Word
import com.hangingspider.game.game.WordBank
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients

enum class GameStatus { PLAYING, WON, LOST }

class HangmanState(val word: Word = WordBank.random()) {
    companion object {
        const val MAX_WRONG = 6
    }

    var guessed by mutableStateOf(emptySet<Char>())
        private set
    var wrong by mutableIntStateOf(0)
        private set
    var status by mutableStateOf(GameStatus.PLAYING)
        private set

    val masked: String get() = word.text.map { if (it in guessed) it else '_' }.joinToString(" ")

    fun guess(letter: Char) {
        if (status != GameStatus.PLAYING || letter in guessed) return
        guessed = guessed + letter
        if (letter !in word.text) wrong++
        updateStatus()
    }

    fun canReveal(): Boolean = status == GameStatus.PLAYING && (word.text.toSet() - guessed).isNotEmpty()

    fun revealHint() {
        if (!canReveal()) return
        guessed = guessed + (word.text.toSet() - guessed).random()
        updateStatus()
    }

    private fun updateStatus() {
        status = when {
            word.text.all { it in guessed } -> GameStatus.WON
            wrong >= MAX_WRONG -> GameStatus.LOST
            else -> GameStatus.PLAYING
        }
    }
}

@Composable
fun HangmanGame(session: GameSession) {
    val state = remember { HangmanState() }

    LaunchedEffect(state.status) {
        val strands = "🟥".repeat(state.wrong) + "⬛".repeat(HangmanState.MAX_WRONG - state.wrong)
        when (state.status) {
            GameStatus.WON -> session.end(
                RoundResult(
                    true,
                    "You unravelled: ${state.word.text}",
                    stars = when {
                        state.wrong <= 1 -> 3
                        state.wrong <= 3 -> 2
                        else -> 1
                    },
                    share = "${state.word.text.length}-letter word · $strands"
                )
            )
            GameStatus.LOST -> session.end(RoundResult(false, "The word was: ${state.word.text}", share = strands))
            GameStatus.PLAYING -> Unit
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(AppGradients.emberVignette))
        Column(
            Modifier
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
            RevealLetterButton(
                enabled = state.canReveal() && !session.paused,
                onReveal = { session.requestHint { state.revealHint() } }
            )
            Spacer(Modifier.height(6.dp))
            GameKeyboard(onLetter = { if (!session.paused) state.guess(it) }, disabled = state.guessed)
            Spacer(Modifier.height(10.dp))
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
private fun RevealLetterButton(enabled: Boolean, onReveal: () -> Unit) {
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
                "✦  Reveal a letter",
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
            "${HangmanState.MAX_WRONG - wrong} strands remain",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.MutedText
        )
    }
}
