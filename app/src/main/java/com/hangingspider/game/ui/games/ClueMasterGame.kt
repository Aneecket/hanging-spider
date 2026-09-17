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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hangingspider.game.game.engine.ClueCard
import com.hangingspider.game.game.engine.ClueMasterPuzzle
import com.hangingspider.game.game.engine.ClueRole
import com.hangingspider.game.ui.theme.AppColors

private const val CLUE_STRIKES = 3

@Composable
fun ClueMasterGame(onRoundEnd: (RoundResult) -> Unit) {
    val puzzle = remember { ClueMasterPuzzle.generate() }
    var revealed by remember { mutableStateOf(emptySet<String>()) }
    var clueGroup by remember { mutableIntStateOf(0) }
    var strikes by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Tap the words that match the clue") }
    var over by remember { mutableStateOf(false) }

    val agents = puzzle.cards.filter { it.role == ClueRole.AGENT }
    val foundAgents = agents.count { it.word in revealed }

    fun remainingIn(group: Int) = agents.count { it.group == group && it.word !in revealed }

    fun nextClue() {
        clueGroup = (1..puzzle.agentGroups.size)
            .map { (clueGroup + it) % puzzle.agentGroups.size }
            .firstOrNull { remainingIn(it) > 0 } ?: clueGroup
    }

    fun reveal(card: ClueCard) {
        if (over || card.word in revealed) return
        revealed = revealed + card.word
        when (card.role) {
            ClueRole.TRAP -> {
                over = true
                onRoundEnd(RoundResult(false, "${card.word} was the trap word."))
            }
            ClueRole.NEUTRAL -> {
                strikes++
                if (strikes == CLUE_STRIKES) {
                    over = true
                    onRoundEnd(RoundResult(false, "Too many wrong guesses. You found $foundAgents of ${agents.size} secret words."))
                } else {
                    message = "${card.word} isn't one of yours"
                    nextClue()
                }
            }
            ClueRole.AGENT -> {
                if (agents.all { it.word in revealed }) {
                    over = true
                    onRoundEnd(RoundResult(true, "All ${agents.size} secret words found."))
                } else {
                    message = "${card.word} is one of yours!"
                    if (remainingIn(clueGroup) == 0) nextClue()
                }
            }
        }
    }

    val clue = puzzle.agentGroups[clueGroup]

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Found $foundAgents / ${agents.size}",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Ivory,
                modifier = Modifier.weight(1f)
            )
            repeat(CLUE_STRIKES) {
                Box(
                    Modifier
                        .padding(3.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (it < CLUE_STRIKES - strikes) AppColors.Signal else AppColors.Charcoal)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(AppColors.Bloodstone)
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("THE SPIDER'S CLUE", style = MaterialTheme.typography.labelMedium, color = AppColors.Silver)
            Text(
                "${clue.clue} · ${remainingIn(clueGroup)}",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp, letterSpacing = 2.sp),
                color = AppColors.GoldBright
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.IvoryDim, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))

        puzzle.cards.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                row.forEach { card ->
                    val shown = card.word in revealed || over
                    val background: Color = when {
                        !shown -> AppColors.CharcoalMid
                        card.role == ClueRole.AGENT -> GameColors.Correct
                        card.role == ClueRole.TRAP -> AppColors.Crimson
                        else -> AppColors.Charcoal
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(background)
                            .clickable(enabled = !over && card.word !in revealed) { reveal(card) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            card.word,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = when {
                                    card.word.length > 9 -> 9.sp
                                    card.word.length > 7 -> 11.sp
                                    else -> 13.sp
                                }
                            ),
                            color = if (shown && card.role == ClueRole.NEUTRAL) AppColors.MutedText else AppColors.Ivory,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        SecondaryButton("Next clue", { if (!over) { nextClue(); message = "New clue" } })
        Spacer(Modifier.height(16.dp))
    }
}
