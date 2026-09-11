package com.hangingspider.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hangingspider.game.data.model.UserProfile
import com.hangingspider.game.data.repo.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Coin economy state and rules.
 *
 * Idle accrual runs client-side while the app is in the foreground. The client
 * batches accrued coins and pushes to Firebase every [SYNC_INTERVAL_MS]. Server-side
 * rules should cap /users/{uid}/coins writes to prevent trivial tampering.
 */
class CoinViewModel(
    private val uid: String,
    private val repo: UserRepository = UserRepository()
) : ViewModel() {

    companion object {
        const val COINS_PER_TICK = 1L
        const val TICK_MS = 10_000L // 1 coin per 10 seconds
        const val SYNC_INTERVAL_MS = 30_000L
        const val DAILY_REWARD = 500L
        const val WIN_REWARD = 200L
        const val WATCH_AD_DOUBLER_MS = 10L * 60L * 1000L // 10 min
        const val GAMES_PER_INTERSTITIAL = 3
        const val PLAY_AGAIN_STREAK_FOR_AD = 4
    }

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _events = MutableStateFlow<CoinEvent?>(null)
    val events: StateFlow<CoinEvent?> = _events

    private var accrualJob: Job? = null
    private var pendingAccrual = 0L
    private var gamesSinceInterstitial = 0
    private var playAgainStreak = 0

    /**
     * Called from the game screen when a round finishes (win or loss). Returns true
     * every [GAMES_PER_INTERSTITIAL] calls so the caller shows an interstitial ad
     * and skips it otherwise. Counter lives only in memory — resetting on process
     * death is intentional so a churn of relaunches doesn't stack ads.
     */
    fun onGameEndedShouldShowAd(): Boolean {
        gamesSinceInterstitial++
        return if (gamesSinceInterstitial >= GAMES_PER_INTERSTITIAL) {
            gamesSinceInterstitial = 0
            true
        } else false
    }

    /**
     * Tracks the "Play again" streak inside a single game screen visit. Returns
     * true every [PLAY_AGAIN_STREAK_FOR_AD] taps so the caller shows an
     * interstitial before the next round; false otherwise. Reset the streak
     * with [resetPlayAgainStreak] on exit or Return home so the count starts
     * fresh next time the player enters the game screen.
     */
    fun onPlayAgainShouldShowAd(): Boolean {
        playAgainStreak++
        return if (playAgainStreak >= PLAY_AGAIN_STREAK_FOR_AD) {
            playAgainStreak = 0
            true
        } else false
    }

    fun resetPlayAgainStreak() { playAgainStreak = 0 }

    init {
        viewModelScope.launch {
            repo.observeProfile(uid).collectLatest { _profile.value = it }
        }
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
            val ok = repo.claimDaily(uid, DAILY_REWARD)
            _events.value = if (ok) CoinEvent.DailyClaimed(DAILY_REWARD) else CoinEvent.DailyOnCooldown
        }
    }

    fun activateDoubler() {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + WATCH_AD_DOUBLER_MS
            repo.setDoublerUntil(uid, until)
            _events.value = CoinEvent.DoublerActivated(until)
        }
    }

    /** Debits [amount] coins if the current balance covers it. Returns true on success. */
    suspend fun spendCoins(amount: Long): Boolean {
        val current = _profile.value?.coins ?: 0L
        if (current < amount) return false
        runCatching { repo.addCoins(uid, -amount) }.onFailure { return false }
        return true
    }

    fun onGameFinished(won: Boolean) {
        viewModelScope.launch {
            val base = if (won) WIN_REWARD else 0L
            val mult = if (isDoublerActive()) 2 else 1
            repo.recordGameResult(uid, won, base * mult)
            _events.value = CoinEvent.GameResult(won, base * mult)
        }
    }

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
    data class GameResult(val won: Boolean, val awarded: Long) : CoinEvent
}
