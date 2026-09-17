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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hangingspider.game.game.engine.RealOrFake
import com.hangingspider.game.ui.theme.AppColors

@Composable
fun RealOrFakeGame(session: GameSession) {
    val questions = remember { RealOrFake.questions(session.random) }
    var index by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<Int?>(null) }
    var correct by remember { mutableIntStateOf(0) }
    var marks by remember { mutableStateOf(emptyList<Boolean>()) }
    var hidden by remember { mutableStateOf(emptySet<Int>()) }

    val q = questions[index]
    val isLast = index == questions.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Word ${index + 1} of ${questions.size} · $correct correct",
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.Ivory
        )
        Text(
            "Get ${RealOrFake.TO_WIN} right to win",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.MutedText
        )
        Spacer(Modifier.weight(1f))
        Text(
            q.word,
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 30.sp, letterSpacing = 2.sp),
            color = AppColors.GoldBright,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        q.options.forEachIndexed { i, option ->
            if (i in hidden) return@forEachIndexed
            val answered = chosen != null
            val background = when {
                !answered -> AppColors.CharcoalMid
                i == q.answer -> GameColors.Correct
                i == chosen -> AppColors.Crimson
                else -> AppColors.Charcoal
            }
            Text(
                option,
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.Ivory,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(background)
                    .clickable(enabled = !answered && !session.paused) {
                        chosen = i
                        marks = marks + (i == q.answer)
                        if (i == q.answer) correct++
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        chosen?.let {
            Text(
                if (it == q.answer) "Correct!" else "Not quite — the real meaning is highlighted.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (it == q.answer) AppColors.GoldBright else AppColors.Rose
            )
        }
        Spacer(Modifier.weight(1f))
        HintButton("Hint · remove two fakes", enabled = chosen == null && hidden.isEmpty() && !session.paused, onClick = {
            session.requestHint {
                hidden = q.options.indices.filter { it != q.answer }.shuffled().take(2).toSet()
            }
        })
        Spacer(Modifier.height(10.dp))
        PrimaryButton(
            if (isLast) "Finish" else "Next word",
            onClick = {
                if (isLast) {
                    session.end(
                        RoundResult(
                            correct >= RealOrFake.TO_WIN,
                            "You picked $correct of ${questions.size} real meanings.",
                            stars = when (correct) {
                                5 -> 3
                                4 -> 2
                                else -> 1
                            },
                            share = marks.joinToString("") { if (it) "✅" else "❌" }
                        )
                    )
                } else {
                    index++
                    chosen = null
                    hidden = emptySet()
                }
            },
            enabled = chosen != null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
    }
}
