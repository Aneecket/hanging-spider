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

private fun LetterMark.emoji(): String = when (this) {
    LetterMark.CORRECT -> "🟩"
    LetterMark.PRESENT -> "🟨"
    LetterMark.ABSENT -> "⬛"
}

@Composable
fun FiveLetterGame(session: GameSession) {
    val answer = remember { FiveLetter.answerCandidates(WordLists.common.words).random(session.random) }
    var guesses by remember { mutableStateOf(emptyList<Pair<String, List<LetterMark>>>()) }
    var typing by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var over by remember { mutableStateOf(false) }
    var tries by remember { mutableIntStateOf(FiveLetter.TRIES) }
    var revealed by remember { mutableStateOf(mapOf<Int, Char>()) }

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

    fun grid(): String = guesses.joinToString("\n") { (_, marks) -> marks.joinToString("") { it.emoji() } }

    fun submit() {
        if (over || session.paused) return
        when {
            typing.length < FiveLetter.LENGTH -> message = "Not enough letters"
            !WordLists.dictionary.isWord(typing) -> message = "Not in word list"
            else -> {
                guesses = guesses + (typing to FiveLetter.mark(typing, answer))
                message = null
                val used = guesses.size
                if (typing == answer) {
                    over = true
                    session.end(
                        RoundResult(
                            true,
                            "Solved $answer in $used ${if (used == 1) "try" else "tries"}.",
                            stars = when {
                                used <= 3 -> 3
                                used <= 5 -> 2
                                else -> 1
                            },
                            share = "$used/$tries\n${grid()}"
                        )
                    )
                } else if (used == tries) {
                    session.offerSecondChance(
                        "Out of guesses!", "one more guess",
                        onGranted = { tries++ },
                        onDeclined = {
                            over = true
                            session.end(RoundResult(false, "The word was $answer.", share = "X/$tries\n${grid()}"))
                        }
                    )
                }
                typing = ""
            }
        }
    }

    fun revealLetter() {
        val known = guesses.flatMap { (word, marks) -> word.indices.filter { marks[it] == LetterMark.CORRECT } }.toSet() + revealed.keys
        val position = answer.indices.firstOrNull { it !in known } ?: return
        revealed = revealed + (position to answer[position])
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
            val byHeight = (maxHeight - gap * (tries - 1)) / tries
            val byWidth = (minOf(maxWidth, 340.dp) - gap * (FiveLetter.LENGTH - 1)) / FiveLetter.LENGTH
            val tile = minOf(byHeight, byWidth)
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                for (row in 0 until tries) {
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
        if (revealed.isNotEmpty()) {
            Text(
                "Hint: " + answer.indices.joinToString(" ") { revealed[it]?.toString() ?: "_" },
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.GoldBright
            )
        }
        Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = AppColors.Rose)
        HintButton("Hint · reveal a letter", enabled = !over && !session.paused, onClick = { session.requestHint { revealLetter() } })
        Spacer(Modifier.height(4.dp))
        GameKeyboard(
            onLetter = { if (!over && !session.paused && typing.length < FiveLetter.LENGTH) typing += it },
            keyColor = { keyMarks[it]?.color() },
            onEnter = ::submit,
            onDelete = { if (!over) typing = typing.dropLast(1) }
        )
        Spacer(Modifier.height(10.dp))
    }
}
