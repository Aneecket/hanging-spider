package com.hangingspider.game.ui.games

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hangingspider.game.game.WordLists
import com.hangingspider.game.game.engine.LetterBoard
import com.hangingspider.game.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val GRID_SECONDS = 150

private class GridSetup(val board: LetterBoard, val everyday: Set<String>, val target: Int)

private fun newGrid(): GridSetup {
    while (true) {
        val board = LetterBoard.rollDice()
        val everyday = board.solve(WordLists.common, 3)
        if (everyday.size < 30) continue
        val target = (everyday.sumOf { LetterBoard.gridScore(it) } * 0.2).toInt().coerceIn(8, 25)
        return GridSetup(board, everyday, target)
    }
}

@Composable
fun LetterGridGame(session: GameSession) {
    val setup by produceState<GridSetup?>(null) { value = withContext(Dispatchers.Default) { newGrid() } }
    val ready = setup ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Shaking the letters…", color = AppColors.MutedText)
        }
        return
    }
    LetterGridPlay(ready, session)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LetterGridPlay(setup: GridSetup, session: GameSession) {
    var found by remember { mutableStateOf(emptyList<String>()) }
    var score by remember { mutableIntStateOf(0) }
    var tracing by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Drag through touching letters") }

    lateinit var countdown: Countdown
    countdown = rememberCountdown(GRID_SECONDS, running = !session.paused) {
        session.offerSecondChance(
            "Out of time!", "$EXTRA_SECONDS more seconds",
            onGranted = { countdown.addTime(EXTRA_SECONDS) },
            onDeclined = {
                val missed = (setup.everyday - found.toSet()).maxByOrNull { it.length }
                session.end(
                    RoundResult(
                        false,
                        "You scored $score of ${setup.target}." + (missed?.let { " Longest word you missed: $it." } ?: "")
                    )
                )
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                "Score $score / ${setup.target}",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Ivory,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatSeconds(countdown.secondsLeft),
                style = MaterialTheme.typography.labelLarge,
                color = if (countdown.secondsLeft <= 20) AppColors.Signal else AppColors.GoldBright
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            tracing.ifEmpty { message },
            style = MaterialTheme.typography.titleLarge,
            color = if (tracing.isEmpty()) AppColors.IvoryDim else AppColors.GoldBright,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(Modifier.height(10.dp))
        TraceBoard(
            board = setup.board,
            enabled = !session.paused,
            onPathChange = { tracing = setup.board.wordFor(it) },
            onSubmit = { path ->
                val word = setup.board.wordFor(path)
                message = when {
                    word.length < 3 -> "Words need 3+ letters"
                    word in found -> "Already found $word"
                    !WordLists.dictionary.isWord(word) -> "$word isn't in the word list"
                    else -> {
                        val points = LetterBoard.gridScore(word)
                        found = listOf(word) + found
                        score += points
                        if (score >= setup.target) {
                            val left = countdown.secondsLeft
                            session.end(
                                RoundResult(
                                    true,
                                    "Scored $score with ${found.size} words.",
                                    stars = when {
                                        left >= 60 -> 3
                                        left >= 20 -> 2
                                        else -> 1
                                    }
                                )
                            )
                        }
                        "$word +$points"
                    }
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        HintButton("Hint · show a word", enabled = !session.paused, onClick = {
            session.requestHint {
                (setup.everyday - found.toSet()).filter { it.length >= 4 }.randomOrNull()?.let { message = "Try: $it" }
            }
        })
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            found.forEach {
                Text(it, style = MaterialTheme.typography.labelLarge, color = AppColors.Silver)
            }
        }
    }
}
