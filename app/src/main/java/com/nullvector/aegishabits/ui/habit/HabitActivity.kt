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
import androidx.compose.material.icons.filled.Analytics
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Surface
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.IconButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.Dp
import com.nullvector.aegishabits.ui.components.HabitDetailBottomSheet
import com.nullvector.aegishabits.viewmodel.AnalyticsViewModel
import com.nullvector.aegishabits.viewmodel.AnalyticsViewModelFactory
import com.nullvector.aegishabits.ui.analytics.AnalyticsScreen

class HabitActivity : ComponentActivity() {

    private val viewModel: HabitViewModel by viewModels {
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = HabitRepository(db.habitDao(), db.completionDao())
        HabitViewModelFactory(repository, applicationContext)
    }

    private val analyticsViewModel: AnalyticsViewModel by viewModels {
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = HabitRepository(db.habitDao(), db.completionDao())
        AnalyticsViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPreferences = getSharedPreferences("aegis_habits_prefs", MODE_PRIVATE)
        val isSystemDark = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.app.UiModeManager.MODE_NIGHT_YES
        val isDarkModeSaved = sharedPreferences.getBoolean("is_dark_mode", isSystemDark)

        window.decorView.setBackgroundColor(
            if (isDarkModeSaved) android.graphics.Color.parseColor("#121212")
            else android.graphics.Color.parseColor("#FFFBFE")
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(isDarkModeSaved) }

            AegisHabitsTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HabitScreen(
                        viewModel = viewModel,
                        analyticsViewModel = analyticsViewModel,
                        isDarkMode = isDarkMode,
                        onToggleTheme = {
                            isDarkMode = !isDarkMode
                            sharedPreferences.edit().putBoolean("is_dark_mode", isDarkMode).apply()
                        }
                    )
                }
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
// Habit Screen (with 4-Tab Bottom Navigation & Pager)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    viewModel: HabitViewModel,
    analyticsViewModel: AnalyticsViewModel,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detailUiState by viewModel.detailUiState.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var selectedHabitForAction by remember { mutableStateOf<HabitUiItem?>(null) }
    var habitToEdit by remember { mutableStateOf<HabitUiItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showConfetti by remember { mutableStateOf(false) }
    var hasCelebratedToday by rememberSaveable { mutableStateOf(false) }
    var userJustToggled by remember { mutableStateOf(false) }

    val isAllDoneToday = state.todayTotalCount > 0 && state.todayCompletedCount == state.todayTotalCount

    LaunchedEffect(state.todayCompletedCount) {
        if (userJustToggled && isAllDoneToday && !hasCelebratedToday) {
            showConfetti = true
            hasCelebratedToday = true
        } else if (!isAllDoneToday) {
            hasCelebratedToday = false
        }
        userJustToggled = false
    }

    val targetBackgroundColor = MaterialTheme.colorScheme.background
    val animatedBgColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "theme_fade"
    )

    LaunchedEffect(Unit) {
        viewModel.refreshDate()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDate()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(animatedBgColor)
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.selectedTab.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = onToggleTheme) {
                            AnimatedContent(
                                targetState = isDarkMode,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f)) togetherWith
                                            (fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f))
                                },
                                label = "theme_icon_transition"
                            ) { dark ->
                                Icon(
                                    imageVector = if (dark) Icons.Default.WbSunny else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Dark Mode"
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    HabitTab.entries.forEach { tab ->
                        val isSelected = tab == state.selectedTab
                        val icon = when (tab) {
                            HabitTab.TODAY -> Icons.Default.Today
                            HabitTab.ALL -> Icons.Default.List
                            HabitTab.ANALYTICS -> Icons.Default.Analytics
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
                // Hide FAB on the Analytics tab
                if (state.selectedTab != HabitTab.ANALYTICS) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val pageTab = HabitTab.entries[page]

                    when (pageTab) {
                        HabitTab.ANALYTICS -> {
                            AnalyticsScreen(viewModel = analyticsViewModel)
                        }
                        else -> {
                            val pageHabits = when (pageTab) {
                                HabitTab.TODAY -> state.todayHabits
                                HabitTab.ALL -> state.allHabitsList
                                HabitTab.ARCHIVED -> state.archivedHabitsList
                                else -> emptyList()
                            }

                            val pageState = state.copy(
                                habits = pageHabits,
                                selectedTab = pageTab
                            )

                            Box(modifier = Modifier.fillMaxSize()) {
                                when {
                                    state.isLoading && pageHabits.isEmpty() ->
                                        CircularProgressIndicator(Modifier.align(Alignment.Center))

                                    pageHabits.isEmpty() -> {
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)),
                                            modifier = Modifier.align(Alignment.Center)
                                        ) {
                                            EmptyState(tab = pageTab)
                                        }
                                    }

                                    else -> HabitList(
                                        state = pageState,
                                        onToggle = { habitId ->
                                            userJustToggled = true
                                            viewModel.toggleCompletion(habitId)
                                        },
                                        onItemLongClick = { habitItem -> selectedHabitForAction = habitItem },
                                        onHabitClick = { habit -> viewModel.openHabitDetail(habit) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showConfetti) {
                    ConfettiEffect(modifier = Modifier.fillMaxSize())
                }
            }
        }
        detailUiState.selectedHabit?.let { habit ->
            HabitDetailBottomSheet(
                habit = habit,
                completions = detailUiState.completions,
                onDismiss = {
                    viewModel.closeHabitDetail()
                }
            )
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color, icon, frequencyType, targetDays, customDays, reminderTime ->
                viewModel.addHabit(name, color, icon, frequencyType, targetDays, customDays, reminderTime)
                showAddDialog = false
            },
        )
    }

    habitToEdit?.let { item ->
        AddHabitDialog(
            initialHabit = item.habit,
            onDismiss = { habitToEdit = null },
            onConfirm = { name, color, icon, frequencyType, targetDays, customDays, reminderTime ->
                val updatedHabit = item.habit.copy(
                    title = name,
                    color = color,
                    icon = icon,
                    frequency = item.habit.frequency.copy(
                        type = frequencyType,
                        customDaysOfWeek = customDays
                    ),
                    reminderTime = reminderTime
                )
                viewModel.updateHabit(updatedHabit)
                habitToEdit = null
            }
        )
    }

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

// ---------------------------------------------------------------------------
// Supporting Components & Helpers
// ---------------------------------------------------------------------------

data class ParticleParams(
    val startX: Float,
    val size: Dp,
    val color: Color,
    val speed: Int,
    val delay: Int
)

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 50
) {
    val colors = listOf(
        Color(0xFF3B6FE0),
        Color(0xFF4CC38A),
        Color(0xFFF5B83D),
        Color(0xFFF2766B),
        Color(0xFF8E7CF3),
        Color(0xFFE86FB0)
    )

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val effectiveWidth = if (screenWidthPx > 0f) screenWidthPx else 1080f
    val effectiveHeight = if (screenHeightPx > 0f) screenHeightPx else 1920f

    val particles = remember {
        List(particleCount) {
            ParticleParams(
                startX = (0..effectiveWidth.toInt()).random().toFloat(),
                size = (12..20).random().dp,
                color = colors.random(),
                speed = (1500..2500).random(),
                delay = (0..400).random()
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            ParticleItem(
                startX = p.startX,
                particleSize = p.size,
                color = p.color,
                speed = p.speed,
                delay = p.delay,
                screenHeightPx = effectiveHeight
            )
        }
    }
}

@Composable
fun ParticleItem(
    startX: Float,
    particleSize: Dp,
    color: Color,
    speed: Int,
    delay: Int,
    screenHeightPx: Float
) {
    val animatableProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        animatableProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = speed, easing = LinearOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                val progress = animatableProgress.value
                translationX = startX
                translationY = progress * screenHeightPx
                alpha = (1f - (progress * 0.8f)).coerceIn(0f, 1f)
                rotationZ = progress * 360f
            }
            .size(particleSize)
            .background(color, CircleShape)
    )
}

@Composable
private fun HabitList(
    state: HabitUiState,
    onToggle: (Long) -> Unit,
    onItemLongClick: (HabitUiItem) -> Unit,
    onHabitClick: (Habit) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.selectedTab == HabitTab.TODAY) {
            item(key = "progress_header") {
                ProgressHeader(done = state.completedCount, total = state.totalCount)
            }
        }

        items(
            items = state.habits,
            key = { item -> item.habit.id }
        ) { item ->
            HabitRow(
                item = item,
                selectedTab = state.selectedTab,
                onToggle = { onToggle(item.habit.id) },
                onLongClick = { onItemLongClick(item) },
                onClick = { onHabitClick(item.habit) }
            )
        }
    }
}

@Composable
private fun ProgressHeader(done: Int, total: Int) {
    val targetFraction = if (total == 0) 0f else done.toFloat() / total

    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress_animation"
    )

    Column(Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "$done of $total done today",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
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
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val habit = item.habit
    val habitColor = Color(habit.color)
    val isTodayView = selectedTab == HabitTab.TODAY
    val view = LocalView.current

    var localCompleted by remember(item.isCompleted) { mutableStateOf(item.isCompleted) }

    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )

    val checkboxScale by animateFloatAsState(
        targetValue = if (localCompleted) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkbox_scale"
    )

    val streakAlpha by animateFloatAsState(
        targetValue = if (item.streak > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "streak_fade"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() }
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (isTodayView && localCompleted) TextDecoration.LineThrough else null,
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = habit.frequencyLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = streakAlpha
                        }
                    ) {
                        Text(
                            text = streakText(item),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (isTodayView) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = checkboxScale
                        scaleY = checkboxScale
                    }
                ) {
                    Checkbox(
                        checked = localCompleted,
                        onCheckedChange = { checked ->
                            localCompleted = checked
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onToggle()
                        },
                        colors = CheckboxDefaults.colors(checkedColor = habitColor),
                    )
                }
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
        HabitTab.ANALYTICS -> "📊" to "Analytics loading..."
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
        if (tab != HabitTab.ARCHIVED && tab != HabitTab.ANALYTICS) {
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

@Composable
private fun AddHabitDialog(
    initialHabit: Habit? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Int, icon: String, frequencyType: FrequencyType, targetDays: Int, customDays: List<Int>, reminderTime: String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialHabit?.title ?: "") }
    var color by rememberSaveable { mutableIntStateOf(initialHabit?.color ?: habitColors.first()) }
    var icon by rememberSaveable { mutableStateOf(initialHabit?.icon ?: habitIcons.first()) }
    var frequencyType by rememberSaveable {
        mutableStateOf(initialHabit?.frequency?.type ?: FrequencyType.DAILY)
    }

    var isReminderEnabled by rememberSaveable { mutableStateOf(!initialHabit?.reminderTime.isNullOrBlank()) }
    var reminderTime by rememberSaveable { mutableStateOf(initialHabit?.reminderTime ?: "08:00") }
    var showTimePicker by remember { mutableStateOf(false) }

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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(daysMapping) { (label, dayCode) ->
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

                // Determine reminder label dynamically based on selected frequency
                val reminderLabelText = when (frequencyType) {
                    FrequencyType.DAILY -> "Daily Reminder"
                    FrequencyType.WEEKLY -> "Weekly Reminder"
                    FrequencyType.CUSTOM -> "Reminder"
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(reminderLabelText, style = MaterialTheme.typography.bodyMedium)
                    androidx.compose.material3.Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { isReminderEnabled = it }
                    )
                }

                if (isReminderEnabled) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remind at: $reminderTime")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val finalReminderTime = if (isReminderEnabled) reminderTime else null
                        onConfirm(name, color, icon, frequencyType, 1, selectedDays.toList(), finalReminderTime)
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

    if (showTimePicker) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val parts = reminderTime.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        android.app.TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                reminderTime = String.format("%02d:%02d", hour, minute)
                showTimePicker = false
            },
            initialHour,
            initialMinute,
            true
        ).apply {
            setOnCancelListener { showTimePicker = false }
        }.show()
    }
}