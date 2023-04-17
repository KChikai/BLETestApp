package com.example.bletestapp

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    // BLE adapter
    private var mBluetoothAdapter: BluetoothAdapter? = null

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
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        // BLE Code to request BLE feature
        private const val REQUEST_ENABLE_BLUETOOTH = 1
    }
}