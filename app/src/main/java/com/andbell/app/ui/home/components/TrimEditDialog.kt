package com.andbell.app.ui.home.components

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.andbell.app.domain.model.AudioItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * 保存済みカスタム音声のトリム範囲を編集するダイアログ。
 * - トリムはメタデータのみ更新（物理ファイルは変更しない・即時保存）
 * - 現在の [item.trimStartMs]..[item.trimEndMs] の範囲内でのみ短くできる
 */
@Composable
fun TrimEditDialog(
    item: AudioItem,
    onDismiss: () -> Unit,
    onSaved: (trimStartMs: Long, trimEndMs: Long?) -> Unit,
) {
    val context = LocalContext.current

    var fileDurationMs by remember { mutableStateOf(0L) }
    var trimStartMs by remember { mutableStateOf(item.trimStartMs) }
    var trimEndMs by remember { mutableStateOf(item.trimEndMs ?: 0L) }
    // 動かせる上下限（これより広くはできない）
    val minBoundMs = item.trimStartMs
    var maxBoundMs by remember { mutableStateOf(item.trimEndMs ?: Long.MAX_VALUE) }

    var playPositionMs by remember { mutableStateOf(item.trimStartMs) }
    var isPlaying by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose { player?.release() }
    }

    // ファイル長を取得してトリム範囲の上限を確定
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(item.uriOrResName))
                val dur = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: return@withContext
                fileDurationMs = dur
                val initialEnd = item.trimEndMs ?: dur
                trimEndMs = initialEnd
                maxBoundMs = initialEnd
                playPositionMs = item.trimStartMs
            } finally {
                retriever.release()
            }
        }
    }

    // 非再生中はトリム開始位置にプレイヘッドを追従
    LaunchedEffect(trimStartMs) {
        if (!isPlaying) playPositionMs = trimStartMs
    }

    // 再生位置ポーリング・trimEndMs で自動停止
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (isActive) {
            val pos = player?.currentPosition?.toLong() ?: break
            playPositionMs = pos
            if (pos >= trimEndMs) {
                player?.stop()
                player?.release()
                player = null
                isPlaying = false
                playPositionMs = trimStartMs
                break
            }
            delay(16)
        }
    }

    fun togglePlay() {
        if (isPlaying) {
            player?.stop()
            player?.release()
            player = null
            isPlaying = false
        } else {
            runCatching {
                val mp = MediaPlayer()
                mp.setDataSource(context, Uri.parse(item.uriOrResName))
                mp.prepare()
                mp.seekTo(trimStartMs.toInt())
                mp.setOnCompletionListener {
                    isPlaying = false
                    player = null
                    it.release()
                }
                mp.start()
                player = mp
                isPlaying = true
            }
        }
    }

    fun save() {
        player?.release()
        player = null
        isPlaying = false
        // 全体と同じならメタデータをリセット（null = ファイル末尾まで）
        val newEnd = if (trimEndMs >= fileDurationMs) null else trimEndMs
        val newStart = if (trimStartMs <= 0L) 0L else trimStartMs
        onSaved(newStart, newEnd)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text = "トリミング編集",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(20.dp))

                // 再生ボタン
                OutlinedButton(
                    onClick = { togglePlay() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = fileDurationMs > 0,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isPlaying) "停止" else "再生して確認")
                }

                // トリムバー（ファイル長取得後に表示）
                if (fileDurationMs > 0) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "トリミング",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    TrimBar(
                        durationMs = fileDurationMs,
                        trimStartMs = trimStartMs,
                        trimEndMs = trimEndMs,
                        playPositionMs = playPositionMs,
                        minBoundMs = minBoundMs,
                        maxBoundMs = maxBoundMs,
                        onTrimChange = { start, end ->
                            trimStartMs = start
                            trimEndMs = end
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatMs(trimStartMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatMs(trimEndMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("キャンセル")
                    }
                    Button(
                        onClick = { save() },
                        modifier = Modifier.weight(1f),
                        enabled = fileDurationMs > 0,
                    ) {
                        Text("保存する")
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
