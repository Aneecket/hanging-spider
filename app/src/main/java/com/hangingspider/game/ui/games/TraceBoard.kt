package com.hangingspider.game.ui.games

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hangingspider.game.game.engine.LetterBoard
import com.hangingspider.game.ui.theme.AppColors

/**
 * Letter board where the player drags through touching tiles. Dragging back onto the
 * previous tile undoes the last step. [onSubmit] receives the traced path when the finger lifts.
 */
@Composable
fun TraceBoard(
    board: LetterBoard,
    enabled: Boolean,
    onPathChange: (List<Int>) -> Unit,
    onSubmit: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    var path by remember(board) { mutableStateOf(emptyList<Int>()) }
    fun update(next: List<Int>) {
        path = next
        onPathChange(next)
    }

    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val gap = 8.dp
        val side = minOf(maxWidth, 360.dp)
        val tile = (side - gap * (board.size - 1)) / board.size
        Column(
            modifier = Modifier
                .size(side)
                .gridDrag(
                    rows = board.size,
                    cols = board.size,
                    enabled = enabled,
                    onStart = { r, c -> update(listOf(r * board.size + c)) },
                    onMove = { r, c ->
                        val index = r * board.size + c
                        val current = path
                        when {
                            current.isEmpty() -> update(listOf(index))
                            current.size >= 2 && index == current[current.size - 2] -> update(current.dropLast(1))
                            index !in current && board.isAdjacent(current.last(), index) -> update(current + index)
                        }
                    },
                    onEnd = {
                        val done = path
                        update(emptyList())
                        if (done.isNotEmpty()) onSubmit(done)
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            for (r in 0 until board.size) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (c in 0 until board.size) {
                        val index = r * board.size + c
                        val inPath = index in path
                        LetterTile(
                            text = board.tiles[index].lowercase().replaceFirstChar { it.uppercase() },
                            size = tile,
                            background = when {
                                index == path.lastOrNull() -> AppColors.Signal
                                inPath -> GameColors.TileSelected
                                else -> GameColors.Tile
                            },
                            textColor = if (inPath) AppColors.Ivory else AppColors.GoldBright,
                            fontScale = 0.42f
                        )
                    }
                }
            }
        }
    }
}
