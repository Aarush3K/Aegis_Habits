package com.nullvector.aegishabits.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nullvector.aegishabits.viewmodel.AnalyticsViewModel

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBackClick: () -> Unit = {}
) {
    val timeRange by viewModel.timeRangeDays.collectAsState()
    val overallRate by viewModel.overallCompletionRate.collectAsState()
    val bestStreak by viewModel.bestStreak.collectAsState()
    val dailyPoints by viewModel.dailyPoints.collectAsState()
    val correlations by viewModel.correlations.collectAsState()
    val habitStats by viewModel.habitStats.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Screen Title
        item {
            Text(
                text = "Stats & Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Time Range Selector Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(7L to "Last 7 Days", 30L to "Last 30 Days", 90L to "Last 90 Days").forEach { (days, label) ->
                    FilterChip(
                        selected = timeRange == days,
                        onClick = { viewModel.setTimeRange(days) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // KPI Grid Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Completion Rate",
                    value = "%.1f%%".format(overallRate),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Best Streak",
                    value = "$bestStreak Days",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Dynamic GitHub-Style Activity Heatmap Card
        item {
            ActivityHeatmapCard(dailyPoints = dailyPoints)
        }

        // Dynamic Row of Circular Progress Cards (Real Habits)
        item {
            if (habitStats.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No active habits found. Add habits to view progress rings!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    habitStats.take(2).forEach { stat ->
                        Box(modifier = Modifier.weight(1f)) {
                            CircularProgressCard(
                                title = stat.habit.title,
                                percentage = stat.completionRate,
                                streakText = "${stat.currentStreak} day streak 🔥"
                            )
                        }
                    }
                }
            }
        }

        // Daily Consistency Trend Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Consistency Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (dailyPoints.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No data available for this range.")
                        }
                    } else {
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val surfaceVariant = MaterialTheme.colorScheme.outlineVariant

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            val barWidth = size.width / (dailyPoints.size * 1.5f)
                            val spacing = barWidth * 0.5f
                            var xOffset = spacing

                            dailyPoints.forEach { point ->
                                val barHeight = size.height * point.completionRatio
                                val yTop = size.height - barHeight

                                drawRect(
                                    color = surfaceVariant.copy(alpha = 0.3f),
                                    topLeft = Offset(xOffset, 0f),
                                    size = Size(barWidth, size.height)
                                )

                                drawRoundRect(
                                    color = primaryColor,
                                    topLeft = Offset(xOffset, yTop),
                                    size = Size(barWidth, barHeight.coerceAtLeast(4f)),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                                )

                                xOffset += barWidth + spacing
                            }
                        }
                    }
                }
            }
        }

        // Smart Habit Correlation Section
        item {
            Text(
                text = "💡 Habit Correlation Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (correlations.isEmpty()) {
            item {
                Text(
                    text = "Complete more habits consistently to unlock automated behavior correlation insights!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(correlations) { correlation ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "When you complete '${correlation.habitA.title}'...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your success rate for '${correlation.habitB.title}' increases by +${"%.0f".format(correlation.liftPercentage)}%!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}