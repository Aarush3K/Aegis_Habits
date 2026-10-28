package com.nullvector.aegishabits.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.SizeMode
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import com.nullvector.aegishabits.model.HabitCompletion
import com.nullvector.aegishabits.repository.HabitRepository
import java.time.LocalDate

class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[HABIT_ID_KEY] ?: return
        val db = AppDatabase.getDatabase(context)
        val todayStr = LocalDate.now().toString()
        val repository = HabitRepository(db.habitDao(), db.completionDao())

        repository.toggleHabitCompletion(habitId, todayStr)
        HabitWidget().updateAll(context)
    }

    companion object {
        val HABIT_ID_KEY = ActionParameters.Key<Long>("habit_id_key")
    }
}

class HabitWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(120.dp, 100.dp),
            DpSize(240.dp, 110.dp),
            DpSize(240.dp, 200.dp)
        )
    )

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

        val allCompletions = try {
            db.completionDao().getAllCompletions()
        } catch (e: Exception) {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                HabitWidgetResponsiveContent(
                    habits = habits,
                    completedIds = completedIds,
                    allCompletions = allCompletions
                )
            }
        }
    }
}

fun isHabitDueOnDate(habit: Habit, date: LocalDate = LocalDate.now()): Boolean {
    val dayCustomCode = when (date.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> 2
        java.time.DayOfWeek.TUESDAY -> 3
        java.time.DayOfWeek.WEDNESDAY -> 4
        java.time.DayOfWeek.THURSDAY -> 5
        java.time.DayOfWeek.FRIDAY -> 6
        java.time.DayOfWeek.SATURDAY -> 7
        java.time.DayOfWeek.SUNDAY -> 1
    }

    return when (habit.frequency.type) {
        FrequencyType.DAILY -> true
        FrequencyType.WEEKLY -> {
            val createdTime = if (habit.createdAt > 0L) habit.createdAt else System.currentTimeMillis()
            val createdDayOfWeek = java.time.Instant.ofEpochMilli(createdTime)
                .atZone(java.time.ZoneId.systemDefault())
                .dayOfWeek
            date.dayOfWeek == createdDayOfWeek
        }
        FrequencyType.CUSTOM -> {
            if (habit.frequency.customDaysOfWeek.isEmpty()) true
            else dayCustomCode in habit.frequency.customDaysOfWeek
        }
    }
}

@Composable
fun HabitWidgetResponsiveContent(
    habits: List<Habit>,
    completedIds: Set<Long>,
    allCompletions: List<HabitCompletion>
) {
    val size = LocalSize.current
    val today = LocalDate.now()
    val dueHabits = habits.filter { isHabitDueOnDate(it, today) }
    val pendingHabits = dueHabits.filter { it.id !in completedIds }
    val completedCount = dueHabits.size - pendingHabits.size
    val progressFraction = if (dueHabits.isEmpty()) 1f else completedCount.toFloat() / dueHabits.size

    val isLarge = size.height >= 160.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(12.dp)
    ) {
        // --- Header Row ---
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ Aegis",
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
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        // --- Progress Visual Bar ---
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(6.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(3.dp)
        ) {
            if (progressFraction > 0f) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .defaultWeight()
                        .background(GlanceTheme.colors.primary)
                        .cornerRadius(3.dp)
                ) {}
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // --- Large View: Week Grid ---
        if (isLarge) {
            WeekGridSection(allCompletions = allCompletions, today = today)
            Spacer(modifier = GlanceModifier.height(8.dp))
        }

        // --- Pending Tasks List or Motivational Banner ---
        if (pendingHabits.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(12.dp)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🌟 Fantastic job!",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = if (dueHabits.isNotEmpty())
                            "All habits completed today! Keep it up! 🔥"
                        else
                            "No habits scheduled for today! 💪",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                val maxVisible = if (isLarge) 3 else 2
                pendingHabits.take(maxVisible).forEach { habit ->
                    InteractiveHabitRow(habit = habit)
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
                if (pendingHabits.size > maxVisible) {
                    Text(
                        text = "+${pendingHabits.size - maxVisible} more pending",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WeekGridSection(allCompletions: List<HabitCompletion>, today: LocalDate) {
    // Current week Monday to Sunday
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val weekDays = (0..6).map { monday.plusDays(it.toLong()) }
    val completedDates = allCompletions.map { it.dateString }.toSet()

    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(10.dp)
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        weekDays.forEachIndexed { index, date ->
            val isToday = date == today
            val hasCompletions = completedDates.contains(date.toString())

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayLabels[index],
                    style = TextStyle(
                        color = if (isToday) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = if (hasCompletions) "●" else "○",
                    style = TextStyle(
                        color = if (hasCompletions) GlanceTheme.colors.primary else GlanceTheme.colors.outline,
                        fontSize = 9.sp
                    )
                )
            }
        }
    }
}

@Composable
fun InteractiveHabitRow(habit: Habit) {
    val symbol = habit.icon.ifBlank { "📌" }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(8.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable(
                actionRunCallback<ToggleHabitAction>(
                    actionParametersOf(ToggleHabitAction.HABIT_ID_KEY to habit.id)
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⬜",
            style = TextStyle(fontSize = 12.sp)
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = symbol,
            style = TextStyle(fontSize = 12.sp)
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = habit.title,
            style = TextStyle(
                color = GlanceTheme.colors.onSecondaryContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
