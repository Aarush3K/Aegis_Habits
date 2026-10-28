package com.nullvector.aegishabits.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nullvector.aegishabits.data.AppDatabase
import com.nullvector.aegishabits.model.HabitCompletion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra("EXTRA_HABIT_ID", -1L)
            .takeIf { it != -1L } ?: intent.getLongExtra("HABIT_ID", -1L)
        val habitTitle = intent.getStringExtra("EXTRA_HABIT_TITLE")
            ?: intent.getStringExtra("HABIT_TITLE")
            ?: "Your Habit"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            NotificationHelper.ACTION_MARK_DONE -> {
                if (habitId != -1L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val database = AppDatabase.getDatabase(context)
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                        // Insert completion using your CompletionDao
                        database.completionDao().insertCompletion(
                            HabitCompletion(
                                habitId = habitId,
                                dateString = todayStr
                            )
                        )
                    }
                }
                notificationManager.cancel(habitId.toInt())
            }

            NotificationHelper.ACTION_SNOOZE -> {
                if (habitId != -1L) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val snoozeTimeMillis = System.currentTimeMillis() + (15 * 60 * 1000)

                    val snoozeIntent = Intent(context, HabitReminderReceiver::class.java).apply {
                        putExtra("EXTRA_HABIT_ID", habitId)
                        putExtra("EXTRA_HABIT_TITLE", habitTitle)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        habitId.toInt(),
                        snoozeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            snoozeTimeMillis,
                            pendingIntent
                        )
                    } catch (e: SecurityException) {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, pendingIntent)
                    }
                }
                notificationManager.cancel(habitId.toInt())
            }

            else -> {
                if (habitId != -1L) {
                    val notificationHelper = NotificationHelper(context)
                    notificationHelper.showHabitNotification(habitId, habitTitle)
                }
            }
        }
    }
}