package com.andbell.app.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.andbell.app.domain.model.AudioCategory
import com.andbell.app.domain.model.AudioItem
import com.andbell.app.domain.model.AudioSourceType

class MediaPlayerAudioPlayer(
    private val context: Context,
    private val onError: (Throwable) -> Unit,
) : AudioPlayer {
    private val melodyStartDelayMs = 500L
    private var primaryMediaPlayer: MediaPlayer? = null
    private var switchCuePlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pendingStartRunnable: Runnable? = null
    private val melodyPlayer = MelodyPlayer()

    // 打ち返し用プレイヤー
    private val uchigaeshiPlayer = MelodyPlayer()
    private var uchigaeshiMediaPlayer: MediaPlayer? = null
    private var pendingUchigaeshiRunnable: Runnable? = null

    private var currentCategory: AudioCategory? = null

    override fun play(item: AudioItem) {
        runCatching {
            val isUchigaeshi = currentCategory == AudioCategory.DoorAnnouncement
                && item.category == AudioCategory.DepartureBell
                && isCurrentlyPlaying()

            if (isUchigaeshi) {
                // 打ち返し: OFF を止めずに ON を同時再生
                currentCategory = null  // 次押しはリセット後の通常再生
                playSwitchCue(item.category)
                startUchigaeshiAudio(item)
            } else {
                stop()
                currentCategory = item.category
                playSwitchCue(item.category)
                schedulePrimaryPlayback(item)
            }
        }.onFailure {
            stop()
            onError(it)
        }
    }

    override fun stop() {
        currentCategory = null
        pendingStartRunnable?.let { handler.removeCallbacks(it) }
        pendingStartRunnable = null
        pendingUchigaeshiRunnable?.let { handler.removeCallbacks(it) }
        pendingUchigaeshiRunnable = null
        stopAndRelease(primaryMediaPlayer); primaryMediaPlayer = null
        stopAndRelease(switchCuePlayer); switchCuePlayer = null
        stopAndRelease(uchigaeshiMediaPlayer); uchigaeshiMediaPlayer = null
        melodyPlayer.stop()
        uchigaeshiPlayer.stop()
    }

    override fun release() {
        stop()
    }

    private fun isCurrentlyPlaying(): Boolean =
        melodyPlayer.isPlaying() || primaryMediaPlayer?.isPlaying == true

    private fun resolveMelody(key: String): List<MelodyPlayer.Note>? = when (key) {
        "melody_departure_1" -> Melodies.departureBell1
        "melody_departure_2" -> Melodies.departureBell2
        "melody_departure_3" -> Melodies.departureBell3
        "melody_door_1" -> Melodies.doorChime1
        "melody_door_2" -> Melodies.doorChime2
        "melody_door_3" -> Melodies.doorChime3
        else -> null
    }

    private fun playSwitchCue(category: AudioCategory) {
        val rawName = when (category) {
            AudioCategory.DepartureBell -> "on"
            AudioCategory.DoorAnnouncement -> "off"
        }
        switchCuePlayer = createStartedRawPlayer(rawName) { completed ->
            if (switchCuePlayer === completed) switchCuePlayer = null
        }
    }

    private fun schedulePrimaryPlayback(item: AudioItem) {
        val runnable = Runnable {
            pendingStartRunnable = null
            runCatching {
                startPrimary(item)
            }.onFailure {
                stop()
                onError(it)
            }
        }
        pendingStartRunnable = runnable
        handler.postDelayed(runnable, melodyStartDelayMs)
    }

    private fun startPrimary(item: AudioItem) {
        when (item.sourceType) {
            AudioSourceType.UserUri -> {
                primaryMediaPlayer = createStartedUriPlayer(item.uriOrResName) { completed ->
                    if (primaryMediaPlayer === completed) primaryMediaPlayer = null
                }
            }
            AudioSourceType.BundledTone -> {
                val melody = resolveMelody(item.uriOrResName)
                if (melody != null) {
                    melodyPlayer.play(melody)
                } else {
                    primaryMediaPlayer = createStartedRawPlayer(item.uriOrResName) { completed ->
                        if (primaryMediaPlayer === completed) primaryMediaPlayer = null
                    }
                }
            }
        }
    }

    private fun startUchigaeshiAudio(item: AudioItem) {
        val runnable = Runnable {
            pendingUchigaeshiRunnable = null
            runCatching {
                startUchigaeshi(item)
            }.onFailure {
                stop()
                onError(it)
            }
        }
        pendingUchigaeshiRunnable = runnable
        handler.postDelayed(runnable, melodyStartDelayMs)
    }

    private fun startUchigaeshi(item: AudioItem) {
        when (item.sourceType) {
            AudioSourceType.UserUri -> {
                uchigaeshiMediaPlayer = createStartedUriPlayer(item.uriOrResName) { completed ->
                    if (uchigaeshiMediaPlayer === completed) uchigaeshiMediaPlayer = null
                }
            }
            AudioSourceType.BundledTone -> {
                val melody = resolveMelody(item.uriOrResName)
                if (melody != null) {
                    uchigaeshiPlayer.play(melody)
                } else {
                    uchigaeshiMediaPlayer = createStartedRawPlayer(item.uriOrResName) { completed ->
                        if (uchigaeshiMediaPlayer === completed) uchigaeshiMediaPlayer = null
                    }
                }
            }
        }
    }

    private fun createStartedUriPlayer(
        uriString: String,
        onCompletion: (MediaPlayer) -> Unit,
    ): MediaPlayer {
        val uri = Uri.parse(uriString)
        return MediaPlayer().apply {
            setDataSource(context, uri)
            setOnCompletionListener { completed ->
                onCompletion(completed)
                completed.release()
            }
            prepare()
            start()
        }
    }

    private fun createStartedRawPlayer(
        rawName: String,
        onCompletion: (MediaPlayer) -> Unit,
    ): MediaPlayer {
        val rawResId = context.resources.getIdentifier(rawName, "raw", context.packageName)
        require(rawResId != 0) { "raw リソースが見つかりません: $rawName" }

        val created = MediaPlayer.create(context, rawResId)
            ?: error("raw リソースの再生準備に失敗しました: $rawName")
        created.setOnCompletionListener { completed ->
            onCompletion(completed)
            completed.release()
        }
        created.start()
        return created
    }

    private fun stopAndRelease(player: MediaPlayer?) {
        player?.runCatching {
            if (isPlaying) stop()
        }
        player?.release()
    }
}
