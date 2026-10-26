package com.nullvector.aegishabits.ui.habit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nullvector.aegishabits.data.AppDatabase
import com.nullvector.aegishabits.model.FrequencyType
import com.nullvector.aegishabits.model.Habit
import com.nullvector.aegishabits.repository.HabitRepository
import com.nullvector.aegishabits.ui.theme.AegisHabitsTheme
import com.nullvector.aegishabits.viewmodel.HabitUiItem
import com.nullvector.aegishabits.viewmodel.HabitUiState
import com.nullvector.aegishabits.viewmodel.HabitViewModel
import com.nullvector.aegishabits.viewmodel.HabitViewModelFactory

class HabitActivity : ComponentActivity() {

    private val viewModel: HabitViewModel by viewModels {
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = HabitRepository(db.habitDao(), db.completionDao())
        HabitViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AegisHabitsTheme {
                HabitScreen(viewModel)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Options offered in the "add habit" form
// ---------------------------------------------------------------------------

private val habitColors: List<Int> = listOf(
    0xFF3B6FE0.toInt(), 0xFF6FD3F5.toInt(), 0xFF8E7CF3.toInt(), 0xFF4CC38A.toInt(),
    0xFFF5B83D.toInt(), 0xFFF2766B.toInt(), 0xFFE86FB0.toInt(),
)

private val habitIcons = listOf("💪", "📚", "💧", "🏃", "🧘", "😴", "🍎", "✍️", "🎯", "💻")

private fun FrequencyType.label(): String = when (this) {
    FrequencyType.DAILY -> "Daily"
    FrequencyType.WEEKLY -> "Weekly"
    FrequencyType.CUSTOM -> "Custom"
}

private fun Habit.frequencyLabel(): String {
    return when (frequency.type) {
        FrequencyType.DAILY -> "Daily"
        FrequencyType.WEEKLY -> {
            val createdTime = if (createdAt > 0L) createdAt else System.currentTimeMillis()
            val dayName = java.text.SimpleDateFormat("EEEE", java.util.Locale.US).format(java.util.Date(createdTime))
            "Every $dayName"
        }
        FrequencyType.CUSTOM -> {
            if (frequency.customDaysOfWeek.isEmpty()) {
                "Today"
            } else {
                val dayMap = mapOf(
                    2 to "Mon", 3 to "Tue", 4 to "Wed",
                    5 to "Thu", 6 to "Fri", 7 to "Sat", 1 to "Sun"
                )
                val sortedDays = frequency.customDaysOfWeek.sortedBy { if (it == 1) 8 else it }
                sortedDays.mapNotNull { dayMap[it] }.joinToString(", ")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Main dashboard
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(viewModel: HabitViewModel) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Today", fontWeight = FontWeight.Bold) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                state.isLoading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.habits.isEmpty() ->
                    EmptyState(Modifier.align(Alignment.Center))

                else -> HabitList(
                    state = state,
                    onToggle = { habitId -> viewModel.toggleCompletion(habitId, context) },
                )
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color, icon, frequencyType, targetDays, customDays ->
                viewModel.addHabit(name, color, icon, frequencyType, targetDays, customDays, context)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun HabitList(
    state: HabitUiState,
    onToggle: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ProgressHeader(done = state.completedCount, total = state.totalCount) }
        items(state.habits, key = { it.habit.id }) { item ->
            HabitRow(item = item, onToggle = { onToggle(item.habit.id) })
        }
    }
}

@Composable
private fun ProgressHeader(done: Int, total: Int) {
    val fraction = if (total == 0) 0f else done.toFloat() / total
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "$done of $total done today",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
private fun HabitRow(item: HabitUiItem, onToggle: () -> Unit) {
    val habit = item.habit
    val habitColor = Color(habit.color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(habitColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(habit.icon, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                )
                Text(
                    text = habit.frequencyLabel() + streakText(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = habitColor),
            )
        }
    }
}

private fun streakText(item: HabitUiItem): String {
    if (item.streak <= 0) return ""
    val unit = if (item.habit.frequency.type == FrequencyType.WEEKLY) "week" else "day"
    val plural = if (item.streak == 1) "" else "s"
    return "  ·  ${item.streak} $unit$plural streak 🔥"
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🌱", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No habits for today",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tap + to add your first habit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------------------------------------------------------------------------
// "Add habit" form
// ---------------------------------------------------------------------------

@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Int, icon: String, frequencyType: FrequencyType, targetDays: Int, customDays: List<Int>) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableIntStateOf(habitColors.first()) }
    var icon by rememberSaveable { mutableStateOf(habitIcons.first()) }
    var frequencyType by rememberSaveable { mutableStateOf(FrequencyType.DAILY) }

    // Calendar mapping: 2 = Mon, 3 = Tue, 4 = Wed, 5 = Thu, 6 = Fri, 7 = Sat, 1 = Sun
    val daysMapping = listOf(
        "M" to 2, "T" to 3, "W" to 4, "T" to 5, "F" to 6, "S" to 7, "S" to 1
    )
    var selectedDays by rememberSaveable { mutableStateOf(listOf(2, 4, 6)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New habit") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Habit name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Frequency", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FrequencyType.entries.forEach { f ->
                        FilterChip(
                            selected = frequencyType == f,
                            onClick = { frequencyType = f },
                            label = { Text(f.label()) },
                        )
                    }
                }

                if (frequencyType == FrequencyType.CUSTOM) {
                    Column {
                        Text("Repeat on days", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            daysMapping.forEach { (label, dayCode) ->
                                val isSelected = dayCode in selectedDays
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedDays = if (isSelected) {
                                            selectedDays - dayCode
                                        } else {
                                            selectedDays + dayCode
                                        }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                Text("Colour", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(habitColors) { c ->
                        val selected = c == color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .then(
                                    if (selected) Modifier.border(
                                        3.dp, MaterialTheme.colorScheme.onSurface, CircleShape
                                    ) else Modifier
                                )
                                .clickable { color = c }
                        )
                    }
                }

                Text("Icon", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(habitIcons) { i ->
                        val selected = i == icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { icon = i },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(i, fontSize = 22.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val targetDays = if (frequencyType == FrequencyType.CUSTOM) selectedDays.size else 7
                    val customDays = if (frequencyType == FrequencyType.CUSTOM) selectedDays else emptyList()
                    onConfirm(name, color, icon, frequencyType, targetDays, customDays)
                },
                enabled = name.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
