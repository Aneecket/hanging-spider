package com.hangingspider.game.data.model

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val coins: Long = 0,
    val createdAt: Long = 0,
    val lastDailyClaimAt: Long = 0,
    val lastSeenAt: Long = 0,
    val doublerUntil: Long = 0,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0
)
