package com.intrax.ozonics

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object{
        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var btSocket: BluetoothSocket? = null
        lateinit var btAdapter: BluetoothAdapter
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
        const val NAME_FLAG_COMMAND = 0x44
        //const val SERIAL_NUMBER_COMMAND =
        const val DEFAULT_NAME_COMMAND = 0x45
        const val SET_NAME_COMMAND = 0x46
        val firstTimeFlagforApp = true
        var unitName:String = "Ozonics"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        address = intent.getStringExtra("DeviceAddress")
        //address = "00:19:09:03:08:0C"
        ConnectBT(this).execute()
        Refresh(this).execute()
        /**
         * try viewgroup and check if it works
         */

        bat0.visibility = View.INVISIBLE
        bat1.visibility = View.INVISIBLE
        bat2.visibility = View.INVISIBLE
        bat3.visibility = View.INVISIBLE
        pwBtn.textOff = ""
        pwBtn.textOn = ""
        pwBtnOn.visibility = View.INVISIBLE
        pwBtnOff.visibility = View.VISIBLE

        GlobalScope.launch(Dispatchers.Main) {
            bat0.visibility = View.VISIBLE
            delay(500)
            bat1.visibility = View.VISIBLE
            delay(500)
            bat2.visibility = View.VISIBLE
            delay(500)
            bat3.visibility = View.VISIBLE
        }

        pwBtn.setOnClickListener {
            if(btAdapter.isEnabled && isConnected) {
                sendCommand(POWER_INFORMATION_COMMAND)
                GlobalScope.launch(Dispatchers.Main) {
                    delay(100)
                    val power = receiveCommand()
                    if(power.contains("P")){
                        Toast.makeText(this@MainActivity, "Device turned ON", Toast.LENGTH_SHORT).show()
                        pwBtnOff.visibility = View.INVISIBLE
                        pwBtnOn.visibility = View.VISIBLE
                    }
                    else if (power.contains("Q")){
                        Toast.makeText(this@MainActivity, "Device turned OFF", Toast.LENGTH_SHORT).show()
                        pwBtnOff.visibility = View.VISIBLE
                        pwBtnOn.visibility = View.INVISIBLE
                    }
                    else
                        Toast.makeText(this@MainActivity, power, Toast.LENGTH_SHORT).show()
                    Log.d("Power", power)
                }
            }
            else{
                Toast.makeText(this, "Bluetooth device not connected", Toast.LENGTH_SHORT).show()
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
                    if(mode.contains("0")){
                        Toast.makeText(this@MainActivity, "Standard mode activated", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else{
                Toast.makeText(this, "Bluetooth device not connected", Toast.LENGTH_SHORT).show()
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
                    if(boost.contains("1"))
                        Toast.makeText(this@MainActivity, "Boost Mode activated", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                Toast.makeText(this, "Bluetooth device not connected", Toast.LENGTH_SHORT).show()
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
                    if(hyBoost.contains("2"))
                        Toast.makeText(this@MainActivity, "Hyperboost mode activated", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                Toast.makeText(this, "Bluetooth device not connected", Toast.LENGTH_SHORT).show()
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
                    if(driwash.contains("3"))
                        Toast.makeText(this@MainActivity, "DriWash Mode activated", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                Toast.makeText(this, "Bluetooth device not connected", Toast.LENGTH_SHORT).show()
                val intent = Intent(applicationContext, DeviceList::class.java)
                startActivity(intent)
                finish()
            }
        }


    }


    /**
     *
     */
    private fun sendCommand(command:Int): Boolean{
        if(btSocket != null){
            try {
                btSocket!!.outputStream.write(command)
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
        val bytes = ByteArray(available)
        btSocket!!.inputStream.read(bytes)
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
                Toast.makeText(context, "Device connected ", Toast.LENGTH_SHORT).show()
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


    /**
     * Refreshes battery, mode, unit name and serial number as soon as the connection is established
     */
    inner class Refresh(c: Context) : AsyncTask<Void, Void, Boolean>() {
        private val context: Context

        init{
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            //Toast.makeText(context, "Device Connected", Toast.LENGTH_SHORT).show()
        }

        override fun doInBackground(vararg params: Void?): Boolean? {
            Thread.sleep(1500)
            while(!isConnected);

            return null
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            if(isConnected){

                /**
                 * Battery information receiving and setting up UI
                 */
                sendCommand(BATTERY_INFORMATION_COMMAND)
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
                    Toast.makeText(context, "Invalid battery response", Toast.LENGTH_SHORT).show()


                /**
                 * To check if ESP already has a name in it or not, if not provide a UI for a new name
                 * else default name ozonics is displayed
                 */
//                sendCommand(FIRST_TIME_FLAG_COMMAND)
//                val firstTimeFlagforESP = receiveCommand()
//                if(firstTimeFlagforESP.contains("True")){
//                    sendCommand(SET_NAME_COMMAND)
//                    sendCommand(unitName)
//                }
//                else if(firstTimeFlagforESP.contains("False")){
//                    sendCommand(GET_NAME_COMMAND)
//                    unitName = receiveCommand()
//                }
//                unitNameView.visibility = View.VISIBLE
//                unitNameView.text = unitName

                /**
                 * Mode information receiving and setting up UI
                 */
                sendCommand(MODE_INFORMATION_COMMAND)
                val mode = receiveCommand()
                if(mode.contains("0")){

                }
                else if(mode.contains("1")){
                    Toast.makeText(context, "Boost mode", Toast.LENGTH_SHORT).show()
                }
                else if(mode.contains("2")){

                }
                else if(mode.contains("3")){

                }
                //else
                    //Toast.makeText(context, mode, Toast.LENGTH_SHORT).show()


                /**
                 * Serial number
                 */
//                sendCommand(SERIAL_NUMBER_COMMAND)
//                val serialNum = receiveCommand()
//                serialN.text = serialNum
//                serialN.visibility = View.VISIBLE

            }
        }

        private fun sendCommand(command:Int): Boolean{
            if(btSocket != null){
                try {
                    btSocket!!.outputStream.write(command)
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
            val bytes = ByteArray(available)
            btSocket!!.inputStream.read(bytes)
            return String(bytes)
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

}
