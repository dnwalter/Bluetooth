package com.voidtech.module_ble.callback;

import com.voidtech.module_ble.exception.BleException;

/**
 * @Description: 获取信号值回调
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/19 15:19
 */
public interface IRssiCallback {
    void onSuccess(int rssi);

    void onFailure(BleException exception);
}
