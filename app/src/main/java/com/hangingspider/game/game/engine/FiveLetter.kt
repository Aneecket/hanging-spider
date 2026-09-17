package com.hangingspider.game.game.engine

enum class LetterMark { CORRECT, PRESENT, ABSENT }

object FiveLetter {
    const val LENGTH = 5
    const val TRIES = 6

    /** Wordle-style marks, handling repeated letters: exact matches claim letters first. */
    fun mark(guess: String, answer: String): List<LetterMark> {
        val marks = MutableList(guess.length) { LetterMark.ABSENT }
        val remaining = HashMap<Char, Int>()
        for (i in guess.indices) {
            if (guess[i] == answer[i]) marks[i] = LetterMark.CORRECT
            else remaining[answer[i]] = (remaining[answer[i]] ?: 0) + 1
        }
        for (i in guess.indices) {
            if (marks[i] == LetterMark.CORRECT) continue
            val left = remaining[guess[i]] ?: 0
            if (left > 0) {
                marks[i] = LetterMark.PRESENT
                remaining[guess[i]] = left - 1
            }
        }
        return marks
    }

    /** Everyday five-letter words, skipping most plurals and past tenses so answers feel fair. */
    fun answerCandidates(common: List<String>): List<String> = common.filter {
        it.length == LENGTH && !(it.endsWith("S") && !it.endsWith("SS")) && !it.endsWith("ED")
    }
}
