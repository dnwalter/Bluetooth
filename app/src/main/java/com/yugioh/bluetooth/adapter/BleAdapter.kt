package com.yugioh.bluetooth.adapter

import android.bluetooth.BluetoothDevice
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.voidtech.module_ble.model.BluetoothLeDevice
import com.yugioh.bluetooth.R

class BleAdapter : BaseQuickAdapter<BluetoothLeDevice, BaseViewHolder>(R.layout.adapter_ble) {
    private val mDeviceMap = mutableMapOf<String, Int>()

    fun getDeviceIndex(address: String) : Int {
        if (mDeviceMap.containsKey(address)) {
            return mDeviceMap[address]!!
        }

        return -1
    }

    override fun convert(holder: BaseViewHolder, item: BluetoothLeDevice) {
        mDeviceMap[item.address] = holder.adapterPosition
        holder.setText(R.id.tv_ble_name, item.name)
        holder.setText(R.id.tv_connect, if (item.state == BluetoothDevice.BOND_BONDED) "已连接" else "未连接")
        holder.setText(R.id.btn_connect, if (item.state == BluetoothDevice.BOND_BONDED) "断开连接" else "点击连接")
    }
}