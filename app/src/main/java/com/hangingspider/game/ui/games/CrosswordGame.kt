package com.hangingspider.game.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hangingspider.game.game.WordBank
import com.hangingspider.game.game.engine.Cell
import com.hangingspider.game.game.engine.CrosswordEntry
import com.hangingspider.game.game.engine.CrosswordGenerator
import com.hangingspider.game.game.engine.CrosswordPuzzle
import com.hangingspider.game.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CrosswordGame(onRoundEnd: (RoundResult) -> Unit) {
    val puzzle by produceState<CrosswordPuzzle?>(null) {
        value = withContext(Dispatchers.Default) {
            val clues = WordBank.all.map { it.text to it.hint.replaceFirstChar { c -> c.uppercase() } }
            generateSequence { Unit }.firstNotNullOf { CrosswordGenerator.generate(clues) }
        }
    }
    val ready = puzzle ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Drawing the grid…", color = AppColors.MutedText)
        }
        return
    }
    CrosswordPlay(ready, onRoundEnd)
}

@Composable
private fun CrosswordPlay(puzzle: CrosswordPuzzle, onRoundEnd: (RoundResult) -> Unit) {
    var letters by remember { mutableStateOf(mapOf<Cell, Char>()) }
    var entry by remember { mutableStateOf(puzzle.entries.first()) }
    var cursor by remember { mutableStateOf(Cell(puzzle.entries.first().row, puzzle.entries.first().col)) }
    var over by remember { mutableStateOf(false) }

    val allCells = puzzle.entries.flatMap { it.cells }.toSet()
    val full = allCells.all { it in letters }
    val wrong = if (full) allCells.filter { letters[it] != puzzle.solution[it.row][it.col] }.toSet() else emptySet()

    LaunchedEffect(full, wrong.isEmpty()) {
        if (full && wrong.isEmpty() && !over) {
            over = true
            onRoundEnd(RoundResult(true, "Grid complete!"))
        }
    }

    fun select(cell: Cell) {
        val options = puzzle.entriesAt(cell)
        if (options.isEmpty()) return
        entry = if (cell == cursor && options.size > 1) options.first { it != entry }
        else options.firstOrNull { it.across == entry.across } ?: options.first()
        cursor = cell
    }

    fun moveWithin(step: Int) {
        val cells = entry.cells
        val next = cells.indexOf(cursor) + step
        if (next in cells.indices) cursor = cells[next]
    }

    fun type(ch: Char) {
        if (over) return
        letters = letters + (cursor to ch)
        val cells = entry.cells
        val after = cells.drop(cells.indexOf(cursor) + 1).firstOrNull { it !in letters }
        if (after != null) cursor = after else moveWithin(1)
    }

    fun delete() {
        if (over) return
        if (cursor in letters) letters = letters - cursor
        else {
            moveWithin(-1)
            letters = letters - cursor
        }
    }

    fun jump(step: Int) {
        val i = (puzzle.entries.indexOf(entry) + step).mod(puzzle.entries.size)
        entry = puzzle.entries[i]
        cursor = entry.cells.firstOrNull { it !in letters } ?: entry.cells.first()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val cell = minOf(maxWidth / puzzle.cols, maxHeight / puzzle.rows, 44.dp)
            Column {
                for (r in 0 until puzzle.rows) {
                    Row {
                        for (c in 0 until puzzle.cols) {
                            val here = Cell(r, c)
                            if (here !in allCells) {
                                Spacer(Modifier.size(cell))
                                continue
                            }
                            val background = when {
                                here == cursor -> AppColors.Crimson
                                here in wrong -> AppColors.Bloodstone
                                here in entry.cells -> AppColors.Ember
                                else -> AppColors.Charcoal
                            }
                            Box(
                                Modifier
                                    .size(cell)
                                    .border(0.5.dp, AppColors.Coal)
                                    .background(background)
                                    .clickable(enabled = !over) { select(here) }
                            ) {
                                puzzle.numbers[here]?.let {
                                    Text(
                                        "$it",
                                        color = AppColors.Silver,
                                        fontSize = (cell.value * 0.24f).sp,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                                Text(
                                    letters[here]?.toString() ?: "",
                                    color = AppColors.Ivory,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = (cell.value * 0.5f).sp),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (full && wrong.isNotEmpty()) {
            Text("Some letters are wrong", style = MaterialTheme.typography.bodyMedium, color = AppColors.Rose)
        }
        Spacer(Modifier.height(8.dp))
        ClueBar(entry, onPrev = { jump(-1) }, onNext = { jump(1) })
        Spacer(Modifier.height(6.dp))
        GameKeyboard(onLetter = ::type, onDelete = ::delete)
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = {
            if (!over) {
                over = true
                val solution = allCells.associateWith { puzzle.solution[it.row][it.col]!! }
                letters = solution
                onRoundEnd(RoundResult(false, "Answers revealed."))
            }
        }) {
            Text("Reveal answers", color = AppColors.MutedText)
        }
    }
}

@Composable
private fun ClueBar(entry: CrosswordEntry, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.CharcoalMid),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrev) { Text("‹", color = AppColors.Silver, fontSize = 22.sp) }
        Text(
            "${entry.number} ${if (entry.across) "Across" else "Down"} · ${entry.clue} (${entry.answer.length})",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.Ivory,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onNext) { Text("›", color = AppColors.Silver, fontSize = 22.sp) }
    }
}
