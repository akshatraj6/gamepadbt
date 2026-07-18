package com.gamepadbt.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.gamepadbt.app.ui.GamepadBTApp

// Screens the app can show.
enum class Screen { HOME, DEVICES, GAMEPAD, SETTINGS }

// The different on-screen controller layouts.
enum class Layout { STANDARD, SIMPLE_REMOTE, TWIN_STICK }

class MainActivity : ComponentActivity() {

    private lateinit var hidManager: BluetoothHidManager
    private lateinit var vibrator: Vibrator

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) hidManager.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hidManager = BluetoothHidManager(this)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        ensureBluetoothPermission()

        setContent {
            GamepadBTApp(hidManager = hidManager, onVibrate = { vibrateTick() })
        }
    }

    private fun ensureBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED
            ) {
                hidManager.start()
            } else {
                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            // Pre-Android 12, BLUETOOTH/BLUETOOTH_ADMIN are normal (install-time) permissions.
            hidManager.start()
        }
    }

    private fun vibrateTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun onDestroy() {
        hidManager.unregister()
        super.onDestroy()
    }
}
