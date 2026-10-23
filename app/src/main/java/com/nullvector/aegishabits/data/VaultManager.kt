package com.nullvector.aegishabits.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nullvector.aegishabits.model.Habit
import com.nullvector.aegishabits.model.HabitCompletion
import com.nullvector.aegishabits.repository.HabitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Data class representing the backup file schema structure for export/import.
 */
data class VaultBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val habits: List<Habit>,
    val completions: List<HabitCompletion>
)

class VaultManager(
    private val context: Context,
    private val repository: HabitRepository,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
) {

    /**
     * Exports all habits and completions into a JSON string and writes it to internal storage.
     */
    suspend fun exportVault(): String = withContext(Dispatchers.IO) {
        val habits = repository.allHabits.first()
        val completions = repository.getAllCompletions()

        val backupData = VaultBackup(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            habits = habits,
            completions = completions
        )

        val jsonString = gson.toJson(backupData)

        // Write to internal file storage
        val backupFile = File(context.filesDir, "aegis_vault_backup.json")
        backupFile.writeText(jsonString)

        return@withContext jsonString
    }

    /**
     * Validates and restores data from a JSON string into the Room database.
     */
    suspend fun restoreVault(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Schema Validation check
            if (!validateSchema(jsonString)) {
                return@withContext false
            }

            // 2. Parse backup payload
            val backupData = gson.fromJson(jsonString, VaultBackup::class.java)
                ?: return@withContext false

            // 3. Restore records into database
            for (habit in backupData.habits) {
                repository.insertHabit(habit)
            }
            for (completion in backupData.completions) {
                repository.insertCompletion(completion)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Performs strict structural schema validation on raw JSON string before restore.
     */
    fun validateSchema(jsonString: String): Boolean {
        return try {
            val jsonMap = gson.fromJson(jsonString, Map::class.java) ?: return false
            jsonMap.containsKey("version") &&
                    jsonMap.containsKey("habits") &&
                    jsonMap.containsKey("completions")
        } catch (e: Exception) {
            false
        }
    }
}