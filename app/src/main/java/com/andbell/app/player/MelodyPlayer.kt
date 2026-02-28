package com.andbell.app.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * サイン波でメロディを再生するプレイヤー
 */
class MelodyPlayer {
    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null

    data class Note(
        val frequency: Float, // Hz（0 = 休符）
        val beats: Float, // 拍数
    )

    /**
     * メロディを再生
     * @param notes 音符のリスト
     * @param bpm テンポ（1分あたりの拍数）
     */
    fun play(notes: List<Note>, bpm: Int = 150) {
        stop()

        val beatDuration = 60f / bpm // 1拍の長さ（秒）
        val samples = generateMelodySamples(notes, beatDuration)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack?.write(samples, 0, samples.size)
        audioTrack?.play()
    }

    fun stop() {
        audioTrack?.runCatching {
            stop()
        }
        audioTrack?.release()
        audioTrack = null
    }

    private fun generateMelodySamples(notes: List<Note>, beatDuration: Float): ShortArray {
        val allSamples = mutableListOf<Short>()

        for (note in notes) {
            val durationSec = note.beats * beatDuration
            val numSamples = (sampleRate * durationSec).toInt()

            if (note.frequency == 0f) {
                repeat(numSamples) { allSamples.add(0) }
            } else {
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate
                    val progress = i.toFloat() / numSamples

                    val wave = sin(2 * PI * note.frequency * t) +
                        0.25 * sin(2 * PI * note.frequency * 2 * t) +
                        0.1 * sin(2 * PI * note.frequency * 3 * t)

                    val envelope = when {
                        progress < 0.02f -> progress / 0.02f
                        progress < 0.4f -> 1f - (progress - 0.02f) * 0.3f
                        progress < 0.95f -> 0.7f
                        else -> 0.7f * (1f - (progress - 0.95f) / 0.05f)
                    }

                    val sample = (wave * envelope * 0.4 * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    allSamples.add(sample.toShort())
                }
            }
        }

        return allSamples.toShortArray()
    }
}
