package com.nullvector.aegishabits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

enum class HabitTab(val title: String) {
    TODAY("Today"),
    ALL("All Habits"),
    ARCHIVED("Archive")
}

data class HabitUiItem(
    val habit: Habit,
    val isCompleted: Boolean,
    val streak: Int = 0,
)

data class HabitUiState(
    val isLoading: Boolean = true,
    val selectedTab: HabitTab = HabitTab.TODAY,
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
    private val currentTab = MutableStateFlow(HabitTab.TODAY)

    fun selectTab(tab: HabitTab) {
        currentTab.value = tab
    }

    fun getFilteredHabits(): Flow<List<HabitUiItem>> {
        val firstTwoFlows = combine(
            repository.allHabits,
            repository.getCompletionsForDate(dateKey)
        ) { habits, todayCompletions ->
            habits to todayCompletions
        }

        return combine(
            firstTwoFlows,
            repository.allCompletions,
            currentTab
        ) { pair, allCompletions, tab ->
            val habits = pair.first
            val todayCompletions = pair.second
            val completedHabitIds = todayCompletions.map { it.habitId }.toSet()

            val filteredHabits = when (tab) {
                HabitTab.TODAY -> habits.filter { !it.isArchived && it.isDueToday() }
                HabitTab.ALL -> habits.filter { !it.isArchived }
                HabitTab.ARCHIVED -> habits.filter { it.isArchived }
            }

            filteredHabits.map { habit ->
                val isDone = habit.id in completedHabitIds

                val habitCompletionDates = allCompletions
                    .filter { it.habitId == habit.id }
                    .map { it.dateString }

                val currentStreak = repository.calculateCurrentStreak(habitCompletionDates)

                HabitUiItem(
                    habit = habit,
                    isCompleted = isDone,
                    streak = currentStreak,
                )
            }
        }
    }

    val uiState: StateFlow<HabitUiState> =
        combine(getFilteredHabits(), currentTab, message) { items, tab, msg ->
            HabitUiState(
                isLoading = false,
                selectedTab = tab,
                habits = items,
                message = msg
            )
        }
            .catch { emit(HabitUiState(isLoading = false, message = "Could not load habits")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HabitUiState(isLoading = true),
            )

    fun addHabit(
        title: String,
        color: Int,
        icon: String,
        frequencyType: FrequencyType,
        targetDaysPerWeek: Int = 7,
        customDaysOfWeek: List<Int> = emptyList()
    ) {
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
                        frequency = Frequency(
                            type = frequencyType,
                            targetDaysPerWeek = targetDaysPerWeek,
                            customDaysOfWeek = customDaysOfWeek
                        )
                    )
                )
                message.value = "Habit added"
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

    fun archiveHabit(habit: Habit) {
        viewModelScope.launch {
            try {
                repository.updateHabit(habit.copy(isArchived = true))
                message.value = "Habit archived"
            } catch (e: Exception) {
                message.value = "Couldn't archive habit"
            }
        }
    }

    fun restoreHabit(habit: Habit) {
        viewModelScope.launch {
            try {
                repository.updateHabit(habit.copy(isArchived = false))
                message.value = "Habit restored"
            } catch (e: Exception) {
                message.value = "Couldn't restore habit"
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            try {
                repository.deleteHabit(habit)
                message.value = "Habit deleted"
            } catch (e: Exception) {
                message.value = "Couldn't delete habit"
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private fun Habit.isDueToday(): Boolean = when (frequency.type) {
        FrequencyType.DAILY -> true
        FrequencyType.WEEKLY -> {
            val createdTime = if (createdAt > 0L) createdAt else System.currentTimeMillis()
            weekdayOf(createdTime) == weekdayOf(System.currentTimeMillis())
        }
        FrequencyType.CUSTOM -> {
            if (frequency.customDaysOfWeek.isNotEmpty()) {
                val todayCalendarDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                todayCalendarDay in frequency.customDaysOfWeek
            } else {
                true
            }
        }
    }

    private fun weekdayOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
        }
    }
}
