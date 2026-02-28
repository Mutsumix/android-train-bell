package com.andbell.app.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.andbell.app.domain.model.AudioItem
import com.andbell.app.domain.model.AudioSourceType

class MediaPlayerAudioPlayer(
    private val context: Context,
    private val onError: (Throwable) -> Unit,
) : AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private val melodyPlayer = MelodyPlayer()
    private val handler = Handler(Looper.getMainLooper())
    private var autoStopRunnable: Runnable? = null

    override fun play(item: AudioItem) {
        runCatching {
            stop()
            when (item.sourceType) {
                AudioSourceType.UserUri -> {
                    val uri = Uri.parse(item.uriOrResName)
                    val mp = MediaPlayer()
                    mp.setDataSource(context, uri)
                    mp.prepare()
                    if (item.trimStartMs > 0L) mp.seekTo(item.trimStartMs.toInt())
                    mp.setOnCompletionListener { stop() }
                    mp.start()
                    mediaPlayer = mp

                    // trimEndMs が設定されていれば、その時点で自動停止
                    item.trimEndMs?.let { endMs ->
                        val playDuration = endMs - item.trimStartMs
                        val runnable = Runnable { stop() }
                        autoStopRunnable = runnable
                        handler.postDelayed(runnable, playDuration)
                    }
                }
                AudioSourceType.BundledTone -> {
                    val melody = resolveMelody(item.uriOrResName)
                    if (melody != null) {
                        melodyPlayer.play(melody)
                    }
                }
            }
        }.onFailure(onError)
    }

    override fun stop() {
        autoStopRunnable?.let { handler.removeCallbacks(it) }
        autoStopRunnable = null
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
        melodyPlayer.stop()
    }

    override fun release() {
        stop()
        handler.removeCallbacksAndMessages(null)
    }

    private fun resolveMelody(key: String): List<MelodyPlayer.Note>? = when (key) {
        "melody_departure_1" -> Melodies.departureBell1
        "melody_departure_2" -> Melodies.departureBell2
        "melody_departure_3" -> Melodies.departureBell3
        "melody_door_1" -> Melodies.doorChime1
        "melody_door_2" -> Melodies.doorChime2
        "melody_door_3" -> Melodies.doorChime3
        else -> null
    }
}
