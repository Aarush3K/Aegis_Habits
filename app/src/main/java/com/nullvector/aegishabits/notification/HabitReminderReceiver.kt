package com.nullvector.aegishabits.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra("EXTRA_HABIT_ID", -1L)
        val habitTitle = intent.getStringExtra("EXTRA_HABIT_TITLE") ?: "Your Habit"

        if (habitId != -1L) {
            val notificationHelper = NotificationHelper(context)
            notificationHelper.showHabitNotification(habitId, habitTitle)
        }
    }
}