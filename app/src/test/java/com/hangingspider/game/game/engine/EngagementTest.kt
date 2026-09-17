package com.hangingspider.game.game.engine

import com.hangingspider.game.game.Achievements
import com.hangingspider.game.game.AppDay
import com.hangingspider.game.game.GameType
import com.hangingspider.game.game.Levels
import com.hangingspider.game.game.PlayerProgress
import com.hangingspider.game.game.Stars
import com.hangingspider.game.game.StreakState
import com.hangingspider.game.game.Streaks
import com.hangingspider.game.game.WordIndex
import org.junit.Assert.*
import org.junit.Test

class EngagementTest {
    @Test fun levelTargets() {
        assertEquals(0L, Levels.pointsToUnlock(1))
        assertEquals(0L, Levels.pointsToUnlock(3))
        assertEquals(300L, Levels.pointsToUnlock(4))
        assertEquals(800L, Levels.pointsToUnlock(5))
        assertEquals(1500L, Levels.pointsToUnlock(6))
        assertEquals(2500L, Levels.pointsToUnlock(7))
        assertEquals(4000L, Levels.pointsToUnlock(8))
        assertEquals(11500L, Levels.pointsToUnlock(13))
    }

    @Test fun streaksGrowBreakAndRepair() {
        val today = 20_000L
        assertEquals(StreakState(1, today), Streaks.afterPlay(StreakState(0, -10), today))
        assertEquals(StreakState(5, today), Streaks.afterPlay(StreakState(4, today - 1), today))
        assertEquals(StreakState(4, today), Streaks.afterPlay(StreakState(4, today), today))
        assertEquals(StreakState(1, today), Streaks.afterPlay(StreakState(9, today - 3), today))

        assertEquals(4, Streaks.current(StreakState(4, today - 1), today))
        assertEquals(0, Streaks.current(StreakState(4, today - 2), today))

        assertEquals(100L, Streaks.dailyReward(StreakState(0, -10), today))
        assertEquals(150L, Streaks.dailyReward(StreakState(1, today - 1), today))
        assertEquals(500L, Streaks.dailyReward(StreakState(30, today), today))

        assertTrue(Streaks.canRepair(StreakState(3, today - 2), today))
        assertFalse(Streaks.canRepair(StreakState(1, today - 2), today))
        assertFalse(Streaks.canRepair(StreakState(3, today - 3), today))
    }

    @Test fun dayAndWeekKeys() {
        // 2026-09-17 12:00 IST is 06:30 UTC.
        val noonIst = 1_789_626_600_000L
        val day = AppDay.today(noonIst)
        assertEquals(AppDay.today(noonIst + 11 * 3_600_000L), day)
        assertEquals(day + 1, AppDay.today(noonIst + 12 * 3_600_000L + 60_000))
        assertEquals("17 Sep", AppDay.label(day))
        // Monday and the following Sunday share a week; the next Monday starts a new one.
        val monday = day - 3
        assertEquals(AppDay.weekKey(monday), AppDay.weekKey(monday + 6))
        assertNotEquals(AppDay.weekKey(monday), AppDay.weekKey(monday + 7))
        assertTrue(AppDay.millisUntilNextWeek(noonIst) in 1..7L * 86_400_000)
    }

    @Test fun starCaps() {
        assertEquals(3, Stars.cap(3, hintsUsed = 0, usedSecondChance = false))
        assertEquals(2, Stars.cap(3, hintsUsed = 1, usedSecondChance = false))
        assertEquals(1, Stars.cap(3, hintsUsed = 0, usedSecondChance = true))
        assertEquals(1, Stars.cap(0, hintsUsed = 0, usedSecondChance = false))
    }

    @Test fun achievementsUnlockFromProgress() {
        val none = PlayerProgress(emptyMap(), emptyMap(), 3, 0)
        assertTrue(Achievements.earned(none).isEmpty())
        val stats = GameType.entries.associate { Achievements.Stat.wins(it) to 1L } +
            (Achievements.Stat.WINS to 13L) + (Achievements.Stat.PERFECT to 1L)
        val ids = Achievements.earned(PlayerProgress(stats, mapOf("L1" to 3), 8, 3)).map { it.id }.toSet()
        assertEquals(setOf("first_win", "explorer", "perfect", "streak_3", "halfway"), ids)
        assertEquals(Achievements.all.size, Achievements.all.map { it.id }.toSet().size)
    }

    @Test fun tileSuggestionUsesOnlyRackLetters() {
        val words = WordIndex(listOf("CAT", "ACT", "TAXI", "QUIZ", "TACT"))
        assertEquals("TAXI", LetterTiles.suggestWord("TAXICEE".toList(), words))
        assertNull(LetterTiles.suggestWord("EEEEEEE".toList(), words))
        assertNotEquals("TACT", LetterTiles.suggestWord("TACEEEE".toList(), words))
    }

    @Test fun reminderMessages() {
        val today = 20_000L
        assertEquals("Your 4-day streak ends tonight", com.hangingspider.game.reminders.Reminders.message(4, today - 1, -1, today)?.first)
        assertEquals("Today's puzzles are ready", com.hangingspider.game.reminders.Reminders.message(1, today, -1, today)?.first)
        assertNull(com.hangingspider.game.reminders.Reminders.message(3, today, today, today))
    }
}
