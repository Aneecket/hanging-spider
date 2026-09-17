package com.hangingspider.game.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.hangingspider.game.data.model.DailyRecord
import com.hangingspider.game.data.model.UserProfile
import com.hangingspider.game.game.Achievement
import com.hangingspider.game.game.Achievements
import com.hangingspider.game.game.AppDay
import com.hangingspider.game.game.GameType
import com.hangingspider.game.game.Levels
import com.hangingspider.game.game.PlayerProgress
import com.hangingspider.game.game.Stars
import com.hangingspider.game.game.StreakState
import com.hangingspider.game.game.Streaks
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private fun userRef(uid: String) = db.getReference("users").child(uid)

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signInAnonymously(): FirebaseUser {
        val result = auth.signInAnonymously().await()
        val user = result.user ?: error("Firebase returned null user")
        ensureProfile(user)
        return user
    }

    /**
     * Links the current anonymous user to a Google account (first sync on this device),
     * or signs in as the Google-linked user if that Google account is already linked to
     * another Firebase UID (typical when the same user syncs on a second device). In the
     * collision case, any coins accrued on the abandoned anonymous session are added to
     * the signed-in account so the player never sees their balance drop after syncing.
     */
    suspend fun linkOrSignInWithGoogle(idToken: String): FirebaseUser {
        val cred = GoogleAuthProvider.getCredential(idToken, null)
        val prior = auth.currentUser
        val priorUid = prior?.uid
        val priorWasAnonymous = prior?.isAnonymous == true
        val priorCoins: Long = if (priorWasAnonymous && priorUid != null) {
            userRef(priorUid).child("coins").get().await().getValue(Long::class.java) ?: 0L
        } else 0L

        val user = try {
            if (priorWasAnonymous && prior != null) {
                prior.linkWithCredential(cred).await().user
            } else {
                auth.signInWithCredential(cred).await().user
            }
        } catch (_: FirebaseAuthUserCollisionException) {
            auth.signInWithCredential(cred).await().user
        } ?: error("Firebase returned null user")

        if (priorUid != null && priorUid != user.uid) {
            if (priorCoins > 0) {
                val existing = userRef(user.uid).child("coins").get().await()
                    .getValue(Long::class.java) ?: 0L
                userRef(user.uid).child("coins").setValue(existing + priorCoins).await()
            }
            runCatching {
                val purge = mapOf<String, Any?>(
                    "users/$priorUid" to null,
                    "leaderboard/$priorUid" to null
                )
                db.reference.updateChildren(purge).await()
            }
        }

        ensureProfile(user)
        return user
    }

    fun signOut() { auth.signOut() }

    sealed class DeleteResult {
        data object Ok : DeleteResult()
        data object NeedsReauth : DeleteResult()
        data class Failed(val message: String) : DeleteResult()
    }

    /**
     * Deletes every trace of the current user: their profile, leaderboard row,
     * cashout history, messages, then the Firebase Auth account itself.
     * If FirebaseAuth refuses because the sign-in is too old, returns
     * NeedsReauth — caller should send the user through Google Sign-In again
     * and retry. RTDB data is still gone at that point (deliberate; the
     * account is unusable and holding data would be worse than losing it).
     */
    suspend fun deleteAccount(): DeleteResult {
        val user = auth.currentUser ?: return DeleteResult.Failed("Not signed in")
        val uid = user.uid
        val purge = mapOf<String, Any?>(
            "users/$uid" to null,
            "leaderboard/$uid" to null,
            "cashouts/$uid" to null,
            "messages/$uid" to null,
            "daily/$uid" to null,
            "weekly/${AppDay.weekKey()}/$uid" to null
        )
        return try {
            db.reference.updateChildren(purge).await()
            user.delete().await()
            DeleteResult.Ok
        } catch (t: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
            DeleteResult.NeedsReauth
        } catch (t: Throwable) {
            DeleteResult.Failed(t.message ?: "Delete failed")
        }
    }

    private suspend fun ensureProfile(user: FirebaseUser) {
        val ref = userRef(user.uid)
        val snap = ref.get().await()
        if (!snap.exists()) {
            val now = System.currentTimeMillis()
            val displayName = user.displayName.orEmpty().ifBlank {
                "Weaver_${user.uid.take(4).uppercase()}"
            }
            // `level` is left out; it is written separately once a level above 1 is unlocked.
            val profile = mapOf(
                "uid" to user.uid,
                "displayName" to displayName,
                "email" to user.email.orEmpty(),
                "photoUrl" to (user.photoUrl?.toString().orEmpty()),
                "coins" to 0L,
                "createdAt" to now,
                "lastDailyClaimAt" to 0L,
                "lastSeenAt" to now,
                "doublerUntil" to 0L,
                "gamesPlayed" to 0,
                "gamesWon" to 0
            )
            ref.setValue(profile).await()
        } else {
            ref.child("lastSeenAt").setValue(System.currentTimeMillis())
        }
    }

    fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val ref = userRef(uid)
        val vel = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snap: com.google.firebase.database.DataSnapshot) {
                trySend(snap.getValue<UserProfile>())
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) { trySend(null) }
        }
        ref.addValueEventListener(vel)
        awaitClose { ref.removeEventListener(vel) }
    }

    // ---------- coin mutations: single atomic multi-path write ----------

    /**
     * Every coin mutation goes through a single [updateChildren] at the root
     * covering /users/{uid}, /leaderboard/{uid} and, when points were earned,
     * this week's /weekly board, so the paths can't drift out of sync.
     */
    private suspend fun writeCoinDelta(
        uid: String,
        newCoins: Long,
        extra: Map<String, Any> = emptyMap(),
        weeklyGain: Long = 0
    ) {
        val snap = userRef(uid).get().await()
        val name = displayName(snap)
        val updates = HashMap<String, Any>().apply {
            put("users/$uid/coins", newCoins)
            put("leaderboard/$uid/uid", uid)
            put("leaderboard/$uid/name", name)
            put("leaderboard/$uid/coins", newCoins)
            for ((k, v) in extra) put("users/$uid/$k", v)
            if (weeklyGain > 0) {
                val week = AppDay.weekKey()
                val current = db.getReference("weekly/$week/$uid/points").get().await().getValue(Long::class.java) ?: 0L
                put("weekly/$week/$uid/uid", uid)
                put("weekly/$week/$uid/name", name)
                put("weekly/$week/$uid/points", current + weeklyGain)
            }
        }
        db.reference.updateChildren(updates).await()
    }

    private fun displayName(snap: DataSnapshot): String =
        snap.child("displayName").getValue(String::class.java)?.takeIf { it.isNotBlank() }?.take(40)
            ?: snap.child("email").getValue(String::class.java)?.substringBefore("@")?.take(40)
            ?: "Adventurer"

    /** [countsForWeek] adds positive earnings to the weekly leaderboard (bonuses, not idle accrual). */
    suspend fun addCoins(uid: String, delta: Long, countsForWeek: Boolean = false): Long {
        val current = userRef(uid).child("coins").get().await().getValue(Long::class.java) ?: 0L
        val next = current + delta
        writeCoinDelta(uid, next, weeklyGain = if (countsForWeek && delta > 0) delta else 0)
        return next
    }

    suspend fun setDoublerUntil(uid: String, ts: Long) {
        userRef(uid).child("doublerUntil").setValue(ts).await()
    }

    /** Claims today's streak-based daily reward. Returns the amount, or null if already claimed today. */
    suspend fun claimDaily(uid: String): Long? {
        val p = userRef(uid).get().await().getValue<UserProfile>() ?: return null
        val today = AppDay.today()
        if (p.lastDailyClaimDay == today) return null
        val amount = Streaks.dailyReward(StreakState(p.streakCount, p.streakLastDay), today)
        writeCoinDelta(
            uid,
            p.coins + amount,
            mapOf("lastDailyClaimDay" to today, "lastDailyClaimAt" to System.currentTimeMillis()),
            weeklyGain = amount
        )
        return amount
    }

    /** Restores a streak that broke yesterday. Returns true if it was repaired. */
    suspend fun repairStreak(uid: String): Boolean {
        val p = userRef(uid).get().await().getValue<UserProfile>() ?: return false
        val today = AppDay.today()
        if (!Streaks.canRepair(StreakState(p.streakCount, p.streakLastDay), today)) return false
        userRef(uid).child("streakLastDay").setValue(today - 1).await()
        return true
    }

    /**
     * Saves a finished round in one write: points, win stats, best stars, streak,
     * the next level if its points target is met, and any newly earned achievements.
     */
    suspend fun finishRound(uid: String, round: RoundRecord): RoundReport? {
        val p = userRef(uid).get().await().getValue<UserProfile>() ?: return null
        val today = AppDay.today()
        val extra = HashMap<String, Any>()
        extra["gamesPlayed"] = p.gamesPlayed + 1
        extra["gamesWon"] = p.gamesWon + if (round.won) 1 else 0

        val stats = p.stats.toMutableMap()
        fun bump(key: String) {
            stats[key] = (stats[key] ?: 0L) + 1
            extra["stats/$key"] = stats.getValue(key)
        }
        if (round.won) {
            bump(Achievements.Stat.WINS)
            bump(Achievements.Stat.wins(round.type))
            if (round.stars == 3) {
                bump(Achievements.Stat.PERFECT)
                bump(Achievements.Stat.perfect(round.type))
            }
        }
        if (round.daily) bump(Achievements.Stat.DAILY)

        val stars = p.stars.toMutableMap()
        val starKey = Stars.key(round.level)
        if (round.won && !round.daily && round.stars > (stars[starKey] ?: 0)) {
            stars[starKey] = round.stars
            extra["stars/$starKey"] = round.stars
        }

        val streak = Streaks.afterPlay(StreakState(p.streakCount, p.streakLastDay), today)
        extra["streakCount"] = streak.count
        extra["streakLastDay"] = streak.lastDay

        val level = maxOf(p.level, Levels.STARTING)
        val streakDays = Streaks.current(streak, today)
        val earned = Achievements.earned(PlayerProgress(stats, stars, level, streakDays)).filter { it.id !in p.achievements }
        val coins = p.coins + round.awarded + earned.size * Achievements.REWARD
        val unlocked = (level + 1).takeIf { it <= Levels.MAX && coins >= Levels.pointsToUnlock(it) }
        val newLevel = unlocked ?: level
        if (newLevel != p.level) extra["level"] = newLevel

        // A level unlocked this round can itself complete a level achievement.
        val allEarned = earned + Achievements.earned(PlayerProgress(stats, stars, newLevel, streakDays))
            .filter { a -> a.id !in p.achievements && earned.none { it.id == a.id } }
        val bonus = allEarned.size * Achievements.REWARD
        val total = p.coins + round.awarded + bonus
        val now = System.currentTimeMillis()
        allEarned.forEach { extra["achievements/${it.id}"] = now }

        writeCoinDelta(uid, total, extra, weeklyGain = round.awarded + bonus)
        return RoundReport(total, unlocked, allEarned)
    }

    private fun dailyRef(uid: String, day: Long) = db.getReference("daily").child(uid).child("d$day")

    suspend fun saveDaily(uid: String, day: Long, game: GameType, record: DailyRecord) {
        dailyRef(uid, day).child(game.name).setValue(record).await()
    }

    fun observeDaily(uid: String, day: Long): Flow<Map<String, DailyRecord>> = callbackFlow {
        val ref = dailyRef(uid, day)
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.children.mapNotNull { c -> c.getValue(DailyRecord::class.java)?.let { c.key.orEmpty() to it } }.toMap())
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) { trySend(emptyMap()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}

data class RoundRecord(
    val level: Int,
    val type: GameType,
    val won: Boolean,
    val stars: Int,
    val daily: Boolean,
    val awarded: Long
)

data class RoundReport(val points: Long, val unlockedLevel: Int?, val achievements: List<Achievement>)
