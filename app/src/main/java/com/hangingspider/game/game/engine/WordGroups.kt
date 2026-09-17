package com.hangingspider.game.game.engine

import kotlin.random.Random

/**
 * A themed set of words. [clue] is the one-word hint Clue Master gives for the group
 * (null for wordplay groups that only work in Word Groups). Words that could belong to two
 * themes are listed in both so the generators never put those groups on the same board.
 */
data class WordGroup(val name: String, val clue: String?, val words: List<String>)

object WordGroups {
    val all: List<WordGroup> = listOf(
        WordGroup("Planets", "ORBIT", listOf("MARS", "VENUS", "SATURN", "JUPITER", "MERCURY", "NEPTUNE", "URANUS", "EARTH")),
        WordGroup("Fruits", "ORCHARD", listOf("APPLE", "MANGO", "BANANA", "CHERRY", "GRAPE", "LEMON", "PEACH", "PEAR", "PLUM", "ORANGE", "KIWI", "LIME")),
        WordGroup("Colours", "PAINT", listOf("RED", "BLUE", "GREEN", "YELLOW", "PURPLE", "ORANGE", "PINK", "BROWN", "LIME", "INDIGO", "VIOLET")),
        WordGroup("Flowers", "PETAL", listOf("ROSE", "TULIP", "DAISY", "LILY", "ORCHID", "VIOLET", "POPPY", "IRIS", "SUNFLOWER")),
        WordGroup("Dog breeds", "KENNEL", listOf("POODLE", "BEAGLE", "BOXER", "PUG", "COLLIE", "HUSKY", "TERRIER", "DALMATIAN")),
        WordGroup("Big cats", "ROAR", listOf("LION", "TIGER", "LEOPARD", "JAGUAR", "CHEETAH", "PUMA", "PANTHER")),
        WordGroup("Car makers", "DEALER", listOf("FORD", "TOYOTA", "HONDA", "JAGUAR", "FERRARI", "TESLA", "AUDI", "NISSAN")),
        WordGroup("Vehicles", "WHEELS", listOf("CAR", "BUS", "TRUCK", "TRAIN", "BICYCLE", "TRAM", "VAN", "SCOOTER", "TAXI")),
        WordGroup("Instruments", "ORCHESTRA", listOf("PIANO", "VIOLIN", "GUITAR", "DRUM", "FLUTE", "TRUMPET", "CELLO", "HARP")),
        WordGroup("Organs", "SURGEON", listOf("HEART", "LIVER", "LUNG", "KIDNEY", "BRAIN", "STOMACH", "SPLEEN")),
        WordGroup("Weather", "FORECAST", listOf("RAIN", "SNOW", "HAIL", "FOG", "STORM", "THUNDER", "SLEET", "WIND", "TORNADO", "HURRICANE")),
        WordGroup("Kitchen tools", "UTENSIL", listOf("SPATULA", "WHISK", "LADLE", "GRATER", "TONGS", "COLANDER", "PEELER")),
        WordGroup("Currencies", "MONEY", listOf("DOLLAR", "EURO", "RUPEE", "YEN", "POUND", "PESO", "FRANC", "RUBLE")),
        WordGroup("Weights", "SCALE", listOf("GRAM", "OUNCE", "POUND", "TON", "STONE", "KILO")),
        WordGroup("Gemstones", "JEWEL", listOf("RUBY", "EMERALD", "DIAMOND", "SAPPHIRE", "PEARL", "OPAL", "TOPAZ", "AMETHYST", "TURQUOISE")),
        WordGroup("Shapes", "GEOMETRY", listOf("CIRCLE", "SQUARE", "TRIANGLE", "OVAL", "HEXAGON", "DIAMOND", "CUBE", "CONE", "SPHERE", "PYRAMID", "STAR")),
        WordGroup("Chess pieces", "CHECKMATE", listOf("KING", "QUEEN", "ROOK", "BISHOP", "KNIGHT", "PAWN")),
        WordGroup("Royalty", "CROWN", listOf("KING", "QUEEN", "PRINCE", "PRINCESS", "DUKE", "EMPEROR", "BARON", "RULER", "KNIGHT")),
        WordGroup("Sea creatures", "REEF", listOf("SHARK", "WHALE", "DOLPHIN", "OCTOPUS", "SQUID", "SEAL", "CRAB", "JELLYFISH")),
        WordGroup("Birds", "FEATHER", listOf("EAGLE", "SPARROW", "PARROT", "OWL", "PENGUIN", "ROBIN", "CROW", "SWAN", "FALCON")),
        WordGroup("Insects", "ANTENNA", listOf("ANT", "BEE", "BEETLE", "WASP", "MOTH", "FLY", "CRICKET", "BUTTERFLY")),
        WordGroup("Sports", "STADIUM", listOf("TENNIS", "SOCCER", "CRICKET", "HOCKEY", "RUGBY", "GOLF", "BOXING", "BASEBALL")),
        WordGroup("Clothing", "WARDROBE", listOf("SHIRT", "JACKET", "SCARF", "SOCKS", "DRESS", "COAT", "SKIRT", "GLOVES")),
        WordGroup("Furniture", "ROOM", listOf("CHAIR", "TABLE", "SOFA", "BED", "DESK", "SHELF", "STOOL", "COUCH")),
        WordGroup("Vegetables", "SALAD", listOf("CARROT", "POTATO", "ONION", "PEA", "CABBAGE", "SPINACH", "LETTUCE", "BROCCOLI")),
        WordGroup("Countries", "PASSPORT", listOf("FRANCE", "INDIA", "BRAZIL", "JAPAN", "EGYPT", "CANADA", "MEXICO", "SPAIN", "CHINA", "AUSTRALIA")),
        WordGroup("Capital cities", "CAPITAL", listOf("PARIS", "LONDON", "TOKYO", "ROME", "CAIRO", "MADRID", "DELHI", "BERLIN")),
        WordGroup("Months", "CALENDAR", listOf("JANUARY", "MARCH", "APRIL", "JUNE", "AUGUST", "OCTOBER", "DECEMBER", "MAY")),
        WordGroup("Feelings", "MOOD", listOf("HAPPY", "ANGRY", "SAD", "SCARED", "PROUD", "JEALOUS", "CALM", "NERVOUS")),
        WordGroup("Metals", "ALLOY", listOf("IRON", "GOLD", "SILVER", "COPPER", "TIN", "ZINC", "NICKEL", "LEAD", "MERCURY")),
        WordGroup("Tools", "TOOLBOX", listOf("HAMMER", "WRENCH", "SAW", "DRILL", "CHISEL", "PLIERS", "SCREWDRIVER")),
        WordGroup("Weapons", "ARMORY", listOf("SWORD", "SPEAR", "DAGGER", "AXE", "BOW", "CROSSBOW")),
        WordGroup("Trees", "FOREST", listOf("OAK", "PINE", "MAPLE", "BIRCH", "WILLOW", "CEDAR", "PALM", "ELM")),
        WordGroup("Body parts", "ANATOMY", listOf("ELBOW", "KNEE", "ANKLE", "WRIST", "SHOULDER", "THUMB", "PALM", "CHIN", "HAND", "FOOT", "EYE")),
        WordGroup("Farm animals", "BARN", listOf("COW", "PIG", "SHEEP", "GOAT", "HORSE", "CHICKEN", "DUCK", "DONKEY")),
        WordGroup("Desserts", "SWEET", listOf("CAKE", "PIE", "COOKIE", "PUDDING", "BROWNIE", "MUFFIN", "DONUT")),
        WordGroup("Drinks", "THIRST", listOf("COFFEE", "TEA", "JUICE", "MILK", "SODA", "WATER", "LEMONADE")),
        WordGroup("Jobs", "CAREER", listOf("DOCTOR", "TEACHER", "PILOT", "CHEF", "FARMER", "NURSE", "LAWYER", "BAKER")),
        WordGroup("Haunted house", "HAUNTED", listOf("GHOST", "BAT", "SKELETON", "COFFIN", "CANDLE", "COBWEB", "SPIDER")),
        WordGroup("Sports gear", "EQUIPMENT", listOf("RACKET", "HELMET", "NET", "BAT", "PUCK", "PADDLE")),
        WordGroup("Continents", "GLOBE", listOf("ASIA", "AFRICA", "EUROPE", "ANTARCTICA", "AUSTRALIA")),
        WordGroup("Oceans", "VOYAGE", listOf("PACIFIC", "ATLANTIC", "INDIAN", "ARCTIC", "SOUTHERN")),
        WordGroup("Dances", "BALLROOM", listOf("SALSA", "TANGO", "WALTZ", "BALLET", "DISCO", "RUMBA", "SAMBA")),
        WordGroup("Sauces", "DIP", listOf("KETCHUP", "MUSTARD", "MAYO", "RELISH", "SALSA", "VINEGAR")),
        WordGroup("Pasta", "ITALIAN", listOf("SPAGHETTI", "PENNE", "LASAGNA", "RAVIOLI", "MACARONI", "FUSILLI")),
        WordGroup("Snakes", "HISS", listOf("COBRA", "PYTHON", "VIPER", "MAMBA", "ANACONDA", "ADDER")),
        WordGroup("Greek letters", "ALPHABET", listOf("ALPHA", "BETA", "GAMMA", "DELTA", "SIGMA", "OMEGA", "THETA")),
        WordGroup("Star signs", "HOROSCOPE", listOf("ARIES", "TAURUS", "GEMINI", "LEO", "LIBRA", "SCORPIO", "VIRGO", "CANCER")),
        WordGroup("Stationery", "SCHOOL", listOf("PENCIL", "ERASER", "RULER", "STAPLER", "MARKER", "CRAYON")),
        WordGroup("Computer parts", "LAPTOP", listOf("MOUSE", "MONITOR", "KEYBOARD", "SCREEN", "MEMORY", "CHIP")),
        WordGroup("Rodents", "CHEESE", listOf("MOUSE", "RAT", "HAMSTER", "SQUIRREL", "BEAVER", "GERBIL")),
        WordGroup("Camping gear", "CAMPFIRE", listOf("TENT", "LANTERN", "COMPASS", "BACKPACK", "FLASHLIGHT", "MATCHES")),
        WordGroup("Bakery", "OVEN", listOf("BREAD", "BAGEL", "CROISSANT", "BAGUETTE", "BUN", "MUFFIN", "PRETZEL")),
        WordGroup("Hairstyles", "SALON", listOf("BRAID", "BUN", "PONYTAIL", "MOHAWK", "MULLET")),
        WordGroup("Seasons", "CLIMATE", listOf("SPRING", "SUMMER", "AUTUMN", "WINTER", "FALL")),
        WordGroup("Mythical creatures", "LEGEND", listOf("DRAGON", "UNICORN", "PHOENIX", "GRIFFIN", "MERMAID", "TROLL", "GOBLIN")),
        WordGroup("Shades of blue", "SKY", listOf("NAVY", "TEAL", "AZURE", "COBALT", "CYAN", "TURQUOISE", "INDIGO")),
        WordGroup("Coffee drinks", "CAFE", listOf("LATTE", "MOCHA", "ESPRESSO", "CAPPUCCINO", "AMERICANO")),
        WordGroup("Garden tools", "YARD", listOf("RAKE", "SHOVEL", "TROWEL", "SHEARS", "SPADE", "HOSE")),
        WordGroup("Baby animals", "NURSERY", listOf("PUPPY", "KITTEN", "CALF", "FOAL", "CUB", "LAMB", "PIGLET", "DUCKLING")),
        WordGroup("Cheeses", "DAIRY", listOf("CHEDDAR", "BRIE", "FETA", "GOUDA", "MOZZARELLA", "PARMESAN")),
        WordGroup("Nuts", "SHELL", listOf("ALMOND", "WALNUT", "PEANUT", "CASHEW", "PECAN", "HAZELNUT", "PISTACHIO")),
        WordGroup("Herbs", "AROMA", listOf("BASIL", "MINT", "THYME", "PARSLEY", "OREGANO", "ROSEMARY", "DILL", "SAGE")),
        WordGroup("Punctuation", "GRAMMAR", listOf("COMMA", "PERIOD", "COLON", "HYPHEN", "APOSTROPHE")),
        WordGroup("Ship parts", "SAILOR", listOf("ANCHOR", "MAST", "DECK", "HULL", "RUDDER", "KEEL")),
        WordGroup("Disasters", "DISASTER", listOf("EARTHQUAKE", "TSUNAMI", "TORNADO", "FLOOD", "AVALANCHE", "HURRICANE", "WILDFIRE")),
        WordGroup("Music styles", "PLAYLIST", listOf("JAZZ", "ROCK", "BLUES", "POP", "REGGAE", "FUNK", "OPERA", "COUNTRY")),
        WordGroup("Rocks", "GEOLOGY", listOf("GRANITE", "MARBLE", "BASALT", "SLATE", "LIMESTONE", "QUARTZ", "ROCK", "STONE")),
        WordGroup("Playing cards", "POKER", listOf("ACE", "JACK", "JOKER", "KING", "QUEEN", "SPADES", "HEARTS", "CLUBS", "SPADE")),
        WordGroup("___BALL", null, listOf("FOOT", "BASKET", "SNOW", "BASE", "HAND", "VOLLEY", "EYE", "MEAT")),
        WordGroup("FIRE___", null, listOf("WORK", "PLACE", "FLY", "ALARM", "WOOD", "TRUCK", "MAN", "PROOF")),
        WordGroup("Keyboard keys", null, listOf("SHIFT", "ENTER", "SPACE", "ESCAPE", "DELETE", "TAB", "CONTROL")),
        WordGroup("SUN___", null, listOf("RISE", "SET", "BURN", "SHINE", "DIAL", "LIGHT", "BEAM")),
        WordGroup("Things with keys", null, listOf("PIANO", "KEYBOARD", "LOCK", "MAP", "TYPEWRITER")),
    )

    /** Picks [count] groups whose word lists don't overlap. */
    fun pickDisjoint(count: Int, random: Random, from: List<WordGroup> = all): List<WordGroup>? {
        repeat(200) {
            val chosen = ArrayList<WordGroup>()
            val used = HashSet<String>()
            for (group in from.shuffled(random)) {
                if (group.words.none { it in used }) {
                    chosen += group
                    used += group.words
                    if (chosen.size == count) return chosen
                }
            }
        }
        return null
    }
}

class GroupsPuzzle(val groups: List<Pair<WordGroup, List<String>>>) {
    fun groupOf(word: String): Int = groups.indexOfFirst { word in it.second }

    companion object {
        fun generate(random: Random = Random.Default): GroupsPuzzle {
            val chosen = WordGroups.pickDisjoint(4, random) ?: error("Not enough word groups")
            return GroupsPuzzle(chosen.map { it to it.words.shuffled(random).take(4) })
        }
    }
}

enum class ClueRole { AGENT, NEUTRAL, TRAP }

data class ClueCard(val word: String, val role: ClueRole, val group: Int)

class ClueMasterPuzzle(val cards: List<ClueCard>, val agentGroups: List<WordGroup>) {
    companion object {
        const val AGENT_GROUPS = 3
        const val PER_GROUP = 2
        const val NEUTRALS = 9

        fun generate(random: Random = Random.Default): ClueMasterPuzzle {
            repeat(100) {
                val chosen = WordGroups.pickDisjoint(AGENT_GROUPS + 4, random) ?: return@repeat
                val agents = chosen.filter { it.clue != null }.take(AGENT_GROUPS)
                if (agents.size < AGENT_GROUPS) return@repeat
                val others = chosen - agents.toSet()
                val cards = ArrayList<ClueCard>()
                agents.forEachIndexed { i, g ->
                    g.words.shuffled(random).take(PER_GROUP).forEach { cards += ClueCard(it, ClueRole.AGENT, i) }
                }
                val trapGroup = others.first()
                cards += ClueCard(trapGroup.words.random(random), ClueRole.TRAP, -1)
                val neutralPool = others.drop(1).flatMap { it.words }.shuffled(random).take(NEUTRALS)
                if (neutralPool.size < NEUTRALS) return@repeat
                neutralPool.forEach { cards += ClueCard(it, ClueRole.NEUTRAL, -1) }
                val board = cards.map { it.word }.toSet()
                if (agents.any { it.clue in board }) return@repeat
                return ClueMasterPuzzle(cards.shuffled(random), agents)
            }
            error("Could not build a Clue Master board")
        }
    }
}
