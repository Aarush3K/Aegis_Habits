package com.nullvector.aegishabits.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nullvector.aegishabits.data.AppDatabase
import com.nullvector.aegishabits.model.Frequency
import com.nullvector.aegishabits.model.FrequencyType
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

data class HabitUiItem(
    val habit: Habit,
    val isCompleted: Boolean,
    val streak: Int = 0,
)

data class HabitUiState(
    val isLoading: Boolean = true,
    val habits: List<HabitUiItem> = emptyList(),
    val message: String? = null,
) {
    val completedCount: Int get() = habits.count { it.isCompleted }
    val totalCount: Int get() = habits.size
}

class HabitViewModel(
    private val repository: HabitRepository,
) : ViewModel() {

    private val dateKey: String = todayKey()
    private val message = MutableStateFlow<String?>(null)

    fun getHabitsForToday(): Flow<List<HabitUiItem>> =
        combine(
            repository.allHabits,
            repository.getCompletionsForDate(dateKey),
        ) { habits, completions ->
            val completedHabitIds = completions.map { it.habitId }.toSet()

            habits
                .filter { !it.isArchived && it.isDueToday() }
                .map { habit ->
                    val isDone = habit.id in completedHabitIds
                    HabitUiItem(
                        habit = habit,
                        isCompleted = isDone,
                        streak = 0,
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

    fun addHabit(title: String, color: Int, icon: String, frequencyType: FrequencyType) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            message.value = "Habit title can't be empty"
            return
        }
        viewModelScope.launch {
            try {
                repository.insertHabit(
                    Habit(
                        title = trimmed,
                        color = color,
                        icon = icon,
                        frequency = Frequency(type = frequencyType)
                    )
                )
            } catch (e: Exception) {
                message.value = "Couldn't save habit"
            }
        }
    }

    fun toggleCompletion(habitId: Long) {
        viewModelScope.launch {
            try {
                repository.toggleHabitCompletion(habitId, dateKey)
            } catch (e: Exception) {
                message.value = "Couldn't update habit"
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private fun Habit.isDueToday(): Boolean = true

    private fun weekdayOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}