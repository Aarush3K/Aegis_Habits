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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi

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


    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            try {
                refreshDate()
                repository.toggleHabitCompletion(habitId, currentDate.value)
            } catch (e: Exception) {
                message.value = "Couldn't update habit"
            }
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
