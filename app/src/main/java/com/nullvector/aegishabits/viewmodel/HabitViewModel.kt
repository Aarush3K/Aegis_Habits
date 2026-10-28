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
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HabitTab(val title: String) {
    TODAY("Today"),
    ALL("All Habits"),
    ANALYTICS("Analytics"),
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
    val todayHabits: List<HabitUiItem> = emptyList(),
    val allHabitsList: List<HabitUiItem> = emptyList(),
    val archivedHabitsList: List<HabitUiItem> = emptyList(),
    val selectedTab: HabitTab = HabitTab.TODAY,
    val isLoading: Boolean = false,
    val message: String? = null
) {
    // Keep total habits for the active tab view if needed elsewhere
    val totalCount: Int get() = habits.size
    val completedCount: Int get() = habits.count { it.isCompleted }

    // Dedicated Today counts so the progress bar never jumps when switching tabs!
    val todayTotalCount: Int get() = todayHabits.size
    val todayCompletedCount: Int get() = todayHabits.count { it.isCompleted }
}

class HabitViewModel(
    private val repository: HabitRepository,
    private val appContext: Context? = null
) : ViewModel() {

    private val currentDate = MutableStateFlow(todayKey())
    private val currentTab = MutableStateFlow(HabitTab.TODAY)
    private val _message = MutableStateFlow<String?>(null)

    fun selectTab(tab: HabitTab) {
        currentTab.value = tab
    }

    fun refreshDate() {
        currentDate.value = todayKey()
    }

    fun clearMessage() {
        _message.value = null
    }

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

    // --- Single Reactive UI State Flow ---
    // --- Single Reactive UI State Flow ---
    val uiState: StateFlow<HabitUiState> = combine(
        repository.allHabits,
        repository.allCompletions,
        currentDate,
        currentTab,
        _message
    ) { habits, allCompletions, todayDateKey, tab, msg ->
        val todayCompletedIds = allCompletions
            .filter { it.dateString == todayDateKey }
            .map { it.habitId }
            .toSet()

        // Helper mapper function
        // Helper mapper function with optimized streak lookup
        fun mapToUiItems(rawHabits: List<Habit>): List<HabitUiItem> {
            // Pre-group all completions by habitId once, instead of filtering list inside a loop
            val completionsByHabitId = allCompletions.groupBy { it.habitId }

            return rawHabits.map { habit ->
                val isDone = habit.id in todayCompletedIds

                // Pull pre-grouped dates instantly
                val habitCompletionDates = completionsByHabitId[habit.id]?.map { it.dateString } ?: emptyList()
                val currentStreak = repository.calculateCurrentStreak(habitCompletionDates)

                HabitUiItem(
                    habit = habit,
                    isCompleted = isDone,
                    streak = currentStreak
                )
            }
        }

        // Pre-calculate all tab lists simultaneously to eliminate switching lag
        val todayList = mapToUiItems(habits.filter { !it.isArchived && it.isDueOn(todayDateKey) })
        val allList = mapToUiItems(habits.filter { !it.isArchived })
        val archivedList = mapToUiItems(habits.filter { it.isArchived })

        // Pick the active list instantly
        val activeHabits = when (tab) {
            HabitTab.TODAY -> todayList
            HabitTab.ALL -> allList
            HabitTab.ARCHIVED -> archivedList
            HabitTab.ANALYTICS -> emptyList()
        }

        HabitUiState(
            habits = activeHabits,
            todayHabits = todayList,
            allHabitsList = allList,
            archivedHabitsList = archivedList,
            selectedTab = tab,
            isLoading = false,
            message = msg
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
                _message.value = "Couldn't update habit"
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
            val todayStr = todayKey()
            repository.toggleHabitCompletion(habitId, todayStr)
            triggerWidgetUpdate(context)
        }
    }

    // --- Helper Functions ---

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


    // --- Habit Detail State & Logic (INSIDE HabitViewModel class) ---
    private val _detailUiState = MutableStateFlow(HabitDetailUiState())
    val detailUiState: StateFlow<HabitDetailUiState> = _detailUiState.asStateFlow()

    fun openHabitDetail(habit: Habit) {
        viewModelScope.launch {
            _detailUiState.update { it.copy(selectedHabit = habit, isLoading = true) }

            val completionList = repository.getCompletionsListForHabit(habit.id)

            _detailUiState.update {
                it.copy(
                    completions = completionList,
                    isLoading = false
                )
            }
        }
    }

    fun closeHabitDetail() {
        _detailUiState.update { HabitDetailUiState() }
    }

} // <--- Class closes HERE!

// Place HabitDetailUiState OUTSIDE the class at the very bottom
data class HabitDetailUiState(
    val selectedHabit: Habit? = null,
    val completions: List<HabitCompletion> = emptyList(),
    val isLoading: Boolean = false
)

