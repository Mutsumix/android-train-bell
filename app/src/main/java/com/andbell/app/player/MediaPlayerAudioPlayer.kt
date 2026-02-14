package com.andbell.app.player

import android.content.Context
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.media.AudioManager
import android.net.Uri
import com.andbell.app.domain.model.AudioItem
import com.andbell.app.domain.model.AudioSourceType

class MediaPlayerAudioPlayer(
    private val context: Context,
    private val onError: (Throwable) -> Unit,
) : AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    override fun play(item: AudioItem) {
        runCatching {
            stop()
            when (item.sourceType) {
                AudioSourceType.UserUri -> {
                    val uri = Uri.parse(item.uriOrResName)
                    val created = MediaPlayer.create(context, uri)
                        ?: error("音声ファイルを読み込めませんでした: ${item.name}")
                    created.start()
                    mediaPlayer = created
                }
                AudioSourceType.BundledTone -> {
                    val toneType = resolveTone(item.uriOrResName)
                    toneGenerator.startTone(toneType, 1200)
                }
            }
        }.onFailure(onError)
    }

    override fun stop() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun release() {
        stop()
        toneGenerator.release()
    }

    private fun resolveTone(key: String): Int = when (key) {
        "tone_dtmf_1" -> ToneGenerator.TONE_DTMF_1
        "tone_dtmf_3" -> ToneGenerator.TONE_DTMF_3
        "tone_prop_ack" -> ToneGenerator.TONE_PROP_ACK
        "tone_prop_beep" -> ToneGenerator.TONE_PROP_BEEP
        else -> ToneGenerator.TONE_PROP_BEEP2
    }
}
