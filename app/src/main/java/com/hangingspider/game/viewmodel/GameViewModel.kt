package com.hangingspider.game.viewmodel

import androidx.lifecycle.ViewModel
import com.hangingspider.game.game.Word
import com.hangingspider.game.game.WordBank
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameViewModel : ViewModel() {

    companion object {
        const val MAX_WRONG = 6
        const val HINT_COST = 50L
    }

    private val _state = MutableStateFlow(freshState())
    val state: StateFlow<GameState> = _state

    fun guess(letter: Char) {
        val s = _state.value
        if (s.status != GameStatus.PLAYING) return
        val up = letter.uppercaseChar()
        if (up in s.guessed) return
        val newGuessed = s.guessed + up
        val hit = up in s.word.text
        val newWrong = if (hit) s.wrong else s.wrong + 1
        val won = s.word.text.all { it in newGuessed }
        val lost = newWrong >= MAX_WRONG
        val status = when {
            won -> GameStatus.WON
            lost -> GameStatus.LOST
            else -> GameStatus.PLAYING
        }
        _state.value = s.copy(guessed = newGuessed, wrong = newWrong, status = status)
    }

    /** Picks a random unguessed letter that IS in the word and reveals it.
     *  Does not increment wrong. Returns the letter or null if nothing left. */
    fun revealHint(): Char? {
        val s = _state.value
        if (s.status != GameStatus.PLAYING) return null
        val remaining = s.word.text.toSet() - s.guessed
        if (remaining.isEmpty()) return null
        val letter = remaining.random()
        val newGuessed = s.guessed + letter
        val won = s.word.text.all { it in newGuessed }
        _state.value = s.copy(
            guessed = newGuessed,
            status = if (won) GameStatus.WON else s.status
        )
        return letter
    }

    fun canReveal(): Boolean {
        val s = _state.value
        return s.status == GameStatus.PLAYING && (s.word.text.toSet() - s.guessed).isNotEmpty()
    }

    fun reset() { _state.value = freshState() }

    private fun freshState(): GameState =
        GameState(word = WordBank.random(), guessed = emptySet(), wrong = 0, status = GameStatus.PLAYING)
}

enum class GameStatus { PLAYING, WON, LOST }

data class GameState(
    val word: Word,
    val guessed: Set<Char>,
    val wrong: Int,
    val status: GameStatus
) {
    val masked: String get() = word.text.map { if (it in guessed) it else '_' }.joinToString(" ")
    val remaining: Int get() = GameViewModel.MAX_WRONG - wrong
}
