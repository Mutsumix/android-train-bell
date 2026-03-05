package com.andbell.app.serial

import android.app.PendingIntent
import android.os.Build
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
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
    private var usbAttachReceiver: BroadcastReceiver? = null

    companion object {
        private const val TAG = "UsbSerial"
        private const val ACTION_USB_PERMISSION = "com.andbell.app.USB_PERMISSION"
        private const val POLL_INTERVAL_MS = 50L
    }

    init {
        registerUsbAttachReceiver()
    }

    /** アプリ起動時・USB接続時に呼ぶ。権限があればそのまま接続、なければ権限ダイアログを表示。 */
    fun tryConnect() {
        val devices = usbManager.deviceList.values.toList()
        Log.d(TAG, "tryConnect: ${devices.size} device(s) found: ${devices.map { it.deviceName }}")
        val device = devices.firstOrNull { UsbSerialProber.getDefaultProber().probeDevice(it) != null }
        if (device == null) {
            Log.d(TAG, "tryConnect: no compatible device found")
            return
        }
        Log.d(TAG, "tryConnect: compatible device=${device.deviceName} hasPermission=${usbManager.hasPermission(device)}")
        if (usbManager.hasPermission(device)) {
            openDevice(device)
        } else {
            requestPermission(device)
        }
    }

    /** MainActivity の onNewIntent から USB_DEVICE_ATTACHED を受け取る */
    fun onDeviceAttached() {
        Log.d(TAG, "onDeviceAttached: triggered")
        if (_isConnected.value) {
            Log.d(TAG, "onDeviceAttached: already connected, ignoring")
            return
        }
        tryConnect()
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
        usbAttachReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        usbAttachReceiver = null
    }

    private fun registerUsbAttachReceiver() {
        // マニフェストの USB_DEVICE_ATTACHED intent-filter はアプリ起動専用。
        // 起動中のアプリへの USB 接続通知には動的 BroadcastReceiver が必要。
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != UsbManager.ACTION_USB_DEVICE_ATTACHED) return
                Log.d(TAG, "USB attached (dynamic receiver)")
                if (!_isConnected.value) tryConnect()
            }
        }
        ContextCompat.registerReceiver(
            context, receiver,
            IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED),
            ContextCompat.RECEIVER_EXPORTED,
        )
        usbAttachReceiver = receiver
    }

    private fun findCompatibleDevice(): UsbDevice? =
        usbManager.deviceList.values.firstOrNull {
            UsbSerialProber.getDefaultProber().probeDevice(it) != null
        }

    private fun requestPermission(device: UsbDevice) {
        if (permissionReceiver != null) {
            Log.d(TAG, "requestPermission: already pending, skipping")
            return
        }
        // Android 12+ では FLAG_MUTABLE が必須（システムが EXTRA_PERMISSION_GRANTED を追加できるように）。
        // Android 14+ では FLAG_MUTABLE + 暗黙的インテントは不可。setPackage で明示的にする。
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(ACTION_USB_PERMISSION).apply { setPackage(context.packageName) },
            flags,
        )
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != ACTION_USB_PERMISSION) return
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                Log.d(TAG, "permission result: granted=$granted")
                if (granted) {
                    // intent の EXTRA_DEVICE は Android バージョンによって取れないことがあるため
                    // deviceList から直接スキャンして取得する
                    val dev = findCompatibleDevice()
                    if (dev != null) openDevice(dev)
                    else Log.e(TAG, "permission granted but no compatible device found")
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
        usbManager.requestPermission(device, pendingIntent)
    }

    private fun openDevice(device: UsbDevice) {
        runCatching {
            Log.d(TAG, "openDevice: probing driver for ${device.deviceName}")
            val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            if (driver == null) {
                Log.e(TAG, "openDevice: no driver found")
                return
            }
            Log.d(TAG, "openDevice: driver=${driver::class.simpleName}, opening connection")
            val connection = usbManager.openDevice(device)
            if (connection == null) {
                Log.e(TAG, "openDevice: openDevice() returned null (permission denied?)")
                return
            }
            val p = driver.ports[0]
            Log.d(TAG, "openDevice: calling port.open()")
            p.open(connection)
            Log.d(TAG, "openDevice: setParameters")
            p.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            Log.d(TAG, "openDevice: setDTR(true)")
            p.setDTR(true)
            port = p
            _isConnected.value = true
            Log.d(TAG, "openDevice: connected successfully, starting monitor")
            startMonitoring()
        }.onFailure { e ->
            Log.e(TAG, "openDevice: failed with exception", e)
            disconnect()
        }
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.IO) {
            Log.d(TAG, "startMonitoring: loop started")
            var lastState = DsrState.Low
            while (isActive) {
                try {
                    val lines = port!!.controlLines
                    val currentState = if (UsbSerialPort.ControlLine.DSR in lines) DsrState.High else DsrState.Low
                    if (currentState != lastState) {
                        lastState = currentState
                        Log.d(TAG, "startMonitoring: DSR changed -> $currentState")
                        withContext(Dispatchers.Main) {
                            _dsrChanges.emit(currentState)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "startMonitoring: error reading control lines, stopping", e)
                    break
                }
                delay(POLL_INTERVAL_MS)
            }
            Log.d(TAG, "startMonitoring: loop ended, disconnecting")
            withContext(Dispatchers.Main) { disconnect() }
        }
    }

}
