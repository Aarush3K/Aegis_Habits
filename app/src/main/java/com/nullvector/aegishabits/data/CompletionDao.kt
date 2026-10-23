package com.nullvector.aegishabits.data

import androidx.room.*
import com.nullvector.aegishabits.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {

    // --- Habit Completion Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion)

    @Delete
    suspend fun deleteCompletion(completion: HabitCompletion)

    @Query("SELECT * FROM habit_completions WHERE dateString = :dateString")
    fun getCompletionsForDate(dateString: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId")
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>>
}