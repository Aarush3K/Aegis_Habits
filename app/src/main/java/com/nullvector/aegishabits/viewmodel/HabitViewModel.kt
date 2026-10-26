package com.nullvector.aegishabits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nullvector.aegishabits.model.Frequency
import com.nullvector.aegishabits.model.FrequencyType
import com.nullvector.aegishabits.model.Habit
import com.nullvector.aegishabits.model.HabitCompletion
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

    fun getHabitsForToday(): Flow<List<HabitUiItem>> {
        val firstTwoFlows = combine(
            repository.allHabits,
            repository.getCompletionsForDate(dateKey)
        ) { habits, todayCompletions ->
            habits to todayCompletions
        }

        return combine(
            firstTwoFlows,
            repository.allCompletions
        ) { pair, allCompletions ->
            val habits = pair.first
            val todayCompletions = pair.second
            val completedHabitIds = todayCompletions.map { it.habitId }.toSet()

            habits
                .filter { !it.isArchived && it.isDueToday() }
                .map { habit ->
                    val isDone = habit.id in completedHabitIds

                    // Gather all completion date strings for this habit
                    val habitCompletionDates = allCompletions
                        .filter { it.habitId == habit.id }
                        .map { it.dateString }

                    // Use repository's built-in streak calculator
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
        combine(getHabitsForToday(), message) { items, msg ->
            HabitUiState(isLoading = false, habits = items, message = msg)
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

    private fun calculateStreakFromDates(dates: List<String>): Int {
        if (dates.isEmpty()) return 0

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sortedDates = dates.mapNotNull {
            try { formatter.parse(it) } catch (e: Exception) { null }
        }.sortedDescending()

        if (sortedDates.isEmpty()) return 0

        val cal = Calendar.getInstance()
        val todayStr = formatter.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = formatter.format(cal.time)

        val latestDateStr = formatter.format(sortedDates.first())

        // If not completed today or yesterday, the streak is broken
        if (latestDateStr != todayStr && latestDateStr != yesterdayStr) {
            return 0
        }

        var streak = 0
        val checkCal = Calendar.getInstance()

        // If today isn't completed yet, start checking backward from yesterday
        if (latestDateStr == yesterdayStr) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val uniqueDateSet = dates.toSet()

        while (true) {
            val checkStr = formatter.format(checkCal.time)
            if (checkStr in uniqueDateSet) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }
}