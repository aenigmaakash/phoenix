package com.intrax.ozonics

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_device_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList


class DeviceList : AppCompatActivity() {

    companion object{
        lateinit var myBluetooth: BluetoothAdapter
        private val deviceName = "Phoenix"
        private var firstTime: Boolean = true
        var mBTDevices: ArrayList<BluetoothDevice> = ArrayList()
        lateinit var bluetoothLeScanner: BluetoothLeScanner
        private val serviceUUID = "e14d460c-32bc-457e-87f8-b56d1eb24318"
        private var clickCount = 0
        private var mTranslate: Float = 0f
        private val mThreshold: Float = 300f
        private lateinit var fusedLocationClient: FusedLocationProviderClient
        private lateinit var locationCallback: LocationCallback
        private lateinit var finalLocation: LocationServices
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        firstTime = intent.getBooleanExtra("first_time", true)
        val scale = resources.displayMetrics.density
        mTranslate = mThreshold * scale + 0.5f
        //ellipse2Layout.visibility = View.INVISIBLE
        scanLayout.visibility = View.INVISIBLE
        select.visibility = View.INVISIBLE
        ellipse1Layout.visibility = View.INVISIBLE
        ellipse2.visibility = View.INVISIBLE
        GlobalScope.launch(Dispatchers.Main){
            logoAnimation()
            delay(1000)
            scanLayout.visibility = View.VISIBLE
        }

        createLocationCallback()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val myBluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        myBluetooth = myBluetoothManager.adapter
        checkBTPermissions()

        deviceList.setOnItemClickListener { parent, view, position, id ->
            //Toast.makeText(this, macId, Toast.LENGTH_SHORT).show()
            bluetoothLeScanner.stopScan(scanCallback)
            bluetoothLeScanner.flushPendingScanResults(scanCallback)
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.putExtra("Device", mBTDevices[position])

            startActivity(intent)
            finish()
        }

        scan.setOnClickListener {
            if(!checkGpsStatus()){
                val dialogBuilder = AlertDialog.Builder(this)
                dialogBuilder.setMessage("Location Services is required to search for devices...")
                    .setCancelable(false)
                    .setPositiveButton("Enable", DialogInterface.OnClickListener { _, _ ->
                        requestLocation()
                    })
                    .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, _ ->
                        dialog.cancel()
                    })
                val alert = dialogBuilder.create()
                alert.setTitle("Location Request")
                alert.show()
            }
            else{
                if (!myBluetooth.isEnabled) {
                    val turnBTon = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(turnBTon, 1)
                }
                else if (myBluetooth.isEnabled){
                    GlobalScope.launch(Dispatchers.Main) {
                        if(clickCount == 0){
                            val translateY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -mTranslate + 100f)
                            val animateScan = ObjectAnimator
                                .ofPropertyValuesHolder(scanLayout, translateY)
                            animateScan.duration = 2000
                            animateScan.start()
                        }
                        scanLeDevice()
                        clickCount++
                    }
                }
            }
            val vibrate = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if(vibrate.hasVibrator()){
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                    vibrate.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                }
                else
                    vibrate.vibrate(500)
            }
        }
    }

    private fun logoAnimation(){
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.5f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.5f)
        val translateY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -mTranslate)

        val animator = ObjectAnimator.ofPropertyValuesHolder(logoLayout, scaleX, scaleY, translateY)
        animator.duration = 1000
        animator.start()
        GlobalScope.launch(Dispatchers.Main) {
            delay(20000)
            animator.end()
        }
    }

    private fun scanAnimation() {
        GlobalScope.launch(Dispatchers.Main) {
            scan.visibility = View.INVISIBLE
            select.visibility = View.VISIBLE
            delay(500)
            select.visibility = View.INVISIBLE
            scan.visibility = View.VISIBLE
            val ellipse1scaleXpos = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.5f)
            val ellipse1scaleYpos = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.5f)
            val ellipse2scaleXpos = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.16f)
            val ellipse2scaleYpos = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.16f)
            //val fade = PropertyValuesHolder.ofFloat(View.ALPHA, 1f)
            //val translateY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -240f)

            val animateEllipse1 = ObjectAnimator
                .ofPropertyValuesHolder(ellipse1Layout, ellipse1scaleXpos, ellipse1scaleYpos)
            //val animateScan = ObjectAnimator
            //    .ofPropertyValuesHolder(scanLayout, translateY)
            val animateEllipse2 = ObjectAnimator
                .ofPropertyValuesHolder(ellipse2, ellipse2scaleXpos, ellipse2scaleYpos)

            animateEllipse1.repeatMode = ObjectAnimator.REVERSE
            animateEllipse1.repeatCount = ObjectAnimator.INFINITE
            animateEllipse2.repeatMode = ObjectAnimator.REVERSE
            animateEllipse2.repeatCount = ObjectAnimator.INFINITE
            animateEllipse1.duration = 1000
            animateEllipse2.duration = 1000
           // animateScan.duration = 2000

            ellipse1Layout.animation = AnimationUtils.loadAnimation(this@DeviceList, R.anim.fade_in)
            ellipse2.animation = AnimationUtils.loadAnimation(this@DeviceList, R.anim.fade_in)
            ellipse1Layout.visibility = View.VISIBLE
            ellipse2.visibility = View.VISIBLE

            animateEllipse1.start()
            //animateScan.start()
            animateEllipse2.start()
            GlobalScope.launch(Dispatchers.Main) {
                delay(20000)
                animateEllipse1.cancel()
                animateEllipse2.cancel()
               // animateScan.end()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1){
            if(resultCode == Activity.RESULT_OK){
                GlobalScope.launch(Dispatchers.Main){
                    if(clickCount == 0){
                        val translateY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -240f)
                        val animateScan = ObjectAnimator
                            .ofPropertyValuesHolder(scanLayout, translateY)
                        animateScan.duration = 2000
                        animateScan.start()
                    }
                    scanLeDevice()
                }
            }
            else{
                Toast.makeText(this, "Bluetooth is not enabled.\nPlease enable in settings!", Toast.LENGTH_LONG).show()
            }
        }
        else if (requestCode == 101){
            when(resultCode){
                Activity.RESULT_OK -> {
                    if (!myBluetooth.isEnabled) {
                        val turnBTon = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(turnBTon, 1)
                    }
                    else if (myBluetooth.isEnabled){
                        GlobalScope.launch(Dispatchers.Main) {
                            if(clickCount == 0){
                                val translateY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -mTranslate + 100f)
                                val animateScan = ObjectAnimator
                                    .ofPropertyValuesHolder(scanLayout, translateY)
                                animateScan.duration = 2000
                                animateScan.start()
                            }
                            scanLeDevice()
                        }
                    }
                }
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(this, "Please enable location to get weather info", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothLeScanner.stopScan(scanCallback)//unregisterReceiver(bluetoothBroadcastReceiver)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if(myBluetooth.isEnabled && checkGpsStatus())
            GlobalScope.launch(Dispatchers.Main) {
                if(clickCount == 0){
                    val translateY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -240f)
                    val animateScan = ObjectAnimator
                        .ofPropertyValuesHolder(scanLayout, translateY)
                    animateScan.duration = 2000
                    animateScan.start()
                }
                scanLeDevice()
            }
        createLocationCallback()
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
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

    private fun checkGpsStatus(): Boolean {
        val locationManager =
            this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private suspend fun scanLeDevice() {
        checkBTPermissions()
        bluetoothLeScanner =
            myBluetooth.bluetoothLeScanner
        val settings =
            ScanSettings
                .Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build()
        val uuid = ParcelUuid(UUID.fromString(serviceUUID))
        val filter = ScanFilter.Builder().setServiceUuid(uuid).build()
        val filters = arrayListOf<ScanFilter>()
        //filters.add(filter)
        val m = bluetoothLeScanner.startScan(filters, settings, scanCallback)
        if (m != null) {
            scanAnimation()
            delay(20000)
            bluetoothLeScanner.flushPendingScanResults(scanCallback)
            bluetoothLeScanner.stopScan(scanCallback)
            ellipse2.visibility = View.INVISIBLE
            ellipse1Layout.visibility = View.INVISIBLE
            //Toast.makeText(applicationContext, "scan stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private val scanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            //Toast.makeText(applicationContext, "onScanResult triggered", Toast.LENGTH_SHORT).show()
            //Log.i("DeviceName:", result?.device?.name.toString())
            if (result!=null){
                if(!mBTDevices.contains(result.device)){
                    Log.d("DeviceName:", result.device.name + result.device.address)
                    mBTDevices.add(result.device)
                    val deviceListAdapter = DeviceListAdapter(this@DeviceList, R.layout.device_list_layout, mBTDevices)
                    deviceList.adapter = deviceListAdapter
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.i("Batch Scan Result:", results.toString())
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("Error scan code:", errorCode.toString())
        }
    }

    private fun requestLocation(){
        val mLocationRequestHighAccuracy = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10000)

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequestHighAccuracy)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            Log.i("Location settings", "All settings satisfied")
            fusedLocationClient.requestLocationUpdates(mLocationRequestHighAccuracy, locationCallback, Looper.myLooper())
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
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }
        }
    }
}

