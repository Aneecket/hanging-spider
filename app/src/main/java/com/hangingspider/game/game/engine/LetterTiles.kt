package com.hangingspider.game.game.engine

import com.hangingspider.game.game.WordIndex
import kotlin.random.Random

enum class Premium { NONE, DOUBLE_LETTER, TRIPLE_LETTER, DOUBLE_WORD, TRIPLE_WORD }

data class TilePlay(val words: List<String>, val score: Int)

/** Solo board-tile game rules: an 11x11 board, standard letter values, no blank tiles. */
object LetterTiles {
    const val SIZE = 11
    const val RACK = 7
    const val TURNS = 8
    const val TARGET = 100
    const val BINGO_BONUS = 50
    val CENTER = Cell(SIZE / 2, SIZE / 2)

    val values: Map<Char, Int> = buildMap {
        "AEILNORSTU".forEach { put(it, 1) }
        "DG".forEach { put(it, 2) }
        "BCMP".forEach { put(it, 3) }
        "FHVWY".forEach { put(it, 4) }
        put('K', 5)
        "JX".forEach { put(it, 8) }
        "QZ".forEach { put(it, 10) }
    }

    private const val DISTRIBUTION =
        "AAAAAAAAABBCCDDDDEEEEEEEEEEEEFFGGGHHIIIIIIIIIJKLLLLMMNNNNNNOOOOOOOOPPQRRRRRRSSSSTTTTTTUUUUVVWWXYYZ"

    /** Two-letter words that board play relies on; the main word list starts at three letters. */
    private val TWO_LETTER = setOf(
        "AA", "AB", "AD", "AE", "AG", "AH", "AI", "AL", "AM", "AN", "AR", "AS", "AT", "AW", "AX", "AY",
        "BA", "BE", "BI", "BO", "BY", "DA", "DE", "DO", "ED", "EF", "EH", "EL", "EM", "EN", "ER", "ES",
        "EX", "FA", "FE", "GO", "HA", "HE", "HI", "HM", "HO", "ID", "IF", "IN", "IS", "IT", "JO", "KA",
        "KI", "LA", "LI", "LO", "MA", "ME", "MI", "MO", "MU", "MY", "NA", "NE", "NO", "NU", "OD", "OE",
        "OF", "OH", "OI", "OK", "OM", "ON", "OP", "OR", "OS", "OW", "OX", "OY", "PA", "PE", "PI", "PO",
        "QI", "RE", "SH", "SI", "SO", "TA", "TI", "TO", "UH", "UM", "UN", "UP", "US", "UT", "WE", "WO",
        "XI", "XU", "YA", "YE", "YO", "ZA"
    )

    fun newBag(random: Random = Random.Default): MutableList<Char> = DISTRIBUTION.toMutableList().apply { shuffle(random) }

    fun premium(cell: Cell): Premium {
        val r = minOf(cell.row, SIZE - 1 - cell.row)
        val c = minOf(cell.col, SIZE - 1 - cell.col)
        val (a, b) = if (r <= c) r to c else c to r
        return when {
            a == 5 && b == 5 -> Premium.DOUBLE_WORD
            a == 0 && b == 0 -> Premium.TRIPLE_WORD
            a == 0 && b == 5 -> Premium.TRIPLE_WORD
            a == 1 && b == 1 -> Premium.DOUBLE_WORD
            a == 2 && b == 2 -> Premium.DOUBLE_WORD
            a == 0 && b == 3 -> Premium.DOUBLE_LETTER
            a == 1 && b == 5 -> Premium.TRIPLE_LETTER
            a == 3 && b == 3 -> Premium.TRIPLE_LETTER
            a == 2 && b == 4 -> Premium.DOUBLE_LETTER
            a == 4 && b == 5 -> Premium.DOUBLE_LETTER
            else -> Premium.NONE
        }
    }

    fun isWord(word: String, dictionary: WordIndex): Boolean =
        if (word.length == 2) word in TWO_LETTER else dictionary.isWord(word)

    /**
     * Validates tiles placed this turn ([placed]) against the board ([board], earlier tiles only)
     * and returns the words formed with their total score, or an error message.
     */
    fun evaluate(
        board: Map<Cell, Char>,
        placed: Map<Cell, Char>,
        dictionary: WordIndex
    ): Result<TilePlay> {
        if (placed.isEmpty()) return fail("Place some tiles first")
        if (placed.keys.any { it in board }) return fail("That square is taken")
        val rows = placed.keys.map { it.row }.toSet()
        val cols = placed.keys.map { it.col }.toSet()
        if (rows.size > 1 && cols.size > 1) return fail("Tiles must be in one line")
        val all = board + placed
        val across = rows.size == 1 && (cols.size > 1 || run {
            val only = placed.keys.first()
            Cell(only.row, only.col - 1) in all || Cell(only.row, only.col + 1) in all
        })

        val mainCells = run(across, placed.keys.first(), all)
        if (!placed.keys.all { it in mainCells }) return fail("Tiles must touch with no gaps")

        if (board.isEmpty()) {
            if (CENTER !in placed) return fail("First word must cover the centre star")
            if (placed.size < 2) return fail("First word needs at least two letters")
        } else {
            val touches = placed.keys.any { cell ->
                neighbours(cell).any { it in board }
            }
            if (!touches) return fail("Word must connect to tiles on the board")
        }

        val wordRuns = ArrayList<List<Cell>>()
        if (mainCells.size >= 2) wordRuns += mainCells
        for (cell in placed.keys) {
            val cross = run(!across, cell, all)
            if (cross.size >= 2) wordRuns += cross
        }
        if (wordRuns.isEmpty()) return fail("Make a word of at least two letters")

        var total = 0
        val words = ArrayList<String>()
        for (cells in wordRuns) {
            val word = cells.joinToString("") { all.getValue(it).toString() }
            if (!isWord(word, dictionary)) return fail("$word is not in the word list")
            words += word
            total += scoreRun(cells, all, placed.keys)
        }
        if (placed.size == RACK) total += BINGO_BONUS
        return Result.success(TilePlay(words, total))
    }

    private fun scoreRun(cells: List<Cell>, all: Map<Cell, Char>, fresh: Set<Cell>): Int {
        var sum = 0
        var multiplier = 1
        for (cell in cells) {
            val value = values.getValue(all.getValue(cell))
            if (cell in fresh) {
                when (premium(cell)) {
                    Premium.DOUBLE_LETTER -> sum += value * 2
                    Premium.TRIPLE_LETTER -> sum += value * 3
                    Premium.DOUBLE_WORD -> { sum += value; multiplier *= 2 }
                    Premium.TRIPLE_WORD -> { sum += value; multiplier *= 3 }
                    Premium.NONE -> sum += value
                }
            } else sum += value
        }
        return sum * multiplier
    }

    /** The maximal run of occupied cells through [cell] in one direction. */
    private fun run(across: Boolean, cell: Cell, all: Map<Cell, Char>): List<Cell> {
        val (dr, dc) = if (across) 0 to 1 else 1 to 0
        var start = cell
        while (Cell(start.row - dr, start.col - dc) in all) start = Cell(start.row - dr, start.col - dc)
        val out = ArrayList<Cell>()
        var cur = start
        while (cur in all) {
            out += cur
            cur = Cell(cur.row + dr, cur.col + dc)
        }
        return out
    }

    private fun neighbours(cell: Cell) = listOf(
        Cell(cell.row - 1, cell.col), Cell(cell.row + 1, cell.col),
        Cell(cell.row, cell.col - 1), Cell(cell.row, cell.col + 1)
    )

    private fun fail(message: String): Result<TilePlay> = Result.failure(IllegalArgumentException(message))
}
