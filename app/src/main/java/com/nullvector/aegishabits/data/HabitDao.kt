package com.nullvector.aegishabits.data

import androidx.room.*
import com.nullvector.aegishabits.model.Habit
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // --- Habit Operations ---

    // ✅ Added this to fetch ALL habits (active + archived) for ViewModel filtering
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("SELECT * FROM habits WHERE isArchived = 0")
    fun getActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE isArchived = 1")
    fun getArchivedHabits(): Flow<List<Habit>>
}