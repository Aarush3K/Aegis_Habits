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
    fun calculateCurrentStreak(completedDates: List<String>): Int {
        if (completedDates.isEmpty()) return 0

        val sortedDates = completedDates
            .mapNotNull { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
            .sortedDescending()

        if (sortedDates.isEmpty()) return 0

        val today = java.time.LocalDate.now()
        val yesterday = today.minusDays(1)

        // Check if the streak is active (completed today or yesterday)
        val latestDate = sortedDates.first()
        if (latestDate != today && latestDate != yesterday) {
            return 0
        }

        var streak = 0
        var expectedDate = latestDate

        for (date in sortedDates.distinct()) {
            if (date == expectedDate) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else if (date.isBefore(expectedDate)) {
                break
            }
        }

        return streak
    }

    // --- Vault Export Helpers ---

    suspend fun getAllCompletions(): List<HabitCompletion> {
        return completionDao.getAllCompletions()
    }

    suspend fun getCompletionsBetweenDates(startDate: String, endDate: String): List<HabitCompletion> {
        return completionDao.getCompletionsBetweenDates(startDate, endDate)
    }

    suspend fun getCompletionDatesForHabit(habitId: Long): List<String> {
        return completionDao.getCompletionDatesForHabit(habitId)
    }
}
