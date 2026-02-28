package com.andbell.app.data

import com.andbell.app.domain.model.AudioCategory
import com.andbell.app.domain.model.AudioItem
import com.andbell.app.domain.model.AudioSourceType

object DefaultAudioData {
    fun departureBells(): List<AudioItem> = listOf(
        AudioItem(
            id = "melody-bell-1",
            name = "穏やかな旅立ち",
            category = AudioCategory.DepartureBell,
            sourceType = AudioSourceType.BundledTone,
            uriOrResName = "melody_departure_1",
            isCustom = false,
        ),
        AudioItem(
            id = "melody-bell-2",
            name = "クラシカルベル",
            category = AudioCategory.DepartureBell,
            sourceType = AudioSourceType.BundledTone,
            uriOrResName = "melody_departure_2",
            isCustom = false,
        ),
        AudioItem(
            id = "melody-bell-3",
            name = "キラキラステーション",
            category = AudioCategory.DepartureBell,
            sourceType = AudioSourceType.BundledTone,
            uriOrResName = "melody_departure_3",
            isCustom = false,
        ),
    )

    fun doorAnnouncements(): List<AudioItem> = listOf(
        AudioItem(
            id = "melody-door-1",
            name = "下降",
            category = AudioCategory.DoorAnnouncement,
            sourceType = AudioSourceType.BundledTone,
            uriOrResName = "melody_door_1",
            isCustom = false,
        ),
        AudioItem(
            id = "melody-door-2",
            name = "ピンポン",
            category = AudioCategory.DoorAnnouncement,
            sourceType = AudioSourceType.BundledTone,
            uriOrResName = "melody_door_2",
            isCustom = false,
        ),
        AudioItem(
            id = "melody-door-3",
            name = "やさしみ",
            category = AudioCategory.DoorAnnouncement,
            sourceType = AudioSourceType.BundledTone,
            uriOrResName = "melody_door_3",
            isCustom = false,
        ),
    )
}
