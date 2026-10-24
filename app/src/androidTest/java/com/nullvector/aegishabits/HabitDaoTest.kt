package com.nullvector.aegishabits

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nullvector.aegishabits.data.AppDatabase
import com.nullvector.aegishabits.data.CompletionDao
import com.nullvector.aegishabits.data.HabitDao
import com.nullvector.aegishabits.model.Frequency
import com.nullvector.aegishabits.model.Habit
import com.nullvector.aegishabits.model.HabitCompletion
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import com.nullvector.aegishabits.model.FrequencyType

@RunWith(AndroidJUnit4::class)
class HabitDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var completionDao: CompletionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Use an in-memory database so information is wiped when process dies
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Allowed for testing
            .build()
        habitDao = db.habitDao()
        completionDao = db.completionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndReadHabit() = runBlocking {
        val habit = Habit(
            id = 1L,
            title = "Morning Run",
            description = "Run 5km every morning",
            frequency = Frequency(type = FrequencyType.DAILY)
        )
        habitDao.insertHabit(habit)

        val retrievedHabit = habitDao.getHabitById(1L)
        assertNotNull(retrievedHabit)
        assertEquals("Morning Run", retrievedHabit?.title)
    }

    @Test
    @Throws(Exception::class)
    fun toggleCompletionAndCheckExistence() = runBlocking {
        val habit = Habit(id = 1L, title = "Read Books")
        habitDao.insertHabit(habit)

        val dateString = "2026-10-23"

        // Insert completion
        completionDao.insertCompletion(
            HabitCompletion(habitId = 1L, dateString = dateString)
        )

        var isCompleted = completionDao.isHabitCompletedOnDate(1L, dateString)
        assertTrue(isCompleted)

        // Delete completion
        completionDao.deleteCompletionByDate(1L, dateString)
        isCompleted = completionDao.isHabitCompletedOnDate(1L, dateString)
        assertFalse(isCompleted)
    }

    @Test
    @Throws(Exception::class)
    fun testCascadeDeleteHabitRemovesCompletions() = runBlocking {
        val habit = Habit(id = 1L, title = "Meditation")
        habitDao.insertHabit(habit)

        completionDao.insertCompletion(
            HabitCompletion(habitId = 1L, dateString = "2026-10-23")
        )

        // Delete parent habit
        habitDao.deleteHabit(habit)

        // Check that foreign key CASCADE removed completion
        val completions = completionDao.getCompletionsListForHabit(1L)
        assertTrue(completions.isEmpty())
    }
}