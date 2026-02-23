package com.andbell.app.ui.home.components

import android.content.Context
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.andbell.app.audio.AudioRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingDialog(
    onDismiss: () -> Unit,
    onSaved: (file: File, displayName: String) -> Unit,
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var amplitudes by remember { mutableStateOf(List(50) { 0f }) }
    var currentFile by remember { mutableStateOf<File?>(null) }

    val recorder = remember { AudioRecorder(context) }
    DisposableEffect(Unit) {
        onDispose { recorder.release() }
    }

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

    Dialog(
        onDismissRequest = {
            if (isRecording) {
                recorder.stop()
                currentFile?.delete()
            }
            onDismiss()
        },
    ) {
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
                    Text(
                        text = "音声を録音",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                recorder.stop()
                                currentFile?.delete()
                            }
                            onDismiss()
                        },
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "閉じる")
                    }
                }

                Spacer(Modifier.height(20.dp))

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
                            if (isRecording) {
                                recorder.stop()
                                isRecording = false
                                val file = currentFile
                                if (file != null) {
                                    val fmt = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                                    onSaved(file, "録音 ${fmt.format(Date())}")
                                }
                            } else {
                                val file = newRecordingFile(context)
                                currentFile = file
                                recorder.start(file)
                                isRecording = true
                            }
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
