package com.intrax.ozonics

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import java.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object{
        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var btSocket: BluetoothSocket? = null
        lateinit var btAdapter: BluetoothAdapter
        var isConnected:Boolean = false
        lateinit var address: String
        lateinit var progressDialog: ProgressDialog
        val STANDARD_MODE_COMMAND = "M0"
        val BOOST_MODE_COMMAND = "M1"
        val HYPERBOOST_MODE_COMMAND = "M2"
        val DRIWASH_MODE_COMMAND = "M3"
        val BATTERY_INFORMATION_COMMAND = "B"
        val POWER_INFORMATION_COMMAND = "P"
        val MODE_INFORMATION_COMMAND = "M"
        val FIRST_TIME_FLAG_COMMAND = "F"
        val SERIAL_NUMBER_COMMAND = "SN"
        val SET_NAME_COMMAND = "SET"
        val GET_NAME_COMMAND = "GET"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //address = intent.getStringExtra("DeviceAddress")
        address = "00:19:09:03:08:0C"
        ConnectBT(this).execute()
        reconnect.visibility = View.INVISIBLE

        bat0.visibility = View.INVISIBLE
        bat1.visibility = View.INVISIBLE
        bat2.visibility = View.INVISIBLE
        bat3.visibility = View.INVISIBLE
        GlobalScope.launch(Dispatchers.Main) {
            bat0.visibility = View.VISIBLE
            delay(500)
            bat1.visibility = View.VISIBLE
            delay(500)
            bat2.visibility = View.VISIBLE
            delay(500)
            bat3.visibility = View.VISIBLE
            delay(400)
            //while(!isConnected);
            if(isConnected){
                sendCommand(BATTERY_INFORMATION_COMMAND)
                delay(100)
                val batteryInformation = receiveCommand().toInt()
                if(batteryInformation in 1..100){
                    if (batteryInformation in 1..25)
                        batDisplay(0)
                    else if(batteryInformation in 26..50)
                        batDisplay(1)
                    else if(batteryInformation in 51..75)
                        batDisplay(2)
                    else if(batteryInformation in 76..100)
                        batDisplay(3)
                }
                else
                    Toast.makeText(this@MainActivity, "Invalid battery response", Toast.LENGTH_SHORT).show()
            }
        }


        refreshBtn.setOnClickListener{
            //while(!(btAdapter.isEnabled && isConnected))
            sendCommand("i")
            GlobalScope.launch(Dispatchers.Main) {
                delay(100)
                receiveCommand()
            }
        }

        pwBtn.setOnClickListener {
            if(btAdapter.isEnabled && isConnected) {
                sendCommand(POWER_INFORMATION_COMMAND)
                GlobalScope.launch(Dispatchers.Main) {
                    delay(100)
                    val power = receiveCommand()
                    if(power.contains("P1")){
                        Toast.makeText(this@MainActivity, "Device turned ON", Toast.LENGTH_SHORT).show()
                    }
                    else if (power.contains("P2")){
                        Toast.makeText(this@MainActivity, "Device turned OFF", Toast.LENGTH_SHORT).show()
                    }
                    else
                        Toast.makeText(this@MainActivity, "Didn't work try again", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                Toast.makeText(this, "Bluetooth not connected", Toast.LENGTH_SHORT).show()
                val intent = Intent(applicationContext, DeviceList::class.java)
                startActivity(intent)
                finish()
            }


        }

        stdBtn.setOnClickListener {
            if(btAdapter.isEnabled && isConnected) {
                sendCommand(STANDARD_MODE_COMMAND)
                GlobalScope.launch(Dispatchers.Main) {
                    delay(100)
                    val mode = receiveCommand()
                    if(mode.contains(STANDARD_MODE_COMMAND)){
                        Toast.makeText(this@MainActivity, "Standard mode activated", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else{
                Toast.makeText(this, "Bluetooth not connected", Toast.LENGTH_SHORT).show()
                val intent = Intent(applicationContext, DeviceList::class.java)
                startActivity(intent)
                finish()
            }
        }

        boostBtn.setOnClickListener {
            if(btAdapter.isEnabled && isConnected) {
                sendCommand(BOOST_MODE_COMMAND)
                GlobalScope.launch(Dispatchers.Main) {
                    delay(100)
                    val boost = receiveCommand()
                    if(boost.contains(BOOST_MODE_COMMAND))
                        Toast.makeText(this@MainActivity, "Boost Mode activated", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                Toast.makeText(this, "Bluetooth not connected", Toast.LENGTH_SHORT).show()
                val intent = Intent(applicationContext, DeviceList::class.java)
                startActivity(intent)
                finish()
            }
        }

        hyBoostBtn.setOnClickListener {
            if(btAdapter.isEnabled && isConnected) {
                sendCommand(HYPERBOOST_MODE_COMMAND)
                GlobalScope.launch(Dispatchers.Main) {
                    delay(100)
                    val hyBoost = receiveCommand()
                    if(hyBoost.contains(HYPERBOOST_MODE_COMMAND))
                        Toast.makeText(this@MainActivity, "Hyperboost mode activated", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                Toast.makeText(this, "Bluetooth not connected", Toast.LENGTH_SHORT).show()
                val intent = Intent(applicationContext, DeviceList::class.java)
                startActivity(intent)
                finish()
            }
        }

        driBtn.setOnClickListener {
            if(btAdapter.isEnabled && isConnected) {
                sendCommand(DRIWASH_MODE_COMMAND)
                GlobalScope.launch(Dispatchers.Main) {
                    delay(100)
                    val driwash = receiveCommand()
                    if(driwash.contains(DRIWASH_MODE_COMMAND))
                        Toast.makeText(this@MainActivity, "DriWash Mode activated", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                Toast.makeText(this, "Bluetooth not connected", Toast.LENGTH_SHORT).show()
                val intent = Intent(applicationContext, DeviceList::class.java)
                startActivity(intent)
                finish()
            }
        }


    }


    /**
     *
     */
    private fun sendCommand(command:String): Boolean{
        if(btSocket != null){
            try {
                btSocket!!.outputStream.write(command.toByteArray())
                return true
            } catch (e: IOException){
                e.printStackTrace()
                return false
            }
        }
        return false
    }

    private fun receiveCommand():String {
        while(btSocket!!.inputStream.available()==0);               //check if any data is there, because 'available' gives int value not boolean
        val available:Int = btSocket!!.inputStream.available()         // 'available' stores the no of bytes available in the buffer
        var bytes = ByteArray(available)
        btSocket!!.inputStream.read(bytes, 0, available)
        return String(bytes)
    }

    private fun disconnect(){
        if(btSocket!=null){
            try{
                btSocket!!.close()
                btSocket = null
                isConnected = false
            }catch (e: IOException){
                e.printStackTrace()
            }
        }
    }

    /**
     * Buetooth connection is an asynchronous task that does not return any value to the thread that called it.
     *
     */
    private class ConnectBT(c: Context): AsyncTask<Void, Void, String>(){
        private var connectionSuccessful: Boolean = true
        private val context: Context

        init{
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            //progressDialog = ProgressDialog.show(context, "Please wait...", "Connecting")
        }

        override fun doInBackground(vararg params: Void?): String? {
           try {
               if(btSocket==null || !isConnected){
                   btAdapter = BluetoothAdapter.getDefaultAdapter()
                   val device = btAdapter.getRemoteDevice(address)
                   btSocket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                   BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                   btSocket!!.connect()
               }

           }catch (e: IOException){
               connectionSuccessful = false
               e.printStackTrace()
           }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(!connectionSuccessful){
                Toast.makeText(context, "Couldn't connect please try again", Toast.LENGTH_SHORT).show()
            }
            else{
                isConnected = true
                Toast.makeText(context, "Device connected", Toast.LENGTH_SHORT).show()
            }
            //progressDialog.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }

    override fun onPause() {
        super.onPause()
        disconnect()
    }

    override fun onResume() {
        super.onResume()
        ConnectBT(this).execute()
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
