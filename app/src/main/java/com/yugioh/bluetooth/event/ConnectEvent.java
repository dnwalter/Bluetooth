package com.yugioh.bluetooth.event;

import android.bluetooth.BluetoothDevice;

import com.voidtech.module_ble.core.DeviceMirror;
import com.voidtech.module_ble.model.BluetoothLeDevice;

public class ConnectEvent{
    private boolean isSuccess;
    private boolean isDisconnected;
    private BluetoothDevice device;

    public boolean isSuccess() {
        return isSuccess;
    }

    public ConnectEvent setSuccess(boolean success) {
        isSuccess = success;
        return this;
    }

    public boolean isDisconnected() {
        return isDisconnected;
    }

    public ConnectEvent setDisconnected(boolean disconnected) {
        isDisconnected = disconnected;
        return this;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public String getAddress() {
        String address = "";
        if (device != null) {
            address = device.getAddress();
        }
        return address;
    }

    public ConnectEvent setDevice(BluetoothDevice deviceMirror) {
        this.device = deviceMirror;
        return this;
    }
}
