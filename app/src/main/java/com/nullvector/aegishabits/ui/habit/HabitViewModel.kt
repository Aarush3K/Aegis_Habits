package com.nullvector.aegishabits.ui.habit

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nullvector.aegishabits.data.ChronosDatabase
import com.nullvector.aegishabits.model.Frequency
import com.nullvector.aegishabits.model.Habit
import com.nullvector.aegishabits.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** One row on the dashboard: a habit, whether it is ticked off today, and its current streak. */
data class HabitUiItem(
    val habit: Habit,
    val isCompleted: Boolean,
    val streak: Int = 0, // consecutive days (weeks for WEEKLY habits)
)

/** Everything the screen needs to draw itself. */
data class HabitUiState(
    val isLoading: Boolean = true,
    val habits: List<HabitUiItem> = emptyList(),
    val message: String? = null, // one-shot error/info text for a Snackbar
) {
    val completedCount: Int get() = habits.count { it.isCompleted }
    val totalCount: Int get() = habits.size
}

class HabitViewModel(
    private val repository: HabitRepository,
) : ViewModel() {

    // Date key used for completion records, e.g. "2026-10-23"
    private val dateKey: String = todayKey()

    private val message = MutableStateFlow<String?>(null)

    /** Habits that are due today, each flagged done / not done with its streak. Updates live from Room. */
    fun getHabitsForToday(): Flow<List<HabitUiItem>> =
        combine(
            repository.getAllHabits(),
            repository.getAllCompletions(),
        ) { habits, completions ->
            // habitId -> set of "yyyy-MM-dd" dates it was completed on
            val datesByHabit: Map<Long, Set<String>> = completions
                .groupBy({ it.habitId }, { it.date })
                .mapValues { it.value.toSet() }

            habits
                .filter { it.isDueToday() }
                .map { habit ->
                    val doneDates = datesByHabit[habit.id].orEmpty()
                    HabitUiItem(
                        habit = habit,
                        isCompleted = dateKey in doneDates,
                        streak = streakFor(habit, doneDates),
                    )
                }
        }

    val uiState: StateFlow<HabitUiState> =
        combine(getHabitsForToday(), message) { items, msg ->
            HabitUiState(isLoading = false, habits = items, message = msg)
        }
            .catch { emit(HabitUiState(isLoading = false, message = "Could not load habits")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HabitUiState(isLoading = true),
            )

    fun addHabit(name: String, color: Int, icon: String, frequency: Frequency) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            message.value = "Habit name can't be empty"
            return
        }
        viewModelScope.launch {
            try {
                repository.addHabit(
                    Habit(name = trimmed, color = color, icon = icon, frequency = frequency)
                )
            } catch (e: Exception) {
                message.value = "Couldn't save habit"
            }
        }
    }

    fun toggleCompletion(habitId: Long) {
        viewModelScope.launch {
            try {
                repository.toggleCompletion(habitId, dateKey)
            } catch (e: Exception) {
                message.value = "Couldn't update habit"
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }

    // ---- helpers ----

    private fun Habit.isDueToday(): Boolean = when (frequency) {
        Frequency.DAILY, Frequency.CUSTOM -> true
        // WEEKLY habits show up on the same weekday they were created
        Frequency.WEEKLY -> weekdayOf(createdAt) == weekdayOf(System.currentTimeMillis())
    }

    /**
     * Counts consecutive completed days (or weeks for WEEKLY) going backwards.
     * If today isn't done yet the streak isn't broken: counting starts from the previous period.
     */
    private fun streakFor(habit: Habit, doneDates: Set<String>): Int {
        if (doneDates.isEmpty()) return 0
        val stepDays = if (habit.frequency == Frequency.WEEKLY) 7 else 1
        val cal = Calendar.getInstance()
        if (keyOf(cal) !in doneDates) cal.add(Calendar.DAY_OF_YEAR, -stepDays)
        var streak = 0
        while (keyOf(cal) in doneDates) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -stepDays)
        }
        return streak
    }

    private fun keyOf(cal: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

    private fun weekdayOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

/**
 * Builds the ViewModel with the real repository.
 * If Member 1 changes constructor names, this is the ONLY place to edit.
 */
class HabitViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = ChronosDatabase.getInstance(app)
        val repository = HabitRepository(db.habitDao(), db.completionDao())
        return HabitViewModel(repository) as T
    }
}
