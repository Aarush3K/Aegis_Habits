package com.nullvector.aegishabits.repository

import com.nullvector.aegishabits.data.CompletionDao
import com.nullvector.aegishabits.data.HabitDao
import com.nullvector.aegishabits.model.Habit
import com.nullvector.aegishabits.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

class HabitRepository(
    private val habitDao: HabitDao,
    private val completionDao: CompletionDao
) {

    // --- Habit Operations ---

    val allHabits: Flow<List<Habit>> = habitDao.getAllActiveHabits()

    suspend fun getHabitById(id: Long): Habit? {
        return habitDao.getHabitById(id)
    }

    suspend fun insertHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    // --- Completion Operations ---

    suspend fun insertCompletion(completion: HabitCompletion) {
        completionDao.insertCompletion(completion)
    }

    suspend fun deleteCompletion(completion: HabitCompletion) {
        completionDao.deleteCompletion(completion)
    }

    fun getCompletionsForDate(dateString: String): Flow<List<HabitCompletion>> {
        return completionDao.getCompletionsForDate(dateString)
    }

    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>> {
        return completionDao.getCompletionsForHabit(habitId)
    }
}