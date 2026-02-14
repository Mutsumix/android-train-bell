package com.andbell.app.player

import com.andbell.app.domain.model.AudioItem

interface AudioPlayer {
    fun play(item: AudioItem)
    fun stop()
    fun release()
}
