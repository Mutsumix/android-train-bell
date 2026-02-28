package com.andbell.app.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal enum class DragHandle { Start, End }

/**
 * トリムバー。
 *
 * - 大きな●（プライマリ色）: 開始・終了ハンドル（ドラッグで移動）
 * - 赤い小さな●: 再生中の現在位置（playPositionMs）
 * - [minBoundMs]..[maxBoundMs] の範囲外はロック済みとして薄く表示
 *
 * @param durationMs      ファイル全体の長さ（ms）
 * @param trimStartMs     現在のトリム開始位置（ms）
 * @param trimEndMs       現在のトリム終了位置（ms）
 * @param playPositionMs  再生中の現在位置（ms、絶対値）
 * @param minBoundMs      開始ハンドルが動ける下限（デフォルト 0）
 * @param maxBoundMs      終了ハンドルが動ける上限（デフォルト durationMs）
 * @param onTrimChange    ハンドルドラッグ時のコールバック
 */
@Composable
internal fun TrimBar(
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    playPositionMs: Long,
    onTrimChange: (startMs: Long, endMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    minBoundMs: Long = 0L,
    maxBoundMs: Long = durationMs,
) {
    if (durationMs <= 0) return

    val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val surfaceVariantColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
    val lockedColor = surfaceVariantColor.copy(alpha = 0.4f)
    val playheadColor = Color(0xFFD32F2F)
    val density = LocalDensity.current

    val currentTrimStart by rememberUpdatedState(trimStartMs)
    val currentTrimEnd by rememberUpdatedState(trimEndMs)
    val currentDuration by rememberUpdatedState(durationMs)
    val currentMinBound by rememberUpdatedState(minBoundMs)
    val currentMaxBound by rememberUpdatedState(maxBoundMs)
    val currentOnTrimChange by rememberUpdatedState(onTrimChange)

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            val hitRadiusPx = with(density) { 24.dp.toPx() }
            var dragging: DragHandle? = null

            detectDragGestures(
                onDragStart = { offset ->
                    val w = size.width.toFloat()
                    val sx = (currentTrimStart.toFloat() / currentDuration) * w
                    val ex = (currentTrimEnd.toFloat() / currentDuration) * w
                    val dStart = abs(offset.x - sx)
                    val dEnd = abs(offset.x - ex)
                    dragging = when {
                        dStart <= hitRadiusPx && dEnd <= hitRadiusPx ->
                            if (dStart <= dEnd) DragHandle.Start else DragHandle.End
                        dStart <= hitRadiusPx -> DragHandle.Start
                        dEnd <= hitRadiusPx -> DragHandle.End
                        else -> null
                    }
                },
                onDrag = { change, _ ->
                    val w = size.width.toFloat()
                    val fraction = (change.position.x / w).coerceIn(0f, 1f)
                    val newMs = (fraction * currentDuration).toLong()
                    when (dragging) {
                        DragHandle.Start -> currentOnTrimChange(
                            newMs.coerceIn(currentMinBound, currentTrimEnd - 200L),
                            currentTrimEnd,
                        )
                        DragHandle.End -> currentOnTrimChange(
                            currentTrimStart,
                            newMs.coerceIn(currentTrimStart + 200L, currentMaxBound),
                        )
                        null -> {}
                    }
                    change.consume()
                },
                onDragEnd = { dragging = null },
                onDragCancel = { dragging = null },
            )
        },
    ) {
        val centerY = size.height / 2f
        val trackH = 4.dp.toPx()
        val handleR = 10.dp.toPx()
        val playR = 6.dp.toPx()

        val startX = (trimStartMs.toFloat() / durationMs) * size.width
        val endX = (trimEndMs.toFloat() / durationMs) * size.width
        val minX = (minBoundMs.toFloat() / durationMs) * size.width
        val maxX = (maxBoundMs.toFloat() / durationMs) * size.width
        val playX = (playPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) * size.width

        // ベーストラック（グレー）
        drawRoundRect(
            color = surfaceVariantColor,
            topLeft = Offset(0f, centerY - trackH / 2),
            size = Size(size.width, trackH),
            cornerRadius = CornerRadius(trackH / 2),
        )

        // ロック済み範囲（左）
        if (minBoundMs > 0L) {
            drawRoundRect(
                color = lockedColor,
                topLeft = Offset(0f, centerY - trackH / 2),
                size = Size(minX, trackH),
                cornerRadius = CornerRadius(trackH / 2),
            )
        }

        // ロック済み範囲（右）
        if (maxBoundMs < durationMs) {
            drawRoundRect(
                color = lockedColor,
                topLeft = Offset(maxX, centerY - trackH / 2),
                size = Size(size.width - maxX, trackH),
                cornerRadius = CornerRadius(trackH / 2),
            )
        }

        // 選択範囲（プライマリ色）
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(startX, centerY - trackH / 2),
            size = Size((endX - startX).coerceAtLeast(0f), trackH),
            cornerRadius = CornerRadius(trackH / 2),
        )

        // プレイヘッド（赤●）— ハンドルより下のレイヤー
        drawCircle(color = playheadColor, radius = playR, center = Offset(playX, centerY))

        // 開始ハンドル
        drawCircle(color = primaryColor, radius = handleR, center = Offset(startX, centerY))
        drawCircle(color = Color.White, radius = handleR * 0.38f, center = Offset(startX, centerY))

        // 終了ハンドル
        drawCircle(color = primaryColor, radius = handleR, center = Offset(endX, centerY))
        drawCircle(color = Color.White, radius = handleR * 0.38f, center = Offset(endX, centerY))
    }
}
