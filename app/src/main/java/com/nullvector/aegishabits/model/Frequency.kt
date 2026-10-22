package com.nullvector.aegishabits.model

enum class FrequencyType {
    DAILY,
    WEEKLY,
    CUSTOM
}

data class Frequency(
    val type: FrequencyType = FrequencyType.DAILY,
    val targetDaysPerWeek: Int = 7,
    val customDaysOfWeek: List<Int> = emptyList() // e.g., 1 = Monday, 7 = Sunday
)
