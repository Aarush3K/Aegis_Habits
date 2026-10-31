package com.nullvector.aegishabits.repository

import com.nullvector.aegishabits.model.Habit
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.firstOrNull

data class HabitCorrelationResult(
    val habitA: Habit,
    val habitB: Habit,
    val probabilityA: Double,      // P(A)
    val probabilityBGivenA: Double,// P(B | A)
    val liftPercentage: Double     // Percentage increase/decrease in B when A is done
)

class HabitCorrelationEngine(
    private val repository: HabitRepository
) {

    /**
     * Computes correlation insights across all active habits over a trailing window (e.g., last 30 or 60 days).
     */
    suspend fun computeTopCorrelations(daysWindow: Long = 30): List<HabitCorrelationResult> {
        val habits = repository.activeHabits.firstOrNull() ?: emptyList()
        if (habits.size < 2) return emptyList()

        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(daysWindow)

        val totalDaysInWindow = ChronoUnit.DAYS.between(startDate, endDate) + 1.0
        if (totalDaysInWindow <= 0) return emptyList()

        // Fetch completion maps per habit within the window
        val habitDatesMap = mutableMapOf<Long, Set<LocalDate>>()
        for (habit in habits) {
            val dates = repository.getCompletionsListForHabit(habit.id)
                .mapNotNull { runCatching { LocalDate.parse(it.dateString) }.getOrNull() }
                .filter { !it.isBefore(startDate) && !it.isAfter(endDate) }
                .toSet()
            habitDatesMap[habit.id] = dates
        }

        val results = mutableListOf<HabitCorrelationResult>()

        // Compare every unique pair (A, B)
        for (i in habits.indices) {
            for (j in i + 1 until habits.size) {
                val habitA = habits[i]
                val habitB = habits[j]

                val datesA = habitDatesMap[habitA.id] ?: emptySet()
                val datesB = habitDatesMap[habitB.id] ?: emptySet()

                if (datesA.size < 3 || datesB.size < 3) continue // Skip if insufficient data

                val pA = datesA.size / totalDaysInWindow
                val pB = datesB.size / totalDaysInWindow

                if (pB == 0.0) continue

                // Intersection: days when both A and B were completed
                val jointDates = datesA.intersect(datesB)
                val pBGivenA = if (datesA.isNotEmpty()) jointDates.size.toDouble() / datesA.size else 0.0

                // Lift formula: How much higher is P(B | A) compared to baseline P(B)
                val lift = ((pBGivenA - pB) / pB) * 100.0

                // Only keep positive, meaningful correlations (lift > 10%)
                if (lift > 10.0) {
                    results.add(
                        HabitCorrelationResult(
                            habitA = habitA,
                            habitB = habitB,
                            probabilityA = pA * 100,
                            probabilityBGivenA = pBGivenA * 100,
                            liftPercentage = lift
                        )
                    )
                }
            }
        }

        // Return top insights sorted by highest lift percentage
        return results.sortedByDescending { it.liftPercentage }
    }
}