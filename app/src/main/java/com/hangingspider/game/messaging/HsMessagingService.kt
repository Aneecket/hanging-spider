package com.hangingspider.game.messaging

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hangingspider.game.MainActivity
import com.hangingspider.game.R
import kotlin.random.Random

/**
 * Receives FCM messages and displays a system notification.
 *
 * Admin pushes go through Firebase Cloud Messaging → single-device targeting via FCM token,
 * or topic-broadcast to all users. Token is persisted per user under /users/{uid}/fcmToken so
 * the admin panel can look them up.
 */
class HsMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance()
            .getReference("users").child(uid).child("fcmToken").setValue(token)
    }

    override fun onMessageReceived(remote: RemoteMessage) {
        val title = remote.notification?.title ?: remote.data["title"] ?: "Admin whisper"
        val body = remote.notification?.body ?: remote.data["body"] ?: ""
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = getString(R.string.default_notification_channel_id)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this)
            .notify(Random.nextInt(), notif)
    }
}
