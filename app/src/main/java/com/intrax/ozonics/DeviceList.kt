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
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_device_list.*


class DeviceList : AppCompatActivity() {

    private val mPairedDevices = arrayOfNulls<BluetoothDevice>(50)
    lateinit var myBluetooth: BluetoothAdapter
    private var deviceMAC:String? = null
    private val deviceName = "phoenix"
    private var firstTime: Boolean = true



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        firstTime = intent.getBooleanExtra("first_time", true)
        Toast.makeText(this, firstTime.toString(), Toast.LENGTH_SHORT).show()
        myBluetooth = BluetoothAdapter.getDefaultAdapter()
        checkBTPermissions()
        if(myBluetooth.isEnabled)
            searchDevices()

        textView.visibility = View.INVISIBLE

        warningText.visibility = View.INVISIBLE

        if(myBluetooth.isEnabled){
            bluetoothOn.isChecked = true
        }

        textView.setOnClickListener(View.OnClickListener  {
            if(deviceMAC!=null) {
                myBluetooth.cancelDiscovery()
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.putExtra("DeviceAddress", deviceMAC)
                startActivity(intent)
                finish()
            }
            else{
                Toast.makeText(this, "Device not found! Please make sure device is nearby", Toast.LENGTH_LONG).show()
            }
        })

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

        warningText.setOnClickListener(View.OnClickListener {
            val intent = Intent()
            intent.action = android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
            startActivity(intent)
        })


    }

    private fun searchDevices(){

        val pairedDevices: Set<BluetoothDevice> = myBluetooth.bondedDevices

        if(firstTime){
            if(checkGpsStatus()){
                if(myBluetooth.isDiscovering){
                    myBluetooth.cancelDiscovery()
                    checkBTPermissions()
                    myBluetooth.startDiscovery()
                    val intent = IntentFilter(BluetoothDevice.ACTION_FOUND)
                    registerReceiver(bluetoothBroadcastReceiver, intent)
                }
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
                    if (device.name.contains(deviceName)){                      //this part searches for 'deviceName' devices that is already paired
                        Toast.makeText(this, "Connecting to " + device.name.toString(), Toast.LENGTH_SHORT).show()
                        deviceMAC = device.address.toString()
                        if(deviceMAC!=null) {
                            myBluetooth.cancelDiscovery()
                            val intent = Intent(applicationContext, MainActivity::class.java)
                            intent.putExtra("DeviceAddress", deviceMAC)
                            startActivity(intent)
                            finish()
                        }
                        else{
                            Toast.makeText(this, "Device not found! Please make sure device is nearby", Toast.LENGTH_LONG).show()
                        }
                        pairedDeviceFound = true
                    }
                    i++
                }
                if(!pairedDeviceFound){                            //if device not paired discovers device to be manually paired
                    if(myBluetooth.isDiscovering)
                        myBluetooth.cancelDiscovery()
                    Toast.makeText(this, "Please pair your device for the first time with passcode '1234'", Toast.LENGTH_LONG).show()
                    warningText.text = ("Click here to pair a new Ozonics device\nPlease use the passcode 1234")
                    warningText.visibility = View.VISIBLE
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
    }

    override fun onResume() {
        super.onResume()
        warningText.visibility = View.INVISIBLE
        if(myBluetooth.isEnabled)
            searchDevices()
//        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
//        registerReceiver(receiver, filter)
    }

    override fun onPause() {
        super.onPause()
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
                if (device.name!=null){
                    Toast.makeText(applicationContext, device.name + "\n" + device.address, Toast.LENGTH_SHORT).show()
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
