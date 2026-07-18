package com.gamepadbt.app.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gamepadbt.app.BluetoothHidManager

@SuppressLint("MissingPermission")
@Composable
fun DeviceListScreen(
    hidManager: BluetoothHidManager,
    onBack: () -> Unit,
    onConnect: (BluetoothDevice) -> Unit
) {
    val devices = remember { hidManager.bondedDevices().toList() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("< Back") }
        Text("Paired devices", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Only shows devices already paired in your phone's Bluetooth " +
                "settings. Pair with your TV there first if it isn't listed.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))

        if (devices.isEmpty()) {
            Text("No paired devices found.")
        } else {
            LazyColumn {
                items(devices) { device ->
                    ListItem(
                        headlineContent = { Text(device.name ?: "Unknown device") },
                        supportingContent = { Text(device.address) },
                        trailingContent = {
                            Button(onClick = { onConnect(device) }) { Text("Connect") }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
