package com.andbell.app.domain.model

enum class AudioCategory {
    DepartureBell,
    DoorAnnouncement,
}

enum class AudioSourceType {
    BundledTone,
    UserUri,
}

data class AudioItem(
    val id: String,
    val name: String,
    val category: AudioCategory,
    val sourceType: AudioSourceType,
    val uriOrResName: String,
    val isCustom: Boolean,
)
