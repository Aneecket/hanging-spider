package com.hangingspider.game.data.model

data class LeaderboardEntry(
    val uid: String = "",
    val name: String = "",
    val coins: Long = 0,
    val isBot: Boolean = false
)
