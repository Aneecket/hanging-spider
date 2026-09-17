package com.hangingspider.game.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hangingspider.game.game.WordLists
import com.hangingspider.game.game.engine.HivePuzzle
import com.hangingspider.game.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

private val HexShape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = minOf(size.width, size.height) / 2f
    for (i in 0..5) {
        val angle = Math.toRadians(60.0 * i).toFloat()
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
fun HiveGame(session: GameSession) {
    val puzzle by produceState<HivePuzzle?>(null) {
        value = withContext(Dispatchers.Default) { HivePuzzle.generate(WordLists.common) }
    }
    val ready = puzzle ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Building the hive…", color = AppColors.MutedText)
        }
        return
    }
    HivePlay(ready, session)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HivePlay(puzzle: HivePuzzle, session: GameSession) {
    var outer by remember { mutableStateOf(puzzle.outer) }
    var typed by remember { mutableStateOf("") }
    var found by remember { mutableStateOf(emptyList<String>()) }
    var score by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Every word must use ${puzzle.center}") }
    var over by remember { mutableStateOf(false) }

    fun enter() {
        if (over || session.paused) return
        val word = typed
        typed = ""
        val problem = puzzle.problemWith(word, WordLists.dictionary)
        message = when {
            problem != null -> problem
            word in found -> "Already found"
            else -> {
                val points = puzzle.score(word)
                found = listOf(word) + found
                score += points
                if (score >= puzzle.goal) {
                    over = true
                    val pangram = found.any(puzzle::isPangram)
                    session.end(
                        RoundResult(
                            true,
                            "Reached $score points with ${found.size} words" + if (pangram) ", including a pangram." else ".",
                            stars = if (pangram) 3 else 2
                        )
                    )
                }
                if (puzzle.isPangram(word)) "Pangram! +$points" else "$word +$points"
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Score $score / ${puzzle.goal}", style = MaterialTheme.typography.titleMedium, color = AppColors.Ivory, modifier = Modifier.weight(1f))
            Text("${found.size} ${if (found.size == 1) "word" else "words"}", style = MaterialTheme.typography.labelLarge, color = AppColors.GoldBright)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (score.toFloat() / puzzle.goal).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
            color = AppColors.GoldBright,
            trackColor = AppColors.Charcoal
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 90.dp)
        ) {
            found.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (puzzle.isPangram(it)) AppColors.GoldBright else AppColors.Silver
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            typed.ifEmpty { " " },
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp, letterSpacing = 3.sp),
            color = AppColors.GoldBright,
            maxLines = 1
        )
        Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.IvoryDim)
        Spacer(Modifier.height(16.dp))

        val cell = 76.dp
        val onTap: (Char) -> Unit = { if (!over && !session.paused) typed += it }
        // Rows overlap slightly so the hexagons sit like a honeycomb.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HexKey(outer[0], cell, false, onTap)
                HexKey(outer[1], cell, false, onTap)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.offset(y = (-8).dp)) {
                HexKey(outer[2], cell, false, onTap)
                HexKey(puzzle.center, cell, true, onTap)
                HexKey(outer[3], cell, false, onTap)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.offset(y = (-16).dp)) {
                HexKey(outer[4], cell, false, onTap)
                HexKey(outer[5], cell, false, onTap)
            }
        }

        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Delete", { typed = typed.dropLast(1) })
            SecondaryButton("Shuffle", { outer = outer.shuffled() })
            PrimaryButton("Enter", ::enter, enabled = typed.isNotEmpty())
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HintButton("Hint · show a word", enabled = !over && !session.paused, onClick = {
                session.requestHint {
                    (puzzle.targets - found.toSet()).filterNot(puzzle::isPangram).randomOrNull()?.let { message = "Try: $it" }
                }
            })
            SecondaryButton("Give up", {
                if (!over) {
                    over = true
                    session.end(RoundResult(false, "You reached $score of ${puzzle.goal}. Pangram: ${puzzle.pangrams.first()}."))
                }
            })
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HexKey(letter: Char, size: Dp, isCenter: Boolean, onTap: (Char) -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(HexShape)
            .background(if (isCenter) AppColors.Crimson else AppColors.CharcoalMid)
            .clickable { onTap(letter) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            letter.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
            color = if (isCenter) AppColors.Ivory else AppColors.GoldBright
        )
    }
}
