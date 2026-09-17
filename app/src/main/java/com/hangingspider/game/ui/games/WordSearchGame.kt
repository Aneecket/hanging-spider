package com.hangingspider.game.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hangingspider.game.game.WordBank
import com.hangingspider.game.game.engine.Cell
import com.hangingspider.game.game.engine.WordSearchGenerator
import com.hangingspider.game.game.engine.WordSearchPuzzle
import com.hangingspider.game.ui.theme.AppColors

private const val WORD_SEARCH_SECONDS = 180
const val EXTRA_SECONDS = 60

private val themes = listOf(
    "creature", "place", "object", "nature", "food", "job", "sport", "body", "plant", "clothing",
    "vegetable", "material", "vehicle", "emotion", "weapon", "color", "fruit", "instrument",
    "measure", "weather", "music", "time", "drink", "tool"
)

private fun newWordSearch(): WordSearchPuzzle {
    for (theme in themes.shuffled()) {
        val words = WordBank.all.filter { it.category == theme && it.text.length in 4..8 }.map { it.text }
        WordSearchGenerator.generate(theme.replaceFirstChar { it.uppercase() }, words)?.let { return it }
    }
    error("No word search could be built")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSearchGame(session: GameSession) {
    val puzzle = remember { newWordSearch() }
    var found by remember { mutableStateOf(mapOf<String, List<Cell>>()) }
    var dragStart by remember { mutableStateOf<Cell?>(null) }
    var dragEnd by remember { mutableStateOf<Cell?>(null) }
    lateinit var countdown: Countdown

    fun markFound(word: String, cells: List<Cell>) {
        found = found + (word to cells)
        if (found.size == puzzle.words.size) {
            val left = countdown.secondsLeft
            session.end(
                RoundResult(
                    true,
                    "Found all ${puzzle.words.size} ${puzzle.theme.lowercase()} words with ${formatSeconds(left)} left.",
                    stars = when {
                        left >= 90 -> 3
                        left >= 30 -> 2
                        else -> 1
                    }
                )
            )
        }
    }

    countdown = rememberCountdown(WORD_SEARCH_SECONDS, running = !session.paused) {
        session.offerSecondChance(
            "Out of time!", "$EXTRA_SECONDS more seconds",
            onGranted = { countdown.addTime(EXTRA_SECONDS) },
            onDeclined = { session.end(RoundResult(false, "Time's up. You found ${found.size} of ${puzzle.words.size} words.")) }
        )
    }

    val selection = dragStart?.let { s -> dragEnd?.let { e -> WordSearchGenerator.line(s, e) } ?: listOf(s) }.orEmpty()
    val foundCells = found.values.flatten().toSet()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Theme: ${puzzle.theme}",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Ivory,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${found.size}/${puzzle.words.size} · ${formatSeconds(countdown.secondsLeft)}",
                style = MaterialTheme.typography.labelLarge,
                color = if (countdown.secondsLeft <= 20) AppColors.Signal else AppColors.GoldBright
            )
        }
        Spacer(Modifier.height(12.dp))

        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, 400.dp)
            val cell = side / puzzle.size
            Column(
                Modifier
                    .size(side)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Charcoal)
                    .gridDrag(
                        rows = puzzle.size,
                        cols = puzzle.size,
                        enabled = !session.paused,
                        onStart = { r, c -> dragStart = Cell(r, c); dragEnd = Cell(r, c) },
                        onMove = { r, c ->
                            val start = dragStart
                            if (start != null && WordSearchGenerator.line(start, Cell(r, c)) != null) dragEnd = Cell(r, c)
                        },
                        onEnd = {
                            val cells = selection
                            dragStart = null
                            dragEnd = null
                            val text = cells.joinToString("") { puzzle.grid[it.row][it.col].toString() }
                            puzzle.words.firstOrNull {
                                it.word !in found && (it.word == text || it.word == text.reversed())
                            }?.let { markFound(it.word, cells) }
                        }
                    )
            ) {
                for (r in 0 until puzzle.size) {
                    Row {
                        for (c in 0 until puzzle.size) {
                            val here = Cell(r, c)
                            Box(
                                Modifier
                                    .size(cell)
                                    .background(
                                        when (here) {
                                            in selection -> GameColors.TileSelected
                                            in foundCells -> GameColors.Found
                                            else -> AppColors.Charcoal
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    puzzle.grid[r][c].toString(),
                                    color = if (here in selection || here in foundCells) AppColors.Ivory else AppColors.Silver,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = (cell.value * 0.5f).sp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            puzzle.words.forEach { pw ->
                val done = pw.word in found
                Text(
                    pw.word,
                    style = MaterialTheme.typography.labelLarge.copy(
                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (done) AppColors.MutedText else AppColors.GoldBright,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.CharcoalMid)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        HintButton("Hint · find a word", enabled = !session.paused, onClick = {
            session.requestHint {
                puzzle.words.firstOrNull { it.word !in found }?.let { markFound(it.word, it.cells) }
            }
        })
    }
}
