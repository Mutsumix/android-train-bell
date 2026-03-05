package com.andbell.app.ui.home

import android.app.Application
import android.hardware.usb.UsbDevice
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andbell.app.config.AudioPolicyProvider
import com.andbell.app.data.AudioRepository
import com.andbell.app.data.InMemoryAudioRepository
import com.andbell.app.data.UserAudioStore
import com.andbell.app.domain.model.AudioCategory
import com.andbell.app.domain.model.AudioItem
import com.andbell.app.domain.model.AudioSourceType
import com.andbell.app.player.AudioPlayer
import com.andbell.app.player.MediaPlayerAudioPlayer
import com.andbell.app.serial.DsrState
import com.andbell.app.serial.UsbSerialManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val departureBells: List<AudioItem> = emptyList(),
    val doorAnnouncements: List<AudioItem> = emptyList(),
    val selectedBellId: String? = null,
    val selectedDoorId: String? = null,
    val isSettingsOpen: Boolean = false,
    val isUsbConnected: Boolean = false,
    val isLinkedMode: Boolean = false,
)

class HomeViewModel(
    application: Application,
    private val repository: AudioRepository = InMemoryAudioRepository(),
    private val store: UserAudioStore = UserAudioStore(application),
) : AndroidViewModel(application) {
    private val settingsOpen = MutableStateFlow(false)
    private val selectedBellId = MutableStateFlow<String?>(null)
    private val selectedDoorId = MutableStateFlow<String?>(null)
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    private val usbSerialManager = UsbSerialManager(application, viewModelScope)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.departureBells,
        repository.doorAnnouncements,
        selectedBellId,
        selectedDoorId,
        combine(settingsOpen, usbSerialManager.isConnected) { open, usb -> Pair(open, usb) },
    ) { bells, doors, bellId, doorId, (isOpen, isConnected) ->
        HomeUiState(
            departureBells = bells,
            doorAnnouncements = doors,
            selectedBellId = bellId ?: bells.firstOrNull()?.id,
            selectedDoorId = doorId ?: doors.firstOrNull()?.id,
            isSettingsOpen = isOpen,
            isUsbConnected = isConnected,
            isLinkedMode = isConnected,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    private var delegatePlayer: AudioPlayer? = null

    init {
        val audioPlayer = MediaPlayerAudioPlayer(application) {
            viewModelScope.launch {
                _messages.emit("音声の再生に失敗しました")
            }
        }
        setPlayer(audioPlayer)
        loadInitialState()
        observeUsbSerial()
    }

    private fun setPlayer(newPlayer: AudioPlayer) {
        delegatePlayer?.release()
        delegatePlayer = newPlayer
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            repository.initialize(AudioPolicyProvider.current().includeBundledAudio)
            store.observe().collect { stored ->
                val userItems = stored.userItems
                val bundledBellIds = repository.departureBells.value.map { it.id }.toSet()
                val bundledDoorIds = repository.doorAnnouncements.value.map { it.id }.toSet()
                userItems.forEach { item ->
                    val exists = when (item.category) {
                        AudioCategory.DepartureBell -> item.id in bundledBellIds || repository.departureBells.value.any { it.id == item.id }
                        AudioCategory.DoorAnnouncement -> item.id in bundledDoorIds || repository.doorAnnouncements.value.any { it.id == item.id }
                    }
                    if (!exists) repository.appendUserAudio(item)
                }
                selectedBellId.value = stored.selectedBellId
                selectedDoorId.value = stored.selectedDoorId
            }
        }
    }

    private fun observeUsbSerial() {
        viewModelScope.launch {
            usbSerialManager.dsrChanges.collect { state ->
                onSwitchPressed(state == DsrState.High)
            }
        }
        usbSerialManager.tryConnect()
    }

    fun onUsbDeviceAttached(device: UsbDevice) {
        usbSerialManager.onDeviceAttached(device)
    }

    fun onSwitchPressed(isOn: Boolean) {
        val state = uiState.value
        val target = if (isOn) {
            state.departureBells.firstOrNull { it.id == state.selectedBellId }
        } else {
            state.doorAnnouncements.firstOrNull { it.id == state.selectedDoorId }
        }
        if (target == null) {
            viewModelScope.launch { _messages.emit("再生できる音源がありません") }
            return
        }
        delegatePlayer?.play(target)
    }

    fun onLinkedModeTap() {
        viewModelScope.launch { _messages.emit("物理スイッチ連動中です") }
    }

    fun onSelectBell(id: String) {
        selectedBellId.value = id
        persistSelectedIds()
    }

    fun onSelectDoor(id: String) {
        selectedDoorId.value = id
        persistSelectedIds()
    }

    fun onOpenSettings() {
        settingsOpen.value = true
    }

    fun onCloseSettings() {
        settingsOpen.value = false
    }

    fun onAddAudio(uri: Uri, fileName: String, category: AudioCategory) {
        viewModelScope.launch {
            val id = "user-${UUID.randomUUID()}"
            val item = AudioItem(
                id = id,
                name = fileName.removeSuffix(".mp3"),
                category = category,
                sourceType = AudioSourceType.UserUri,
                uriOrResName = uri.toString(),
                isCustom = true,
            )
            repository.appendUserAudio(item)
            persistUserItems()
            ensureSelectedIdsAreValid()
            persistSelectedIds()
        }
    }

    fun onRenameAudio(id: String, newName: String, category: AudioCategory) {
        viewModelScope.launch {
            repository.renameAudio(id, newName, category)
            persistUserItems()
        }
    }

    fun onRetrimAudio(id: String, trimStartMs: Long, trimEndMs: Long?, category: AudioCategory) {
        viewModelScope.launch {
            repository.retrimAudio(id, trimStartMs, trimEndMs, category)
            persistUserItems()
        }
    }

    fun onDeleteAudio(id: String, category: AudioCategory) {
        viewModelScope.launch {
            repository.removeAudio(id, category)
            ensureSelectedIdsAreValid()
            persistUserItems()
            persistSelectedIds()
        }
    }

    private fun ensureSelectedIdsAreValid() {
        val bells = repository.departureBells.value
        val doors = repository.doorAnnouncements.value

        if (selectedBellId.value != null && bells.none { it.id == selectedBellId.value }) {
            selectedBellId.value = bells.firstOrNull()?.id
        }
        if (selectedDoorId.value != null && doors.none { it.id == selectedDoorId.value }) {
            selectedDoorId.value = doors.firstOrNull()?.id
        }
    }

    private fun persistSelectedIds() {
        viewModelScope.launch {
            store.saveSelectedIds(
                selectedBellId = selectedBellId.value,
                selectedDoorId = selectedDoorId.value,
            )
        }
    }

    private suspend fun persistUserItems() {
        val userItems = (repository.departureBells.value + repository.doorAnnouncements.value)
            .filter { it.isCustom }
        store.saveUserItems(userItems)
    }

    override fun onCleared() {
        super.onCleared()
        delegatePlayer?.release()
        usbSerialManager.release()
    }
}
