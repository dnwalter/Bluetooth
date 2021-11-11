package com.yugioh.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.voidtech.module_ble.ViseBle
import com.voidtech.module_ble.common.PropertyType
import com.voidtech.module_ble.model.BluetoothLeDevice
import com.voidtech.module_ble.model.resolver.GattAttributeResolver
import com.voidtech.module_ble.utils.HexUtil
import com.yugioh.bluetooth.event.NotifyDataEvent
import kotlinx.android.synthetic.main.activity_device_control.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class DeviceControlActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DEVICE = "extra_device"
    }

    //设备信息
    private lateinit var mDevice: BluetoothLeDevice
    private var mWriteDialog: AlertDialog? = null
    private var mReadDialog: AlertDialog? = null
    private var mNotifyDialog: AlertDialog? = null
    private var mServiceDialog: AlertDialog? = null
    private lateinit var mServiceList: List<BluetoothGattService>
    private var mWriteCList = mutableListOf<BluetoothGattCharacteristic>()
    private var mReadCList = mutableListOf<BluetoothGattCharacteristic>()
    private var mNotifyCList = mutableListOf<BluetoothGattCharacteristic>()
    private var mServiceIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        EventBus.getDefault().register(this)
        mDevice = intent.getParcelableExtra(EXTRA_DEVICE)!!

        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private fun initView() {
        tv_ble_name.text = mDevice.name
        tv_mac.text = mDevice.address

        btn_send.setOnClickListener {
            if (et_send.text == null) {
                Toast.makeText(this@DeviceControlActivity, "请输入内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            BluetoothDeviceManager.getInstance().write(mDevice, et_send.text.toString())
        }

        tv_service.setOnClickListener {
            mServiceDialog?.show()
        }

        tv_write.setOnClickListener {
            mWriteDialog?.show()
        }

        tv_read.setOnClickListener {
            mReadDialog?.show()
        }

        tv_notify.setOnClickListener {
            mNotifyDialog?.show()
        }

        initDialog()
    }

    private fun initDialog() {
        val unknownServiceString = resources.getString(R.string.unknown_service)
        val deviceMirror = ViseBle.getInstance().getDeviceMirror(mDevice)

        mServiceList = deviceMirror.gattServiceList
        val items = arrayOfNulls<String>(mServiceList.size)
        for (i in mServiceList.indices) {
            val uuid = mServiceList[i].uuid.toString()
            val name = GattAttributeResolver.getAttributeName(uuid, unknownServiceString)
            items[i] = name + "\n" + uuid
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("选择服务")
        builder.setItems(items) { _, position ->
            mServiceIndex = position
            tv_service.text = mServiceList[mServiceIndex].uuid.toString()
            setWRDialog(mServiceList[mServiceIndex].characteristics)
            mServiceDialog?.dismiss()
        }
        mServiceDialog = builder.create()
    }

    private fun setWRDialog(characteristics: List<BluetoothGattCharacteristic>) {
        val unknownCharaString = resources.getString(R.string.unknown_characteristic)

        mWriteCList.clear()
        mReadCList.clear()
        mNotifyCList.clear()
        var wItems = mutableListOf<String>()
        var rItems = mutableListOf<String>()
        var nItems = mutableListOf<String>()

        for (model in characteristics) {
            val uuid = model.uuid.toString()
            val name = GattAttributeResolver.getAttributeName(uuid, unknownCharaString)
            var content = StringBuilder(name + "\n" + uuid + "\n")
            val properties = model.properties
            if ((properties and BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                content.append("[WRITE]")
            }
            if ((properties and BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                content.append("[READ]")
            }
            if ((properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                content.append("[NOTIFY]")
            }
            if ((properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                content.append("[INDICATE]")
            }

            if ((properties and BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                mWriteCList.add(model)
                wItems.add(content.toString())
            }
            if ((properties and BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                mReadCList.add(model)
                rItems.add(content.toString())
            }
            if ((properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0 || (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                mNotifyCList.add(model)
                nItems.add(content.toString())
            }
        }

        if (wItems.size > 0) {
            tv_write.text = "点击查看相关特征"
            mWriteDialog = buildDialog("选择写特征", wItems.toTypedArray(), DialogInterface.OnClickListener { _, index ->
                val characteristic = mWriteCList[index]
                tv_write.text = characteristic.uuid.toString()
                BluetoothDeviceManager.getInstance().bindChannel(
                    mDevice,
                    PropertyType.PROPERTY_WRITE,
                    mServiceList[mServiceIndex].uuid,
                    characteristic.uuid,
                    null
                )
                mWriteDialog?.dismiss()
            })
        } else {
            mWriteDialog = null
            tv_write.text = "该服务没有写特征"
        }

        if (rItems.size > 0) {
            tv_read.text = "点击查看相关特征"
            mReadDialog = buildDialog("选择读特征", rItems.toTypedArray(), DialogInterface.OnClickListener { _, index ->
                val characteristic = mReadCList[index]
                tv_read.text = characteristic.uuid.toString()
                BluetoothDeviceManager.getInstance().bindChannel(
                    mDevice,
                    PropertyType.PROPERTY_READ,
                    mServiceList[mServiceIndex].uuid,
                    characteristic.uuid,
                    null
                )
                BluetoothDeviceManager.getInstance().read(mDevice)
                mReadDialog?.dismiss()
            })
        } else {
            mReadDialog = null
            tv_read.text = "该服务没有读特征"
        }

        if (nItems.size > 0) {
            tv_notify.text = "点击查看相关特征"
            mNotifyDialog = buildDialog("选择通知特征", nItems.toTypedArray(), DialogInterface.OnClickListener { _, index ->
                val characteristic = mNotifyCList[index]
                val isNotIndicate = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0
                tv_notify.text = characteristic.uuid.toString()
                BluetoothDeviceManager.getInstance().bindChannel(
                    mDevice,
                    if (isNotIndicate) PropertyType.PROPERTY_NOTIFY else PropertyType.PROPERTY_INDICATE,
                    mServiceList[mServiceIndex].uuid,
                    characteristic.uuid,
                    null
                )
                BluetoothDeviceManager.getInstance().registerNotify(mDevice, !isNotIndicate)
                mNotifyDialog?.dismiss()
            })
        } else {
            mNotifyDialog = null
            tv_notify.text = "该服务没有通知特征"
        }
    }

    private fun buildDialog(title: String, items: Array<String>, listener: DialogInterface.OnClickListener): AlertDialog? {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setItems(items, listener)
        return builder.create()
    }

    @Subscribe
    fun showDeviceNotifyData(event: NotifyDataEvent?) {
        if (event?.data != null && event.bluetoothLeDevice != null && event.bluetoothLeDevice.address.equals(mDevice.address)) {
//            mOutputInfo.append(HexUtil.encodeHexStr(event.data)).append("\n")
            tv_receive.text = String(event.data)
        }
    }
}