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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class HabitTab(val title: String) {
    TODAY("Today"),
    ALL("All Habits"),
    ARCHIVED("Archived")
}

// --- UI Data Models required by HabitActivity ---
data class HabitUiItem(
    val habit: Habit,
    val isCompleted: Boolean = false,
    val streak: Int = 0
)

data class HabitUiState(
    val habits: List<HabitUiItem> = emptyList(),
    val selectedTab: HabitTab = HabitTab.TODAY,
    val isLoading: Boolean = false,
    val message: String? = null
) {
    val totalCount: Int get() = habits.size
    val completedCount: Int get() = habits.count { it.isCompleted }
}

class HabitViewModel(
    private val repository: HabitRepository,
    private val appContext: Context? = null
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _selectedTab = MutableStateFlow(HabitTab.TODAY)

    private fun triggerWidgetUpdate(context: Context?) {
        val targetContext = context ?: appContext
        targetContext?.let { ctx ->
            viewModelScope.launch {
                try {
                    HabitWidget().updateAll(ctx)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Expose habits mapped to HabitUiItem for Compose UI based on selected tab
    val uiState: StateFlow<HabitUiState> = combine(
        repository.allHabits,
        repository.allCompletions,
        _selectedTab,
        _message
    ) { habitList, completions, selectedTab, message ->
        val todayStr = LocalDate.now().toString()
        val today = LocalDate.now()

        val filteredHabits = habitList.filter { habit ->
            when (selectedTab) {
                HabitTab.TODAY -> !habit.isArchived && isDueToday(habit, today)
                HabitTab.ALL -> !habit.isArchived
                HabitTab.ARCHIVED -> habit.isArchived
            }
        }

        val habitItems = filteredHabits.map { habit ->
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
            selectedTab = selectedTab,
            isLoading = false,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitUiState(isLoading = true)
    )

    val habits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectTab(tab: HabitTab) {
        _selectedTab.value = tab
    }

    fun clearMessage() {
        _message.value = null
    }

    // --- User Actions with Widget Refresh Triggers ---

    fun addHabit(habit: Habit, context: Context? = null) {
        viewModelScope.launch {
            repository.insertHabit(habit)
            _message.value = "Habit added"
            triggerWidgetUpdate(context)
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
            triggerWidgetUpdate(context)
        }
    }

    fun archiveHabit(habit: Habit, context: Context? = null) {
        viewModelScope.launch {
            repository.updateHabit(habit.copy(isArchived = true))
            _message.value = "Habit archived"
            triggerWidgetUpdate(context)
        }
    }

    fun restoreHabit(habit: Habit, context: Context? = null) {
        viewModelScope.launch {
            repository.updateHabit(habit.copy(isArchived = false))
            _message.value = "Habit restored"
            triggerWidgetUpdate(context)
        }
    }

    fun deleteHabit(habit: Habit, context: Context? = null) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            _message.value = "Habit deleted"
            triggerWidgetUpdate(context)
        }
    }

    fun toggleCompletion(completion: HabitCompletion, isCompleted: Boolean, context: Context? = null) {
        viewModelScope.launch {
            if (isCompleted) {
                repository.insertCompletion(completion)
            } else {
                repository.deleteCompletion(completion)
            }
            triggerWidgetUpdate(context)
        }
    }

    fun toggleCompletion(habitId: Long, context: Context? = null) {
        viewModelScope.launch {
            val todayStr = LocalDate.now().toString()
            repository.toggleHabitCompletion(habitId, todayStr)
            triggerWidgetUpdate(context)
        }
    }

    private fun isDueToday(habit: Habit, today: LocalDate): Boolean {
        return when (habit.frequency.type) {
            FrequencyType.DAILY -> true
            FrequencyType.WEEKLY -> {
                val createdTime = if (habit.createdAt > 0L) habit.createdAt else System.currentTimeMillis()
                val createdDayOfWeek = java.time.Instant.ofEpochMilli(createdTime)
                    .atZone(java.time.ZoneId.systemDefault())
                    .dayOfWeek
                today.dayOfWeek == createdDayOfWeek
            }
            FrequencyType.CUSTOM -> {
                if (habit.frequency.customDaysOfWeek.isEmpty()) {
                    true
                } else {
                    val todayCustomCode = when (today.dayOfWeek) {
                        java.time.DayOfWeek.MONDAY -> 2
                        java.time.DayOfWeek.TUESDAY -> 3
                        java.time.DayOfWeek.WEDNESDAY -> 4
                        java.time.DayOfWeek.THURSDAY -> 5
                        java.time.DayOfWeek.FRIDAY -> 6
                        java.time.DayOfWeek.SATURDAY -> 7
                        java.time.DayOfWeek.SUNDAY -> 1
                    }
                    todayCustomCode in habit.frequency.customDaysOfWeek
                }
            }
        }
    }
}
