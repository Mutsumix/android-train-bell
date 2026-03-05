package com.andbell.app

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.andbell.app.ui.home.HomeScreen
import com.andbell.app.ui.home.HomeViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by lazy {
        ViewModelProvider(
            this,
            viewModelFactory {
                initializer {
                    HomeViewModel(application)
                }
            }
        )[HomeViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
        handleUsbIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUsbIntent(intent)
    }

    private fun handleUsbIntent(intent: Intent) {
        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            @Suppress("DEPRECATION")
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
            viewModel.onUsbDeviceAttached(device)
        }
    }
}
