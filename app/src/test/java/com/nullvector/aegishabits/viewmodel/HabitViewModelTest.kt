package com.nullvector.aegishabits.viewmodel

import com.nullvector.aegishabits.model.Habit
import com.nullvector.aegishabits.model.HabitCompletion
import com.nullvector.aegishabits.repository.HabitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeHabitRepository
    private lateinit var viewModel: HabitViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeHabitRepository()
        viewModel = HabitViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addHabit_callsRepositoryInsert() = runTest {
        val habit = Habit(id = 1, title = "Exercise")

        viewModel.addHabit(habit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.insertedHabits.size)
        assertEquals("Exercise", fakeRepository.insertedHabits[0].title)
    }

    @Test
    fun updateHabit_callsRepositoryUpdate() = runTest {
        val habit = Habit(id = 1, title = "Updated Exercise")

        viewModel.updateHabit(habit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.updatedHabits.size)
        assertEquals("Updated Exercise", fakeRepository.updatedHabits[0].title)
    }

    @Test
    fun deleteHabit_callsRepositoryDelete() = runTest {
        val habit = Habit(id = 2, title = "Read")

        viewModel.deleteHabit(habit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.deletedHabits.size)
        assertEquals("Read", fakeRepository.deletedHabits[0].title)
    }

    @Test
    fun toggleCompletion_true_insertsCompletion() = runTest {
        val completion = HabitCompletion(id = 1, habitId = 10, dateString = "2026-10-23")

        viewModel.toggleCompletion(completion, isCompleted = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.insertedCompletions.size)
        assertEquals(completion, fakeRepository.insertedCompletions[0])
    }

    @Test
    fun toggleCompletion_false_deletesCompletion() = runTest {
        val completion = HabitCompletion(id = 1, habitId = 10, dateString = "2026-10-23")

        viewModel.toggleCompletion(completion, isCompleted = false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.deletedCompletions.size)
        assertEquals(completion, fakeRepository.deletedCompletions[0])
    }
}

/**
 * A custom fake repository passing mock DAOs and exposing test action trackers.
 */
class FakeHabitRepository : HabitRepository(
    habitDao = mock(),
    completionDao = mock()
) {
    private val _allHabits = MutableStateFlow<List<Habit>>(emptyList())
    val allHabitsFlow: Flow<List<Habit>> = _allHabits

    val insertedHabits = mutableListOf<Habit>()
    val updatedHabits = mutableListOf<Habit>()
    val deletedHabits = mutableListOf<Habit>()
    val insertedCompletions = mutableListOf<HabitCompletion>()
    val deletedCompletions = mutableListOf<HabitCompletion>()

    override suspend fun insertHabit(habit: Habit): Long {
        insertedHabits.add(habit)
        return habit.id
    }

    override suspend fun updateHabit(habit: Habit) {
        updatedHabits.add(habit)
    }

    override suspend fun deleteHabit(habit: Habit) {
        deletedHabits.add(habit)
    }

    override suspend fun insertCompletion(completion: HabitCompletion) {
        insertedCompletions.add(completion)
    }

    override suspend fun deleteCompletion(completion: HabitCompletion) {
        deletedCompletions.add(completion)
    }
}