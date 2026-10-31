package com.nullvector.aegishabits.repository

import com.nullvector.aegishabits.data.CompletionDao
import com.nullvector.aegishabits.data.HabitDao
import com.nullvector.aegishabits.model.Habit
import com.nullvector.aegishabits.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

open class HabitRepository(
    private val habitDao: HabitDao,
    private val completionDao: CompletionDao
) {

    open suspend fun getCompletionsListForHabit(habitId: Long): List<HabitCompletion> {
        return completionDao.getCompletionsListForHabit(habitId)
    }

    // --- Habit Operations ---

    open val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    open val activeHabits: Flow<List<Habit>> = habitDao.getAllActiveHabits()
    open val archivedHabits: Flow<List<Habit>> = habitDao.getArchivedHabits()
    open val allCompletions: Flow<List<HabitCompletion>> = completionDao.getAllCompletionsFlow()

    suspend fun getHabitById(id: Long): Habit? {
        return habitDao.getHabitById(id)
    }

    open suspend fun insertHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    open suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    open suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    // --- Completion Operations ---

    open suspend fun insertCompletion(completion: HabitCompletion) {
        completionDao.insertCompletion(completion)
    }

    open suspend fun deleteCompletion(completion: HabitCompletion) {
        completionDao.deleteCompletion(completion)
    }

    fun getCompletionsForDate(dateString: String): Flow<List<HabitCompletion>> {
        return completionDao.getCompletionsForDate(dateString)
    }

    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>> {
        return completionDao.getCompletionsForHabit(habitId)
    }

    open suspend fun getCompletionsBetweenDates(startDate: String, endDate: String): List<HabitCompletion> {
        return completionDao.getCompletionsBetweenDates(startDate, endDate)
    }

    // Toggle completion for a specific habit on a given ISO date (e.g. "YYYY-MM-DD")
    suspend fun toggleHabitCompletion(habitId: Long, dateString: String) {
        val isCompleted = completionDao.isHabitCompletedOnDate(habitId, dateString)
        if (isCompleted) {
            completionDao.deleteCompletionByDate(habitId, dateString)
        } else {
            completionDao.insertCompletion(
                HabitCompletion(habitId = habitId, dateString = dateString)
            )
        }
    }

    // Helper method to calculate current streak from a list of completed ISO dates
    // Helper method to calculate current streak from a list of completed ISO dates, factoring in shields
    // Helper method to calculate current streak with Aegis Shield support
    fun calculateCurrentStreak(completedDates: List<String>, availableShields: Int = 0): Int {
        if (completedDates.isEmpty()) return 0

        val completedSet = completedDates.mapNotNull { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }.toSet()
        if (completedSet.isEmpty()) return 0

        val today = java.time.LocalDate.now()
        val yesterday = today.minusDays(1)

        var checkDate = if (completedSet.contains(today)) {
            today
        } else if (completedSet.contains(yesterday)) {
            yesterday
        } else {
            val latest = completedSet.maxOrNull() ?: return 0
            if (java.time.temporal.ChronoUnit.DAYS.between(latest, today) > 2) {
                return 0
            }
            latest
        }

        var streak = 0

        while (true) {
            if (completedSet.contains(checkDate)) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                if (checkDate == today && !completedSet.contains(today)) {
                    checkDate = checkDate.minusDays(1)
                    continue
                }

                val prevDate = checkDate.minusDays(1)
                if (completedSet.contains(prevDate)) {
                    checkDate = prevDate
                } else {
                    break
                }
            }

            if (java.time.temporal.ChronoUnit.DAYS.between(checkDate, today) > 365) {
                break
            }
        }

        return streak
    }

    suspend fun getAllCompletions(): List<HabitCompletion> {
        return completionDao.getAllCompletions()
    }
}
