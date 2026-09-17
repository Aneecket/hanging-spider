package com.hangingspider.game.ui.games

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hangingspider.game.game.WordLists
import com.hangingspider.game.game.engine.GhostOutcome
import com.hangingspider.game.game.engine.GhostRules
import com.hangingspider.game.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GhostGame(session: GameSession) {
    var fragment by remember { mutableStateOf("") }
    var playerLosses by remember { mutableIntStateOf(0) }
    var spiderLosses by remember { mutableIntStateOf(0) }
    var playerStarts by remember { mutableStateOf(true) }
    var playerTurn by remember { mutableStateOf(true) }
    var roundMessage by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Your turn. Add any letter.") }
    var matchOver by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun endRound(playerLost: Boolean, why: String) {
        if (playerLost) playerLosses++ else spiderLosses++
        roundMessage = why
        if (playerLosses == GhostRules.LOSSES_TO_LOSE || spiderLosses == GhostRules.LOSSES_TO_LOSE) {
            matchOver = true
            val won = spiderLosses == GhostRules.LOSSES_TO_LOSE
            session.end(
                RoundResult(
                    won,
                    (if (won) "You beat the spider " else "The spider won ") + "${spiderLosses.coerceAtLeast(playerLosses)}–${minOf(spiderLosses, playerLosses)}. $why",
                    stars = when (playerLosses) {
                        0 -> 3
                        1 -> 2
                        else -> 1
                    }
                )
            )
        }
    }

    fun playerMove(letter: Char) {
        if (!playerTurn || roundMessage != null || matchOver || session.paused) return
        val next = fragment + letter
        fragment = next
        when (val outcome = GhostRules.outcome(next, WordLists.dictionary)) {
            is GhostOutcome.CompletedWord -> endRound(true, "You finished the word ${outcome.word}.")
            is GhostOutcome.DeadEnd -> endRound(true, "No word starts with $next.")
            GhostOutcome.Continue -> playerTurn = false
        }
    }

    LaunchedEffect(playerTurn, roundMessage) {
        if (playerTurn || roundMessage != null || matchOver) return@LaunchedEffect
        status = "The spider is thinking…"
        delay(700)
        val current = fragment
        val move = withContext(Dispatchers.Default) {
            GhostRules.spiderMove(current, WordLists.dictionary, WordLists.common)
        }
        if (move == null) {
            val example = WordLists.dictionary.withPrefix(current).firstOrNull { it.length > current.length }
            endRound(false, "The spider is stuck after $current" + (example?.let { " (it could have led to $it)." } ?: "."))
        } else {
            fragment = current + move
            status = "Spider added $move. Your turn."
            playerTurn = true
        }
    }

    fun nextRound() {
        playerStarts = !playerStarts
        fragment = ""
        roundMessage = null
        playerTurn = playerStarts
        status = if (playerStarts) "Your turn. Add any letter." else "The spider starts this round."
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ScoreLetters("You", playerLosses)
            ScoreLetters("Spider", spiderLosses)
        }
        Text(
            "Lose ${GhostRules.LOSSES_TO_LOSE} rounds and you're out",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.MutedText
        )
        Spacer(Modifier.weight(1f))
        Text(
            fragment.ifEmpty { "_" }.toList().joinToString(" "),
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp, letterSpacing = 2.sp),
            color = AppColors.GoldBright,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            roundMessage ?: status,
            style = MaterialTheme.typography.bodyLarge,
            color = if (roundMessage != null) AppColors.Rose else AppColors.IvoryDim,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Don't finish a word of ${GhostRules.MIN_WORD}+ letters, and don't make a dead end.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.MutedText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        if (roundMessage == null && playerTurn && !matchOver) {
            HintButton("Hint · safe letter", enabled = !session.paused, onClick = {
                session.requestHint {
                    val current = fragment
                    scope.launch {
                        val letter = withContext(Dispatchers.Default) {
                            GhostRules.spiderMove(current, WordLists.dictionary, WordLists.common, mistakeRate = 0.0)
                        }
                        status = letter?.let { "Try adding $it" } ?: "Every letter loses here."
                    }
                }
            })
            Spacer(Modifier.height(8.dp))
        }
        if (roundMessage != null && !matchOver) {
            PrimaryButton("Next round", ::nextRound)
            Spacer(Modifier.height(12.dp))
        }
        GameKeyboard(onLetter = ::playerMove)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ScoreLetters(who: String, losses: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(who, style = MaterialTheme.typography.labelLarge, color = AppColors.Silver)
        Text(
            GhostRules.LETTERS.take(GhostRules.LOSSES_TO_LOSE).mapIndexed { i, ch -> if (i < losses) ch else '·' }.joinToString(" "),
            style = MaterialTheme.typography.titleLarge,
            color = if (losses > 0) AppColors.Signal else AppColors.MutedText
        )
    }
}
