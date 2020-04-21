package com.intrax.ozonics

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_weather_test.*

class WeatherTest : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_test)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocation()
        createLocationCallback()
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

    private fun requestLocation(){
        val mLocationRequestLowPower = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_LOW_POWER)
            .setInterval(10000)

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequestLowPower)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
                locationSettingsResponse ->
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
                    weatherUpdateTask().execute(location!!.lastLocation)
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }
        }
    }

    inner class weatherUpdateTask(): AsyncTask<Location, Void, WeatherClient>() {

        override fun doInBackground(vararg params: Location): WeatherClient {
            val weatherClient = WeatherClient()
            weatherClient.updateWeatherData(params[0], false)
            return weatherClient
        }

        override fun onPostExecute(weatherClient: WeatherClient) {
            Log.i("Temp", weatherClient.temp)
            tempText.text = weatherClient.temp
            windText.text = weatherClient.wind
            humidityText.text = weatherClient.humidity
            cityText.text = weatherClient.cityName + ", " + weatherClient.cityCountry
            sunriseText.text = weatherClient.sunRise
            sunsetText.text = weatherClient.sunSet
        }
    }
}
