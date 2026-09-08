package com.hangingspider.game.data.repo

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hangingspider.game.data.model.AdminMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Per-user inbox. Reads /messages/{uid} where uid is the current signed-in user.
 * Admin writes go through the admin console.
 */
class MessageRepository {
    private val root = FirebaseDatabase.getInstance().getReference("messages")

    fun observeInbox(uid: String): Flow<List<AdminMessage>> = callbackFlow {
        val ref = root.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val list = snap.children.mapNotNull { c ->
                    AdminMessage(
                        id = c.key.orEmpty(),
                        title = c.child("title").getValue(String::class.java).orEmpty(),
                        body = c.child("body").getValue(String::class.java).orEmpty(),
                        sender = c.child("sender").getValue(String::class.java) ?: "Admin",
                        sentAt = c.child("sentAt").getValue(Long::class.java) ?: 0L
                    )
                }.sortedByDescending { it.sentAt }
                trySend(list)
            }
            override fun onCancelled(err: DatabaseError) { trySend(emptyList()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
