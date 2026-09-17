package com.hangingspider.game.game

enum class GameType(val title: String, val howToPlay: String) {
    HANGMAN("Hangman", "Guess the hidden word one letter at a time before the spider drops."),
    WORD_SEARCH("Word Search", "Find the hidden words in the grid. Drag across letters in any straight line."),
    UNSCRAMBLE("Unscramble", "Rebuild five jumbled words from their letters before time runs out."),
    FIVE_LETTER("Five Letters", "Guess the five-letter word in six tries. Colours show which letters are right."),
    LETTER_GRID("Letter Grid", "Drag through touching letters to make words. Hit the target score before time runs out."),
    HIVE("Word Hive", "Make words of four or more letters from the hive. Every word must use the centre letter."),
    CROSSWORD("Mini Crossword", "Fill the grid using the clues. Words cross each other across and down."),
    GROUPS("Word Groups", "Sort 16 words into four groups of four that share something in common."),
    WORD_PATH("Word Path", "Trace words through touching letters. Find the star word and half of the hidden words."),
    LETTER_TILES("Letter Tiles", "Place tiles on the board to build crossing words. Reach the target score in eight turns."),
    GHOST("Ghost", "Take turns adding letters with the spider. Don't finish a word, and don't make a dead end."),
    REAL_OR_FAKE("Real or Fake", "Five strange words, four meanings each. Pick the real meaning."),
    CLUE_MASTER("Clue Master", "The spider gives a one-word clue. Find your secret words and avoid the trap word."),
}

object Levels {
    private const val POINTS_PER_LEVEL = 1500L

    val MAX: Int = GameType.entries.size

    /** Points needed to unlock [level]: Level 2 at 1,500, Level 3 at 3,000 ... Level 13 at 18,000. */
    fun pointsToUnlock(level: Int): Long = POINTS_PER_LEVEL * (level - 1)

    fun gameFor(level: Int): GameType = GameType.entries[(level - 1).coerceIn(0, MAX - 1)]
}
