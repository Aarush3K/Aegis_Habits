package com.nullvector.aegishabits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nullvector.aegishabits.model.Habit
import com.nullvector.aegishabits.model.HabitCompletion
import com.nullvector.aegishabits.repository.HabitRepository
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
    fun addHabit(habit: Habit) {
        viewModelScope.launch {
            repository.insertHabit(habit)
        }
    }

    // 3. User Actions: Update an existing habit
    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
        }
    }

    // 4. User Actions: Delete a habit
    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    // 5. User Actions: Toggle completion / log check-in
    fun toggleCompletion(completion: HabitCompletion, isCompleted: Boolean) {
        viewModelScope.launch {
            if (isCompleted) {
                repository.insertCompletion(completion)
            } else {
                repository.deleteCompletion(completion)
            }
        }
    }
}