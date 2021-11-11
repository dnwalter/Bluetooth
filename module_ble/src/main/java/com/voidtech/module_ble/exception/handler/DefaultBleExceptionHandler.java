package com.voidtech.module_ble.exception.handler;

import com.voidtech.module_ble.exception.ConnectException;
import com.voidtech.module_ble.exception.GattException;
import com.voidtech.module_ble.exception.InitiatedException;
import com.voidtech.module_ble.exception.OtherException;
import com.voidtech.module_ble.exception.TimeoutException;

/**
 * @Description: 异常默认处理
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/14 10:35.
 */
public class DefaultBleExceptionHandler extends BleExceptionHandler {
    @Override
    protected void onConnectException(ConnectException e) {
    }

    @Override
    protected void onGattException(GattException e) {
    }

    @Override
    protected void onTimeoutException(TimeoutException e) {
    }

    @Override
    protected void onInitiatedException(InitiatedException e) {
    }

    @Override
    protected void onOtherException(OtherException e) {
    }
}
