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
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate

class HabitWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val todayStr = LocalDate.now().toString()

        val habits: List<Habit> = try {
            db.habitDao().getAllActiveHabits().firstOrNull() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val completions = try {
            db.completionDao().getCompletionsForDate(todayStr).firstOrNull() ?: emptyList()
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
            FrequencyType.WEEKLY -> true
            FrequencyType.CUSTOM -> {
                if (habit.frequency.customDaysOfWeek.isEmpty()) true
                else todayCustomCode in habit.frequency.customDaysOfWeek
            }
        }
    }

    val dueHabits = habits.filter { isDueToday(it) }
    val upcomingHabits = habits.filter { !isDueToday(it) }

    val leftToComplete = dueHabits.filter { it.id !in completedIds }
    val completedToday = dueHabits.filter { it.id in completedIds }

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
                text = "${completedToday.size}/${dueHabits.size} done",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // --- Content Sections ---
        if (habits.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(12.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No habits added yet.",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
                )
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                // 1. Left to complete
                if (leftToComplete.isNotEmpty()) {
                    Text(
                        text = "To Complete:",
                        style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    leftToComplete.take(2).forEach { habit ->
                        HabitItemRow(symbol = "⬜", habitName = habit.title)
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                }

                // 2. Completed
                if (completedToday.isNotEmpty()) {
                    Text(
                        text = "Completed:",
                        style = TextStyle(color = GlanceTheme.colors.outline, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    completedToday.take(2).forEach { habit ->
                        HabitItemRow(symbol = "✅", habitName = habit.title)
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                }

                // 3. Upcoming
                if (upcomingHabits.isNotEmpty()) {
                    Text(
                        text = "Upcoming / Other:",
                        style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    upcomingHabits.take(1).forEach { habit ->
                        HabitItemRow(symbol = "⏳", habitName = habit.title)
                    }
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
        Text(text = symbol, style = TextStyle(fontSize = 12.sp))
        Spacer(modifier = GlanceModifier.width(6.dp))
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
