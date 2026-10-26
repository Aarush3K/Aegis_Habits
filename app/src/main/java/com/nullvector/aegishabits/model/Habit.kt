package com.nullvector.aegishabits.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val frequency: Frequency = Frequency(),
    val reminderTime: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val color: Int = 0xFF3B6FE0.toInt(),
    val icon: String = "💪"
)