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
import com.nullvector.aegishabits.model.Habit
import kotlinx.coroutines.flow.firstOrNull

class HabitWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)

        // Grab habit list snapshot safely
        val habits: List<Habit> = try {
            db.habitDao().getAllActiveHabits().firstOrNull() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                HabitWidgetContent(habits)
            }
        }
    }
}

@Composable
fun HabitWidgetContent(habits: List<Habit>) {
    // Outer Container Card with rounded corners
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(16.dp)
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Habit Count Badge
            Text(
                text = "${habits.size} active",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        // --- Content Body ---
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
                    text = "🎉 All done for today!",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                )
            }
        } else {
            // Display up to 4 habits nicely in a clean list
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                habits.take(4).forEachIndexed { index, habit ->
                    HabitItemRow(habitName = habit.title)

                    // Add a thin spacer divider between rows
                    if (index < habits.take(4).lastIndex) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                    }
                }

                // Indicator if there are more habits than fit on the widget card
                if (habits.size > 4) {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Text(
                        text = "+ ${habits.size - 4} more habits",
                        style = TextStyle(
                            color = GlanceTheme.colors.outline,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HabitItemRow(habitName: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(10.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox bullet point styling
        Text(
            text = "▫️",
            style = TextStyle(fontSize = 12.sp)
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        Text(
            text = habitName,
            style = TextStyle(
                color = GlanceTheme.colors.onSecondaryContainer,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}