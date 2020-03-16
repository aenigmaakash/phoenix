package com.intrax.ozonics

import android.app.ProgressDialog
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_test.*
import kotlinx.android.synthetic.main.activity_test.bat0
import kotlinx.android.synthetic.main.activity_test.bat1
import kotlinx.android.synthetic.main.activity_test.bat2
import kotlinx.android.synthetic.main.activity_test.bat3
import kotlinx.android.synthetic.main.activity_test.boostBtn
import kotlinx.android.synthetic.main.activity_test.driBtn
import kotlinx.android.synthetic.main.activity_test.hyBoostBtn
import kotlinx.android.synthetic.main.activity_test.pwBtn
import kotlinx.android.synthetic.main.activity_test.stdBtn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object{
        private val serviceUUID = UUID.fromString("e14d460c-32bc-457e-87f8-b56d1eb24318")
        private val characteristicUUID = UUID.fromString("08b332a8-f4f6-4222-b645-60073ac6823f")
        private val batteryServiceUUID = UUID.fromString("7c29343f-f1c9-4bb0-be5d-e5f8e16cb95c")
        private val batteryCharacteristicUUID = UUID.fromString("6d531619-6ab6-4ab6-ae49-ff00e96d88f4")
        var btSocket: BluetoothSocket? = null
        lateinit var btAdapter: BluetoothAdapter
        lateinit var bluetoothGatt: BluetoothGatt
        var isConnected:Boolean = false
        lateinit var address: String
        lateinit var progressDialog: ProgressDialog
        const val STANDARD_MODE_COMMAND = 0x30
        const val BOOST_MODE_COMMAND = 0x31
        const val HYPERBOOST_MODE_COMMAND = 0x32
        const val DRIWASH_MODE_COMMAND = 0x33
        const val BATTERY_INFORMATION_COMMAND = 0x41
        const val POWER_INFORMATION_COMMAND = 0x42
        const val MODE_INFORMATION_COMMAND = 0x43
        const val STATUS_FLAG_COMMAND = 0x44
        const val NAME_FLAG_COMMAND = 0x44
        //const val SERIAL_NUMBER_COMMAND =
        const val DEFAULT_NAME_COMMAND = 0x45
        const val SET_NAME_COMMAND = 0x46
        val firstTimeFlagforApp = true
        var unitName:String = "Ozonics"
        var connectionState = false
        var WHITE = 0
        var BLACK = 0
        var bluetoothGattCharacteristic: BluetoothGattCharacteristic? = null
        var batteryGattCharacteristic: BluetoothGattCharacteristic? = null
        val grayed = 0.5
        val nonGrayed = 1.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        val myBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = myBluetoothManager.adapter
        val device = intent.extras["Device"] as BluetoothDevice
        bluetoothGatt = device.connectGatt(this, true, bluetoothGattCallback)

        WHITE = Color.parseColor("#FFFFFF")
        BLACK = Color.parseColor("#000000")

        bat0.visibility = View.INVISIBLE
        bat1.visibility = View.INVISIBLE
        bat2.visibility = View.INVISIBLE
        bat3.visibility = View.INVISIBLE

        pwrbtnon.visibility = View.INVISIBLE
        pwrbtnoff.visibility = View.VISIBLE
        settings_pressed.visibility = View.INVISIBLE
        info_pressed.visibility = View.INVISIBLE
        controlLayout.visibility = View.INVISIBLE
        standardselect.visibility = View.INVISIBLE
        boostselect.visibility = View.INVISIBLE
        hyperboostselect.visibility = View.INVISIBLE
        driwashselect.visibility = View.INVISIBLE
        standardLayout.visibility = View.INVISIBLE
        boostLayout.visibility = View.INVISIBLE
        driwashLayout.visibility = View.INVISIBLE
        hyperboostLayout.visibility = View.INVISIBLE

        GlobalScope.launch(Dispatchers.Main) {
            bat0.visibility = View.VISIBLE
            delay(500)
            bat1.visibility = View.VISIBLE
            delay(500)
            bat2.visibility = View.VISIBLE
            delay(500)
            bat3.visibility = View.VISIBLE
            sNum.text = device.address
        }           //initial battery animation

        Refresh().execute()
        pwBtn.setOnClickListener {
            if(connectionState){
                bluetoothGattCharacteristic!!.value = byteArrayOf(POWER_INFORMATION_COMMAND.toByte())
                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                pwBtn.isClickable = false
                pwrbtnoff.alpha = grayed.toFloat()
                Toast.makeText(this, "Changing...", Toast.LENGTH_SHORT).show()
                GlobalScope.launch(Dispatchers.Main) {
                    delay(5000)
                    pwBtn.isClickable = true
                    pwrbtnoff.alpha = nonGrayed.toFloat()
                }
            }
            else
                Toast.makeText(this@MainActivity, "Connection Lost", Toast.LENGTH_SHORT).show()
        }

        stdBtn.setOnClickListener {
            if(connectionState){
                bluetoothGattCharacteristic!!.value = byteArrayOf(STANDARD_MODE_COMMAND.toByte())
                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                stdBtn.isClickable = false
            }
        }

        boostBtn.setOnClickListener {
            if(connectionState){
                bluetoothGattCharacteristic!!.value = byteArrayOf(BOOST_MODE_COMMAND.toByte())
                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                boostBtn.isClickable = false
            }
        }

        hyBoostBtn.setOnClickListener {
            if(connectionState){
                bluetoothGattCharacteristic!!.value = byteArrayOf(HYPERBOOST_MODE_COMMAND.toByte())
                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                hyBoostBtn.isClickable = false
            }
        }

        driBtn.setOnClickListener {
            if(connectionState){
                bluetoothGattCharacteristic!!.value = byteArrayOf(DRIWASH_MODE_COMMAND.toByte())
                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                driBtn.isClickable = false
            }
        }

        settings.setOnClickListener {
            settingsLayout.visibility = View.VISIBLE
            controlLayout.visibility = View.INVISIBLE
            infoLayout.visibility = View.INVISIBLE
            GlobalScope.launch(Dispatchers.Main) {
                settings_pressed.visibility = View.VISIBLE
                delay(500)
                settings_pressed.visibility = View.INVISIBLE
            }
        }

        info.setOnClickListener {
            infoLayout.visibility = View.VISIBLE
            settingsLayout.visibility = View.INVISIBLE
            controlLayout.visibility = View.INVISIBLE
            GlobalScope.launch(Dispatchers.Main) {
                info_pressed.visibility = View.VISIBLE
                delay(500)
                info_pressed.visibility = View.INVISIBLE
            }
        }

        scanDevice.setOnClickListener {
            val intent = Intent(applicationContext, DeviceList::class.java)
            startActivity(intent)
            finish()
        }

        checkUpdate.setOnClickListener {
            Toast.makeText(this, "App upto-date!", Toast.LENGTH_SHORT).show()
        }

        logo.setOnClickListener {
            controlLayout.visibility = View.VISIBLE
            infoLayout.visibility = View.INVISIBLE
            settingsLayout.visibility = View.INVISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(connectionState){
            bluetoothGatt.disconnect()
            bluetoothGatt.close()

        }
        bluetoothGatt.disconnect()
        //disconnect()
    }

    override fun onPause() {
        super.onPause()
        if(connectionState)
        bluetoothGatt.disconnect()//disconnect()
    }

    override fun onResume() {
        super.onResume()
        bluetoothGatt.connect()//ConnectBT(this).execute()
    }


    /**
     * Refreshes battery, mode, unit name and serial number as soon as the connection is established
     */
    inner class Refresh() : AsyncTask<Void, Void, Boolean>() {

        override fun onPreExecute() {
            super.onPreExecute()
            //Toast.makeText(context, "Device Connected", Toast.LENGTH_SHORT).show()
        }

        override fun doInBackground(vararg params: Void?): Boolean? {
            Thread.sleep(1000)
            while(!connectionState && batteryGattCharacteristic!=null && bluetoothGattCharacteristic!=null);
            Thread.sleep(2000)
            return null
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            Log.i("Refresh Task", "Executed before if")
            if (connectionState) {
                bluetoothGattCharacteristic!!.value = byteArrayOf(BATTERY_INFORMATION_COMMAND.toByte())
                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)

                Log.i("Refresh Task", "Executed after if")
            }
        }

        private fun batDisplay(num: Int){
            if(num == 0){
                bat0.visibility = View.VISIBLE
                bat1.visibility = View.INVISIBLE
                bat2.visibility = View.INVISIBLE
                bat3.visibility = View.INVISIBLE
            }
            else if(num == 1){
                bat0.visibility = View.VISIBLE
                bat1.visibility = View.VISIBLE
                bat2.visibility = View.INVISIBLE
                bat3.visibility = View.INVISIBLE
            }
            else if(num == 2){
                bat0.visibility = View.VISIBLE
                bat1.visibility = View.VISIBLE
                bat2.visibility = View.VISIBLE
                bat3.visibility = View.INVISIBLE
            }
            else if(num == 3){
                bat0.visibility = View.VISIBLE
                bat1.visibility = View.VISIBLE
                bat2.visibility = View.VISIBLE
                bat3.visibility = View.VISIBLE
            }
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when(newState){
                BluetoothGatt.STATE_DISCONNECTED -> GlobalScope.launch(Dispatchers.Main) {
                    connectionState = false
                    controlLayout.visibility = View.INVISIBLE
                    Log.w("Connection Status", "Disconnected")
                }
                BluetoothGatt.STATE_CONNECTED -> GlobalScope.launch(Dispatchers.Main) {
                    connectionState = true
                    bluetoothGatt.discoverServices()
                    controlLayout.visibility = View.VISIBLE
                    Log.w("Connection Status", "Connected")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            bluetoothGattCharacteristic = gatt!!.getService(serviceUUID)!!.getCharacteristic(characteristicUUID)
            //batteryGattCharacteristic = gatt!!.getService(batteryServiceUUID)!!.getCharacteristic(batteryCharacteristicUUID)
            bluetoothGattCharacteristic!!.value = byteArrayOf(STATUS_FLAG_COMMAND.toByte())
            bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
            bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.i("From Write", characteristic?.getStringValue(0))
            gatt?.readCharacteristic(characteristic)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.i("Read value", characteristic?.getStringValue(0))
            GlobalScope.launch(Dispatchers.Main) {
                controlInfo(characteristic)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            GlobalScope.launch(Dispatchers.Main) {
                controlInfo(characteristic)
            }
        }
    }

    private fun controlInfo(characteristic: BluetoothGattCharacteristic?){
        when(characteristic?.getStringValue(0)){
            "P" -> {
                bluetoothGattCharacteristic!!.value = byteArrayOf(MODE_INFORMATION_COMMAND.toByte())
                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)
                pwBtn.isClickable = true
                pwrbtnoff.alpha = nonGrayed.toFloat()
                pwrbtnoff.visibility = View.INVISIBLE
                pwrbtnon.visibility = View.VISIBLE
                standardLayout.visibility = View.VISIBLE
                boostLayout.visibility = View.VISIBLE
                hyperboostLayout.visibility = View.VISIBLE
                driwashLayout.visibility = View.VISIBLE
            }
            "Q" -> {
                pwBtn.isClickable = true
                pwrbtnoff.alpha = nonGrayed.toFloat()
                pwrbtnoff.visibility = View.VISIBLE
                pwrbtnon.visibility = View.INVISIBLE
                standardLayout.visibility = View.INVISIBLE
                boostLayout.visibility = View.INVISIBLE
                hyperboostLayout.visibility = View.INVISIBLE
                driwashLayout.visibility = View.INVISIBLE
            }
            "0" -> {
                stdBtn.isClickable = true
                standardselect.visibility = View.VISIBLE
                boostselect.visibility = View.INVISIBLE
                hyperboostselect.visibility = View.INVISIBLE
                driwashselect.visibility = View.INVISIBLE
                standard.setTextColor(BLACK)
                boost.setTextColor(WHITE)
                hyperboost.setTextColor(WHITE)
                driwash.setTextColor(WHITE)
            }
            "1" -> {
                boostBtn.isClickable = true
                standardselect.visibility = View.INVISIBLE
                boostselect.visibility = View.VISIBLE
                hyperboostselect.visibility = View.INVISIBLE
                driwashselect.visibility = View.INVISIBLE
                standard.setTextColor(WHITE)
                boost.setTextColor(BLACK)
                hyperboost.setTextColor(WHITE)
                driwash.setTextColor(WHITE)
            }
            "2" -> {
                hyBoostBtn.isClickable = true
                standardselect.visibility = View.INVISIBLE
                boostselect.visibility = View.INVISIBLE
                hyperboostselect.visibility = View.VISIBLE
                driwashselect.visibility = View.INVISIBLE
                standard.setTextColor(WHITE)
                boost.setTextColor(WHITE)
                hyperboost.setTextColor(BLACK)
                driwash.setTextColor(WHITE)
            }
            "3" -> {
                driBtn.isClickable = true
                standardselect.visibility = View.INVISIBLE
                boostselect.visibility = View.INVISIBLE
                hyperboostselect.visibility = View.INVISIBLE
                driwashselect.visibility = View.VISIBLE
                standard.setTextColor(WHITE)
                boost.setTextColor(WHITE)
                hyperboost.setTextColor(WHITE)
                driwash.setTextColor(BLACK)
            }
            "a" -> {
                batDisplay(3)
            }
            "b" -> {
                batDisplay(2)
            }
            "c" -> {
                batDisplay(1)
            }
            "d" -> {
                batDisplay(0)
            }
            "e" -> {
                pwBtn.isClickable = true
                pwrbtnoff.visibility = View.INVISIBLE
                pwrbtnon.visibility = View.VISIBLE
                standardLayout.visibility = View.VISIBLE
                boostLayout.visibility = View.VISIBLE
                hyperboostLayout.visibility = View.VISIBLE
                driwashLayout.visibility = View.VISIBLE
            }
            "f" ->  {
                stdBtn.isClickable = true
                pwrbtnoff.visibility = View.VISIBLE
                pwrbtnon.visibility = View.INVISIBLE
                standardLayout.visibility = View.INVISIBLE
                boostLayout.visibility = View.INVISIBLE
                hyperboostLayout.visibility = View.INVISIBLE
                driwashLayout.visibility = View.INVISIBLE
            }
        }
    }

    private fun batDisplay(num: Int){
        if(num == 0){
            bat0.visibility = View.VISIBLE
            bat1.visibility = View.INVISIBLE
            bat2.visibility = View.INVISIBLE
            bat3.visibility = View.INVISIBLE
        }
        else if(num == 1){
            bat0.visibility = View.VISIBLE
            bat1.visibility = View.VISIBLE
            bat2.visibility = View.INVISIBLE
            bat3.visibility = View.INVISIBLE
        }
        else if(num == 2){
            bat0.visibility = View.VISIBLE
            bat1.visibility = View.VISIBLE
            bat2.visibility = View.VISIBLE
            bat3.visibility = View.INVISIBLE
        }
        else if(num == 3){
            bat0.visibility = View.VISIBLE
            bat1.visibility = View.VISIBLE
            bat2.visibility = View.VISIBLE
            bat3.visibility = View.VISIBLE
        }
    }
}
