package com.andbell.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.andbell.app.domain.model.AudioCategory
import com.andbell.app.domain.model.AudioItem
import com.andbell.app.domain.model.AudioSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.audioDataStore by preferencesDataStore(name = "audio_preferences")

data class StoredAudioState(
    val selectedBellId: String?,
    val selectedDoorId: String?,
    val userItems: List<AudioItem>,
)

class UserAudioStore(
    private val context: Context,
) {
    private val selectedBellKey = stringPreferencesKey("selected_bell_id")
    private val selectedDoorKey = stringPreferencesKey("selected_door_id")
    private val userItemsKey = stringPreferencesKey("user_items")

    fun observe(): Flow<StoredAudioState> = context.audioDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            StoredAudioState(
                selectedBellId = prefs[selectedBellKey],
                selectedDoorId = prefs[selectedDoorKey],
                userItems = decodeItems(prefs[userItemsKey]),
            )
        }

    suspend fun saveSelectedIds(selectedBellId: String?, selectedDoorId: String?) {
        context.audioDataStore.edit { prefs ->
            if (selectedBellId == null) {
                prefs.remove(selectedBellKey)
            } else {
                prefs[selectedBellKey] = selectedBellId
            }
            if (selectedDoorId == null) {
                prefs.remove(selectedDoorKey)
            } else {
                prefs[selectedDoorKey] = selectedDoorId
            }
        }
    }

    suspend fun saveUserItems(items: List<AudioItem>) {
        context.audioDataStore.edit { prefs ->
            prefs[userItemsKey] = encodeItems(items)
        }
    }

    private fun encodeItems(items: List<AudioItem>): String = items.joinToString(separator = "\n") { item ->
        val category = when (item.category) {
            AudioCategory.DepartureBell -> "bell"
            AudioCategory.DoorAnnouncement -> "door"
        }
        val safeName = item.name.replace("|", " ")
        val safeUri = item.uriOrResName.replace("|", "%7C")
        listOf(item.id, safeName, category, safeUri).joinToString(separator = "|")
    }

    private fun decodeItems(raw: String?): List<AudioItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw
            .split("\n")
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size < 4) return@mapNotNull null
                val category = when (parts[2]) {
                    "bell" -> AudioCategory.DepartureBell
                    "door" -> AudioCategory.DoorAnnouncement
                    else -> return@mapNotNull null
                }
                AudioItem(
                    id = parts[0],
                    name = parts[1],
                    category = category,
                    sourceType = AudioSourceType.UserUri,
                    uriOrResName = parts[3].replace("%7C", "|"),
                    isCustom = true,
                )
            }
    }
}
