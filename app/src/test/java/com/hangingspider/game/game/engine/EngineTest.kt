package com.hangingspider.game.game.engine

import com.hangingspider.game.game.WordBank
import com.hangingspider.game.game.WordIndex
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class EngineTest {
    companion object {
        private fun load(name: String) = WordIndex(File("src/main/assets/words/$name").readLines().filter { it.isNotBlank() })
        val common by lazy { load("common.txt") }
        val dictionary by lazy { load("dictionary.txt") }
    }

    @Test fun wordIndexPrefixes() {
        val idx = WordIndex(listOf("CAT", "CATS", "DOG"))
        assertTrue(idx.hasPrefix("CA"))
        assertTrue(idx.hasPrefix("DOG"))
        assertFalse(idx.hasPrefix("CX"))
        assertEquals(listOf("CAT", "CATS"), idx.withPrefix("CAT"))
    }

    @Test fun fiveLetterMarksRepeatedLetters() {
        val c = LetterMark.CORRECT; val p = LetterMark.PRESENT; val a = LetterMark.ABSENT
        assertEquals(listOf(c, c, c, c, c), FiveLetter.mark("APPLE", "APPLE"))
        assertEquals(listOf(p, p, a, a, a), FiveLetter.mark("LLAMA", "HELLO"))
        // Only one E in the answer: the exact match claims it, the other Es are absent.
        assertEquals(listOf(a, c, a, a, a), FiveLetter.mark("EERIE", "BEAST"))
    }

    @Test fun wordSearchWordsReadAlongTheirCells() {
        val words = WordBank.all.filter { it.category == "food" }.map { it.text }
        repeat(50) { seed ->
            val puzzle = WordSearchGenerator.generate("Food", words, random = Random(seed))!!
            assertEquals(7, puzzle.words.size)
            for (pw in puzzle.words) {
                assertEquals(pw.word, pw.cells.joinToString("") { puzzle.grid[it.row][it.col].toString() })
                assertEquals(pw.cells, WordSearchGenerator.line(pw.cells.first(), pw.cells.last()))
            }
            assertTrue(puzzle.grid.all { row -> row.all { it in 'A'..'Z' } })
        }
    }

    @Test fun letterBoardSolvesAndHidesWords() {
        val board = LetterBoard(3, listOf("C", "A", "T", "X", "X", "S", "X", "X", "X"))
        val found = board.solve(WordIndex(listOf("CAT", "CATS", "ACT", "TACS")), 3)
        assertEquals(setOf("CAT", "CATS"), found)
        repeat(30) { seed ->
            val word = "SPIDERWEB"
            val hidden = LetterBoard.withHiddenWord(word, 3, Random(seed))!!
            assertTrue(word in hidden.solve(WordIndex(listOf(word)), 4))
        }
        val quBoard = LetterBoard(2, listOf("QU", "I", "T", "E"))
        assertTrue("QUIT" in quBoard.solve(WordIndex(listOf("QUIT")), 3))
    }

    @Test fun hivePuzzlesAreConsistent() {
        repeat(10) { seed ->
            val hive = HivePuzzle.generate(common, Random(seed))!!
            assertEquals(6, hive.outer.size)
            assertEquals(7, hive.letters.size)
            assertTrue(hive.targets.size in HivePuzzle.MIN_TARGETS..HivePuzzle.MAX_TARGETS)
            assertTrue(hive.pangrams.isNotEmpty())
            hive.targets.forEach { assertNull(it, hive.problemWith(it, dictionary)) }
            assertTrue(hive.goal <= hive.totalScore)
        }
    }

    @Test fun crosswordsHaveNoAccidentalWords() {
        val clues = WordBank.all.map { it.text to it.hint }
        repeat(60) { seed ->
            val puzzle = CrosswordGenerator.generate(clues, random = Random(seed))!!
            assertEquals(7, puzzle.entries.size)
            assertTrue(puzzle.rows <= 9 && puzzle.cols <= 9)
            puzzle.entries.forEach { e ->
                assertEquals(e.answer, e.cells.joinToString("") { puzzle.solution[it.row][it.col].toString() })
                assertEquals(e.number, puzzle.numbers[Cell(e.row, e.col)])
            }
            val entryRuns = puzzle.entries.map { it.cells }.toSet()
            for (across in listOf(true, false)) {
                val outer = if (across) puzzle.rows else puzzle.cols
                val inner = if (across) puzzle.cols else puzzle.rows
                for (o in 0 until outer) {
                    var run = ArrayList<Cell>()
                    for (i in 0..inner) {
                        val cell = if (across) Cell(o, i) else Cell(i, o)
                        val filled = i < inner && puzzle.solution[cell.row][cell.col] != null
                        if (filled) run.add(cell) else {
                            if (run.size >= 2) assertTrue("Unexpected run $run (seed $seed)", run in entryRuns)
                            run = ArrayList()
                        }
                    }
                }
            }
        }
    }

    @Test fun letterTilesRules() {
        val c = LetterTiles.CENTER
        fun at(dr: Int, dc: Int) = Cell(c.row + dr, c.col + dc)

        assertTrue(LetterTiles.evaluate(emptyMap(), mapOf(at(0, 1) to 'C', at(0, 2) to 'A', at(0, 3) to 'T'), dictionary).isFailure)

        val first = LetterTiles.evaluate(emptyMap(), mapOf(at(0, -1) to 'C', at(0, 0) to 'A', at(0, 1) to 'T'), dictionary)
        // C and T sit on double-letter squares beside the centre star: (6 + 1 + 2) x 2.
        assertEquals(listOf("CAT"), first.getOrThrow().words)
        assertEquals(18, first.getOrThrow().score)

        val board = mapOf(at(0, -1) to 'C', at(0, 0) to 'A', at(0, 1) to 'T')
        assertTrue(LetterTiles.evaluate(board, mapOf(at(0, 2) to 'S'), dictionary).getOrThrow().words == listOf("CATS"))
        assertTrue(LetterTiles.evaluate(board, mapOf(at(3, 3) to 'A', at(3, 4) to 'T'), dictionary).isFailure)
        assertTrue(LetterTiles.evaluate(board, mapOf(at(1, -1) to 'O', at(1, 1) to 'O'), dictionary).isFailure)
        assertTrue(LetterTiles.evaluate(board, mapOf(at(1, 0) to 'X', at(1, 1) to 'Q'), dictionary).isFailure)

        val down = LetterTiles.evaluate(board, mapOf(at(1, 1) to 'O', at(2, 1) to 'E'), dictionary).getOrThrow()
        assertEquals(listOf("TOE"), down.words)
    }

    @Test fun ghostSpiderNeverLosesWhenItCanAvoidIt() {
        assertEquals(GhostOutcome.CompletedWord("GAME"), GhostRules.outcome("GAME", dictionary))
        assertTrue(GhostRules.outcome("QZX", dictionary) is GhostOutcome.DeadEnd)
        assertEquals(GhostOutcome.Continue, GhostRules.outcome("CAT", dictionary))
        for ((seed, fragment) in listOf("", "S", "PL", "TRA", "QU", "ELE").withIndex()) {
            val move = GhostRules.spiderMove(fragment, dictionary, common, Random(seed), mistakeRate = 0.0)
            assertNotNull(move)
            assertEquals(GhostOutcome.Continue, GhostRules.outcome(fragment + move, dictionary))
        }
    }

    @Test fun wordGroupsAreWellFormed() {
        WordGroups.all.forEach { g ->
            assertTrue(g.name, g.words.size >= 4)
            assertEquals(g.name, g.words.size, g.words.toSet().size)
            g.words.forEach { assertTrue(it, it.all { ch -> ch in 'A'..'Z' }) }
            g.clue?.let { assertFalse("${g.name} clue is one of its words", it in g.words) }
        }
        repeat(200) { seed ->
            val puzzle = GroupsPuzzle.generate(Random(seed))
            val words = puzzle.groups.flatMap { it.second }
            assertEquals(16, words.toSet().size)

            val clue = ClueMasterPuzzle.generate(Random(seed))
            assertEquals(16, clue.cards.map { it.word }.toSet().size)
            assertEquals(6, clue.cards.count { it.role == ClueRole.AGENT })
            assertEquals(1, clue.cards.count { it.role == ClueRole.TRAP })
            assertTrue(clue.agentGroups.none { g -> clue.cards.any { it.word == g.clue } })
        }
    }

    @Test fun realOrFakeQuestions() {
        RealOrFake.words.forEach { assertEquals(it.word, 3, it.fakes.toSet().size) }
        assertEquals(RealOrFake.words.size, RealOrFake.words.map { it.word }.toSet().size)
        val qs = RealOrFake.questions(Random(1))
        assertEquals(RealOrFake.QUESTIONS, qs.size)
        qs.forEach { q -> assertEquals(RealOrFake.words.first { it.word == q.word }.meaning, q.options[q.answer]) }
    }
}
