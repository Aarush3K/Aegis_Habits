package com.nullvector.aegishabits.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun scheduleHabitReminder(context: Context, delayMillis: Long, habitName: String) {
        // Will be connected to HabitReminderWorker on Days 2 & 3
    }
}