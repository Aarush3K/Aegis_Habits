package com.nullvector.aegishabits.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nullvector.aegishabits.MainActivity
import com.nullvector.aegishabits.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "habit_reminders_channel"
        const val CHANNEL_NAME = "Habit Reminders"

        const val ACTION_MARK_DONE = "ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "ACTION_SNOOZE"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders for your habits"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showHabitNotification(habitId: Long, habitTitle: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 1. "Mark as Done" Action Intent
        val doneIntent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra("HABIT_ID", habitId)
            putExtra("HABIT_TITLE", habitTitle)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId * 3).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. "Snooze" Action Intent
        val snoozeIntent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("HABIT_ID", habitId)
            putExtra("HABIT_TITLE", habitTitle)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId * 3 + 1).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time for your habit!")
            .setContentText("Don't forget to complete: $habitTitle")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Mark as Done", donePendingIntent)
            .addAction(0, "Snooze 15m", snoozePendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(habitId.toInt(), builder.build())
    }
}