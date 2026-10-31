package com.nullvector.aegishabits.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nullvector.aegishabits.viewmodel.DailyCompletionPoint

@Composable
fun ActivityHeatmapCard(
    modifier: Modifier = Modifier,
    dailyPoints: List<DailyCompletionPoint> = emptyList()
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Consistency Heatmap",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Render up to the last 28 days (7 cols x 4 rows)
            val recentPoints = dailyPoints.takeLast(28)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(7) { col ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(4) { row ->
                            val index = col * 4 + row
                            val point = recentPoints.getOrNull(index)

                            val intensity = when {
                                point == null -> 0
                                point.completionRatio <= 0f -> 0
                                point.completionRatio <= 0.4f -> 1
                                point.completionRatio <= 0.8f -> 2
                                else -> 3
                            }

                            val boxColor = when (intensity) {
                                0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(boxColor)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Past 28 days activity overview",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}