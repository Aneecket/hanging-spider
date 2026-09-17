package com.hangingspider.game.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hangingspider.game.MainActivity
import com.hangingspider.game.R
import com.hangingspider.game.game.AppDay
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Local copy of the few facts the daily reminder needs, so the worker can decide what to say
 * without the app running or network access.
 */
class EngagementPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("engagement", Context.MODE_PRIVATE)

    var reminderEnabled: Boolean
        get() = prefs.getBoolean("reminder_enabled", false)
        set(value) = prefs.edit().putBoolean("reminder_enabled", value).apply()

    var reminderAsked: Boolean
        get() = prefs.getBoolean("reminder_asked", false)
        set(value) = prefs.edit().putBoolean("reminder_asked", value).apply()

    fun mirror(streakCount: Int, streakLastDay: Long, dailyDoneToday: Boolean) {
        prefs.edit()
            .putInt("streak_count", streakCount)
            .putLong("streak_last_day", streakLastDay)
            .putLong("daily_done_day", if (dailyDoneToday) AppDay.today() else -1)
            .apply()
    }

    val streakCount: Int get() = prefs.getInt("streak_count", 0)
    val streakLastDay: Long get() = prefs.getLong("streak_last_day", -10)
    val dailyDoneDay: Long get() = prefs.getLong("daily_done_day", -1)
}

object Reminders {
    const val CHANNEL_ID = "daily_reminders"
    private const val WORK_NAME = "daily-reminder"
    private const val HOUR = 19

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "One reminder a day about daily puzzles and streaks."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        EngagementPrefs(context).reminderEnabled = enabled
        val work = WorkManager.getInstance(context)
        if (!enabled) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntilNextReminder(), TimeUnit.MILLISECONDS)
            .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private fun delayUntilNextReminder(): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis - now.timeInMillis
    }

    /** The reminder text for today, or null when there's nothing worth interrupting for. */
    fun message(streakCount: Int, streakLastDay: Long, dailyDoneDay: Long, today: Long): Pair<String, String>? = when {
        streakLastDay == today - 1 && streakCount >= 2 ->
            "Your $streakCount-day streak ends tonight" to "Play one round to keep it going."
        dailyDoneDay != today ->
            "Today's puzzles are ready" to "A new Five Letters and Word Groups puzzle is waiting."
        else -> null
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = EngagementPrefs(applicationContext)
        if (!prefs.reminderEnabled) return Result.success()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()
        val (title, body) = Reminders.message(prefs.streakCount, prefs.streakLastDay, prefs.dailyDoneDay, AppDay.today())
            ?: return Result.success()
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            applicationContext, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, Reminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(REMINDER_ID, notification)
        return Result.success()
    }

    private companion object { const val REMINDER_ID = 4201 }
}
