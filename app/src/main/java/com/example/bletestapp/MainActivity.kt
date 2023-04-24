package com.example.bletestapp

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    // BLE adapter
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mDeviceAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // check if ble is supported
        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
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
        requestBluetoothFeature()
    }

    /** check BLE on/off */
    private fun requestBluetoothFeature() {
        if (mBluetoothAdapter!!.isEnabled) {
            return
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_ENABLE_BLUETOOTH -> if (Activity.RESULT_CANCELED == resultCode) {
                // User denied to use BLE feature
                Toast.makeText(this, R.string.bluetooth_is_not_working, Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            REQUEST_CONNECT_DEVICE -> {
                val strDeviceName: String?
                if (RESULT_OK == resultCode) {
                    // デバイスリストアクティビティからの情報の取得
                    strDeviceName = data?.getStringExtra(DeviceListActivity.EXTRAS_DEVICE_NAME)!!
                    mDeviceAddress = data?.getStringExtra(DeviceListActivity.EXTRAS_DEVICE_ADDRESS)!!
                } else {
                    strDeviceName = ""
                    mDeviceAddress = ""
                }
                (findViewById<View>(R.id.textview_devicename) as TextView).text =
                    strDeviceName
                (findViewById<View>(R.id.textview_deviceaddress) as TextView).text =
                    mDeviceAddress
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
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
                startActivityForResult(deviceListActivityIntent, REQUEST_CONNECT_DEVICE)
                return true
            }
        }
        return false
    }

    companion object {
        // BLE Code to request BLE feature
        private const val REQUEST_ENABLE_BLUETOOTH = 1
        private const val REQUEST_CONNECT_DEVICE = 2 // デバイス接続要求時の識別コード
    }
}