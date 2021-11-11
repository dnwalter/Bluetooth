package com.yugioh.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.voidtech.module_ble.ViseBle
import com.voidtech.module_ble.callback.scan.IScanCallback
import com.voidtech.module_ble.callback.scan.KeywordFilterScanCallback
import com.voidtech.module_ble.model.BluetoothLeDevice
import com.voidtech.module_ble.model.BluetoothLeDeviceStore
import com.voidtech.module_ble.utils.BleUtil
import com.yugioh.bluetooth.adapter.BleAdapter
import com.yugioh.bluetooth.event.ConnectEvent
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_BLUETOOTH = 111
        const val REQUEST_CODE_LOCATION = 222
    }

    private lateinit var mAdapter: BleAdapter
    private val mBluetoothLeDeviceStore = BluetoothLeDeviceStore()

    // 是否正在检索蓝牙
    private var mIsSearch = false
    // 是否正在连接中
    private var mIsConnecting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        EventBus.getDefault().register(this)
        BluetoothDeviceManager.getInstance().init(this)

        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun showConnectedDevice(event: ConnectEvent?) {
        if (event != null) {
            if (event.isSuccess) {
                // 连接成功
                mIsConnecting = false
                runOnUiThread {
                    val index = mAdapter.getDeviceIndex(event.address)
                    if (index >= 0 && index < mAdapter.data.size) {
                        mAdapter.data[index].state = BluetoothDevice.BOND_BONDED
                        mAdapter.notifyItemChanged(index)
                    }
                }
            } else if (event.isDisconnected) {
                runOnUiThread {
                    val index = mAdapter.getDeviceIndex(event.address)
                    if (index >= 0 && index < mAdapter.data.size) {
                        mAdapter.data[index].state = BluetoothDevice.BOND_NONE
                        mAdapter.notifyItemChanged(index)
                    }
                }
            } else {
                // 连接失败
                mIsConnecting = false
                Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 打开或关闭蓝牙后的回调
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_BLUETOOTH) {
                mBlueTv.text = "是否已打开蓝牙：是"
                startScanBle()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun initView() {
        val isSupport: Boolean = BleUtil.isSupportBle(this)

        mBleTv.text = if (isSupport) "是否支持BLE：是" else "是否支持BLE：否"
        mBlueTv.text = if (BleUtil.isBleEnable(this)) "是否已打开蓝牙：是" else "是否已打开蓝牙：否"

        val layoutManager = LinearLayoutManager(this)
        mRecyclerView.layoutManager = layoutManager
        mAdapter = BleAdapter()
        mRecyclerView.adapter = mAdapter

        mAdapter.addChildClickViewIds(R.id.btn_connect, R.id.btn_send)
        mAdapter.setOnItemChildClickListener { adapter, view, position ->
            val device = mAdapter.data[position]
            when (view.id) {
                R.id.btn_connect -> {
                    // 正在连接中
                    if (mIsConnecting) return@setOnItemChildClickListener

                    if (BluetoothDeviceManager.getInstance().isConnected(device)) {
                        BluetoothDeviceManager.getInstance().disconnect(device)
                    }else {
                        mIsConnecting = true
                        BluetoothDeviceManager.getInstance().connect(device)
                    }
                }

                R.id.btn_send -> {
                    if (mIsConnecting || device.state != BluetoothDevice.BOND_BONDED) {
                        Toast.makeText(this, "还未连接该蓝牙", Toast.LENGTH_SHORT).show()
                        return@setOnItemChildClickListener
                    }
                    val intent = Intent(this, DeviceControlActivity::class.java)
                    intent.putExtra(DeviceControlActivity.Companion.EXTRA_DEVICE, device)
                    startActivity(intent)
                }
            }
        }
    }

    fun onSearchClick(view: View) {
        if (mIsSearch) {
            stopScanBle()
            return
        }
        val locationManager =
            this@MainActivity.getSystemService(LOCATION_SERVICE) as LocationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android10以上才要进行判断
            // 判断GPS模块是否开启，如果没有则跳转至设置开启界面，设置完毕后返回到当前页面
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // 转到手机设置界面，用户设置GPS
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, REQUEST_CODE_LOCATION) // 设置完成后返回到原来的界面
                return
            }
        }
        // todo ousy 检查权限 这里没有检查权限的代码，自己去设置那里打开，要不搜不出来东西
        if (!BleUtil.isBleEnable(this)) {
            BleUtil.enableBluetooth(this, REQUEST_CODE_BLUETOOTH)
        } else {
            startScanBle()
        }
    }

    // 开始扫描
    private fun startScanBle() {
        if (mIsSearch) return

        mBluetoothLeDeviceStore.clear()
        ViseBle.getInstance().startScan(mScanCallback)
        refreshPro(true)
    }

    // 停止扫描
    private fun stopScanBle() {
        if (!mIsSearch) return

        ViseBle.getInstance().stopScan(mScanCallback)
        refreshPro(false)
    }

    private fun refreshPro(isSearch: Boolean) {
        mIsSearch = isSearch
        mProgressBar.visibility = if (mIsSearch) View.VISIBLE else View.INVISIBLE
    }

    /**
     * 扫描回调
     */
    private val mScanCallback: KeywordFilterScanCallback =
        KeywordFilterScanCallback("HL", object : IScanCallback {
            override fun onDeviceFound(bluetoothLeDevice: BluetoothLeDevice) {
                mBluetoothLeDeviceStore.addDevice(bluetoothLeDevice)
                runOnUiThread {
                    if (!this@MainActivity.isFinishing) {
                        mAdapter.setNewInstance(mBluetoothLeDeviceStore.deviceList)
                    }
                }
            }

            override fun onScanFinish(bluetoothLeDeviceStore: BluetoothLeDeviceStore) {
                refreshPro(false)
            }

            override fun onScanTimeout() {
                refreshPro(false)
            }
        })
}