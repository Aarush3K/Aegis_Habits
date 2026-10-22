package com.nullvector.aegishabits.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun scheduleHabitReminder(
        context: Context,
        habitId: Int,
        habitName: String,
        delayMillis: Long
    ) {
        val inputData = Data.Builder()
            .putInt(HabitReminderWorker.KEY_HABIT_ID, habitId)
            .putString(HabitReminderWorker.KEY_HABIT_NAME, habitName)
            .build()

        val reminderWorkRequest = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInputData(inputData)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        val workName = "habit_reminder_$habitId"

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            reminderWorkRequest
        )
    }

    fun cancelHabitReminder(context: Context, habitId: Int) {
        val workName = "habit_reminder_$habitId"
        WorkManager.getInstance(context).cancelUniqueWork(workName)
    }
}