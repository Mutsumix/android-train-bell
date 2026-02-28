package com.andbell.app.ui.home.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.andbell.app.audio.AudioRecorder
import com.andbell.app.audio.AudioTrimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Phase { Idle, Recording, Review }

@Composable
fun RecordingDialog(
    onDismiss: () -> Unit,
    onSaved: (file: File, displayName: String) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val trimmer = remember { AudioTrimmer() }

    var phase by remember { mutableStateOf(Phase.Idle) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var amplitudes by remember { mutableStateOf(List(50) { 0f }) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    var recordedDuration by remember { mutableStateOf(0) }
    var nameInput by remember { mutableStateOf("") }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    var recordedDurationMs by remember { mutableStateOf(0L) }
    var trimStartMs by remember { mutableStateOf(0L) }
    var trimEndMs by remember { mutableStateOf(0L) }
    var playPositionMs by remember { mutableStateOf(0L) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var showDismissConfirm by remember { mutableStateOf(false) }

    val recorder = remember { AudioRecorder(context) }

    DisposableEffect(Unit) {
        onDispose {
            recorder.release()
            previewPlayer?.release()
        }
    }

    val isRecording = phase == Phase.Recording

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            while (isActive) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isActive) {
                delay(80)
                val amp = (recorder.getMaxAmplitude() / 32767f).coerceIn(0f, 1f)
                amplitudes = (amplitudes + amp).takeLast(50)
            }
        } else {
            amplitudes = List(50) { 0f }
        }
    }

    // Reviewフェーズ突入時にファイル実長を取得してトリム範囲を初期化
    LaunchedEffect(phase) {
        if (phase == Phase.Review) {
            val file = currentFile ?: return@LaunchedEffect
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    val dur = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                        ?: (recordedDuration * 1000L)
                    recordedDurationMs = dur
                    trimStartMs = 0L
                    trimEndMs = dur
                    playPositionMs = 0L
                } finally {
                    retriever.release()
                }
            }
        }
    }

    // 非再生中はトリム開始位置にプレイヘッドを追従させる
    LaunchedEffect(trimStartMs) {
        if (!isPreviewPlaying) playPositionMs = trimStartMs
    }

    // 再生位置をポーリングし、trimEndMs で自動停止
    LaunchedEffect(isPreviewPlaying) {
        if (!isPreviewPlaying) return@LaunchedEffect
        while (isActive) {
            val pos = previewPlayer?.currentPosition?.toLong() ?: break
            playPositionMs = pos
            if (pos >= trimEndMs) {
                previewPlayer?.stop()
                previewPlayer?.release()
                previewPlayer = null
                isPreviewPlaying = false
                playPositionMs = trimStartMs
                break
            }
            delay(16)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            val file = newRecordingFile(context)
            currentFile = file
            recorder.start(file)
            phase = Phase.Recording
        }
    }

    fun startRecording() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val file = newRecordingFile(context)
            currentFile = file
            recorder.start(file)
            phase = Phase.Recording
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopRecording() {
        recorder.stop()
        recordedDuration = elapsedSeconds
        phase = Phase.Review
    }

    fun reRecord() {
        previewPlayer?.release()
        previewPlayer = null
        isPreviewPlaying = false
        currentFile?.delete()
        currentFile = null
        nameInput = ""
        trimStartMs = 0L
        trimEndMs = 0L
        recordedDurationMs = 0L
        playPositionMs = 0L
        isSaving = false
        saveError = null
        phase = Phase.Idle
    }

    fun togglePreview() {
        val file = currentFile ?: return
        if (isPreviewPlaying) {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
            isPreviewPlaying = false
        } else {
            runCatching {
                val player = MediaPlayer()
                player.setDataSource(file.absolutePath)
                player.prepare()
                player.seekTo(trimStartMs.toInt())
                player.setOnCompletionListener {
                    isPreviewPlaying = false
                    previewPlayer = null
                    it.release()
                }
                player.start()
                previewPlayer = player
                isPreviewPlaying = true
            }
        }
    }

    fun save() {
        val file = currentFile ?: return
        val name = nameInput.trim().ifEmpty {
            val fmt = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            "録音 ${fmt.format(Date())}"
        }
        previewPlayer?.release()
        previewPlayer = null
        isPreviewPlaying = false

        val needsTrim = trimStartMs > 0 || trimEndMs < recordedDurationMs
        if (!needsTrim) {
            onSaved(file, name)
            return
        }

        isSaving = true
        saveError = null
        coroutineScope.launch {
            val dir = File(context.filesDir, "recordings")
            val trimmedFile = File(dir, "trimmed_${System.currentTimeMillis()}.m4a")
            val result = trimmer.trim(file, trimmedFile, trimStartMs, trimEndMs)
            isSaving = false
            result.fold(
                onSuccess = { outFile ->
                    file.delete()
                    onSaved(outFile, name)
                },
                onFailure = {
                    saveError = "トリミングに失敗しました"
                },
            )
        }
    }

    fun dismiss() {
        if (phase == Phase.Recording) recorder.stop()
        currentFile?.delete()
        previewPlayer?.release()
        previewPlayer = null
        onDismiss()
    }

    fun requestDismiss() {
        if (phase == Phase.Review) {
            showDismissConfirm = true
        } else {
            dismiss()
        }
    }

    if (showDismissConfirm) {
        AlertDialog(
            onDismissRequest = { showDismissConfirm = false },
            title = { Text("録音を破棄しますか？") },
            text = { Text("編集中の内容は保存されません。") },
            confirmButton = {
                TextButton(onClick = { showDismissConfirm = false; dismiss() }) {
                    Text("破棄する", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDismissConfirm = false }) {
                    Text("キャンセル")
                }
            },
        )
    }

    Dialog(onDismissRequest = { requestDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("音声を録音", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { requestDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "閉じる")
                    }
                }

                Spacer(Modifier.height(20.dp))

                when (phase) {
                    Phase.Idle, Phase.Recording -> {
                        WaveformView(
                            amplitudes = amplitudes,
                            isRecording = isRecording,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
                            style = MaterialTheme.typography.displayMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isRecording) "録音中..." else "録音ボタンを押して開始",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(28.dp))
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD32F2F))
                                .clickable {
                                    if (isRecording) stopRecording() else startRecording()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isRecording) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(Color.White),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    Phase.Review -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("録音完了", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "%02d:%02d".format(recordedDuration / 60, recordedDuration % 60),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { togglePreview() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isPreviewPlaying) "停止" else "再生して確認")
                        }

                        // トリムバー（ファイル長取得後に表示）
                        if (recordedDurationMs > 0) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "トリミング",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            TrimBar(
                                durationMs = recordedDurationMs,
                                trimStartMs = trimStartMs,
                                trimEndMs = trimEndMs,
                                playPositionMs = playPositionMs,
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

                        Spacer(Modifier.height(16.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("録音名", style = MaterialTheme.typography.labelMedium)
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                placeholder = { Text("例: 自作メロディ1") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }

                        if (saveError != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = saveError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { reRecord() },
                                modifier = Modifier.weight(1f),
                                enabled = !isSaving,
                            ) {
                                Text("録り直す")
                            }
                            Button(
                                onClick = { save() },
                                modifier = Modifier.weight(1f),
                                enabled = !isSaving,
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("保存中...")
                                } else {
                                    Text("保存する")
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}


@Composable
private fun WaveformView(
    amplitudes: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
) {
    val activeColor = Color(0xFFD32F2F)
    val idleColor = Color(0xFFBDBDBD)

    Canvas(modifier = modifier) {
        val barWidth = 4.dp.toPx()
        val gap = 3.dp.toPx()
        val stride = barWidth + gap
        val centerY = size.height / 2f
        val minBarHeight = 4.dp.toPx()

        amplitudes.forEachIndexed { index, amp ->
            val x = index * stride
            if (x + barWidth > size.width) return@forEachIndexed
            val barHeight = if (isRecording) {
                (amp * size.height).coerceAtLeast(minBarHeight)
            } else {
                minBarHeight
            }
            drawRoundRect(
                color = if (isRecording) activeColor else idleColor,
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

private fun newRecordingFile(context: Context): File {
    val dir = File(context.filesDir, "recordings")
    dir.mkdirs()
    return File(dir, "recording_${System.currentTimeMillis()}.m4a")
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
