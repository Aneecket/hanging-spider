package com.hangingspider.game.game.engine

import com.hangingspider.game.game.WordIndex
import kotlin.random.Random

class HivePuzzle(
    val center: Char,
    val outer: List<Char>,
    /** Everyday words the puzzle is scored against. */
    val targets: List<String>
) {
    val letters: Set<Char> = (outer + center).toSet()
    val totalScore: Int = targets.sumOf { score(it) }
    val goal: Int = (totalScore * 0.25).toInt().coerceAtLeast(10)
    val pangrams: List<String> = targets.filter { isPangram(it) }

    fun isPangram(word: String) = word.toSet() == letters

    fun score(word: String): Int = (if (word.length == 4) 1 else word.length) + if (isPangram(word)) 7 else 0

    /** Null when the word is acceptable, otherwise a short reason. */
    fun problemWith(word: String, dictionary: WordIndex): String? = when {
        word.length < 4 -> "Too short"
        center !in word -> "Missing centre letter"
        word.any { it !in letters } -> "Bad letters"
        !dictionary.isWord(word) -> "Not in word list"
        else -> null
    }

    companion object {
        const val MIN_TARGETS = 15
        const val MAX_TARGETS = 60

        fun generate(common: WordIndex, random: Random = Random.Default): HivePuzzle? {
            val usable = common.words.filter { it.length >= 4 && 'S' !in it }
            val seeds = usable.filter { it.toSet().size == 7 }.shuffled(random)
            for (seed in seeds.take(200)) {
                val letters = seed.toSet()
                val fitting = usable.filter { w -> w.all { it in letters } }
                for (center in letters.shuffled(random)) {
                    val targets = fitting.filter { center in it }
                    if (targets.size in MIN_TARGETS..MAX_TARGETS) {
                        return HivePuzzle(center, (letters - center).shuffled(random), targets)
                    }
                }
            }
            return null
        }
    }
}
