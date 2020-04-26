package com.intrax.ozonics

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
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
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object{
        private val serviceUUID = UUID.fromString("e14d460c-32bc-457e-87f8-b56d1eb24318")
        private val characteristicUUID = UUID.fromString("08b332a8-f4f6-4222-b645-60073ac6823f")
        private val batteryServiceUUID = UUID.fromString("7c29343f-f1c9-4bb0-be5d-e5f8e16cb95c")
        private val batteryCharacteristicUUID = UUID.fromString("6d531619-6ab6-4ab6-ae49-ff00e96d88f4")
        lateinit var btAdapter: BluetoothAdapter
        lateinit var bluetoothGatt: BluetoothGatt
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
        var unitName:String = "Ozonics"
        var connectionState = false
        var WHITE = 0
        var BLACK = 0
        var bluetoothGattCharacteristic: BluetoothGattCharacteristic? = null
        var batteryGattCharacteristic: BluetoothGattCharacteristic? = null
        const val grayed = 0.5
        const val nonGrayed = 1.0
        private lateinit var fusedLocationClient: FusedLocationProviderClient
        private lateinit var locationCallback: LocationCallback
        var tempUnits = " \u2109"
        var windUnits = " mi/h"
        var units: Boolean = true
        private var finalLocation: Location? = null
        private var backNavigation = false
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

        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        units = sharedPref.getBoolean("units", true)
        if(!units){
            tempUnits = " \u2103"
            windUnits = " m/s"
        }
        unitsBtn.isChecked = !units
        unitsBtn.textOff = "\u2109"
        unitsBtn.textOn = "\u2103"

        bat0.visibility = View.INVISIBLE
        bat1.visibility = View.INVISIBLE
        bat2.visibility = View.INVISIBLE
        bat3.visibility = View.INVISIBLE

        pwrbtnoff.visibility = View.VISIBLE
        pwrbtnon.visibility = View.INVISIBLE
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

        if(!isOnline(this))
            cityText.text = "Internet Connectivity Required"

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationCallback()
        requestLocation()


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
            Log.i("connection state", connectionState.toString())
            if(connectionState){
                try {
                    bluetoothGattCharacteristic!!.value = byteArrayOf(POWER_INFORMATION_COMMAND.toByte())
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                    pwBtn.isClickable = false
                    pwrbtnoff.alpha = grayed.toFloat()
                    val vibrate = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if(vibrate.hasVibrator()){
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                            vibrate.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                        else
                            vibrate.vibrate(500)
                    }
                    //Toast.makeText(this, "Changing...", Toast.LENGTH_SHORT).show()
                    GlobalScope.launch(Dispatchers.Main) {
                        delay(5000)
                        pwBtn.isClickable = true
                        pwrbtnoff.alpha = nonGrayed.toFloat()
                    }
                }
                catch (e: Exception){
                    e.printStackTrace()
                }
            }
            else
                Toast.makeText(this@MainActivity, "Connection Lost", Toast.LENGTH_SHORT).show()
        }

        stdBtn.setOnClickListener {
            if(connectionState){
                try {
                    bluetoothGattCharacteristic!!.value = byteArrayOf(STANDARD_MODE_COMMAND.toByte())
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                    stdBtn.isClickable = false
                    val vibrate = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if(vibrate.hasVibrator()){
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                            vibrate.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                        else
                            vibrate.vibrate(500)
                    }
                }
                catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }

        boostBtn.setOnClickListener {
            if(connectionState){
                try {
                    bluetoothGattCharacteristic!!.value = byteArrayOf(BOOST_MODE_COMMAND.toByte())
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                    boostBtn.isClickable = false
                    val vibrate = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if(vibrate.hasVibrator()){
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                            vibrate.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                        else
                            vibrate.vibrate(500)
                    }
                }
                catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }

        hyBoostBtn.setOnClickListener {
            if(connectionState){
                try {
                    bluetoothGattCharacteristic!!.value = byteArrayOf(HYPERBOOST_MODE_COMMAND.toByte())
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                    hyBoostBtn.isClickable = false
                    val vibrate = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if(vibrate.hasVibrator()){
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                            vibrate.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                        else
                            vibrate.vibrate(500)
                    }
                }
                catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }

        driBtn.setOnClickListener {
            if(connectionState){
                try {
                    bluetoothGattCharacteristic!!.value = byteArrayOf(DRIWASH_MODE_COMMAND.toByte())
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                    driBtn.isClickable = false
                    val vibrate = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if(vibrate.hasVibrator()){
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                            vibrate.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                        else
                            vibrate.vibrate(500)
                    }
                }
                catch (e: Exception){
                    e.printStackTrace()
                }
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
            backNavigation = true
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
            backNavigation = true
        }

        scanDevice.setOnClickListener {
            val intent = Intent(applicationContext, DeviceList::class.java)
            startActivity(intent)
            finish()
        }

        checkUpdate.setOnClickListener {
            Toast.makeText(this, "App upto date!", Toast.LENGTH_SHORT).show()
        }

        logo.setOnClickListener {
            controlLayout.visibility = View.VISIBLE
            infoLayout.visibility = View.INVISIBLE
            settingsLayout.visibility = View.INVISIBLE
            backNavigation = false
        }

        unitsBtn.setOnCheckedChangeListener { _, isChecked ->
            if(isOnline(this)){
                if(isChecked){
                    tempText.text = ""
                    windText.text = ""
                    humidityText.text = ""
                    cityText.text = "Loading.."
                    sunriseText.text = ""
                    sunsetText.text = ""
                    tempUnits = " \u2103"
                    windUnits = " m/s"
                    units = false
                    with (sharedPref.edit()){
                        putBoolean("units", units)
                        commit()
                    }
                    if(finalLocation!=null)
                        weatherUpdateTask().execute(finalLocation)
                    else
                        requestLocation()
                    //Log.i("Units Value", units.toString())
                }
                else{
                    tempText.text = ""
                    windText.text = ""
                    humidityText.text = ""
                    cityText.text = "Loading.."
                    sunriseText.text = ""
                    sunsetText.text = ""
                    tempUnits = " \u2109"
                    windUnits = " mi/h"
                    units = true
                    with (sharedPref.edit()){
                        putBoolean("units", units)
                        commit()
                    }
                    if(finalLocation!=null)
                        weatherUpdateTask().execute(finalLocation)
                    else
                        requestLocation()
                    //Log.i("Units Value", units.toString())
                }
            }
            else{
                tempText.text = ""
                windText.text = ""
                humidityText.text = ""
                cityText.text = "Internet Connectivity Required"
                sunriseText.text = ""
                sunsetText.text = ""
            }

        }
    }

    override fun onBackPressed() {

        if(!backNavigation){
            super.onBackPressed()

        }
        else{
            controlLayout.visibility = View.VISIBLE
            infoLayout.visibility = View.INVISIBLE
            settingsLayout.visibility = View.INVISIBLE
            backNavigation = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(connectionState){
            bluetoothGatt.disconnect()
            bluetoothGatt.close()

        }
        //bluetoothGatt.disconnect()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        //disconnect()
    }

    override fun onPause() {
        super.onPause()
        if(connectionState)
            bluetoothGatt.disconnect()//disconnect()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        bluetoothGatt.connect()//ConnectBT(this).execute()
        createLocationCallback()
        if(finalLocation!=null)
            weatherUpdateTask().execute(finalLocation)
        else
            requestLocation()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            101 -> {
                when(resultCode){
                    Activity.RESULT_OK -> {
                        requestLocation()
                    }
                    Activity.RESULT_CANCELED -> {
                        Toast.makeText(this, "Please enable location to get weather info", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    /**
     * Refreshes battery, mode, unit name and serial number as soon as the connection is established
     */
    inner class Refresh() : AsyncTask<Void, Void, Boolean>() {

        //No need of onPreExecuted here, nothing to do
        override fun doInBackground(vararg params: Void?): Boolean? {
            Thread.sleep(1000)
            while(!connectionState && batteryGattCharacteristic!=null && bluetoothGattCharacteristic!=null);
            Thread.sleep(2000)
            return null
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            if(connectionState){
                try {
                    bluetoothGattCharacteristic!!.value = byteArrayOf(BATTERY_INFORMATION_COMMAND.toByte())
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                    bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)
                }
                catch (e: Exception){
                    e.printStackTrace()
                }
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
                    val vibrate = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if(vibrate.hasVibrator()){
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                            vibrate.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                        else
                            vibrate.vibrate(300)
                    }
                    Log.w("Connection Status", connectionState.toString())
                    Toast.makeText(applicationContext, "Disconnected", Toast.LENGTH_SHORT).show()
                }
                BluetoothGatt.STATE_CONNECTED -> GlobalScope.launch(Dispatchers.Main) {
                    connectionState = true
                    bluetoothGatt.discoverServices()
                    controlLayout.visibility = View.VISIBLE
                    val vibrate = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if(vibrate.hasVibrator()){
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                            vibrate.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                        else
                            vibrate.vibrate(1000)
                    }
                    Log.w("Connection Status", connectionState.toString())
                    Toast.makeText(applicationContext, "Connected", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            try {
                bluetoothGattCharacteristic = gatt!!.getService(serviceUUID)!!.getCharacteristic(characteristicUUID)
                //batteryGattCharacteristic = gatt!!.getService(batteryServiceUUID)!!.getCharacteristic(batteryCharacteristicUUID)
                bluetoothGattCharacteristic!!.value = byteArrayOf(STATUS_FLAG_COMMAND.toByte())
                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)
            }
            catch (e: Exception){
                e.printStackTrace()
            }
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
        when (num) {
            0 -> {
                bat0.visibility = View.VISIBLE
                bat1.visibility = View.INVISIBLE
                bat2.visibility = View.INVISIBLE
                bat3.visibility = View.INVISIBLE
            }
            1 -> {
                bat0.visibility = View.VISIBLE
                bat1.visibility = View.VISIBLE
                bat2.visibility = View.INVISIBLE
                bat3.visibility = View.INVISIBLE
            }
            2 -> {
                bat0.visibility = View.VISIBLE
                bat1.visibility = View.VISIBLE
                bat2.visibility = View.VISIBLE
                bat3.visibility = View.INVISIBLE
            }
            3 -> {
                bat0.visibility = View.VISIBLE
                bat1.visibility = View.VISIBLE
                bat2.visibility = View.VISIBLE
                bat3.visibility = View.VISIBLE
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun requestLocation(){
        val mLocationRequestLowPower = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_LOW_POWER)
            .setInterval(20000)

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequestLowPower)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            Log.i("Location settings", "All settings satisfied")
            fusedLocationClient.requestLocationUpdates(mLocationRequestLowPower, locationCallback, Looper.myLooper())
        }

        task.addOnFailureListener {
            if(it is ResolvableApiException){
                try {
                    it.startResolutionForResult(this, 101)
                }
                catch (sendEx: IntentSender.SendIntentException){
                    Log.i("Location Settings", "Error in changing settings")
                }
            }
        }
    }

    private fun createLocationCallback() {
        locationCallback = object: LocationCallback() {
            override fun onLocationResult(location: LocationResult?) {
                super.onLocationResult(location)
                if(location!=null){
                    finalLocation = location.lastLocation
                    weatherUpdateTask().execute(location.lastLocation)
                }
            }
        }
    }

    inner class weatherUpdateTask(): AsyncTask<Location, Void, WeatherClient>() {

        override fun doInBackground(vararg params: Location): WeatherClient {
            val weatherClient = WeatherClient()
            weatherClient.updateWeatherData(params[0], units)
            return weatherClient
        }

        @SuppressLint("SetTextI18n")
        override fun onPostExecute(weatherClient: WeatherClient) {
            //Log.i("Temp", weatherClient.temp)
            tempText.text = "Temperature: " + weatherClient.temp + tempUnits
            windText.text = "Wind: " + weatherClient.wind + windUnits
            humidityText.text = "Humidity: "+ weatherClient.humidity + " %"
            cityText.text = "City: " + weatherClient.cityName + ", " + weatherClient.cityCountry
            sunriseText.text = "Sunrise: " + setTime(weatherClient.sunRise).toString()
            sunsetText.text = "Sunset: " + setTime(weatherClient.sunSet).toString()
        }
    }

    private fun setTime(time: String?): String? {
        if(time == null)
            return null
        val date = Date((time.toLong())*1000L)
        val sdf = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}
