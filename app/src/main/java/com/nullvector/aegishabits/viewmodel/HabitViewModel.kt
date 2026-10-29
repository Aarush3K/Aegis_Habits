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
import kotlinx.coroutines.flow.asStateFlow
import com.nullvector.aegishabits.notification.HabitReminderManager

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

    private val reminderManager: HabitReminderManager
        get() = HabitReminderManager(appContext!!)

    private val currentDate = MutableStateFlow(todayKey())
    private val currentTab = MutableStateFlow(HabitTab.TODAY)
    private val _message = MutableStateFlow<String?>(null)

    fun applyShieldIfMissed(habit: Habit): Habit {
        if (habit.shieldsAvailable > 0) {
            // Consume 1 shield to protect the streak
            return habit.copy(shieldsAvailable = habit.shieldsAvailable - 1)
        }
        return habit
    }

    fun checkAndAwardShields(habit: Habit, currentStreak: Int): Habit {
        // Grant a shield every 7 days of streak, up to a max of 3 shields
        val maxShields = 3
        val nextMilestone = habit.lastShieldMilestone + 7
        if (currentStreak >= nextMilestone && habit.shieldsAvailable < maxShields) {
            val earnedCount = (currentStreak / 7) - (habit.lastShieldMilestone / 7)
            val shieldsToAdd = minOf(maxShields - habit.shieldsAvailable, maxOf(1, earnedCount))
            return habit.copy(
                shieldsAvailable = minOf(maxShields, habit.shieldsAvailable + shieldsToAdd),
                lastShieldMilestone = (currentStreak / 7) * 7
            )
        }
        return habit
    }

    // Inside HabitViewModel methods:

    fun addHabit(name: String, color: Int, icon: String, frequencyType: FrequencyType, targetDays: Int, customDays: List<Int>, reminderTime: String?) {
        viewModelScope.launch {
            val newHabit = Habit(
                title = name,
                color = color,
                icon = icon,
                frequency = Frequency(type = frequencyType, customDaysOfWeek = customDays),
                reminderTime = reminderTime
            )
            val insertedId = repository.insertHabit(newHabit)

            if (!reminderTime.isNullOrBlank()) {
                reminderManager.scheduleReminder(insertedId, name, reminderTime)
            }
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            if (!habit.reminderTime.isNullOrBlank()) {
                reminderManager.scheduleReminder(habit.id, habit.title, habit.reminderTime)
            } else {
                reminderManager.cancelReminder(habit.id)
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            reminderManager.cancelReminder(habit.id)
            repository.deleteHabit(habit)
        }
    }

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

        // Helper mapper function with optimized streak lookup
        fun mapToUiItems(rawHabits: List<Habit>): List<HabitUiItem> {
            // Pre-group all completions by habitId once, instead of filtering list inside a loop
            val completionsByHabitId = allCompletions.groupBy { it.habitId }

            return rawHabits.map { habit ->
                val isDone = habit.id in todayCompletedIds

                // Pull pre-grouped dates instantly
                val habitCompletionDates = completionsByHabitId[habit.id]?.map { it.dateString } ?: emptyList()
                val currentStreak = repository.calculateCurrentStreak(habitCompletionDates, habit.shieldsAvailable)

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
            val habit = habits.value.find { it.id == habitId } ?: repository.getHabitById(habitId)

            // 1. Fetch existing completion dates before toggling today
            val completionList = repository.getCompletionsListForHabit(habitId)
            val completionDates = completionList.map { it.dateString }
            val isCompletedToday = completionDates.contains(todayStr)

            var currentHabit = habit
            var missedDays = 0

            // 2. Check if a shield is needed (if completing today and last completion was more than 1 day ago)
            if (currentHabit != null && !isCompletedToday && completionDates.isNotEmpty()) {
                val latestDateStr = completionDates.filter { it != todayStr }.maxOrNull()
                if (latestDateStr != null) {
                    val latestLocalDate = java.time.LocalDate.parse(latestDateStr)
                    val todayLocalDate = java.time.LocalDate.parse(todayStr)
                    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(latestLocalDate, todayLocalDate).toInt()

                    if (daysBetween > 1) {
                        missedDays = daysBetween - 1
                        if (currentHabit.shieldsAvailable >= missedDays) {
                            currentHabit = currentHabit.copy(shieldsAvailable = currentHabit.shieldsAvailable - missedDays)
                            repository.updateHabit(currentHabit)
                        }
                    }
                }
            }

            // 3. Proceed with standard toggle for today
            repository.toggleHabitCompletion(habitId, todayStr)

            // 4. Update milestones / award new shields if earned
            val updatedCompletionList = repository.getCompletionsListForHabit(habitId)
            val updatedDates = updatedCompletionList.map { it.dateString }
            val effectiveShields = (currentHabit?.shieldsAvailable ?: 0) + missedDays
            val newStreak = repository.calculateCurrentStreak(updatedDates, effectiveShields)

            if (currentHabit != null) {
                val awardedHabit = checkAndAwardShields(currentHabit, newStreak)
                if (awardedHabit.shieldsAvailable != currentHabit.shieldsAvailable) {
                    repository.updateHabit(awardedHabit)
                }
            }

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

    // --- Habit Detail State & Logic ---
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
}

data class HabitDetailUiState(
    val selectedHabit: Habit? = null,
    val completions: List<HabitCompletion> = emptyList(),
    val isLoading: Boolean = false
)