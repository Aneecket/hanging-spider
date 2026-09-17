package com.hangingspider.game.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.hangingspider.game.game.Word
import com.hangingspider.game.game.WordBank
import com.hangingspider.game.game.WordLists
import com.hangingspider.game.ui.theme.AppColors
import kotlinx.coroutines.delay

private const val UNSCRAMBLE_WORDS = 5
private const val UNSCRAMBLE_SECONDS = 150
private const val SKIP_PENALTY = 15

private fun pickScrambleWord(exclude: Set<String>): Pair<Word, List<Char>> {
    val word = WordBank.all.filter { it.text.length in 5..7 && it.text !in exclude }.random()
    var letters = word.text.toList().shuffled()
    while (String(letters.toCharArray()) == word.text) letters = letters.shuffled()
    return word to letters
}

@Composable
fun UnscrambleGame(paused: Boolean, onRoundEnd: (RoundResult) -> Unit) {
    var current by remember { mutableStateOf(pickScrambleWord(emptySet())) }
    var seen by remember { mutableStateOf(setOf(current.first.text)) }
    var picked by remember { mutableStateOf(emptyList<Int>()) }
    var solved by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    val countdown = rememberCountdown(UNSCRAMBLE_SECONDS, running = !paused) {
        onRoundEnd(RoundResult(false, "Time's up. You solved $solved of $UNSCRAMBLE_WORDS. The last word was ${current.first.text}."))
    }

    val (word, letters) = current
    val attempt = picked.joinToString("") { letters[it].toString() }

    fun nextWord() {
        val next = pickScrambleWord(seen)
        seen = seen + next.first.text
        current = next
        picked = emptyList()
    }

    LaunchedEffect(attempt) {
        if (attempt.length < word.text.length) return@LaunchedEffect
        // Any real word using all the letters counts, not just the one we picked.
        if (attempt == word.text || WordLists.dictionary.isWord(attempt)) {
            solved++
            message = "Nice! $attempt"
            if (solved == UNSCRAMBLE_WORDS) {
                onRoundEnd(RoundResult(true, "All $UNSCRAMBLE_WORDS words solved with ${formatSeconds(countdown.secondsLeft)} left."))
            } else {
                delay(500)
                nextWord()
            }
        } else {
            message = "Not quite, try again"
            delay(600)
            picked = emptyList()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                "Word ${minOf(solved + 1, UNSCRAMBLE_WORDS)} of $UNSCRAMBLE_WORDS",
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
        Spacer(Modifier.weight(1f))

        Text(word.category.uppercase(), style = MaterialTheme.typography.labelMedium, color = AppColors.Signal)
        Spacer(Modifier.height(6.dp))
        Text("“${word.hint}”", style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic), color = AppColors.ChromeBright)
        Spacer(Modifier.height(24.dp))

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 8.dp
            val tile = minOf((maxWidth - gap * (letters.size - 1)) / letters.size, 52.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    letters.indices.forEach { slot ->
                        val index = picked.getOrNull(slot)
                        LetterTile(
                            text = index?.let { letters[it].toString() } ?: "",
                            size = tile,
                            background = if (index != null) AppColors.Crimson else AppColors.CharcoalMid,
                            modifier = Modifier.clickable(enabled = index != null && !paused) {
                                picked = picked.filterIndexed { i, _ -> i != slot }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    letters.forEachIndexed { i, ch ->
                        val used = i in picked
                        LetterTile(
                            text = if (used) "" else ch.toString(),
                            size = tile,
                            background = if (used) AppColors.Charcoal.copy(alpha = 0.3f) else GameColors.Tile,
                            textColor = AppColors.GoldBright,
                            modifier = Modifier.clickable(enabled = !used && !paused) { picked = picked + i }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = AppColors.IvoryDim)
        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Shuffle", {
                val order = letters.indices.shuffled()
                current = word to order.map { letters[it] }
                picked = emptyList()
            })
            SecondaryButton("Clear", { picked = emptyList() })
            SecondaryButton("Skip −${SKIP_PENALTY}s", {
                countdown.secondsLeft = (countdown.secondsLeft - SKIP_PENALTY).coerceAtLeast(0)
                message = "Skipped ${word.text}"
                nextWord()
            }, enabled = !paused)
        }
        Spacer(Modifier.height(24.dp))
    }
}
