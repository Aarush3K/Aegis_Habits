package com.nullvector.aegishabits.viewmodel

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nullvector.aegishabits.model.Habit
import com.nullvector.aegishabits.model.HabitCompletion
import com.nullvector.aegishabits.repository.HabitRepository
import com.nullvector.aegishabits.ui.widget.HabitWidget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(private val repository: HabitRepository) : ViewModel() {

    // 1. Expose active habits as a StateFlow for Compose UI screens
    val habits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. User Actions: Insert a new habit
    fun addHabit(habit: Habit, context: Context? = null) {
        viewModelScope.launch {
            repository.insertHabit(habit)
            context?.let { HabitWidget().updateAll(it) }
        }
    }

    // 3. User Actions: Update an existing habit
    fun updateHabit(habit: Habit, context: Context? = null) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            context?.let { HabitWidget().updateAll(it) }
        }
    }

    // 4. User Actions: Delete a habit
    fun deleteHabit(habit: Habit, context: Context? = null) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            context?.let { HabitWidget().updateAll(it) }
        }
    }

    // 5. User Actions: Toggle completion / log check-in
    fun toggleCompletion(completion: HabitCompletion, isCompleted: Boolean, context: Context? = null) {
        viewModelScope.launch {
            if (isCompleted) {
                repository.insertCompletion(completion)
            } else {
                repository.deleteCompletion(completion)
            }
            context?.let { HabitWidget().updateAll(it) }
        }
    }
}