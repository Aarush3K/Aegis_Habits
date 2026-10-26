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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi

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

    private val currentDate = MutableStateFlow(todayKey())
    private val message = MutableStateFlow<String?>(null)
    private val currentTab = MutableStateFlow(HabitTab.TODAY)

    fun selectTab(tab: HabitTab) {
        currentTab.value = tab
    }

    fun refreshDate() {
        currentDate.value = todayKey()
    }

    fun getFilteredHabits(): Flow<List<HabitUiItem>> {
        return combine(
            repository.allHabits,
            repository.allCompletions,
            currentDate,
            currentTab
        ) { habits, allCompletions, todayDateKey, tab ->
            // 1. Get completions strictly matching today's date key
            val todayCompletedIds = allCompletions
                .filter { it.dateString == todayDateKey }
                .map { it.habitId }
                .toSet()

            // 2. Filter habits based on selected tab and current date
            val filteredHabits = when (tab) {
                HabitTab.TODAY -> habits.filter { !it.isArchived && it.isDueOn(todayDateKey) }
                HabitTab.ALL -> habits.filter { !it.isArchived }
                HabitTab.ARCHIVED -> habits.filter { it.isArchived }
            }

            // 3. Map habits to UI items with accurate streaks & completion status
            filteredHabits.map { habit ->
                val isDone = habit.id in todayCompletedIds

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
                // Ensure date is fresh before toggling
                refreshDate()
                repository.toggleHabitCompletion(habitId, currentDate.value)
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

    private fun Habit.isDueOn(dateString: String): Boolean = when (frequency.type) {
        FrequencyType.DAILY -> true
        FrequencyType.WEEKLY -> {
            val createdTime = if (createdAt > 0L) createdAt else System.currentTimeMillis()
            val createdDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(createdTime))
            weekdayOf(createdDateKey) == weekdayOf(dateString)
        }
        FrequencyType.CUSTOM -> {
            if (frequency.customDaysOfWeek.isNotEmpty()) {
                weekdayOf(dateString) in frequency.customDaysOfWeek
            } else {
                true
            }
        }
    }

    private fun weekdayOf(dateString: String): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(dateString) ?: Date()
        return Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_WEEK)
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
        }
    }
}
