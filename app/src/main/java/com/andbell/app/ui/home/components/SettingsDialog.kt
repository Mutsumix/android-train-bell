package com.andbell.app.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andbell.app.domain.model.AudioCategory
import com.andbell.app.domain.model.AudioItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsDialog(
    isOpen: Boolean,
    departureBells: List<AudioItem>,
    doorAnnouncements: List<AudioItem>,
    onClose: () -> Unit,
    onDeleteAudio: (id: String, category: AudioCategory) -> Unit,
    onRenameAudio: (id: String, newName: String, category: AudioCategory) -> Unit,
    onRequestRecord: (AudioCategory) -> Unit,
    onRequestAddAudio: (AudioCategory) -> Unit,
    onRequestTrim: (AudioItem) -> Unit,
) {
    if (!isOpen) return

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val isBellTab = selectedTab == 0
    val currentCategory = if (isBellTab) AudioCategory.DepartureBell else AudioCategory.DoorAnnouncement
    val currentItems = if (isBellTab) departureBells else doorAnnouncements

    var renamingItem by remember { mutableStateOf<AudioItem?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var contextMenuItem by remember { mutableStateOf<AudioItem?>(null) }

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
                        androidx.compose.foundation.layout.Box {
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = if (item.isCustom) {
                                                { contextMenuItem = item }
                                            } else {
                                                null
                                            },
                                        ),
                                )
                                IconButton(onClick = { onDeleteAudio(item.id, item.category) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "削除",
                                    )
                                }
                            }
                            // 長押しで表示するコンテキストメニュー
                            DropdownMenu(
                                expanded = contextMenuItem?.id == item.id,
                                onDismissRequest = { contextMenuItem = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("名前を変更") },
                                    onClick = {
                                        renamingItem = item
                                        renameInput = item.name
                                        contextMenuItem = null
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("トリミング編集") },
                                    onClick = {
                                        onRequestTrim(item)
                                        contextMenuItem = null
                                    },
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = { onRequestRecord(currentCategory) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null)
                    Text(" 録音して追加")
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

    renamingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { renamingItem = null },
            title = { Text("名前を変更") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = renameInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onRenameAudio(item.id, trimmed, item.category)
                        }
                        renamingItem = null
                    },
                ) { Text("変更") }
            },
            dismissButton = {
                TextButton(onClick = { renamingItem = null }) { Text("キャンセル") }
            },
        )
    }
}
