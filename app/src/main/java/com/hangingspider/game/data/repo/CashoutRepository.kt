package com.hangingspider.game.data.repo

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hangingspider.game.data.model.CashoutRequest
import com.hangingspider.game.data.model.RewardCatalog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CashoutRepository {
    private val db = FirebaseDatabase.getInstance()
    private fun userCoinsRef(uid: String) = db.getReference("users").child(uid).child("coins")
    private fun requestsRoot(uid: String) = db.getReference("cashouts").child(uid)

    sealed class Result {
        data class Ok(val requestId: String) : Result()
        data class NotEnoughCoins(val have: Long, val need: Long) : Result()
        data class Failed(val message: String) : Result()
    }

    /**
     * Creates a cashout request and atomically debits the coins in a single
     * multi-path update. If Firebase rejects (rules validation or race), we
     * return an error result without any partial state.
     */
    suspend fun createRequest(uid: String, rewardId: String, email: String): Result {
        val tier = RewardCatalog.byId(rewardId) ?: return Result.Failed("Unknown reward")
        val currentCoins = userCoinsRef(uid).get().await().getValue(Long::class.java) ?: 0L
        if (currentCoins < tier.coins) return Result.NotEnoughCoins(currentCoins, tier.coins)

        val newRef = requestsRoot(uid).push()
        val requestId = newRef.key ?: return Result.Failed("Push key null")

        val payload = mapOf(
            "id" to requestId,
            "rewardId" to tier.id,
            "displayLabel" to tier.displayLabel,
            "coins" to tier.coins,
            "amountInr" to tier.amountInr,
            "provider" to tier.provider.name,
            "email" to email,
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )
        val updates = mapOf<String, Any>(
            "users/$uid/coins" to (currentCoins - tier.coins),
            "cashouts/$uid/$requestId" to payload,
            "leaderboard/$uid/coins" to (currentCoins - tier.coins)
        )
        return try {
            db.reference.updateChildren(updates).await()
            Result.Ok(requestId)
        } catch (t: Throwable) {
            Result.Failed(t.message ?: "Write failed")
        }
    }

    fun observeUserRequests(uid: String): Flow<List<CashoutRequest>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val list = snap.children.map { it.toRequest() }.sortedByDescending { it.createdAt }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                // Rules not yet published, or user offline — degrade to empty rather than crash.
                trySend(emptyList())
            }
        }
        requestsRoot(uid).addValueEventListener(listener)
        awaitClose { requestsRoot(uid).removeEventListener(listener) }
    }

    private fun DataSnapshot.toRequest() = CashoutRequest(
        id = child("id").getValue(String::class.java) ?: key.orEmpty(),
        rewardId = child("rewardId").getValue(String::class.java).orEmpty(),
        displayLabel = child("displayLabel").getValue(String::class.java).orEmpty(),
        coins = child("coins").getValue(Long::class.java) ?: 0L,
        amountInr = child("amountInr").getValue(Int::class.java) ?: 0,
        provider = child("provider").getValue(String::class.java).orEmpty(),
        email = child("email").getValue(String::class.java).orEmpty(),
        status = child("status").getValue(String::class.java) ?: "pending",
        createdAt = child("createdAt").getValue(Long::class.java) ?: 0L,
        fulfilledAt = child("fulfilledAt").getValue(Long::class.java) ?: 0L,
        giftCode = child("giftCode").getValue(String::class.java).orEmpty(),
        adminNote = child("adminNote").getValue(String::class.java).orEmpty()
    )
}
