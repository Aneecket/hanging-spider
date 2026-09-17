package com.hangingspider.game.game

data class StreakState(val count: Int, val lastDay: Long)

object Streaks {
    private val DAILY_REWARDS = listOf(100L, 150L, 200L, 250L, 300L, 400L, 500L)

    fun afterPlay(state: StreakState, today: Long): StreakState = when (state.lastDay) {
        today -> state
        today - 1 -> StreakState(state.count + 1, today)
        else -> StreakState(1, today)
    }

    /** Days in a row that are still alive (played today or yesterday). */
    fun current(state: StreakState, today: Long): Int = if (state.lastDay >= today - 1) state.count else 0

    /** Which day of the reward ladder today's daily reward uses (1-7). */
    fun rewardDay(state: StreakState, today: Long): Int = when (state.lastDay) {
        today -> state.count
        today - 1 -> state.count + 1
        else -> 1
    }.coerceIn(1, DAILY_REWARDS.size)

    fun dailyReward(state: StreakState, today: Long): Long = DAILY_REWARDS[rewardDay(state, today) - 1]

    /** A streak of 2+ days that broke only yesterday can be saved with a rewarded ad. */
    fun canRepair(state: StreakState, today: Long): Boolean = state.count >= 2 && state.lastDay == today - 2
}

object Stars {
    /** Hints cap a round at 2 stars; an ad second chance caps it at 1. */
    fun cap(base: Int, hintsUsed: Int, usedSecondChance: Boolean): Int {
        var stars = base.coerceIn(1, 3)
        if (hintsUsed > 0) stars = minOf(stars, 2)
        if (usedSecondChance) stars = 1
        return stars
    }

    fun key(level: Int) = "L$level"
}

/** Everything achievements are judged on, read from the player's profile. */
data class PlayerProgress(
    val stats: Map<String, Long>,
    val stars: Map<String, Int>,
    val level: Int,
    val streak: Int
) {
    fun stat(key: String): Long = stats[key] ?: 0L
}

data class Achievement(val id: String, val title: String, val description: String, val target: Long, val progress: (PlayerProgress) -> Long)

object Achievements {
    const val REWARD = 100L

    object Stat {
        const val WINS = "wins_total"
        const val PERFECT = "perfect_total"
        const val DAILY = "daily_completed"
        fun wins(type: GameType) = "wins_${type.name}"
        fun perfect(type: GameType) = "perfect_${type.name}"
    }

    val all: List<Achievement> = listOf(
        Achievement("first_win", "First victory", "Win any round", 1) { it.stat(Stat.WINS) },
        Achievement("explorer", "Explorer", "Win every game at least once", GameType.entries.size.toLong()) { p ->
            GameType.entries.count { p.stat(Stat.wins(it)) > 0 }.toLong()
        },
        Achievement("perfect", "Perfect round", "Earn 3 stars in a round", 1) { it.stat(Stat.PERFECT) },
        Achievement("star_collector", "Star collector", "Earn 3 stars on 5 different levels", 5) { p ->
            p.stars.values.count { it == 3 }.toLong()
        },
        Achievement("crossword_ace", "Crossword ace", "Finish 10 perfect crosswords", 10) { it.stat(Stat.perfect(GameType.CROSSWORD)) },
        Achievement("streak_3", "Warming up", "Play 3 days in a row", 3) { it.streak.toLong() },
        Achievement("streak_7", "On fire", "Play 7 days in a row", 7) { it.streak.toLong() },
        Achievement("daily_10", "Daily devotee", "Finish 10 daily puzzles", 10) { it.stat(Stat.DAILY) },
        Achievement("halfway", "Halfway there", "Unlock Level 8", 8) { it.level.toLong() },
        Achievement("summit", "Summit", "Unlock all ${GameType.entries.size} levels", GameType.entries.size.toLong()) { it.level.toLong() },
        Achievement("wins_50", "Word hunter", "Win 50 rounds", 50) { it.stat(Stat.WINS) },
        Achievement("wins_200", "Word master", "Win 200 rounds", 200) { it.stat(Stat.WINS) },
    )

    fun earned(progress: PlayerProgress): List<Achievement> = all.filter { it.progress(progress) >= it.target }
}
