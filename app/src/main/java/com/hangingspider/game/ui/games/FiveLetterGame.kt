package com.hangingspider.game.ui.games

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hangingspider.game.game.WordLists
import com.hangingspider.game.game.engine.FiveLetter
import com.hangingspider.game.game.engine.LetterMark
import com.hangingspider.game.ui.theme.AppColors

private fun LetterMark.color(): Color = when (this) {
    LetterMark.CORRECT -> GameColors.Correct
    LetterMark.PRESENT -> GameColors.Present
    LetterMark.ABSENT -> GameColors.Absent
}

@Composable
fun FiveLetterGame(onRoundEnd: (RoundResult) -> Unit) {
    val answer = remember { FiveLetter.answerCandidates(WordLists.common.words).random() }
    var guesses by remember { mutableStateOf(emptyList<Pair<String, List<LetterMark>>>()) }
    var typing by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var over by remember { mutableStateOf(false) }

    val keyMarks = remember(guesses) {
        val best = HashMap<Char, LetterMark>()
        guesses.forEach { (word, marks) ->
            word.forEachIndexed { i, ch ->
                val prev = best[ch]
                if (prev == null || marks[i].ordinal < prev.ordinal) best[ch] = marks[i]
            }
        }
        best
    }

    fun submit() {
        if (over) return
        when {
            typing.length < FiveLetter.LENGTH -> message = "Not enough letters"
            !WordLists.dictionary.isWord(typing) -> message = "Not in word list"
            else -> {
                val marks = FiveLetter.mark(typing, answer)
                guesses = guesses + (typing to marks)
                message = null
                if (typing == answer) {
                    over = true
                    onRoundEnd(RoundResult(true, "Solved $answer in ${guesses.size} ${if (guesses.size == 1) "try" else "tries"}."))
                } else if (guesses.size == FiveLetter.TRIES) {
                    over = true
                    onRoundEnd(RoundResult(false, "The word was $answer."))
                }
                typing = ""
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val gap = 6.dp
            val byHeight = (maxHeight - gap * (FiveLetter.TRIES - 1)) / FiveLetter.TRIES
            val byWidth = (minOf(maxWidth, 340.dp) - gap * (FiveLetter.LENGTH - 1)) / FiveLetter.LENGTH
            val tile = minOf(byHeight, byWidth)
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                for (row in 0 until FiveLetter.TRIES) {
                    val guess = guesses.getOrNull(row)
                    val text = guess?.first ?: if (row == guesses.size) typing else ""
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (col in 0 until FiveLetter.LENGTH) {
                            LetterTile(
                                text = text.getOrNull(col)?.toString() ?: "",
                                size = tile,
                                background = guess?.second?.get(col)?.color() ?: AppColors.CharcoalMid
                            )
                        }
                    }
                }
            }
        }
        Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = AppColors.Rose)
        Spacer(Modifier.height(6.dp))
        GameKeyboard(
            onLetter = { if (!over && typing.length < FiveLetter.LENGTH) typing += it },
            keyColor = { keyMarks[it]?.color() },
            onEnter = ::submit,
            onDelete = { if (!over) typing = typing.dropLast(1) }
        )
        Spacer(Modifier.height(10.dp))
    }
}
