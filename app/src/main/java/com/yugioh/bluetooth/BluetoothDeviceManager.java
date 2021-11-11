package com.yugioh.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.voidtech.module_ble.ViseBle;
import com.voidtech.module_ble.callback.IBleCallback;
import com.voidtech.module_ble.callback.IConnectCallback;
import com.voidtech.module_ble.common.PropertyType;
import com.voidtech.module_ble.core.BluetoothGattChannel;
import com.voidtech.module_ble.core.DeviceMirror;
import com.voidtech.module_ble.core.DeviceMirrorPool;
import com.voidtech.module_ble.exception.BleException;
import com.voidtech.module_ble.model.BluetoothLeDevice;
import com.voidtech.module_ble.utils.HexUtil;
import com.yugioh.bluetooth.event.CallbackDataEvent;
import com.yugioh.bluetooth.event.ConnectEvent;
import com.yugioh.bluetooth.event.NotifyDataEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class BluetoothDeviceManager {
    private final static String TAG = "BluetoothManager";
    private DeviceMirrorPool mDeviceMirrorPool;
    private ConnectEvent mConnectEvent = new ConnectEvent();
    private CallbackDataEvent mCallbackDataEvent = new CallbackDataEvent();
    private NotifyDataEvent mNotifyDataEvent = new NotifyDataEvent();

    private static class SingletonHolder {
        private static final BluetoothDeviceManager INSTANCE = new BluetoothDeviceManager();
    }
    private BluetoothDeviceManager(){}
    public static final BluetoothDeviceManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 连接回调
     */
    private IConnectCallback mConnectCallback = new IConnectCallback() {

        @Override
        public void onConnectSuccess(final DeviceMirror deviceMirror) {
            EventBus.getDefault().post(mConnectEvent.setDevice(deviceMirror.getBluetoothLeDevice().getDevice()).setSuccess(true));
        }

        @Override
        public void onConnectFailure(BleException exception) {
            EventBus.getDefault().post(mConnectEvent.setSuccess(false).setDisconnected(false));
        }

        @Override
        public void onDisconnect(BluetoothDevice device, boolean isActive) {
            EventBus.getDefault().post(mConnectEvent.setDevice(device).setSuccess(false).setDisconnected(true));
        }
    };

    /**
     * 接收数据回调
     */
    private IBleCallback mReceiveCallback = new IBleCallback() {
        @Override
        public void onSuccess(final byte[] data, BluetoothGattChannel bluetoothGattInfo, BluetoothLeDevice bluetoothLeDevice) {
            if (data == null) {
                return;
            }
            Log.d(TAG, "notify success:" + HexUtil.encodeHexStr(data));
            EventBus.getDefault().post(mNotifyDataEvent.setData(data)
                    .setBluetoothLeDevice(bluetoothLeDevice)
                    .setBluetoothGattChannel(bluetoothGattInfo));
        }

        @Override
        public void onFailure(BleException exception) {
            if (exception == null) {
                return;
            }
        }
    };

    /**
     * 发送数据回调
     */
    private IBleCallback mSendCallback = new IBleCallback() {
        @Override
        public void onSuccess(final byte[] data, BluetoothGattChannel bluetoothGattInfo, BluetoothLeDevice bluetoothLeDevice) {
            if (data == null) {
                return;
            }
            Log.d(TAG, "callback success:" + HexUtil.encodeHexStr(data));
            EventBus.getDefault().post(mCallbackDataEvent.setData(data).setSuccess(true)
                    .setBluetoothLeDevice(bluetoothLeDevice)
                    .setBluetoothGattChannel(bluetoothGattInfo));
            if (bluetoothGattInfo != null && (bluetoothGattInfo.getPropertyType() == PropertyType.PROPERTY_INDICATE
                    || bluetoothGattInfo.getPropertyType() == PropertyType.PROPERTY_NOTIFY)) {
                DeviceMirror deviceMirror = mDeviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
                if (deviceMirror != null) {
                    deviceMirror.setNotifyListener(bluetoothGattInfo.getGattInfoKey(), mReceiveCallback);
                }
            }
        }

        @Override
        public void onFailure(BleException exception) {
            if (exception == null) {
                return;
            }
            EventBus.getDefault().post(mCallbackDataEvent.setSuccess(false));
        }
    };

    public void init(Context context) {
        if (context == null) {
            return;
        }
        //蓝牙相关配置修改
        ViseBle.config()
                .setScanTimeout(10000) // todo ousy
                .setScanRepeatInterval(5 * 1000)//扫描间隔5秒
                .setConnectTimeout(10 * 1000)//连接超时时间
                .setOperateTimeout(5 * 1000)//设置数据操作超时时间
                .setConnectRetryCount(3)//设置连接失败重试次数
                .setConnectRetryInterval(1000)//设置连接失败重试间隔时间
                .setOperateRetryCount(3)//设置数据操作失败重试次数
                .setOperateRetryInterval(1000)//设置数据操作失败重试间隔时间
                .setMaxConnectCount(3);//设置最大连接设备数量
        //蓝牙信息初始化，全局唯一，必须在应用初始化时调用
        ViseBle.getInstance().init(context.getApplicationContext());
        mDeviceMirrorPool = ViseBle.getInstance().getDeviceMirrorPool();
    }

    public void connect(BluetoothLeDevice bluetoothLeDevice) {
        ViseBle.getInstance().connect(bluetoothLeDevice, mConnectCallback);
    }

    public void disconnect(BluetoothLeDevice bluetoothLeDevice) {
        ViseBle.getInstance().disconnect(bluetoothLeDevice);
    }

    public boolean isConnected(BluetoothLeDevice bluetoothLeDevice) {
        return ViseBle.getInstance().isConnect(bluetoothLeDevice);
    }

    public void bindChannel(BluetoothLeDevice bluetoothLeDevice, PropertyType propertyType, UUID serviceUUID,
                            UUID characteristicUUID, UUID descriptorUUID) {
        DeviceMirror deviceMirror = mDeviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
        if (deviceMirror != null) {
            BluetoothGattChannel bluetoothGattChannel = new BluetoothGattChannel.Builder()
                    .setBluetoothGatt(deviceMirror.getBluetoothGatt())
                    .setPropertyType(propertyType)
                    .setServiceUUID(serviceUUID)
                    .setCharacteristicUUID(characteristicUUID)
                    .setDescriptorUUID(descriptorUUID)
                    .builder();
            deviceMirror.bindChannel(mSendCallback, bluetoothGattChannel);
        }
    }

    public void write(final BluetoothLeDevice bluetoothLeDevice, byte[] data) {
        if (mDataInfoQueue != null) {
            mDataInfoQueue.clear();
            mDataInfoQueue = splitPacketFor20Byte(data);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    send(bluetoothLeDevice);
                }
            });
        }
    }

    public void write(BluetoothLeDevice bluetoothLeDevice, String msg) {
        DeviceMirror deviceMirror = mDeviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
        deviceMirror.write(msg);
    }

    public void read(BluetoothLeDevice bluetoothLeDevice) {
        DeviceMirror deviceMirror = mDeviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
        if (deviceMirror != null) {
            deviceMirror.readData();
        }
    }

    public void registerNotify(BluetoothLeDevice bluetoothLeDevice, boolean isIndicate) {
        DeviceMirror deviceMirror = mDeviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
        if (deviceMirror != null) {
            deviceMirror.registerNotify(isIndicate);
        }
    }

    //发送队列，提供一种简单的处理方式，实际项目场景需要根据需求优化
    private Queue<byte[]> mDataInfoQueue = new LinkedList<>();
    private void send(final BluetoothLeDevice bluetoothLeDevice) {
        if (mDataInfoQueue != null && !mDataInfoQueue.isEmpty()) {
            DeviceMirror deviceMirror = mDeviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
            if (mDataInfoQueue.peek() != null && deviceMirror != null) {
                deviceMirror.writeData(mDataInfoQueue.poll());
            }
            if (mDataInfoQueue.peek() != null) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        send(bluetoothLeDevice);
                    }
                }, 100);
            }
        }
    }

    /**
     * 数据分包
     *
     * @param data
     * @return
     */
    private Queue<byte[]> splitPacketFor20Byte(byte[] data) {
        Queue<byte[]> dataInfoQueue = new LinkedList<>();
        if (data != null) {
            int index = 0;
            do {
                byte[] surplusData = new byte[data.length - index];
                byte[] currentData;
                System.arraycopy(data, index, surplusData, 0, data.length - index);
                if (surplusData.length <= 20) {
                    currentData = new byte[surplusData.length];
                    System.arraycopy(surplusData, 0, currentData, 0, surplusData.length);
                    index += surplusData.length;
                } else {
                    currentData = new byte[20];
                    System.arraycopy(data, index, currentData, 0, 20);
                    index += 20;
                }
                dataInfoQueue.offer(currentData);
            } while (index < data.length);
        }
        return dataInfoQueue;
    }
}
