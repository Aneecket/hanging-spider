package com.hangingspider.game.game.engine

import com.hangingspider.game.game.WordIndex
import kotlin.random.Random

/** A square board of letter tiles (a tile can hold "QU"). Words follow touching tiles, each used once. */
class LetterBoard(val size: Int, val tiles: List<String>) {

    fun isAdjacent(a: Int, b: Int): Boolean {
        val dr = kotlin.math.abs(a / size - b / size)
        val dc = kotlin.math.abs(a % size - b % size)
        return a != b && dr <= 1 && dc <= 1
    }

    fun wordFor(path: List<Int>): String = path.joinToString("") { tiles[it] }

    /** Every word in [index] of at least [minLength] letters that can be traced on the board. */
    fun solve(index: WordIndex, minLength: Int): Set<String> {
        val found = HashSet<String>()
        val used = BooleanArray(tiles.size)
        fun dfs(i: Int, prefix: String) {
            val word = prefix + tiles[i]
            if (!index.hasPrefix(word)) return
            if (word.length >= minLength && index.isWord(word)) found += word
            used[i] = true
            for (j in tiles.indices) if (!used[j] && isAdjacent(i, j)) dfs(j, word)
            used[i] = false
        }
        for (i in tiles.indices) dfs(i, "")
        return found
    }

    companion object {
        private val DICE = listOf(
            "AAEEGN", "ABBJOO", "ACHOPS", "AFFKPS", "AOOTTW", "CIMOTU", "DEILRX", "DELRVY",
            "DISTTY", "EEGHNW", "EEINSU", "EHRTVW", "EIOSST", "ELRTTY", "HIMNQU", "HLNNRZ"
        )
        private const val FREQUENT = "EEEEEAAAAIIIOOOTTTNNNSSRRRLLDDHCUMPGBYFWKV"

        /** Shaken 4x4 board using the classic 16-dice letter distribution. */
        fun rollDice(random: Random = Random.Default): LetterBoard {
            val tiles = DICE.shuffled(random).map { die ->
                val face = die[random.nextInt(die.length)]
                if (face == 'Q') "QU" else face.toString()
            }
            return LetterBoard(4, tiles)
        }

        /** Places [word] along a random self-avoiding path, filling other tiles with frequent letters. */
        fun withHiddenWord(word: String, size: Int, random: Random = Random.Default): LetterBoard? {
            val cells = size * size
            if (word.length > cells) return null
            repeat(200) {
                val path = ArrayList<Int>()
                val used = BooleanArray(cells)
                fun extend(at: Int): Boolean {
                    path += at
                    used[at] = true
                    if (path.size == word.length) return true
                    val next = (0 until cells).filter { !used[it] && adjacent(at, it, size) }.shuffled(random)
                    for (n in next) if (extend(n)) return true
                    path.removeAt(path.lastIndex)
                    used[at] = false
                    return false
                }
                if (extend(random.nextInt(cells))) {
                    val tiles = Array(cells) { FREQUENT[random.nextInt(FREQUENT.length)].toString() }
                    path.forEachIndexed { i, cell -> tiles[cell] = word[i].toString() }
                    return LetterBoard(size, tiles.toList())
                }
            }
            return null
        }

        private fun adjacent(a: Int, b: Int, size: Int): Boolean =
            a != b && kotlin.math.abs(a / size - b / size) <= 1 && kotlin.math.abs(a % size - b % size) <= 1

        /** Letter Grid points: 3-4 letters 1, 5 letters 2, 6 letters 3, 7 letters 5, 8+ letters 11. */
        fun gridScore(word: String): Int = when (word.length) {
            in 0..4 -> 1
            5 -> 2
            6 -> 3
            7 -> 5
            else -> 11
        }
    }
}
