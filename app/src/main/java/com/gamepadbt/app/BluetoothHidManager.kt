package com.gamepadbt.app

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Wraps Android's BluetoothHidDevice profile so the phone can register
 * itself as a Bluetooth HID gamepad and exchange input reports with
 * whatever it's paired to (an Android TV, in our case).
 *
 * This talks to a real, documented Android system API, but it hasn't been
 * built or run against actual hardware from this environment - if pairing
 * behaves oddly on first try, the registerApp() call below is the most
 * likely spot needing adjustment.
 */
@SuppressLint("MissingPermission")
class BluetoothHidManager(private val context: Context) {

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private var currentReport: ByteArray = GamepadDescriptor.emptyReport()

    var onConnectionChanged: ((Boolean, String?) -> Unit)? = null
    var onRegistered: ((Boolean) -> Unit)? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                registerApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) hidDevice = null
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            onRegistered?.invoke(registered)
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    onConnectionChanged?.invoke(true, device?.name)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (device == connectedDevice) connectedDevice = null
                    onConnectionChanged?.invoke(false, null)
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            hidDevice?.replyReport(device, BluetoothHidDevice.REPORT_TYPE_INPUT, id, currentReport)
        }
    }

    /** Starts the HID device profile proxy. Call once permission is granted. */
    fun start() {
        bluetoothAdapter?.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
    }

    private fun registerApp() {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "GamepadBT",
            "Virtual Bluetooth gamepad",
            "GamepadBT",
            BluetoothHidDevice.SUBCLASS1_NONE,
            GamepadDescriptor.DESCRIPTOR
        )
        val qos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            11250
        )
        hidDevice?.registerApp(sdp, null, qos, executor, hidCallback)
    }

    /** Makes the phone Bluetooth-discoverable so the TV can find it to pair. */
    fun requestDiscoverable(activity: Activity, seconds: Int = 300) {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds)
        }
        activity.startActivity(intent)
    }

    /** Devices already paired via the phone's system Bluetooth settings. */
    fun bondedDevices(): Set<BluetoothDevice> = bluetoothAdapter?.bondedDevices ?: emptySet()

    fun connect(device: BluetoothDevice) {
        hidDevice?.connect(device)
    }

    fun disconnect() {
        connectedDevice?.let { hidDevice?.disconnect(it) }
    }

    fun sendReport(report: ByteArray) {
        currentReport = report
        connectedDevice?.let {
            hidDevice?.sendReport(it, GamepadDescriptor.REPORT_ID.toInt(), report)
        }
    }

    fun isConnected(): Boolean = connectedDevice != null

    fun unregister() {
        hidDevice?.unregisterApp()
    }
}
