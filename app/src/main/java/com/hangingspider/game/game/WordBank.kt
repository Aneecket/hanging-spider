package com.hangingspider.game.game

/** Word with its clue — the clue is what makes the game playable. */
data class Word(val text: String, val category: String, val hint: String)

object WordBank {
    private val words = listOf(
        Word("SPIDER",   "creature",  "eight-legged hunter"),
        Word("COBWEB",   "spider",    "an old abandoned trap"),
        Word("VENOM",    "danger",    "delivered by a bite"),
        Word("WEBSITE",  "modern",    "an internet destination"),
        Word("SILK",     "spider",    "the spider's thread"),
        Word("ARACHNID", "science",   "the spider family"),
        Word("FANG",     "predator",  "a sharp curved tooth"),
        Word("LEGS",     "anatomy",   "spiders have eight of these"),
        Word("PREY",     "hunt",      "what the hunter seeks"),
        Word("SPIN",     "verb",      "what a spider does to a web"),
        Word("NEST",     "shelter",   "a creature's home"),
        Word("HANGING",  "position",  "suspended in mid-air"),
        Word("CRAWL",    "motion",    "how many legs move"),
        Word("MYSTIC",   "mystery",   "of hidden truths"),
        Word("SHADOW",   "darkness",  "cast when light is blocked"),
        Word("MIDNIGHT", "time",      "the witching hour"),
        Word("PHANTOM",  "ghost",     "a shadowy presence"),
        Word("WHISPER",  "sound",     "quiet, almost silent speech"),
        Word("CRYPTIC",  "mystery",   "hard to decipher"),
        Word("SILENT",   "quiet",     "making no sound"),
        Word("STALK",    "hunt",      "to follow patiently"),
        Word("PATIENT",  "virtue",    "waits without hurry")
    )
    fun random(): Word = words.random()
}
