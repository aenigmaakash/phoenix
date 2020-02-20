package com.intrax.ozonics

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_device_list.*
import java.util.ArrayList


class DeviceList : AppCompatActivity() {

    private val mPairedDevices = arrayOfNulls<BluetoothDevice>(50)
    lateinit var myBluetooth: BluetoothAdapter
    private val deviceName = "Phoenix"
    private var firstTime: Boolean = true
    var mBTDevices: ArrayList<BluetoothDevice> = ArrayList()




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        firstTime = intent.getBooleanExtra("first_time", true)
        //Toast.makeText(this, firstTime.toString(), Toast.LENGTH_SHORT).show()
        myBluetooth = BluetoothAdapter.getDefaultAdapter()
        checkBTPermissions()
        if(myBluetooth.isEnabled){
            bluetoothOn.isChecked = true
            searchDevices()
        }

        bluetoothOn.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                if (myBluetooth == null) {
                    Toast.makeText(
                        applicationContext,
                        "Bluetooth Device Not Available", Toast.LENGTH_LONG).show()
                    finish()
                } else if (!myBluetooth.isEnabled) {
                    val turnBTon = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(turnBTon, 1)
                }
                //searchDevices()
                else if (myBluetooth.isEnabled){
                    searchDevices()
                }
            }
            else{
                myBluetooth.disable()
            }
        }

        deviceList.setOnItemClickListener { parent, view, position, id ->
            val macId = view.findViewById<TextView>(R.id.deviceAddress).text.toString()
            //Toast.makeText(this, macId, Toast.LENGTH_SHORT).show()
            myBluetooth.cancelDiscovery()
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.putExtra("DeviceAddress", macId)
            startActivity(intent)
            finish()
        }


    }

    private fun searchDevices(){

        val pairedDevices: Set<BluetoothDevice> = myBluetooth.bondedDevices

        if(firstTime){
            if(myBluetooth.isDiscovering)
                myBluetooth.cancelDiscovery()
            if(checkGpsStatus()){
                if(!myBluetooth.isDiscovering){
                    checkBTPermissions()
                    myBluetooth.startDiscovery()
                    val intent = IntentFilter(BluetoothDevice.ACTION_FOUND)
                    registerReceiver(bluetoothBroadcastReceiver, intent)
                }
            }
            else{
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
                finish()
            }
        }
        else{
            if (pairedDevices.isNotEmpty()) {
                var i = 0
                var pairedDeviceFound = false
                for  (device in pairedDevices) {
                    mPairedDevices.set(i, device)
                    if (device.name.contains(deviceName)){                  //this part searches for 'deviceName' devices that is already paired
                        if(!mBTDevices.contains(device)){
                            mBTDevices.add(device)
                            val deviceListAdapter = DeviceListAdapter(this, R.layout.device_list_layout, mBTDevices)
                            deviceList.adapter = deviceListAdapter
                        }
                    }
                    i++
                }
                if(!pairedDeviceFound){                            //if device not paired discovers device to be manually paired
                    if(myBluetooth.isDiscovering)
                        myBluetooth.cancelDiscovery()
                    if(checkGpsStatus()){
                        if(!myBluetooth.isDiscovering){
                            checkBTPermissions()
                            myBluetooth.startDiscovery()
                            val intent = IntentFilter(BluetoothDevice.ACTION_FOUND)
                            registerReceiver(bluetoothBroadcastReceiver, intent)
                        }
                    }
                    else{
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                        finish()
                    }
                }
            }
            else {
                mPairedDevices[0] = null
            }
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode==1){
            if(resultCode==Activity.RESULT_OK){
                searchDevices()
            }
            else{
                Toast.makeText(this, "Bluetooth is not enabled.\nPlease enable in settings!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothBroadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
        if(myBluetooth.isEnabled)
            searchDevices()
    }

    override fun onPause() {
        super.onPause()
        //unregisterReceiver(bluetoothBroadcastReceiver)
    }

    private fun checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if(ContextCompat.checkSelfPermission(applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION),
                    2)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            2 -> {
                if (grantResults.isEmpty()) {
                }
                else {
                    for(i in 0 until (grantResults.size)) {
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED)
                            Toast.makeText(this,
                                "Please allow all the permissions",
                                Toast.LENGTH_LONG)
                                .show()
                    }
                }
            }
        }
    }

    private val bluetoothBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothDevice.ACTION_FOUND) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Log.d("DeviceName:", device.name + device.address)
                if (device.name!=null && device.name.contains(deviceName)){
                    if(!mBTDevices.contains(device)){
                        mBTDevices.add(device)
                        val deviceListAdapter = DeviceListAdapter(context, R.layout.device_list_layout, mBTDevices)
                        deviceList.adapter = deviceListAdapter

                    }
                }
            }
        }
    }

    private fun checkGpsStatus(): Boolean {
        val locationManager =
            this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}
