package com.hangingspider.game.data.repo

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.hangingspider.game.data.model.LeaderboardEntry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LeaderboardRepository {
    private val db = FirebaseDatabase.getInstance()
    private val root = db.getReference("leaderboard")

    // Client-side bot seeds. They stay visible until real players overtake them.
    // Labelled clearly as "AI Bot" in the UI to comply with consumer-protection rules.
    private val bots = listOf(
        LeaderboardEntry(uid = "bot_weaver",    name = "The Weaver",    coins = 12_450, isBot = true),
        LeaderboardEntry(uid = "bot_moonshade", name = "Moonshade",     coins =  9_820, isBot = true),
        LeaderboardEntry(uid = "bot_silk",      name = "Silk Runner",   coins =  7_100, isBot = true),
        LeaderboardEntry(uid = "bot_arachne",   name = "Arachne",       coins =  5_340, isBot = true),
        LeaderboardEntry(uid = "bot_night",     name = "Nightbite",     coins =  3_900, isBot = true),
        LeaderboardEntry(uid = "bot_fang",      name = "Fang & Cry",    coins =  2_700, isBot = true),
        LeaderboardEntry(uid = "bot_widow",     name = "Widow's Wick",  coins =  1_620, isBot = true),
        LeaderboardEntry(uid = "bot_whisper",   name = "Whisper",       coins =    980, isBot = true)
    )

    /** Mirror the user's current name+coins to /leaderboard/{uid}. Cheap denormalized view. */
    suspend fun publishSelf(uid: String, name: String, coins: Long) {
        root.child(uid).setValue(mapOf(
            "uid" to uid,
            "name" to name,
            "coins" to coins
        )).await()
    }

    fun observeTop(limit: Int = 20): Flow<List<LeaderboardEntry>> = callbackFlow {
        val query: Query = root.orderByChild("coins").limitToLast(limit)
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val real = snap.children.mapNotNull { c ->
                    LeaderboardEntry(
                        uid = c.child("uid").getValue(String::class.java) ?: c.key ?: "",
                        name = c.child("name").getValue(String::class.java)?.takeIf { it.isNotBlank() } ?: "Adventurer",
                        coins = c.child("coins").getValue(Long::class.java) ?: 0L,
                        isBot = false
                    )
                }
                val merged = (real + bots).sortedByDescending { it.coins }.take(limit)
                trySend(merged)
            }
            override fun onCancelled(err: DatabaseError) { trySend(bots) }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
}
