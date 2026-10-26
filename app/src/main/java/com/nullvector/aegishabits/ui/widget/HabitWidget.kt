package com.nullvector.aegishabits.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.nullvector.aegishabits.data.AppDatabase
import com.nullvector.aegishabits.model.FrequencyType
import com.nullvector.aegishabits.model.Habit
import java.time.LocalDate

class HabitWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val todayStr = LocalDate.now().toString()

        val habits: List<Habit> = try {
            db.habitDao().getAllActiveHabitsList()
        } catch (e: Exception) {
            emptyList()
        }

        val completions = try {
            db.completionDao().getCompletionsListForDate(todayStr)
        } catch (e: Exception) {
            emptyList()
        }

        val completedIds = completions.map { it.habitId }.toSet()

        provideContent {
            GlanceTheme {
                HabitWidgetContent(habits, completedIds)
            }
        }
    }
}

@Composable
fun HabitWidgetContent(habits: List<Habit>, completedIds: Set<Long>) {
    val today = LocalDate.now()
    val todayCustomCode = when (today.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> 2
        java.time.DayOfWeek.TUESDAY -> 3
        java.time.DayOfWeek.WEDNESDAY -> 4
        java.time.DayOfWeek.THURSDAY -> 5
        java.time.DayOfWeek.FRIDAY -> 6
        java.time.DayOfWeek.SATURDAY -> 7
        java.time.DayOfWeek.SUNDAY -> 1
    }

    fun isDueToday(habit: Habit): Boolean {
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
                if (habit.frequency.customDaysOfWeek.isEmpty()) true
                else todayCustomCode in habit.frequency.customDaysOfWeek
            }
        }
    }

    val dueHabits = habits.filter { isDueToday(it) }
    val pendingHabits = dueHabits.filter { it.id !in completedIds }
    val completedCount = dueHabits.size - pendingHabits.size

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(14.dp)
    ) {
        // --- Widget Header ---
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ Aegis Habits",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                text = if (dueHabits.isNotEmpty()) "$completedCount/${dueHabits.size} done" else "Today",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // --- Pending Tasks or Motivational Message ---
        if (pendingHabits.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(12.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌟 Fantastic job!",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = if (dueHabits.isNotEmpty())
                            "All tasks completed today! Keep pushing forward! 🔥"
                        else
                            "No pending tasks for today. Rest up or add new goals! 💪",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                pendingHabits.take(4).forEach { habit ->
                    HabitItemRow(symbol = habit.icon.ifBlank { "📌" }, habitName = habit.title)
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
                if (pendingHabits.size > 4) {
                    Text(
                        text = "+${pendingHabits.size - 4} more pending",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HabitItemRow(symbol: String, habitName: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(8.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = symbol, style = TextStyle(fontSize = 13.sp))
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = habitName,
            style = TextStyle(
                color = GlanceTheme.colors.onSecondaryContainer,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
