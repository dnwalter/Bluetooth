package com.yugioh.bluetooth.event;

import com.voidtech.module_ble.core.BluetoothGattChannel;
import com.voidtech.module_ble.model.BluetoothLeDevice;

public class NotifyDataEvent {
    private byte[] data;
    private BluetoothLeDevice bluetoothLeDevice;
    private BluetoothGattChannel bluetoothGattChannel;

    public byte[] getData() {
        return data;
    }

    public NotifyDataEvent setData(byte[] data) {
        this.data = data;
        return this;
    }

    public BluetoothLeDevice getBluetoothLeDevice() {
        return bluetoothLeDevice;
    }

    public NotifyDataEvent setBluetoothLeDevice(BluetoothLeDevice bluetoothLeDevice) {
        this.bluetoothLeDevice = bluetoothLeDevice;
        return this;
    }

    public BluetoothGattChannel getBluetoothGattChannel() {
        return bluetoothGattChannel;
    }

    public NotifyDataEvent setBluetoothGattChannel(BluetoothGattChannel bluetoothGattChannel) {
        this.bluetoothGattChannel = bluetoothGattChannel;
        return this;
    }
}
