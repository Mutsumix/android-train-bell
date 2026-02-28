package com.andbell.app.data

import com.andbell.app.domain.model.AudioCategory
import com.andbell.app.domain.model.AudioItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryAudioRepository : AudioRepository {
    private val _departureBells = MutableStateFlow<List<AudioItem>>(emptyList())
    private val _doorAnnouncements = MutableStateFlow<List<AudioItem>>(emptyList())

    override val departureBells: StateFlow<List<AudioItem>> = _departureBells.asStateFlow()
    override val doorAnnouncements: StateFlow<List<AudioItem>> = _doorAnnouncements.asStateFlow()

    override suspend fun initialize(includeBundledAudio: Boolean) {
        if (includeBundledAudio) {
            _departureBells.value = DefaultAudioData.departureBells()
            _doorAnnouncements.value = DefaultAudioData.doorAnnouncements()
        } else {
            _departureBells.value = emptyList()
            _doorAnnouncements.value = emptyList()
        }
    }

    override suspend fun appendUserAudio(item: AudioItem) {
        when (item.category) {
            AudioCategory.DepartureBell -> _departureBells.value = _departureBells.value + item
            AudioCategory.DoorAnnouncement -> _doorAnnouncements.value = _doorAnnouncements.value + item
        }
    }

    override suspend fun removeAudio(id: String, category: AudioCategory) {
        when (category) {
            AudioCategory.DepartureBell -> _departureBells.value = _departureBells.value.filterNot { it.id == id }
            AudioCategory.DoorAnnouncement -> _doorAnnouncements.value = _doorAnnouncements.value.filterNot { it.id == id }
        }
    }

    override suspend fun renameAudio(id: String, newName: String, category: AudioCategory) {
        when (category) {
            AudioCategory.DepartureBell -> _departureBells.value = _departureBells.value.map {
                if (it.id == id) it.copy(name = newName) else it
            }
            AudioCategory.DoorAnnouncement -> _doorAnnouncements.value = _doorAnnouncements.value.map {
                if (it.id == id) it.copy(name = newName) else it
            }
        }
    }

    override suspend fun retrimAudio(id: String, trimStartMs: Long, trimEndMs: Long?, category: AudioCategory) {
        when (category) {
            AudioCategory.DepartureBell -> _departureBells.value = _departureBells.value.map {
                if (it.id == id) it.copy(trimStartMs = trimStartMs, trimEndMs = trimEndMs) else it
            }
            AudioCategory.DoorAnnouncement -> _doorAnnouncements.value = _doorAnnouncements.value.map {
                if (it.id == id) it.copy(trimStartMs = trimStartMs, trimEndMs = trimEndMs) else it
            }
        }
    }
}
