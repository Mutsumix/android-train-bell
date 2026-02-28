package com.andbell.app.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

class AudioTrimmer {

    /**
     * M4Aファイルを再エンコードなしでトリミングする。
     * AACはフレーム単位（約21ms）でしか切れないため、指定時刻に最も近いフレーム境界で切り出す。
     */
    suspend fun trim(
        inputFile: File,
        outputFile: File,
        startMs: Long,
        endMs: Long,
    ): Result<File> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(inputFile.absolutePath)

            val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString("mime")?.startsWith("audio/") == true
            } ?: error("音声トラックが見つかりません")

            val format = extractor.getTrackFormat(trackIndex)
            extractor.selectTrack(trackIndex)

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrack = muxer.addTrack(format)
            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L

            // 開始時刻の直前のキーフレームへシーク
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val bufferSize = format.getInteger("max-input-size", 512 * 1024)
            val buffer = ByteBuffer.allocate(bufferSize)
            val info = MediaCodec.BufferInfo()

            while (true) {
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) break

                // 開始位置より前のフレームはスキップ（シークが前に戻る場合）
                if (sampleTimeUs < startUs) {
                    extractor.advance()
                    continue
                }

                info.offset = 0
                info.size = sampleSize
                info.presentationTimeUs = sampleTimeUs - startUs
                info.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrack, buffer, info)
                extractor.advance()
            }

            Result.success(outputFile)
        } catch (e: CancellationException) {
            outputFile.delete()
            throw e
        } catch (e: Exception) {
            outputFile.delete()
            Result.failure(e)
        } finally {
            runCatching { muxer?.stop(); muxer?.release() }
            extractor.release()
        }
    }
}
