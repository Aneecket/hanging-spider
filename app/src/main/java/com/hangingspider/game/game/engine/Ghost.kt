package com.hangingspider.game.game.engine

import com.hangingspider.game.game.WordIndex
import kotlin.random.Random

sealed interface GhostOutcome {
    data object Continue : GhostOutcome
    /** The move finished a real word of [GhostRules.MIN_WORD] or more letters. */
    data class CompletedWord(val word: String) : GhostOutcome
    /** No word starts with the fragment. */
    data class DeadEnd(val fragment: String) : GhostOutcome
}

object GhostRules {
    const val MIN_WORD = 4
    const val LOSSES_TO_LOSE = 3
    const val LETTERS = "GHOST"

    fun outcome(fragment: String, dictionary: WordIndex): GhostOutcome = when {
        fragment.length >= MIN_WORD && dictionary.isWord(fragment) -> GhostOutcome.CompletedWord(fragment)
        !dictionary.hasPrefix(fragment) -> GhostOutcome.DeadEnd(fragment)
        else -> GhostOutcome.Continue
    }

    /**
     * Picks the spider's letter. Safe letters keep the fragment alive without finishing a word;
     * among them it prefers letters where more everyday words would end on the player's turn.
     * Returns null when every letter loses.
     */
    fun spiderMove(
        fragment: String,
        dictionary: WordIndex,
        common: WordIndex,
        random: Random = Random.Default,
        mistakeRate: Double = 0.25
    ): Char? {
        val safe = ('A'..'Z').filter { outcome(fragment + it, dictionary) == GhostOutcome.Continue }
        if (safe.isEmpty()) return null
        if (random.nextDouble() < mistakeRate) return safe.random(random)
        return safe.maxBy { letter ->
            val next = fragment + letter
            val words = common.withPrefix(next).filter { it.length >= MIN_WORD && !finishesEarly(it, next.length, dictionary) }
            val playerFinishes = words.count { (it.length - next.length) % 2 == 1 }
            playerFinishes * 3 - (words.size - playerFinishes) + random.nextInt(3)
        }
    }

    /** True if a shorter real word would end the round before [word] could be spelled out. */
    private fun finishesEarly(word: String, from: Int, dictionary: WordIndex): Boolean =
        (maxOf(MIN_WORD, from + 1) until word.length).any { dictionary.isWord(word.substring(0, it)) }
}
