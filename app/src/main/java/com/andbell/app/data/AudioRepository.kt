package com.andbell.app.data

import com.andbell.app.domain.model.AudioCategory
import com.andbell.app.domain.model.AudioItem
import kotlinx.coroutines.flow.StateFlow

interface AudioRepository {
    val departureBells: StateFlow<List<AudioItem>>
    val doorAnnouncements: StateFlow<List<AudioItem>>

    suspend fun initialize(includeBundledAudio: Boolean)
    suspend fun appendUserAudio(item: AudioItem)
    suspend fun removeAudio(id: String, category: AudioCategory)
    suspend fun renameAudio(id: String, newName: String, category: AudioCategory)
    suspend fun retrimAudio(id: String, trimStartMs: Long, trimEndMs: Long?, category: AudioCategory)
}
