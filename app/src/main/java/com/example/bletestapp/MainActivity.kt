package com.example.bletestapp

import android.app.Activity
import android.bluetooth.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity(), View.OnClickListener {

    // BLE adapter
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mDeviceAddress = ""
    private var mBluetoothGatt: BluetoothGatt? = null // Gattサービスの検索、キャラスタリスティックの読み書き
    private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)

    // GUIアイテム
    private lateinit var mButtonConnect: Button     // 接続ボタン
    private lateinit var mButtonDisconnect: Button  // 切断ボタン

    // BluetoothGattコールバック
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        // 接続状態変更（connectGatt()の結果として呼ばれる。）
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (BluetoothGatt.GATT_SUCCESS != status) {
                return
            }
            if (BluetoothProfile.STATE_CONNECTED == newState) {    // 接続完了
                runOnUiThread { // GUIアイテムの有効無効の設定
                    // 切断ボタンを有効にする
                    mButtonDisconnect.isEnabled = true
                }
                return
            }
            if (BluetoothProfile.STATE_DISCONNECTED == newState) {    // 切断完了（接続可能範囲から外れて切断された）
                // 接続可能範囲に入ったら自動接続するために、mBluetoothGatt.connect()を呼び出す。
                mBluetoothGatt!!.connect()
                return
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mButtonConnect = findViewById(R.id.button_connect)
        mButtonDisconnect = findViewById(R.id.button_disconnect)
        mButtonConnect.setOnClickListener(this)
        mButtonDisconnect.setOnClickListener(this)

        // check if ble is supported
        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }?.let {
            Toast.makeText(this, R.string.bluetooth_is_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // get ble adapter
        var bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        if (null == mBluetoothAdapter) {
            Toast.makeText(this, R.string.bluetooth_is_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()

        // Android端末のBLE有効化機能
        requestBluetoothFeature()

        // GUIアイテムの有効無効の設定
        mButtonConnect.isEnabled = false
        mButtonDisconnect.isEnabled = false

        // デバイスアドレスが空でなければ、接続ボタンを有効にする。
        if (mDeviceAddress != "") {
            mButtonConnect.isEnabled = true
        }

        // 接続ボタンを押す
        mButtonConnect.callOnClick()
    }

    override fun onPause() {
        super.onPause()
        disconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBluetoothGatt != null) {
            mBluetoothGatt?.close()
            mBluetoothGatt = null
        }
    }

    /** check BLE on/off */
    private fun requestBluetoothFeature() {
        if (mBluetoothAdapter!!.isEnabled) {
            return
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)
    }

    override fun onClick(v: View) {
        if (mButtonConnect.id == v.id) {
            mButtonConnect.isEnabled = false // 接続ボタンの無効化（連打対策）
            connect() // 接続
            return
        }
        if (mButtonDisconnect.id == v.id) {
            mButtonDisconnect.isEnabled = false // 切断ボタンの無効化（連打対策）
            disconnect() // 切断
            return
        }
    }

    private var enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Callback function when checking BLE is enabled or not
        if (result.resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, R.string.bluetooth_is_not_working, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private var connectDeviceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Callback function when connecting a devise
        val strDeviceName: String?
        val data: Intent? = result.data
        if (result.resultCode == RESULT_OK) {
            strDeviceName = data?.getStringExtra(DeviceListActivity.EXTRAS_DEVICE_NAME)!!
            mDeviceAddress = data.getStringExtra(DeviceListActivity.EXTRAS_DEVICE_ADDRESS)!!
        } else {
            strDeviceName = ""
            mDeviceAddress = ""
        }
        (findViewById<View>(R.id.textview_devicename) as TextView).text = strDeviceName
        (findViewById<View>(R.id.textview_deviceaddress) as TextView).text = mDeviceAddress
    }

    // オプションメニュー作成時の処理
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    // オプションメニューのアイテム選択時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuitem_search -> {
                val deviceListActivityIntent = Intent(this, DeviceListActivity::class.java)
                connectDeviceLauncher.launch(deviceListActivityIntent)
                return true
            }
        }
        return false
    }

    // 接続
    private fun connect() {
        if (mDeviceAddress == "") { return }
        if (mBluetoothGatt != null) { return }
        var device = mBluetoothAdapter!!.getRemoteDevice(mDeviceAddress)
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
    }

    // 切断
    private fun disconnect() {
        if (null == mBluetoothGatt) { return }
        // 切断
        //   mBluetoothGatt.disconnect()ではなく、mBluetoothGatt.close()しオブジェクトを解放する。
        //   理由：「ユーザーの意思による切断」と「接続範囲から外れた切断」を区別するため。
        //   ①「ユーザーの意思による切断」は、mBluetoothGattオブジェクトを解放する。再接続は、オブジェクト構築から。
        //   ②「接続可能範囲から外れた切断」は、内部処理でmBluetoothGatt.disconnect()処理が実施される。
        //     切断時のコールバックでmBluetoothGatt.connect()を呼んでおくと、接続可能範囲に入ったら自動接続する。
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
        // GUIアイテムの有効無効の設定
        // 接続ボタンのみ有効にする
        mButtonConnect.isEnabled = true
        mButtonDisconnect.isEnabled = false
    }
}