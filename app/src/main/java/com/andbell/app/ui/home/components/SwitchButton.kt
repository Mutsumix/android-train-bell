package com.andbell.app.ui.home.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SwitchButton(
    onPress: (Boolean) -> Unit,
    onLatched: Boolean,
    isLinkedMode: Boolean = false,
    onLinkedModeTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 筐体の色（ベージュ/グレー）
    val housingColor = Color(0xFFD4CFC4)
    val housingDarkColor = Color(0xFFB8B3A8)
    val housingBorderColor = Color(0xFF9E9B92)

    // ラベルの色
    val labelBgColor = Color(0xFFFFF9C4)
    val labelBorderColor = Color(0xFFB8B3A8)

    // ボタンの色
    val onButtonColor = Color(0xFF1A1A1A)
    val onButtonPressedColor = Color(0xFF0D0D0D)
    val offButtonColor = Color(0xFFD32F2F)
    val offButtonPressedColor = Color(0xFFB71C1C)

    Box(
        modifier = modifier
            .width(280.dp)
            .height(380.dp),
        contentAlignment = Alignment.Center
    ) {
        val alpha = if (isLinkedMode) 0.5f else 1f
        // 筐体本体
        Surface(
            modifier = Modifier
                .width(260.dp)
                .height(360.dp)
                .graphicsLayer { this.alpha = alpha },
            shape = RoundedCornerShape(28.dp),
            color = housingColor,
            shadowElevation = 16.dp,
            border = BorderStroke(4.dp, housingBorderColor)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                housingColor,
                                housingDarkColor.copy(alpha = 0.3f),
                                housingColor
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ラベルプレート
                    Surface(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .width(180.dp)
                            .height(40.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = labelBgColor,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, labelBorderColor)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = "発車ベル",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF424242)
                            )
                        }
                    }

                    // ボタンコンテナ
                    Column(
                        modifier = Modifier.padding(top = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ONボタン
                        IndustrialButton(
                            text = "ON",
                            baseColor = onButtonColor,
                            pressedColor = onButtonPressedColor,
                            isPressed = onLatched,
                            onClick = {
                                if (isLinkedMode) {
                                    onLinkedModeTap()
                                } else {
                                    onPress(true)
                                }
                            }
                        )

                        // OFFボタン
                        IndustrialButton(
                            text = "OFF",
                            baseColor = offButtonColor,
                            pressedColor = offButtonPressedColor,
                            isPressed = false,
                            onClick = {
                                if (isLinkedMode) {
                                    onLinkedModeTap()
                                } else {
                                    onPress(false)
                                }
                            }
                        )
                    }
                }

                // ネジ穴（4隅）
                MountingScrew(modifier = Modifier.align(Alignment.TopStart).offset((-8).dp, (-8).dp))
                MountingScrew(modifier = Modifier.align(Alignment.TopEnd).offset(8.dp, (-8).dp))
                MountingScrew(modifier = Modifier.align(Alignment.BottomStart).offset((-8).dp, 8.dp))
                MountingScrew(modifier = Modifier.align(Alignment.BottomEnd).offset(8.dp, 8.dp))
            }
        }
    }
}

@Composable
private fun IndustrialButton(
    text: String,
    baseColor: Color,
    pressedColor: Color,
    isPressed: Boolean,
    onClick: () -> Unit,
) {
    val offsetY by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        label = "button-offset"
    )

    Surface(
        modifier = Modifier
            .width(160.dp)
            .height(80.dp)
            .offset(y = offsetY)
            .shadow(
                elevation = if (isPressed) 4.dp else 18.dp,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isPressed) pressedColor else baseColor
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = if (isPressed) {
                            listOf(pressedColor, pressedColor)
                        } else {
                            listOf(
                                baseColor.copy(alpha = 1.1f),
                                baseColor,
                                baseColor.copy(alpha = 0.9f)
                            )
                        }
                    )
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // 内側のボタン面
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.8f),
                                baseColor
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun MountingScrew(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp)
            .shadow(2.dp, CircleShape)
            .background(Color(0xFF8D8D8D), CircleShape)
            .border(1.dp, Color(0xFF666666), CircleShape)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(8.dp)
                .background(Color(0xFF666666), CircleShape)
        )
    }
}
