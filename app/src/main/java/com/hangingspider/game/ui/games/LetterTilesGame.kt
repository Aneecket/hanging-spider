package com.hangingspider.game.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hangingspider.game.game.WordLists
import com.hangingspider.game.game.engine.Cell
import com.hangingspider.game.game.engine.LetterTiles
import com.hangingspider.game.game.engine.Premium
import com.hangingspider.game.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val TileFace = Color(0xFFE9DCC4)
private val TileFresh = Color(0xFFF5C76B)

private fun Premium.color(): Color = when (this) {
    Premium.TRIPLE_WORD -> Color(0xFF8E1B24)
    Premium.DOUBLE_WORD -> Color(0xFF6B3A48)
    Premium.TRIPLE_LETTER -> Color(0xFF1F4E79)
    Premium.DOUBLE_LETTER -> Color(0xFF2F5F6E)
    Premium.NONE -> AppColors.Charcoal
}

private fun Premium.label(): String = when (this) {
    Premium.TRIPLE_WORD -> "3W"
    Premium.DOUBLE_WORD -> "2W"
    Premium.TRIPLE_LETTER -> "3L"
    Premium.DOUBLE_LETTER -> "2L"
    Premium.NONE -> ""
}

@Composable
fun LetterTilesGame(session: GameSession) {
    val bag = remember { LetterTiles.newBag() }
    var rack by remember { mutableStateOf(List(LetterTiles.RACK) { bag.removeAt(bag.lastIndex) }) }
    var board by remember { mutableStateOf(mapOf<Cell, Char>()) }
    var placed by remember { mutableStateOf(mapOf<Cell, Int>()) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var turn by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Tap a tile, then a square. Start on the ★") }
    var over by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val placedLetters = placed.mapValues { rack[it.value] }
    val preview = if (placed.isEmpty()) null else LetterTiles.evaluate(board, placedLetters, WordLists.dictionary)

    fun finishTurn() {
        turn++
        placed = emptyMap()
        selected = null
        when {
            score >= LetterTiles.TARGET -> {
                over = true
                val used = turn - 1
                session.end(
                    RoundResult(
                        true,
                        "Scored $score in $used turns.",
                        stars = when {
                            used <= 5 -> 3
                            used <= 7 -> 2
                            else -> 1
                        }
                    )
                )
            }
            turn > LetterTiles.TURNS || rack.isEmpty() -> {
                over = true
                session.end(RoundResult(false, "You scored $score of ${LetterTiles.TARGET}."))
            }
        }
    }

    fun play() {
        if (over || session.paused) return
        val result = preview ?: return
        result.onFailure { message = it.message.orEmpty() }
        result.onSuccess { play ->
            board = board + placedLetters
            score += play.score
            val used = placed.values.toSet()
            val kept = rack.filterIndexed { i, _ -> i !in used }
            rack = kept + List(minOf(used.size, bag.size)) { bag.removeAt(bag.lastIndex) }
            message = "${play.words.joinToString(", ")} +${play.score}"
            finishTurn()
        }
    }

    fun swap() {
        if (over || session.paused) return
        if (bag.size < LetterTiles.RACK) {
            message = "Not enough tiles left to swap"
            return
        }
        val old = rack
        rack = List(LetterTiles.RACK) { bag.removeAt(bag.lastIndex) }
        bag.addAll(old)
        bag.shuffle()
        message = "Swapped your tiles"
        finishTurn()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                "Score $score / ${LetterTiles.TARGET}",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Ivory,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Turn ${minOf(turn, LetterTiles.TURNS)} / ${LetterTiles.TURNS}",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.GoldBright
            )
        }
        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val cell = minOf(maxWidth, 440.dp) / LetterTiles.SIZE
            Column(Modifier.border(1.dp, AppColors.Coal)) {
                for (r in 0 until LetterTiles.SIZE) {
                    Row {
                        for (c in 0 until LetterTiles.SIZE) {
                            val here = Cell(r, c)
                            BoardSquare(
                                cell = here,
                                size = cell,
                                fixed = board[here],
                                fresh = placed[here]?.let { rack[it] },
                                onClick = {
                                    if (over || session.paused || here in board) return@BoardSquare
                                    val pick = selected
                                    placed = when {
                                        here in placed -> placed - here
                                        pick != null -> (placed.filterValues { it != pick }) + (here to pick)
                                        else -> placed
                                    }
                                    selected = null
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            when {
                preview == null -> message
                preview.isSuccess -> "Play for ${preview.getOrThrow().score} points"
                else -> preview.exceptionOrNull()?.message.orEmpty()
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (preview?.isSuccess == true) AppColors.GoldBright else AppColors.IvoryDim,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        HintButton("Hint · suggest a word", enabled = !over && !session.paused, onClick = {
            session.requestHint {
                val letters = rack
                scope.launch {
                    val word = withContext(Dispatchers.Default) { LetterTiles.suggestWord(letters, WordLists.common) }
                    message = word?.let { "Try: $it" } ?: "No word found in these tiles. Try a swap."
                }
            }
        })
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            rack.forEachIndexed { i, letter ->
                val onBoard = i in placed.values
                RackTile(
                    letter = letter,
                    selected = selected == i,
                    hidden = onBoard,
                    onClick = { if (!over && !onBoard) selected = if (selected == i) null else i }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Recall", { placed = emptyMap(); selected = null })
            SecondaryButton("Shuffle", {
                if (placed.isEmpty()) rack = rack.shuffled()
            })
            SecondaryButton("Swap", ::swap)
            PrimaryButton("Play", ::play, enabled = preview?.isSuccess == true && !over)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BoardSquare(cell: Cell, size: Dp, fixed: Char?, fresh: Char?, onClick: () -> Unit) {
    val premium = LetterTiles.premium(cell)
    val letter = fixed ?: fresh
    Box(
        Modifier
            .size(size)
            .border(0.5.dp, AppColors.Coal)
            .background(
                when {
                    fresh != null -> TileFresh
                    fixed != null -> TileFace
                    else -> premium.color()
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            letter != null -> {
                Text(letter.toString(), color = AppColors.Coal, style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * 0.5f).sp))
                Text(
                    "${LetterTiles.values.getValue(letter)}",
                    color = AppColors.Coal,
                    fontSize = (size.value * 0.22f).sp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp)
                )
            }
            cell == LetterTiles.CENTER -> Text("★", color = AppColors.GoldBright, fontSize = (size.value * 0.5f).sp)
            premium != Premium.NONE -> Text(premium.label(), color = AppColors.Ivory.copy(alpha = 0.8f), fontSize = (size.value * 0.28f).sp)
        }
    }
}

@Composable
private fun RackTile(letter: Char, selected: Boolean, hidden: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    hidden -> AppColors.Charcoal.copy(alpha = 0.4f)
                    selected -> TileFresh
                    else -> TileFace
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!hidden) {
            Text(letter.toString(), color = AppColors.Coal, style = MaterialTheme.typography.titleLarge)
            Text(
                "${LetterTiles.values.getValue(letter)}",
                color = AppColors.Coal,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 2.dp)
            )
        }
    }
}
