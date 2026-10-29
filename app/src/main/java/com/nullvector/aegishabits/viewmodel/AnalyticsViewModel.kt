package com.nullvector.aegishabits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nullvector.aegishabits.model.Habit
import com.nullvector.aegishabits.repository.HabitCorrelationEngine
import com.nullvector.aegishabits.repository.HabitCorrelationResult
import com.nullvector.aegishabits.repository.HabitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DailyCompletionPoint(
    val dateString: String,
    val completedCount: Int,
    val totalActiveHabits: Int
) {
    val completionRatio: Float
        get() = if (totalActiveHabits > 0) completedCount.toFloat() / totalActiveHabits else 0f
}

data class HabitAnalyticsItem(
    val habit: Habit,
    val completionRate: Float,
    val currentStreak: Int
)

class AnalyticsViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val correlationEngine = HabitCorrelationEngine(repository)

    private val _timeRangeDays = MutableStateFlow(30L)
    val timeRangeDays: StateFlow<Long> = _timeRangeDays.asStateFlow()

    private val _correlations = MutableStateFlow<List<HabitCorrelationResult>>(emptyList())
    val correlations: StateFlow<List<HabitCorrelationResult>> = _correlations.asStateFlow()

    private val _dailyPoints = MutableStateFlow<List<DailyCompletionPoint>>(emptyList())
    val dailyPoints: StateFlow<List<DailyCompletionPoint>> = _dailyPoints.asStateFlow()

    // Overall KPIs
    private val _overallCompletionRate = MutableStateFlow(0f)
    val overallCompletionRate: StateFlow<Float> = _overallCompletionRate.asStateFlow()

    private val _bestStreak = MutableStateFlow(0)
    val bestStreak: StateFlow<Int> = _bestStreak.asStateFlow()

    // Real Habits with calculated stats for progress cards
    private val _habitStats = MutableStateFlow<List<HabitAnalyticsItem>>(emptyList())
    val habitStats: StateFlow<List<HabitAnalyticsItem>> = _habitStats.asStateFlow()

    init {
        loadAnalyticsData()
    }

    fun setTimeRange(days: Long) {
        _timeRangeDays.value = days
        loadAnalyticsData()
    }

    fun loadAnalyticsData() {
        viewModelScope.launch {
            val days = _timeRangeDays.value
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(days - 1)

            val activeHabits = repository.activeHabits.firstOrNull() ?: emptyList()
            val completions = repository.getCompletionsBetweenDates(startDate.toString(), endDate.toString())

            // 1. Build daily chart points
            val points = mutableListOf<DailyCompletionPoint>()
            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val dateStr = currentDate.toString()
                val countForDay = completions.count { it.dateString == dateStr }
                points.add(
                    DailyCompletionPoint(
                        dateString = dateStr,
                        completedCount = countForDay,
                        totalActiveHabits = activeHabits.size
                    )
                )
                currentDate = currentDate.plusDays(1)
            }
            _dailyPoints.value = points

            // 2. Compute overall completion rate for the period
            val totalPossible = activeHabits.size * days
            val totalCompleted = completions.size
            _overallCompletionRate.value = if (totalPossible > 0) {
                (totalCompleted.toFloat() / totalPossible) * 100f
            } else 0f

            // 3. Compute best streak and individual habit stats
            var maxStreak = 0
            val habitItems = mutableListOf<HabitAnalyticsItem>()

            for (habit in activeHabits) {
                val habitCompletions = repository.getCompletionsListForHabit(habit.id)
                val completionDates = habitCompletions.map { it.dateString }
                val streak = repository.calculateCurrentStreak(completionDates, habit.shieldsAvailable)
                if (streak > maxStreak) maxStreak = streak

                // Calculate individual completion rate over the selected timeframe
                val completedInPeriod = habitCompletions.count {
                    val d = LocalDate.parse(it.dateString)
                    !d.isBefore(startDate) && !d.isAfter(endDate)
                }
                val rate = if (days > 0) completedInPeriod.toFloat() / days.toFloat() else 0f

                habitItems.add(HabitAnalyticsItem(habit = habit, completionRate = rate.coerceIn(0f, 1f), currentStreak = streak))
            }
            _bestStreak.value = maxStreak
            _habitStats.value = habitItems

            // 4. Compute habit correlations
            val topCorrelations = correlationEngine.computeTopCorrelations(days)
            _correlations.value = topCorrelations
        }
    }
}