package com.hangingspider.game.game.engine

import kotlin.random.Random

data class OddWord(val word: String, val meaning: String, val fakes: List<String>)

data class OddWordQuestion(val word: String, val options: List<String>, val answer: Int)

object RealOrFake {
    const val QUESTIONS = 5
    const val TO_WIN = 3

    val words: List<OddWord> = listOf(
        OddWord("PETRICHOR", "The earthy smell after rain falls on dry ground", listOf("A fear of thunderstorms", "A small stone used to sharpen blades", "The glow of a candle seen through fog")),
        OddWord("DEFENESTRATE", "To throw someone or something out of a window", listOf("To remove the fence around a field", "To cut down a whole forest", "To strip paint from a wall")),
        OddWord("SNOLLYGOSTER", "A shrewd, dishonest person, often a politician", listOf("A large wading bird of the marshes", "A sweet pastry filled with jam", "A child's spinning toy")),
        OddWord("GALLIMAUFRY", "A confused jumble or medley of things", listOf("A formal dance of the royal court", "A tall hat worn by bishops", "A fever caused by too much sun")),
        OddWord("BORBORYGMUS", "A rumbling noise made by gas in the intestines", listOf("An ancient Greek wrestling hold", "A sea sponge found in warm reefs", "A monster from old Norse tales")),
        OddWord("LOLLYGAG", "To waste time by dawdling", listOf("A lollipop shaped like a spiral", "A sailor's rope knot", "To laugh until you cry")),
        OddWord("ABSQUATULATE", "To leave suddenly and secretly", listOf("To sit down on the ground", "To square up the corners of a box", "To argue loudly in public")),
        OddWord("BUMFUZZLE", "To confuse or fluster someone", listOf("A soft woollen blanket", "A bee's buzzing flight", "To polish shoes until they shine")),
        OddWord("CATTYWAMPUS", "Askew or out of alignment", listOf("A wild cat of the mountains", "A cat's loud, angry yowl", "A game played with string")),
        OddWord("FLIBBERTIGIBBET", "A frivolous, flighty person who talks too much", listOf("A small fish that skips over water", "A wooden puppet on strings", "A bell rung at weddings")),
        OddWord("KERFUFFLE", "A commotion or fuss", listOf("A knitted winter hat", "A dog's shaggy coat", "A crumbly biscuit")),
        OddWord("WIDDERSHINS", "In a direction contrary to the sun's course; anticlockwise", listOf("Shin guards worn by old knights", "The ripples left behind a boat", "A widow's black mourning clothes")),
        OddWord("SESQUIPEDALIAN", "Given to using long words", listOf("Having six feet, like an insect", "Walking on tiptoe", "A bridge built for walkers only")),
        OddWord("OMPHALOS", "The navel; a central point or hub", listOf("A large clay pot for olive oil", "A shout of triumph in Greek plays", "A long curved sword")),
        OddWord("SUSURRUS", "A soft whispering or rustling sound", listOf("A south wind in summer", "A sour fruit from the tropics", "A clumsy fall")),
        OddWord("TATTERDEMALION", "A person in ragged clothing", listOf("A lion statue at a gate", "A crumbling old castle", "A drum beaten at parades")),
        OddWord("COLLYWOBBLES", "Stomach pain or nervousness", listOf("A wobbly jelly dessert", "The rocking of a small boat", "Loose cobblestones in a road")),
        OddWord("HULLABALOO", "A noisy uproar", listOf("A hollow log used as a canoe", "A round hat with a brim", "A greeting shouted from ships")),
        OddWord("GOBBLEDYGOOK", "Pompous or unintelligible jargon", listOf("A turkey's call in spring", "Sticky mud at a riverbank", "A greedy eater")),
        OddWord("QUIDNUNC", "A nosy person who always wants the latest gossip", listOf("A coin worth one old penny", "A lawyer's formal apology", "A monk who never speaks")),
        OddWord("ERINACEOUS", "Of or like a hedgehog", listOf("Relating to Ireland", "Easily angered", "Covered in rust")),
        OddWord("PANDICULATION", "Stretching and yawning, as when waking", listOf("Caring for pandas in zoos", "Making bread without yeast", "Painting on wet plaster")),
        OddWord("ULULATE", "To howl or wail loudly", listOf("To wave both hands overhead", "To swim in circles", "To gather fruit from trees")),
        OddWord("VERISIMILITUDE", "The appearance of being true or real", listOf("A strong dislike of green", "A flat, calm stretch of sea", "The ability to copy voices")),
        OddWord("ZUGZWANG", "A chess position where any move worsens your position", listOf("A German marching song", "A two-handed saw", "A sudden gust of wind")),
        OddWord("SCHADENFREUDE", "Pleasure taken in someone else's misfortune", listOf("Fear of making mistakes", "A cake eaten at harvest time", "Joy at seeing snow")),
        OddWord("LACHRYMOSE", "Tearful or given to weeping", listOf("Smelling of roses", "Lazy and slow", "Covered in lace")),
        OddWord("OBSEQUIOUS", "Excessively eager to please or obey", listOf("Hard to see clearly", "Stubbornly silent", "Relating to funerals")),
        OddWord("PERSPICACIOUS", "Having a ready insight; shrewd", listOf("Sweating heavily", "Easily seen through", "Spicy to the taste")),
        OddWord("RUMBUSTIOUS", "Boisterous and unruly", listOf("Made with rum", "Rough to the touch", "Round and fat")),
        OddWord("SKULDUGGERY", "Underhand or dishonest behaviour", listOf("Digging up old bones", "A pirate's flag", "Rowing a boat with one oar")),
        OddWord("WHIPPERSNAPPER", "A young, overconfident person", listOf("A crisp wafer biscuit", "A snapping turtle", "A shepherd's whistle")),
        OddWord("BAILIWICK", "A person's area of activity or interest", listOf("A candle wick made of straw", "A small riverside hut", "A bucket for bailing water")),
        OddWord("CURMUDGEON", "A bad-tempered, surly person", listOf("A thick porridge", "A heavy wooden club", "A castle dungeon")),
        OddWord("DISCOMBOBULATE", "To confuse or disconcert", listOf("To take apart a machine", "To stop a music party", "To remove a door's hinges")),
        OddWord("FLUMMOX", "To perplex or bewilder", listOf("A shaggy mountain goat", "A frothy dessert", "To float on water")),
        OddWord("JENTACULAR", "Relating to breakfast", listOf("Relating to jellyfish", "Very gentle", "Jagged like teeth")),
        OddWord("MELLIFLUOUS", "Sweet and smooth sounding", listOf("Full of honey bees", "Soft and melting", "Tired and sleepy")),
        OddWord("NUDIUSTERTIAN", "Relating to the day before yesterday", listOf("Relating to bare trees", "Relating to newborn animals", "Relating to the far north")),
        OddWord("RAGAMUFFIN", "A person in ragged, dirty clothes", listOf("A spicy breakfast muffin", "A cloth for polishing", "A shaggy pony")),
        OddWord("SLUBBERDEGULLION", "A slovenly, worthless person", listOf("A seagull that steals food", "A heavy rain shower", "A sailor's thick jumper")),
        OddWord("TROGLODYTE", "A cave dweller", listOf("A giant river toad", "A three-wheeled cart", "A tiny hummingbird")),
        OddWord("YONDERLY", "Mentally distant; absent-minded", listOf("Very far to the west", "Once a year", "Stubborn and young")),
        OddWord("BRUMOUS", "Foggy or wintry", listOf("Relating to brooms", "Loud and deep", "Brownish red")),
        OddWord("FUSTIGATE", "To beat with a stick; to criticise severely", listOf("To make something dusty", "To act in a hurry", "To fuss over details")),
        OddWord("GRANDILOQUENT", "Pompous or extravagant in language", listOf("Very old and wise", "Owning huge lands", "Talking to grandchildren")),
        OddWord("HOBBLEDEHOY", "A clumsy or awkward youth", listOf("A horse with a limp", "A cheerful sea song", "A wooden hobby horse")),
        OddWord("MUMPSIMUS", "A person who clings to a mistake despite correction", listOf("A children's illness", "A silent actor", "A small burrowing rodent")),
    )

    fun questions(random: Random = Random.Default): List<OddWordQuestion> =
        words.shuffled(random).take(QUESTIONS).map { w ->
            val options = (w.fakes + w.meaning).shuffled(random)
            OddWordQuestion(w.word, options, options.indexOf(w.meaning))
        }
}
