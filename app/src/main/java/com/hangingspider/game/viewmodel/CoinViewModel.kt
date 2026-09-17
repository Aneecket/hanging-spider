package com.hangingspider.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hangingspider.game.data.model.DailyRecord
import com.hangingspider.game.data.model.UserProfile
import com.hangingspider.game.data.repo.RoundRecord
import com.hangingspider.game.data.repo.RoundReport
import com.hangingspider.game.data.repo.UserRepository
import com.hangingspider.game.game.AppDay
import com.hangingspider.game.game.GameType
import com.hangingspider.game.game.Levels
import com.hangingspider.game.game.StreakState
import com.hangingspider.game.game.Streaks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Points, levels, streaks and daily state. Points are stored under the legacy `coins` field.
 *
 * Idle accrual runs client-side while the app is in the foreground. The client
 * batches accrued points and pushes to Firebase every [SYNC_INTERVAL_MS].
 */
class CoinViewModel(
    private val uid: String,
    private val repo: UserRepository = UserRepository()
) : ViewModel() {

    companion object {
        const val COINS_PER_TICK = 1L
        const val TICK_MS = 10_000L // 1 point per 10 seconds
        const val SYNC_INTERVAL_MS = 30_000L
        const val WIN_REWARD = 50L
        const val HINT_COST = 10L
        const val WATCH_AD_DOUBLER_MS = 10L * 60L * 1000L // 10 min
    }

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _level = MutableStateFlow(Levels.STARTING)
    val level: StateFlow<Int> = _level

    private val _today = MutableStateFlow(AppDay.today())
    val today: StateFlow<Long> = _today

    private val _daily = MutableStateFlow<Map<String, DailyRecord>>(emptyMap())
    /** Today's finished daily puzzles, keyed by [GameType.name]. */
    val daily: StateFlow<Map<String, DailyRecord>> = _daily

    private val _events = MutableStateFlow<CoinEvent?>(null)
    val events: StateFlow<CoinEvent?> = _events

    private var accrualJob: Job? = null
    private var pendingAccrual = 0L

    init {
        viewModelScope.launch {
            repo.observeProfile(uid).collectLatest { p ->
                _profile.value = p
                if (p != null) _level.value = maxOf(_level.value, p.level.coerceIn(Levels.STARTING, Levels.MAX))
            }
        }
        viewModelScope.launch {
            _today.collectLatest { day -> repo.observeDaily(uid, day).collect { _daily.value = it } }
        }
    }

    /** Called when the app returns to the foreground so a new day's puzzles appear. */
    fun refreshDay() {
        _today.value = AppDay.today()
    }

    fun streak(): StreakState {
        val p = _profile.value
        return StreakState(p?.streakCount ?: 0, p?.streakLastDay ?: -10)
    }

    fun startIdleAccrual() {
        if (accrualJob?.isActive == true) return
        accrualJob = viewModelScope.launch {
            var sinceSync = 0L
            while (true) {
                delay(TICK_MS)
                val multiplier = if (isDoublerActive()) 2 else 1
                pendingAccrual += COINS_PER_TICK * multiplier
                sinceSync += TICK_MS
                if (sinceSync >= SYNC_INTERVAL_MS && pendingAccrual > 0) {
                    val toFlush = pendingAccrual
                    pendingAccrual = 0
                    sinceSync = 0
                    runCatching { repo.addCoins(uid, toFlush) }
                }
            }
        }
    }

    fun stopIdleAccrual() {
        accrualJob?.cancel()
        accrualJob = null
        if (pendingAccrual > 0) {
            val toFlush = pendingAccrual
            pendingAccrual = 0
            viewModelScope.launch { runCatching { repo.addCoins(uid, toFlush) } }
        }
    }

    fun claimDaily() {
        viewModelScope.launch {
            val amount = runCatching { repo.claimDaily(uid) }.getOrNull()
            _events.value = if (amount != null) CoinEvent.DailyClaimed(amount) else CoinEvent.DailyOnCooldown
        }
    }

    /** Rewarded-ad bonuses: doubling a win or the daily reward. */
    fun addBonus(amount: Long) {
        viewModelScope.launch {
            runCatching { repo.addCoins(uid, amount, countsForWeek = true) }
                .onSuccess { _events.value = CoinEvent.BonusAdded(amount) }
        }
    }

    fun repairStreak() {
        viewModelScope.launch {
            if (runCatching { repo.repairStreak(uid) }.getOrDefault(false)) {
                _events.value = CoinEvent.StreakSaved(Streaks.current(streak(), AppDay.today()))
            }
        }
    }

    fun activateDoubler() {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + WATCH_AD_DOUBLER_MS
            repo.setDoublerUntil(uid, until)
            _events.value = CoinEvent.DoublerActivated(until)
        }
    }

    /** Debits [amount] points if the current balance covers it. Returns true on success. */
    suspend fun spendCoins(amount: Long): Boolean {
        val current = _profile.value?.coins ?: 0L
        if (current < amount) return false
        runCatching { repo.addCoins(uid, -amount) }.onFailure { return false }
        return true
    }

    /** Saves a finished round; returns what changed (new level, achievements) for the results panel. */
    suspend fun finishRound(level: Int, type: GameType, won: Boolean, stars: Int, daily: Boolean): RoundReport? {
        val awarded = if (won) WIN_REWARD * (if (isDoublerActive()) 2 else 1) else 0L
        val report = runCatching {
            repo.finishRound(uid, RoundRecord(level, type, won, stars, daily, awarded))
        }.getOrNull()
        report?.unlockedLevel?.let { _level.value = maxOf(_level.value, it) }
        return report
    }

    suspend fun saveDaily(type: GameType, record: DailyRecord) {
        runCatching { repo.saveDaily(uid, _today.value, type, record) }
    }

    fun winReward(): Long = WIN_REWARD * (if (isDoublerActive()) 2 else 1)

    fun consumeEvent() { _events.value = null }

    fun isDoublerActive(): Boolean {
        val until = _profile.value?.doublerUntil ?: 0L
        return until > System.currentTimeMillis()
    }
}

sealed interface CoinEvent {
    data class DailyClaimed(val amount: Long) : CoinEvent
    data object DailyOnCooldown : CoinEvent
    data class DoublerActivated(val untilEpochMs: Long) : CoinEvent
    data class BonusAdded(val amount: Long) : CoinEvent
    data class StreakSaved(val days: Int) : CoinEvent
}
