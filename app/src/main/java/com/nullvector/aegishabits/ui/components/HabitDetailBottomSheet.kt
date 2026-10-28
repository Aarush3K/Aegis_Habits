package com.nullvector.aegishabits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nullvector.aegishabits.model.Habit
import com.nullvector.aegishabits.model.HabitCompletion
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailBottomSheet(
    habit: Habit,
    completions: List<HabitCompletion>,
    onDismiss: () -> Unit
) {
    val habitColor = Color(habit.color)

    // Calculate longest streak dynamically from completion date strings (YYYY-MM-DD)
    val longestStreak = remember(completions) {
        if (completions.isEmpty()) return@remember 0

        val dates = completions.mapNotNull {
            try { LocalDate.parse(it.dateString, DateTimeFormatter.ISO_LOCAL_DATE) }
            catch (e: Exception) { null }
        }.sorted()

        if (dates.isEmpty()) return@remember 0

        var maxStreak = 1
        var currentStreak = 1

        for (i in 1 until dates.size) {
            val prev = dates[i - 1]
            val curr = dates[i]
            if (curr.toEpochDay() == prev.toEpochDay() + 1) {
                currentStreak++
            } else if (curr.toEpochDay() != prev.toEpochDay()) {
                currentStreak = 1
            }
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak
            }
        }
        maxStreak
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.Start
        ) {
            // Header Row with Icon Badge, Title, and Longest Streak Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(habitColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = habit.icon, fontSize = 26.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Completion History",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Longest Streak Pill Badge
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🔥", fontSize = 14.sp)
                        Text(
                            text = "$longestStreak Best",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Heatmap Grid
            HabitCalendarHeatmap(
                habitColor = habitColor,
                completions = completions
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}