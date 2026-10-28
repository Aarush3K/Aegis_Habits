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

    // Delete directly by habitId and date string
    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun deleteCompletionByDate(habitId: Long, dateString: String)

    // Check if a habit is completed on a specific date (for toggle logic)
    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habitId = :habitId AND dateString = :dateString)")
    suspend fun isHabitCompletedOnDate(habitId: Long, dateString: String): Boolean

    @Query("SELECT * FROM habit_completions WHERE dateString = :dateString")
    fun getCompletionsForDate(dateString: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE dateString = :dateString")
    suspend fun getCompletionsListForDate(dateString: String): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId")
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>>

    // Suspend version to fetch a list directly (used for streaks and JSON export)
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId")
    suspend fun getCompletionsListForHabit(habitId: Long): List<HabitCompletion>

    // Get all completions in the database (crucial for VaultManager backup)
    @Query("SELECT * FROM habit_completions")
    suspend fun getAllCompletions(): List<HabitCompletion>

    // Get all completions as a reactive Flow
    @Query("SELECT * FROM habit_completions")
    fun getAllCompletionsFlow(): Flow<List<HabitCompletion>>

    // --- Analytics & Correlation Queries ---

    // Get all completions within a specific date range (inclusive)
    @Query("SELECT * FROM habit_completions WHERE dateString BETWEEN :startDate AND :endDate")
    suspend fun getCompletionsBetweenDates(startDate: String, endDate: String): List<HabitCompletion>

    // Get distinct completion dates for a specific habit
    @Query("SELECT dateString FROM habit_completions WHERE habitId = :habitId")
    suspend fun getCompletionDatesForHabit(habitId: Long): List<String>

    // Get joint completions where both habitA and habitB were completed on the exact same date
    @Query("""
        SELECT c1.dateString 
        FROM habit_completions c1
        INNER JOIN habit_completions c2 ON c1.dateString = c2.dateString
        WHERE c1.habitId = :habitIdA AND c2.habitId = :habitIdB
    """)
    suspend fun getJointCompletionDates(habitIdA: Long, habitIdB: Long): List<String>
}
