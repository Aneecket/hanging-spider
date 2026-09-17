package com.hangingspider.game.game

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Days and weeks on India time, so daily puzzles and weekly boards turn over together for everyone. */
object AppDay {
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private val zone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    fun today(now: Long = System.currentTimeMillis()): Long = Math.floorDiv(now + zone.getOffset(now), DAY_MS)

    /** Weeks start on Monday; epoch day 4 (5 Jan 1970) was a Monday. */
    fun weekKey(day: Long = today()): String = "w" + Math.floorDiv(day - 4, 7L)

    fun millisUntilNextWeek(now: Long = System.currentTimeMillis()): Long {
        val day = today(now)
        val nextMonday = Math.floorDiv(day - 4, 7L) * 7 + 4 + 7
        val startOfNextMonday = nextMonday * DAY_MS - zone.getOffset(now)
        return startOfNextMonday - now
    }

    fun label(day: Long): String = SimpleDateFormat("d MMM", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(day * DAY_MS))
}
