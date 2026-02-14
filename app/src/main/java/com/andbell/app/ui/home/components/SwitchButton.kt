package com.andbell.app.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SwitchButton(
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressedOn by remember { mutableStateOf(false) }
    var pressedOff by remember { mutableStateOf(false) }
    val onAlpha by animateFloatAsState(if (pressedOn) 0.75f else 1f, label = "on-alpha")
    val offAlpha by animateFloatAsState(if (pressedOff) 0.75f else 1f, label = "off-alpha")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFD9D9D9))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "発車ベル", style = MaterialTheme.typography.labelMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .alpha(onAlpha)
                .background(Color.Black)
                .clickable {
                    pressedOn = true
                    pressedOff = false
                    onPress(true)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("ON", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .alpha(offAlpha)
                .background(Color(0xFFC62828))
                .clickable {
                    pressedOff = true
                    pressedOn = false
                    onPress(false)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("OFF", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Box(modifier = Modifier.size(1.dp))
    }
}
