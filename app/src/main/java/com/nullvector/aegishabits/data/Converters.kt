package com.nullvector.aegishabits.data

import androidx.room.TypeConverter
import com.nullvector.aegishabits.model.Frequency
import com.nullvector.aegishabits.model.FrequencyType

class Converters {

    @TypeConverter
    fun fromFrequency(frequency: Frequency?): String {
        return frequency?.type?.name ?: FrequencyType.DAILY.name
    }

    @TypeConverter
    fun toFrequency(frequencyString: String?): Frequency {
        val type = try {
            if (frequencyString != null) FrequencyType.valueOf(frequencyString) else FrequencyType.DAILY
        } catch (e: Exception) {
            FrequencyType.DAILY
        }
        return Frequency(type = type)
    }
}