package com.nullvector.aegishabits.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class HabitReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val habitName = inputData.getString(KEY_HABIT_NAME) ?: "Your Habit"
        val habitId = inputData.getInt(KEY_HABIT_ID, 0)

        showNotification(habitId, habitName)

        return Result.success()
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun showNotification(habitId: Int, habitName: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled habit reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build the local notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Time to complete your habit!")
            .setContentText("Don't forget to complete: $habitName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(habitId, notification)
    }

    companion object {
        const val CHANNEL_ID = "aegis_habit_reminders"
        const val KEY_HABIT_NAME = "key_habit_name"
        const val KEY_HABIT_ID = "key_habit_id"
    }
}