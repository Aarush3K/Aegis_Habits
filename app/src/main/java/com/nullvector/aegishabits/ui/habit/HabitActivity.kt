package com.nullvector.aegishabits.ui.habit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.nullvector.aegishabits.viewmodel.HabitTab
import com.nullvector.aegishabits.viewmodel.HabitUiItem
import com.nullvector.aegishabits.viewmodel.HabitUiState
import com.nullvector.aegishabits.viewmodel.HabitViewModel
import com.nullvector.aegishabits.viewmodel.HabitViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

class HabitActivity : ComponentActivity() {

    private val viewModel: HabitViewModel by viewModels {
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = HabitRepository(db.habitDao(), db.completionDao())
        HabitViewModelFactory(repository, applicationContext)
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var selectedHabitForAction by remember { mutableStateOf<HabitUiItem?>(null) }
    var habitToEdit by remember { mutableStateOf<HabitUiItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Inside HabitScreen:
    LaunchedEffect(Unit) {
        viewModel.refreshDate()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDate() // Triggers date refresh whenever app comes to foreground
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = state.selectedTab.ordinal,
        pageCount = { HabitTab.entries.size }
    )

    // Sync Pager state when ViewModel tab changes externally
    LaunchedEffect(state.selectedTab) {
        if (pagerState.currentPage != state.selectedTab.ordinal) {
            pagerState.animateScrollToPage(state.selectedTab.ordinal)
        }
    }

    // Sync ViewModel tab when user swipes the pager
    LaunchedEffect(pagerState.currentPage) {
        val targetTab = HabitTab.entries[pagerState.currentPage]
        if (targetTab != state.selectedTab) {
            viewModel.selectTab(targetTab)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.selectedTab.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                HabitTab.entries.forEach { tab ->
                    val isSelected = tab == state.selectedTab
                    val icon: ImageVector = when (tab) {
                        HabitTab.TODAY -> Icons.Default.Today
                        HabitTab.ALL -> Icons.Default.List
                        HabitTab.ARCHIVED -> Icons.Default.Archive
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tab.ordinal)
                            }
                        },
                        icon = {
                            Icon(imageVector = icon, contentDescription = tab.title)
                        },
                        label = {
                            Text(tab.title)
                        }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) { page ->
            val pageTab = HabitTab.entries[page]

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))

                    state.habits.isEmpty() ->
                        EmptyState(tab = pageTab, modifier = Modifier.align(Alignment.Center))

                    else -> HabitList(
                        state = state,
                        onToggle = { habitId -> viewModel.toggleCompletion(habitId) },
                        onItemLongClick = { habitItem -> selectedHabitForAction = habitItem }
                    )
                }
            }
        }
    }

    // Add Habit Dialog
    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color, icon, frequencyType, targetDays, customDays ->
                viewModel.addHabit(name, color, icon, frequencyType, targetDays, customDays)
                showAddDialog = false
            },
        )
    }

    // Edit Habit Dialog
    habitToEdit?.let { item ->
        AddHabitDialog(
            initialHabit = item.habit,
            onDismiss = { habitToEdit = null },
            onConfirm = { name, color, icon, frequencyType, targetDays, customDays ->
                val updatedHabit = item.habit.copy(
                    title = name,
                    color = color,
                    icon = icon,
                    frequency = item.habit.frequency.copy(
                        type = frequencyType,
                        customDaysOfWeek = customDays
                    )
                )
                viewModel.updateHabit(updatedHabit)
                habitToEdit = null
            }
        )
    }

    // Long Press Action Dialog
    selectedHabitForAction?.let { item ->
        HabitActionDialog(
            item = item,
            onDismiss = { selectedHabitForAction = null },
            onEdit = {
                habitToEdit = item
                selectedHabitForAction = null
            },
            onArchive = {
                viewModel.archiveHabit(item.habit)
                selectedHabitForAction = null
            },
            onRestore = {
                viewModel.restoreHabit(item.habit)
                selectedHabitForAction = null
            },
            onDelete = {
                viewModel.deleteHabit(item.habit)
                selectedHabitForAction = null
            }
        )
    }
}

@Composable
private fun HabitList(
    state: HabitUiState,
    onToggle: (Long) -> Unit,
    onItemLongClick: (HabitUiItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.selectedTab == HabitTab.TODAY) {
            item { ProgressHeader(done = state.completedCount, total = state.totalCount) }
        }
        items(state.habits, key = { it.habit.id }) { item ->
            HabitRow(
                item = item,
                selectedTab = state.selectedTab,
                onToggle = { onToggle(item.habit.id) },
                onLongClick = { onItemLongClick(item) }
            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitRow(
    item: HabitUiItem,
    selectedTab: HabitTab,
    onToggle: () -> Unit,
    onLongClick: () -> Unit
) {
    val habit = item.habit
    val habitColor = Color(habit.color)
    val isTodayView = selectedTab == HabitTab.TODAY

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick
            ),
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
                    textDecoration = if (isTodayView && item.isCompleted) TextDecoration.LineThrough else null,
                )
                Text(
                    text = habit.frequencyLabel() + streakText(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isTodayView) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = habitColor),
                )
            }
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
private fun EmptyState(tab: HabitTab, modifier: Modifier = Modifier) {
    val (emoji, title) = when (tab) {
        HabitTab.TODAY -> "🌱" to "No habits for today"
        HabitTab.ALL -> "📋" to "No active habits"
        HabitTab.ARCHIVED -> "📦" to "No archived habits"
    }

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        if (tab != HabitTab.ARCHIVED) {
            Text(
                text = "Tap + to add your first habit.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HabitActionDialog(
    item: HabitUiItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val isArchived = item.habit.isArchived

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.habit.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Edit Option
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Habit", fontSize = 16.sp)
                }

                if (isArchived) {
                    TextButton(
                        onClick = onRestore,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore Habit", fontSize = 16.sp)
                    }
                } else {
                    TextButton(
                        onClick = onArchive,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Archive Habit", fontSize = 16.sp)
                    }
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
// ---------------------------------------------------------------------------
// "Add habit" form
// ---------------------------------------------------------------------------

@Composable
private fun AddHabitDialog(
    initialHabit: Habit? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Int, icon: String, frequencyType: FrequencyType, targetDays: Int, customDays: List<Int>) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialHabit?.title ?: "") }
    var color by rememberSaveable { mutableIntStateOf(initialHabit?.color ?: habitColors.first()) }
    var icon by rememberSaveable { mutableStateOf(initialHabit?.icon ?: habitIcons.first()) }
    var frequencyType by rememberSaveable {
        mutableStateOf(initialHabit?.frequency?.type ?: FrequencyType.DAILY)
    }

    val daysMapping = listOf(
        "M" to 2, "T" to 3, "W" to 4, "T" to 5, "F" to 6, "S" to 7, "S" to 1
    )
    val selectedDays = remember {
        mutableStateListOf<Int>().apply {
            addAll(initialHabit?.frequency?.customDaysOfWeek ?: listOf(2, 4, 6))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialHabit == null) "New Habit" else "Edit Habit") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon", style = MaterialTheme.typography.bodyMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(habitIcons) { itemIcon ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (icon == itemIcon) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { icon = itemIcon },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(itemIcon, fontSize = 20.sp)
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.bodyMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(habitColors) { itemColor ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(itemColor))
                                .border(
                                    width = if (color == itemColor) 3.dp else 0.dp,
                                    color = if (color == itemColor) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { color = itemColor }
                        )
                    }
                }

                Text("Frequency", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FrequencyType.entries.forEach { type ->
                        FilterChip(
                            selected = frequencyType == type,
                            onClick = { frequencyType = type },
                            label = { Text(type.label()) }
                        )
                    }
                }

                if (frequencyType == FrequencyType.CUSTOM) {
                    Text("Select Days", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        daysMapping.forEach { (label, dayCode) ->
                            val isSelected = selectedDays.contains(dayCode)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedDays.remove(dayCode)
                                    else selectedDays.add(dayCode)
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, color, icon, frequencyType, 1, selectedDays.toList())
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}