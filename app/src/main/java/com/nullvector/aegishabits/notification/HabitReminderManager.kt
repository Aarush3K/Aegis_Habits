package com.nullvector.aegishabits.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nullvector.aegishabits.model.FrequencyType
import java.util.Calendar

class HabitReminderManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(
        habitId: Long,
        habitTitle: String,
        reminderTime: String?,
        frequencyType: FrequencyType = FrequencyType.DAILY,
        customDaysOfWeek: List<Int> = emptyList(),
        createdAt: Long = System.currentTimeMillis()
    ) {
        if (reminderTime.isNullOrBlank()) return

        val parts = reminderTime.split(":")
        if (parts.size != 2) return

        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("EXTRA_HABIT_ID", habitId)
            putExtra("EXTRA_HABIT_TITLE", habitTitle)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Calculate correct trigger time based on Frequency type
        when (frequencyType) {
            FrequencyType.DAILY -> {
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            FrequencyType.WEEKLY -> {
                // Match the day of the week it was created on, or advance to next week if time passed
                val createdCalendar = Calendar.getInstance().apply { timeInMillis = createdAt }
                val targetDayOfWeek = createdCalendar.get(Calendar.DAY_OF_WEEK)

                calendar.set(Calendar.DAY_OF_WEEK, targetDayOfWeek)
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            FrequencyType.CUSTOM -> {
                if (customDaysOfWeek.isNotEmpty()) {
                    // Find the next closest allowed day of the week (Calendar days: Sun=1, Mon=2... Sat=7)
                    var found = false
                    for (i in 0..7) {
                        val testCalendar = Calendar.getInstance().apply {
                            timeInMillis = System.currentTimeMillis()
                            add(Calendar.DAY_OF_YEAR, i)
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val currentDayCode = testCalendar.get(Calendar.DAY_OF_WEEK)
                        if (customDaysOfWeek.contains(currentDayCode) && testCalendar.timeInMillis > System.currentTimeMillis()) {
                            calendar.timeInMillis = testCalendar.timeInMillis
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                } else {
                    if (calendar.timeInMillis <= System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(habitId: Long) {
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}