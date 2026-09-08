package com.hangingspider.game

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class HangingSpiderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        MobileAds.initialize(this) {}
        createAdminChannel()
    }

    private fun createAdminChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val id = getString(R.string.default_notification_channel_id)
        val channel = NotificationChannel(id, "Admin Whispers", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Messages and rewards from the admins."
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }
}
