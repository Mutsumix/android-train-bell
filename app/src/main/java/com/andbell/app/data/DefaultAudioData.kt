package com.andbell.app.data

import com.andbell.app.domain.model.AudioCategory
import com.andbell.app.domain.model.AudioItem
import com.andbell.app.domain.model.AudioSourceType

object DefaultAudioData {
    fun departureBells(): List<AudioItem> = listOf(
        AudioItem(
            id = "sample-bell-1",
            name = "サンプル 発車ベル 1",
            category = AudioCategory.DepartureBell,
            sourceType = AudioSourceType.BundledTone,
            uriOrResName = "tone_dtmf_1",
            isCustom = false,
        ),
        AudioItem(
            id = "sample-bell-2",
            name = "サンプル 発車ベル 2",
            category = AudioCategory.DepartureBell,
            sourceType = AudioSourceType.BundledTone,
            uriOrResName = "tone_dtmf_3",
            isCustom = false,
        ),
    )

    fun doorAnnouncements(): List<AudioItem> = listOf(
        AudioItem(
            id = "sample-door-1",
            name = "サンプル 戸閉放送 1",
            category = AudioCategory.DoorAnnouncement,
            sourceType = AudioSourceType.BundledTone,
            uriOrResName = "tone_prop_ack",
            isCustom = false,
        ),
        AudioItem(
            id = "sample-door-2",
            name = "サンプル 戸閉放送 2",
            category = AudioCategory.DoorAnnouncement,
            sourceType = AudioSourceType.BundledTone,
            uriOrResName = "tone_prop_beep",
            isCustom = false,
        ),
    )
}
