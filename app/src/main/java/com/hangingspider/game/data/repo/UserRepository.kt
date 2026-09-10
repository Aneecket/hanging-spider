package com.hangingspider.game.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.hangingspider.game.data.model.UserProfile
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
            "messages/$uid" to null
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
            val profile = UserProfile(
                uid = user.uid,
                displayName = displayName,
                email = user.email.orEmpty(),
                photoUrl = user.photoUrl?.toString().orEmpty(),
                coins = 0,
                createdAt = now,
                lastDailyClaimAt = 0,
                lastSeenAt = now
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
     * covering BOTH /users/{uid}/... AND /leaderboard/{uid}/... — partial writes
     * cannot drift the two paths out of sync any more.
     */
    private suspend fun writeCoinDelta(
        uid: String,
        newCoins: Long,
        extra: Map<String, Any> = emptyMap()
    ) {
        val snap = userRef(uid).get().await()
        val name = displayName(snap)
        val updates = HashMap<String, Any>().apply {
            put("users/$uid/coins", newCoins)
            put("leaderboard/$uid/uid", uid)
            put("leaderboard/$uid/name", name)
            put("leaderboard/$uid/coins", newCoins)
            for ((k, v) in extra) put("users/$uid/$k", v)
        }
        db.reference.updateChildren(updates).await()
    }

    private fun displayName(snap: DataSnapshot): String =
        snap.child("displayName").getValue(String::class.java)?.takeIf { it.isNotBlank() }
            ?: snap.child("email").getValue(String::class.java)?.substringBefore("@")
            ?: "Adventurer"

    suspend fun addCoins(uid: String, delta: Long): Long {
        val current = userRef(uid).child("coins").get().await().getValue(Long::class.java) ?: 0L
        val next = current + delta
        writeCoinDelta(uid, next)
        return next
    }

    suspend fun setDoublerUntil(uid: String, ts: Long) {
        userRef(uid).child("doublerUntil").setValue(ts).await()
    }

    suspend fun claimDaily(uid: String, amount: Long): Boolean {
        val snap = userRef(uid).get().await()
        val profile = snap.getValue<UserProfile>() ?: return false
        val now = System.currentTimeMillis()
        if (now - profile.lastDailyClaimAt < 20 * 60 * 60 * 1000L) return false
        writeCoinDelta(uid, profile.coins + amount, mapOf("lastDailyClaimAt" to now))
        return true
    }

    suspend fun recordGameResult(uid: String, won: Boolean, coinsAwarded: Long) {
        val snap = userRef(uid).get().await()
        val p = snap.getValue<UserProfile>() ?: return
        writeCoinDelta(
            uid = uid,
            newCoins = p.coins + coinsAwarded,
            extra = mapOf(
                "gamesPlayed" to (p.gamesPlayed + 1),
                "gamesWon" to (p.gamesWon + if (won) 1 else 0)
            )
        )
    }
}
