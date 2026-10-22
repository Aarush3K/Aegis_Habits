package com.nullvector.aegishabits.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.nullvector.aegishabits.model.Frequency

class Converters {

    @TypeConverter
    fun fromFrequency(frequency: Frequency): String {
        return Gson().toJson(frequency)
    }

    @TypeConverter
    fun toFrequency(frequencyString: String): Frequency {
        return Gson().fromJson(frequencyString, Frequency::class.java)
    }
}