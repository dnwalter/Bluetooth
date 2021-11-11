package com.yugioh.bluetooth.model

import com.voidtech.module_ble.model.BluetoothLeDevice
import android.bluetooth.BluetoothDevice

class BleModel(device: BluetoothLeDevice) {
    var name = ""
    var isConnected = false

    init {
        val bluetoothDevice = device.device
        name = bluetoothDevice.name
        isConnected = bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED
    }
}