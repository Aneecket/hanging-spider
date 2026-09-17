package com.hangingspider.game.game.engine

import kotlin.random.Random

data class CrosswordEntry(
    val number: Int,
    val across: Boolean,
    val row: Int,
    val col: Int,
    val answer: String,
    val clue: String
) {
    val cells: List<Cell> get() = answer.indices.map { if (across) Cell(row, col + it) else Cell(row + it, col) }
}

class CrosswordPuzzle(val rows: Int, val cols: Int, val entries: List<CrosswordEntry>) {
    /** Solution letter per cell, null for black squares. */
    val solution: List<List<Char?>> = run {
        val g = List(rows) { MutableList<Char?>(cols) { null } }
        entries.forEach { e -> e.cells.forEachIndexed { i, c -> g[c.row][c.col] = e.answer[i] } }
        g
    }

    val numbers: Map<Cell, Int> = entries.associate { Cell(it.row, it.col) to it.number }

    fun entriesAt(cell: Cell): List<CrosswordEntry> = entries.filter { cell in it.cells }
}

object CrosswordGenerator {
    private const val WORK = 15

    private data class Placement(val word: String, val clue: String, val row: Int, val col: Int, val across: Boolean)

    fun generate(
        clues: List<Pair<String, String>>,
        targetWords: Int = 7,
        maxSide: Int = 9,
        random: Random = Random.Default
    ): CrosswordPuzzle? {
        val pool = clues.filter { it.first.length in 3..maxSide }.distinctBy { it.first }
        repeat(60) {
            val shuffled = pool.shuffled(random)
            val grid = Array(WORK) { arrayOfNulls<Char>(WORK) }
            val owned = HashSet<Triple<Int, Int, Boolean>>()
            val placed = ArrayList<Placement>()
            val first = shuffled.first()
            val start = Placement(first.first, first.second, WORK / 2, (WORK - first.first.length) / 2, true)
            write(grid, owned, start)
            placed += start
            for ((word, clue) in shuffled.drop(1)) {
                if (placed.size == targetWords) break
                if (placed.any { it.word == word }) continue
                val options = candidatePlacements(grid, owned, word, clue).filter { fitsBounds(placed + it, maxSide) }
                val best = options.maxByOrNull { crossings(grid, it) * 10 + random.nextInt(10) } ?: continue
                write(grid, owned, best)
                placed += best
            }
            if (placed.size == targetWords) return build(placed)
        }
        return null
    }

    private fun candidatePlacements(
        grid: Array<Array<Char?>>,
        owned: Set<Triple<Int, Int, Boolean>>,
        word: String,
        clue: String
    ): List<Placement> {
        val out = ArrayList<Placement>()
        for (r in 0 until WORK) for (c in 0 until WORK) {
            val letter = grid[r][c] ?: continue
            word.forEachIndexed { i, ch ->
                if (ch != letter) return@forEachIndexed
                for (across in listOf(true, false)) {
                    val p = if (across) Placement(word, clue, r, c - i, true) else Placement(word, clue, r - i, c, false)
                    if (canPlace(grid, owned, p)) out += p
                }
            }
        }
        return out
    }

    private fun canPlace(grid: Array<Array<Char?>>, owned: Set<Triple<Int, Int, Boolean>>, p: Placement): Boolean {
        val (dr, dc) = if (p.across) 0 to 1 else 1 to 0
        val endR = p.row + dr * (p.word.length - 1)
        val endC = p.col + dc * (p.word.length - 1)
        if (p.row < 0 || p.col < 0 || endR >= WORK || endC >= WORK) return false
        if (at(grid, p.row - dr, p.col - dc) != null || at(grid, endR + dr, endC + dc) != null) return false
        var crossing = 0
        for (i in p.word.indices) {
            val r = p.row + dr * i
            val c = p.col + dc * i
            val existing = grid[r][c]
            if (existing != null) {
                // Crossing squares must belong to a word running the other way.
                if (existing != p.word[i] || Triple(r, c, p.across) in owned) return false
                crossing++
                continue
            }
            // A new letter must not touch neighbours sideways, or it would form an unintended word.
            if (at(grid, r + dc, c + dr) != null || at(grid, r - dc, c - dr) != null) return false
        }
        return crossing in 1 until p.word.length
    }

    private fun crossings(grid: Array<Array<Char?>>, p: Placement): Int {
        val (dr, dc) = if (p.across) 0 to 1 else 1 to 0
        return p.word.indices.count { grid[p.row + dr * it][p.col + dc * it] != null }
    }

    private fun fitsBounds(placements: List<Placement>, maxSide: Int): Boolean {
        val cells = placements.flatMap { p -> p.word.indices.map { if (p.across) p.row to p.col + it else p.row + it to p.col } }
        val height = cells.maxOf { it.first } - cells.minOf { it.first } + 1
        val width = cells.maxOf { it.second } - cells.minOf { it.second } + 1
        return height <= maxSide && width <= maxSide
    }

    private fun at(grid: Array<Array<Char?>>, r: Int, c: Int): Char? =
        if (r in 0 until WORK && c in 0 until WORK) grid[r][c] else null

    private fun write(grid: Array<Array<Char?>>, owned: MutableSet<Triple<Int, Int, Boolean>>, p: Placement) {
        p.word.forEachIndexed { i, ch ->
            val r = if (p.across) p.row else p.row + i
            val c = if (p.across) p.col + i else p.col
            grid[r][c] = ch
            owned += Triple(r, c, p.across)
        }
    }

    private fun build(placements: List<Placement>): CrosswordPuzzle {
        val minR = placements.minOf { it.row }
        val minC = placements.minOf { it.col }
        val shifted = placements.map { it.copy(row = it.row - minR, col = it.col - minC) }
        val rows = shifted.maxOf { if (it.across) it.row else it.row + it.word.length - 1 } + 1
        val cols = shifted.maxOf { if (it.across) it.col + it.word.length - 1 else it.col } + 1
        val starts = shifted.map { it.row to it.col }.distinct().sortedWith(compareBy({ it.first }, { it.second }))
        val numberOf = starts.withIndex().associate { (i, rc) -> rc to i + 1 }
        val entries = shifted.map {
            CrosswordEntry(numberOf.getValue(it.row to it.col), it.across, it.row, it.col, it.word, it.clue)
        }.sortedWith(compareBy({ !it.across }, { it.number }))
        return CrosswordPuzzle(rows, cols, entries)
    }
}
