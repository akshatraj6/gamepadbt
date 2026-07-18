package com.gamepadbt.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gamepadbt.app.BluetoothHidManager
import com.gamepadbt.app.Layout
import com.gamepadbt.app.Screen

@Composable
fun GamepadBTApp(hidManager: BluetoothHidManager, onVibrate: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gamepadbt", Context.MODE_PRIVATE) }

    var screen by remember { mutableStateOf(Screen.HOME) }
    var connected by remember { mutableStateOf(false) }
    var connectedName by remember { mutableStateOf<String?>(null) }
    var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean("vibration", true)) }
    var currentLayout by remember {
        mutableStateOf(Layout.valueOf(prefs.getString("layout", Layout.STANDARD.name)!!))
    }

    DisposableEffect(Unit) {
        hidManager.onConnectionChanged = { isConnected, name ->
            connected = isConnected
            connectedName = name
            if (isConnected) screen = Screen.GAMEPAD
        }
        onDispose { hidManager.onConnectionChanged = null }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    connected = connected,
                    connectedName = connectedName,
                    onFindDevices = { screen = Screen.DEVICES },
                    onOpenGamepad = { screen = Screen.GAMEPAD },
                    onOpenSettings = { screen = Screen.SETTINGS },
                    onMakeDiscoverable = {
                        (context as? android.app.Activity)?.let { hidManager.requestDiscoverable(it) }
                    }
                )
                Screen.DEVICES -> DeviceListScreen(
                    hidManager = hidManager,
                    onBack = { screen = Screen.HOME },
                    onConnect = { device -> hidManager.connect(device) }
                )
                Screen.GAMEPAD -> GamepadScreen(
                    layout = currentLayout,
                    connected = connected,
                    connectedName = connectedName,
                    vibrationEnabled = vibrationEnabled,
                    onVibrate = onVibrate,
                    onSendReport = { report -> hidManager.sendReport(report) },
                    onBack = { screen = Screen.HOME },
                    onChangeLayout = {
                        currentLayout = it
                        prefs.edit().putString("layout", it.name).apply()
                    }
                )
                Screen.SETTINGS -> SettingsScreen(
                    vibrationEnabled = vibrationEnabled,
                    onVibrationChanged = {
                        vibrationEnabled = it
                        prefs.edit().putBoolean("vibration", it).apply()
                    },
                    onBack = { screen = Screen.HOME },
                    onDisconnect = { hidManager.disconnect() }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    connected: Boolean,
    connectedName: String?,
    onFindDevices: () -> Unit,
    onOpenGamepad: () -> Unit,
    onOpenSettings: () -> Unit,
    onMakeDiscoverable: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("GamepadBT", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(if (connected) "Connected to ${connectedName ?: "TV"}" else "Not connected")
        Spacer(Modifier.height(24.dp))

        Button(onClick = onMakeDiscoverable, modifier = Modifier.fillMaxWidth()) {
            Text("1. Make phone discoverable")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "On the TV: Settings > Remotes & Accessories > Add accessory, " +
                "then pick this phone from the list.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(16.dp))
        Button(onClick = onFindDevices, modifier = Modifier.fillMaxWidth()) {
            Text("2. Connect to a paired TV")
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenGamepad, enabled = connected, modifier = Modifier.fillMaxWidth()) {
            Text("3. Open gamepad")
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            OutlinedButton(onClick = onOpenSettings) {
                Text("Settings")
            }
        }
    }
}
