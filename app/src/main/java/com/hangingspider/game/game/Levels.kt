package com.hangingspider.game.game

object Levels {
    private const val POINTS_STEP = 150L

    /** Points balance needed to unlock [level]. Level 1 is free; L2=300, L3=900, L4=1800, L5=3000... */
    fun pointsToUnlock(level: Int): Long = POINTS_STEP * (level - 1) * level

    fun levelForPoints(points: Long): Int {
        var level = 1
        while (points >= pointsToUnlock(level + 1)) level++
        return level
    }

    /** Word lengths played at [level]: L1 3-4 letters, L2 4-5, ... L5 7-8, L6+ 8 and longer. */
    fun wordLengths(level: Int): IntRange {
        val min = (level + 2).coerceIn(3, 8)
        return if (level >= 6) min..Int.MAX_VALUE else min..(min + 1)
    }
}
