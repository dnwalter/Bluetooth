package com.voidtech.module_ble.callback;

import com.voidtech.module_ble.core.BluetoothGattChannel;
import com.voidtech.module_ble.exception.BleException;
import com.voidtech.module_ble.model.BluetoothLeDevice;

/**
 * @Description: 操作数据回调
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/17 19:42
 */
public interface IBleCallback {
    void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice);

    void onFailure(BleException exception);
}
