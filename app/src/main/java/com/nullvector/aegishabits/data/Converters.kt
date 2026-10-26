package com.nullvector.aegishabits.data

import androidx.room.TypeConverter
import com.nullvector.aegishabits.model.Frequency
import com.nullvector.aegishabits.model.FrequencyType

class Converters {

    @TypeConverter
    fun fromFrequency(frequency: Frequency?): String {
        if (frequency == null) return "DAILY:7:"
        val daysString = frequency.customDaysOfWeek.joinToString(",")
        return "${frequency.type.name}:${frequency.targetDaysPerWeek}:$daysString"
    }

    @TypeConverter
    fun toFrequency(frequencyString: String?): Frequency {
        if (frequencyString.isNullOrBlank()) return Frequency(FrequencyType.DAILY, 7, emptyList())

        val parts = frequencyString.split(":")
        val type = try {
            FrequencyType.valueOf(parts[0])
        } catch (e: Exception) {
            FrequencyType.DAILY
        }
        val targetDays = parts.getOrNull(1)?.toIntOrNull() ?: 7
        val daysListStr = parts.getOrNull(2) ?: ""
        val daysList = if (daysListStr.isBlank()) {
            emptyList()
        } else {
            daysListStr.split(",").mapNotNull { it.toIntOrNull() }
        }

        return Frequency(
            type = type,
            targetDaysPerWeek = targetDays,
            customDaysOfWeek = daysList
        )
    }
}