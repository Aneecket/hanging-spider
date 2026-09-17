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

private const val STAR_LENGTH = 8

private class PathSetup(val board: LetterBoard, val targets: Set<String>) {
    val needed: Int = (targets.size + 1) / 2
    fun isStar(word: String) = word.length >= STAR_LENGTH
}

private fun newPath(): PathSetup {
    val seeds = WordLists.common.ofLength(STAR_LENGTH)
    while (true) {
        val board = LetterBoard.withHiddenWord(seeds.random(), 4) ?: continue
        val targets = board.solve(WordLists.common, 4)
        if (targets.size in 15..45) return PathSetup(board, targets)
    }
}

@Composable
fun WordPathGame(onRoundEnd: (RoundResult) -> Unit) {
    val setup by produceState<PathSetup?>(null) { value = withContext(Dispatchers.Default) { newPath() } }
    val ready = setup ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Laying the path…", color = AppColors.MutedText)
        }
        return
    }
    WordPathPlay(ready, onRoundEnd)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordPathPlay(setup: PathSetup, onRoundEnd: (RoundResult) -> Unit) {
    var found by remember { mutableStateOf(emptySet<String>()) }
    var tracing by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Words of 4+ letters") }
    var over by remember { mutableStateOf(false) }

    val starFound = found.any(setup::isStar)
    val lengths = setup.targets.groupBy { minOf(it.length, STAR_LENGTH) }.toSortedMap()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Found ${found.size} of ${setup.needed} needed · ${if (starFound) "★ star found" else "☆ star word hidden"}",
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.Ivory
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
            lengths.forEach { (len, words) ->
                val label = if (len >= STAR_LENGTH) "★ $len+" else "$len letters"
                Text(
                    "$label: ${words.count { it in found }}/${words.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (len >= STAR_LENGTH) AppColors.GoldBright else AppColors.Silver
                )
            }
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
            enabled = !over,
            onPathChange = { tracing = setup.board.wordFor(it) },
            onSubmit = { path ->
                val word = setup.board.wordFor(path)
                message = when {
                    word.length < 4 -> "Words need 4+ letters"
                    word in found -> "Already found"
                    word in setup.targets -> {
                        found = found + word
                        val done = found.size >= setup.needed && found.any(setup::isStar)
                        if (done) {
                            over = true
                            onRoundEnd(RoundResult(true, "Found ${found.size} words including the star word."))
                        }
                        if (setup.isStar(word)) "★ Star word: $word" else word
                    }
                    WordLists.dictionary.isWord(word) -> "$word is a bonus word"
                    else -> "$word isn't in the word list"
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
            found.sortedBy { it.length }.forEach {
                Text(it, style = MaterialTheme.typography.labelLarge, color = if (setup.isStar(it)) AppColors.GoldBright else AppColors.Silver)
            }
        }
        SecondaryButton("Give up", {
            if (!over) {
                over = true
                val star = setup.targets.filter(setup::isStar).maxByOrNull { it.length }
                onRoundEnd(RoundResult(false, "You found ${found.size} of ${setup.needed}." + (star?.let { " Star word: $it." } ?: "")))
            }
        })
        Spacer(Modifier.height(16.dp))
    }
}
