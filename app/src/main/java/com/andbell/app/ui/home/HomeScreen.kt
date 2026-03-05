package com.andbell.app.ui.home

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andbell.app.domain.model.AudioCategory
import com.andbell.app.domain.model.AudioItem
import com.andbell.app.ui.home.components.AudioSelector
import com.andbell.app.ui.home.components.RecordingDialog
import com.andbell.app.ui.home.components.SettingsDialog
import com.andbell.app.ui.home.components.SwitchButton
import com.andbell.app.ui.home.components.TrimEditDialog
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var addTargetCategory by remember { mutableStateOf(AudioCategory.DepartureBell) }
    var showRecordingDialog by remember { mutableStateOf(false) }
    var recordTargetCategory by remember { mutableStateOf(AudioCategory.DepartureBell) }
    var trimmingItem by remember { mutableStateOf<AudioItem?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uris.forEach { uri ->
            context.contentResolver.persistReadPermission(uri)
            val name = context.contentResolver.resolveDisplayName(uri) ?: "custom-${System.currentTimeMillis()}.mp3"
            viewModel.onAddAudio(uri, name, addTargetCategory)
        }
    }

    ObserveMessages(viewModel.messages, snackbarHostState)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("どこでも発車メロディ") },
                actions = {
                    IconButton(onClick = viewModel::onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SwitchButton(
                onPress = viewModel::onSwitchPressed,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            AudioSelector(
                label = "発車ベル（ON時）",
                items = uiState.departureBells,
                selectedId = uiState.selectedBellId,
                onSelect = viewModel::onSelectBell,
            )
            Spacer(modifier = Modifier.height(10.dp))
            AudioSelector(
                label = "戸閉放送（OFF時）",
                items = uiState.doorAnnouncements,
                selectedId = uiState.selectedDoorId,
                onSelect = viewModel::onSelectDoor,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("スイッチを押すと音声が再生されます", style = MaterialTheme.typography.bodySmall)
        }
    }

    SettingsDialog(
        isOpen = uiState.isSettingsOpen,
        departureBells = uiState.departureBells,
        doorAnnouncements = uiState.doorAnnouncements,
        onClose = viewModel::onCloseSettings,
        onDeleteAudio = viewModel::onDeleteAudio,
        onRenameAudio = viewModel::onRenameAudio,
        onRequestRecord = { category ->
            recordTargetCategory = category
            showRecordingDialog = true
        },
        onRequestAddAudio = { category ->
            addTargetCategory = category
            filePicker.launch(arrayOf("audio/mpeg", "audio/mp3"))
        },
        onRequestTrim = { item ->
            trimmingItem = item
        },
    )

    trimmingItem?.let { item ->
        TrimEditDialog(
            item = item,
            onDismiss = { trimmingItem = null },
            onSaved = { trimStart, trimEnd ->
                viewModel.onRetrimAudio(item.id, trimStart, trimEnd, item.category)
                trimmingItem = null
            },
        )
    }

    if (showRecordingDialog) {
        RecordingDialog(
            onDismiss = { showRecordingDialog = false },
            onSaved = { file, name ->
                viewModel.onAddAudio(Uri.fromFile(file), name, recordTargetCategory)
                showRecordingDialog = false
            },
        )
    }
}

@Composable
private fun ObserveMessages(
    messages: SharedFlow<String>,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(messages) {
        messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
}

private fun ContentResolver.persistReadPermission(uri: Uri) {
    runCatching {
        takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun ContentResolver.resolveDisplayName(uri: Uri): String? {
    val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
    return query(uri, projection, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
}
