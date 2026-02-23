package com.andbell.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null

    fun start(outputFile: File) {
        outputFile.parentFile?.mkdirs()
        val r = createRecorder()
        r.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        recorder = r
    }

    fun stop() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }

    fun getMaxAmplitude(): Int = recorder?.maxAmplitude ?: 0

    fun release() {
        recorder?.runCatching {
            stop()
            release()
        }
        recorder = null
    }

    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
}
