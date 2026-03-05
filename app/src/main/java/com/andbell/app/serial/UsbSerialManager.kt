package com.andbell.app.serial

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsbSerialManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _dsrChanges = MutableSharedFlow<DsrState>(extraBufferCapacity = 8)
    val dsrChanges: SharedFlow<DsrState> = _dsrChanges.asSharedFlow()

    private var port: UsbSerialPort? = null
    private var monitorJob: Job? = null
    private var permissionReceiver: BroadcastReceiver? = null

    companion object {
        private const val ACTION_USB_PERMISSION = "com.andbell.app.USB_PERMISSION"
        private const val POLL_INTERVAL_MS = 50L
    }

    /** アプリ起動時・USB接続時に呼ぶ。権限があればそのまま接続、なければ権限ダイアログを表示。 */
    fun tryConnect() {
        val device = findCompatibleDevice() ?: return
        if (usbManager.hasPermission(device)) {
            openDevice(device)
        } else {
            requestPermission(device)
        }
    }

    /** MainActivity の onNewIntent から USB_DEVICE_ATTACHED を受け取る */
    fun onDeviceAttached(device: UsbDevice) {
        if (UsbSerialProber.getDefaultProber().probeDevice(device) == null) return
        if (_isConnected.value) return
        if (usbManager.hasPermission(device)) {
            openDevice(device)
        } else {
            requestPermission(device)
        }
    }

    fun disconnect() {
        monitorJob?.cancel()
        monitorJob = null
        runCatching { port?.close() }
        port = null
        _isConnected.value = false
    }

    fun release() {
        disconnect()
        permissionReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        permissionReceiver = null
    }

    private fun findCompatibleDevice(): UsbDevice? =
        usbManager.deviceList.values.firstOrNull {
            UsbSerialProber.getDefaultProber().probeDevice(it) != null
        }

    private fun requestPermission(device: UsbDevice) {
        val intent = PendingIntent.getBroadcast(
            context, 0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != ACTION_USB_PERMISSION) return
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (granted) {
                    @Suppress("DEPRECATION")
                    val dev = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (dev != null) openDevice(dev)
                }
                runCatching { ctx.unregisterReceiver(this) }
                permissionReceiver = null
            }
        }
        permissionReceiver = receiver
        ContextCompat.registerReceiver(
            context, receiver,
            IntentFilter(ACTION_USB_PERMISSION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        usbManager.requestPermission(device, intent)
    }

    private fun openDevice(device: UsbDevice) {
        runCatching {
            val driver = UsbSerialProber.getDefaultProber().probeDevice(device) ?: return
            val connection = usbManager.openDevice(device) ?: return
            val p = driver.ports[0]
            p.open(connection)
            p.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            p.setDTR(true)
            port = p
            _isConnected.value = true
            startMonitoring()
        }.onFailure {
            disconnect()
        }
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.IO) {
            var lastState = DsrState.Low
            while (isActive) {
                try {
                    val lines = port!!.controlLines
                    val currentState = if (UsbSerialPort.ControlLine.DSR in lines) DsrState.High else DsrState.Low
                    if (currentState != lastState) {
                        lastState = currentState
                        withContext(Dispatchers.Main) {
                            _dsrChanges.emit(currentState)
                        }
                    }
                } catch (e: Exception) {
                    break
                }
                delay(POLL_INTERVAL_MS)
            }
            withContext(Dispatchers.Main) { disconnect() }
        }
    }
}
