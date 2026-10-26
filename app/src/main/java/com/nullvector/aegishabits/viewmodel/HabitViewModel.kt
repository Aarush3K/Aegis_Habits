package com.nullvector.aegishabits.viewmodel

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nullvector.aegishabits.model.Frequency
import com.nullvector.aegishabits.model.FrequencyType
import com.nullvector.aegishabits.model.Habit
import com.nullvector.aegishabits.model.HabitCompletion
import com.nullvector.aegishabits.repository.HabitRepository
import com.nullvector.aegishabits.ui.widget.HabitWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// --- UI Data Models required by HabitActivity ---
data class HabitUiItem(
    val habit: Habit,
    val isCompleted: Boolean = false,
    val streak: Int = 0
)

data class HabitUiState(
    val habits: List<HabitUiItem> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
) {
    val totalCount: Int get() = habits.size
    val completedCount: Int get() = habits.count { it.isCompleted }
}

class HabitViewModel(private val repository: HabitRepository) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)

    // 1. Expose active habits as StateFlow mapped to HabitUiItem for Compose UI
    val uiState: StateFlow<HabitUiState> = combine(
        repository.allHabits,
        repository.allCompletions,
        _message
    ) { habitList, completions, message ->
        val todayStr = LocalDate.now().toString()
        val habitItems = habitList.map { habit ->
            val habitCompletions = completions.filter { it.habitId == habit.id }
            val completedDates = habitCompletions.map { it.dateString }
            val isCompletedToday = completedDates.contains(todayStr)
            val streak = repository.calculateCurrentStreak(completedDates)
            HabitUiItem(
                habit = habit,
                isCompleted = isCompletedToday,
                streak = streak
            )
        }
        HabitUiState(
            habits = habitItems,
            isLoading = false,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = HabitUiState(isLoading = true)
    )

    val habits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearMessage() {
        _message.value = null
    }

    // 2. User Actions with Widget Refresh Triggers
    fun addHabit(habit: Habit, context: Context? = null) {
        viewModelScope.launch {
            repository.insertHabit(habit)
            _message.value = "Habit added"
            context?.applicationContext?.let { ctx ->
                delay(100)
                HabitWidget().updateAll(ctx)
            }
        }
    }

    fun addHabit(
        name: String,
        color: Int,
        icon: String,
        frequencyType: FrequencyType,
        targetDays: Int,
        customDays: List<Int>,
        context: Context? = null
    ) {
        val habit = Habit(
            title = name,
            color = color,
            icon = icon,
            frequency = Frequency(
                type = frequencyType,
                targetDaysPerWeek = targetDays,
                customDaysOfWeek = customDays
            )
        )
        addHabit(habit, context)
    }

    fun updateHabit(habit: Habit, context: Context? = null) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            _message.value = "Habit updated"
            context?.applicationContext?.let { ctx ->
                delay(100)
                HabitWidget().updateAll(ctx)
            }
        }
    }

    fun deleteHabit(habit: Habit, context: Context? = null) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            _message.value = "Habit deleted"
            context?.applicationContext?.let { ctx ->
                delay(100)
                HabitWidget().updateAll(ctx)
            }
        }
    }

    fun toggleCompletion(completion: HabitCompletion, isCompleted: Boolean, context: Context? = null) {
        viewModelScope.launch {
            if (isCompleted) {
                repository.insertCompletion(completion)
            } else {
                repository.deleteCompletion(completion)
            }
            context?.applicationContext?.let { ctx ->
                delay(100)
                HabitWidget().updateAll(ctx)
            }
        }
    }

    fun toggleCompletion(habitId: Long, context: Context? = null) {
        viewModelScope.launch {
            val todayStr = LocalDate.now().toString()
            repository.toggleHabitCompletion(habitId, todayStr)
            context?.applicationContext?.let { ctx ->
                delay(100)
                HabitWidget().updateAll(ctx)
            }
        }
    }
}
