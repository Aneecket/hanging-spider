package com.hangingspider.game.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hangingspider.game.game.engine.GroupsPuzzle
import com.hangingspider.game.ui.theme.AppColors

private const val GROUP_MISTAKES = 4

@Composable
fun GroupsGame(session: GameSession) {
    val puzzle = remember { GroupsPuzzle.generate(session.random) }
    var remaining by remember { mutableStateOf(puzzle.groups.flatMap { it.second }.shuffled()) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var solved by remember { mutableStateOf(emptyList<Int>()) }
    var mistakes by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Pick four words that belong together") }
    var over by remember { mutableStateOf(false) }
    var allowedMistakes by remember { mutableIntStateOf(GROUP_MISTAKES) }
    var guesses by remember { mutableStateOf(emptyList<List<Int>>()) }

    val squares = listOf("🟨", "🟩", "🟦", "🟪")
    fun grid() = guesses.joinToString("\n") { row -> row.joinToString("") { squares[it % squares.size] } }

    fun submit() {
        if (over || session.paused || selected.size != 4) return
        val groupIds = selected.map { puzzle.groupOf(it) }
        guesses = guesses + listOf(groupIds)
        val counts = groupIds.groupingBy { it }.eachCount()
        if (counts.size == 1) {
            val g = groupIds.first()
            solved = solved + g
            remaining = remaining - selected
            selected = emptySet()
            message = puzzle.groups[g].first.name
            if (solved.size == puzzle.groups.size) {
                over = true
                session.end(
                    RoundResult(
                        true,
                        "All four groups found with $mistakes ${if (mistakes == 1) "mistake" else "mistakes"}.",
                        stars = when {
                            mistakes == 0 -> 3
                            mistakes <= 2 -> 2
                            else -> 1
                        },
                        share = grid()
                    )
                )
            }
        } else {
            mistakes++
            message = if (counts.values.max() == 3) "One away…" else "Not a group"
            if (mistakes == allowedMistakes) {
                session.offerSecondChance(
                    "Out of mistakes!", "one more mistake",
                    onGranted = { allowedMistakes++ },
                    onDeclined = {
                        over = true
                        solved = solved + puzzle.groups.indices.filter { it !in solved }
                        remaining = emptyList()
                        session.end(
                            RoundResult(false, "Out of mistakes. The groups were: ${puzzle.groups.joinToString { it.first.name }}.", share = grid())
                        )
                    }
                )
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        solved.forEach { g ->
            val (group, words) = puzzle.groups[g]
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GameColors.Groups[g % GameColors.Groups.size])
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(group.name.uppercase(), style = MaterialTheme.typography.titleMedium, color = AppColors.Coal)
                Text(words.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = AppColors.Coal)
            }
        }

        remaining.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { word ->
                    val isSelected = word in selected
                    Box(
                        Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AppColors.Crimson else AppColors.CharcoalMid)
                            .clickable(enabled = !over) {
                                selected = when {
                                    isSelected -> selected - word
                                    selected.size < 4 -> selected + word
                                    else -> selected
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            word,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = when {
                                    word.length > 9 -> 9.sp
                                    word.length > 7 -> 11.sp
                                    else -> 13.sp
                                }
                            ),
                            color = AppColors.Ivory,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.IvoryDim)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mistakes left ", style = MaterialTheme.typography.labelLarge, color = AppColors.MutedText)
            repeat(allowedMistakes) {
                Box(
                    Modifier
                        .padding(3.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (it < allowedMistakes - mistakes) AppColors.Signal else AppColors.Charcoal)
                )
            }
        }
        HintButton("Hint · pair two words", enabled = !over && !session.paused, onClick = {
            session.requestHint {
                val group = puzzle.groups.indices.first { it !in solved }
                selected = puzzle.groups[group].second.take(2).toSet()
                message = "These two belong together"
            }
        })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Shuffle", { remaining = remaining.shuffled() })
            SecondaryButton("Deselect", { selected = emptySet() })
            PrimaryButton("Submit", ::submit, enabled = selected.size == 4 && !over)
        }
        Spacer(Modifier.height(16.dp))
    }
}
