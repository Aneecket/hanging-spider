package com.hangingspider.game.data.model

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val coins: Long = 0,
    val createdAt: Long = 0,
    val lastDailyClaimAt: Long = 0,
    val lastDailyClaimDay: Long = -1,
    val lastSeenAt: Long = 0,
    val doublerUntil: Long = 0,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val level: Int = 1,
    val streakCount: Int = 0,
    val streakLastDay: Long = -10,
    /** Best stars per level, keyed "L1".."L13" (letter prefix keeps RTDB from turning it into a list). */
    val stars: Map<String, Int> = emptyMap(),
    val stats: Map<String, Long> = emptyMap(),
    /** Achievement id -> time it was earned. */
    val achievements: Map<String, Long> = emptyMap()
)

data class DailyRecord(val won: Boolean = false, val at: Long = 0, val share: String = "")
