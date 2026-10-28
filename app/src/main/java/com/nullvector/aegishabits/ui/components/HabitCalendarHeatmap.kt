package com.nullvector.aegishabits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nullvector.aegishabits.model.HabitCompletion
import java.time.YearMonth
import java.util.Locale

@Composable
fun HabitCalendarHeatmap(
    habitColor: Color,
    completions: List<HabitCompletion>,
    modifier: Modifier = Modifier
) {
    // Collect all completion date strings into a set for fast lookup
    val completedDates = remember(completions) {
        completions.map { it.dateString }.toSet()
    }

    // Get current year/month layout details
    val currentMonth = remember { YearMonth.now() }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7 // 0 = Sunday

    val dayHeaders = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(modifier = modifier.fillMaxWidth()) {
        // Weekday Headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayHeaders.forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Heatmap Calendar Grid
        val totalGridSlots = firstDayOfWeek + daysInMonth
        val rows = (totalGridSlots + 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (rowIndex in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (colIndex in 0 until 7) {
                        val dayNumber = (rowIndex * 7 + colIndex) - firstDayOfWeek + 1
                        val isValidDay = dayNumber in 1..daysInMonth

                        if (isValidDay) {
                            val dateKey = String.format(
                                Locale.US,
                                "%04d-%02d-%02d",
                                currentMonth.year,
                                currentMonth.monthValue,
                                dayNumber
                            )
                            val isCompleted = dateKey in completedDates

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isCompleted) habitColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            // Empty slot for days outside the current month
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}