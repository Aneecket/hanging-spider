package com.hangingspider.game.data.model

data class CashoutRequest(
    val id: String = "",
    val rewardId: String = "",
    val displayLabel: String = "",
    val coins: Long = 0,
    val amountInr: Int = 0,
    val provider: String = "",
    val email: String = "",
    val status: String = "pending", // pending | fulfilled | rejected
    val createdAt: Long = 0,
    val fulfilledAt: Long = 0,
    val giftCode: String = "",
    val adminNote: String = ""
)
