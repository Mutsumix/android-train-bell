package com.andbell.app.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andbell.app.domain.model.AudioCategory
import com.andbell.app.domain.model.AudioItem

@Composable
fun SettingsDialog(
    isOpen: Boolean,
    departureBells: List<AudioItem>,
    doorAnnouncements: List<AudioItem>,
    onClose: () -> Unit,
    onDeleteAudio: (id: String, category: AudioCategory) -> Unit,
    onRequestAddAudio: (AudioCategory) -> Unit,
) {
    if (!isOpen) return

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val isBellTab = selectedTab == 0
    val currentCategory = if (isBellTab) AudioCategory.DepartureBell else AudioCategory.DoorAnnouncement
    val currentItems = if (isBellTab) departureBells else doorAnnouncements

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("音声ファイル管理") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = isBellTab,
                        onClick = { selectedTab = 0 },
                        text = { Text("発車ベル") },
                    )
                    Tab(
                        selected = !isBellTab,
                        onClick = { selectedTab = 1 },
                        text = { Text("戸閉放送") },
                    )
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                ) {
                    items(currentItems, key = { it.id }) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onDeleteAudio(item.id, item.category) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "削除",
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = { onRequestAddAudio(currentCategory) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("MP3ファイルを追加")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("閉じる")
            }
        },
    )
}
