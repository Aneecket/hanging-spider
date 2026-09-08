package com.hangingspider.game.data.model

data class AdminMessage(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val sender: String = "Admin",
    val sentAt: Long = 0
)
