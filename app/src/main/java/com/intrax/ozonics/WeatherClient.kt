package com.intrax.ozonics
import android.location.Location
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL


class WeatherClient {

    private val testJSONString = "{\"coord\":{\"lon\":88.42,\"lat\":22.58},\"weather\":[{\"id\":721,\"main\":\"Haze\",\"description\":\"haze\",\"icon\":\"50d\"}],\"base\":\"stations\",\"main\":{\"temp\":25,\"feels_like\":27.66,\"temp_min\":25,\"temp_max\":25,\"pressure\":1009,\"humidity\":78},\"visibility\":3500,\"wind\":{\"speed\":2.1,\"deg\":50},\"clouds\":{\"all\":40},\"dt\":1587470186,\"sys\":{\"type\":1,\"id\":9114,\"country\":\"IN\",\"sunrise\":1587426057,\"sunset\":1587472120},\"timezone\":19800,\"id\":1272243,\"name\":\"Dum Dum\",\"cod\":200}"


    private val apiKey: String = "cbb7a4e4f02b9f6a772db26269f1d363"
    private val apiURL: String = "https://api.openweathermap.org/data/2.5/weather?"
    private val imgURL: String = "https://openweathermap.org/img/w/"
    private lateinit var location: Location
    var temp: String = ""
    var wind: String = ""
    var humidity: String = ""
    var sunRise: String = ""
    var sunSet: String = ""
    var cityName: String = ""
    var cityCountry: String = ""
    var timezone: String = ""

    fun updateWeatherData(location: Location, dumbAmericans: Boolean): Boolean {

        this.location = location
        var completeURL = apiURL + "lat=" + location.latitude + "&lon=" + location.longitude + "&appid=" + apiKey + "&units="
        completeURL += if(dumbAmericans)
            "imperial"
        else
            "metric"
        //Log.i("Complete URL", completeURL)
        try{
            val client = OkHttpClient()
            val requestBuilder = Request.Builder().url(completeURL).build()
            val jsonData = client.newCall(requestBuilder).execute()
            //Log.i("JSON Response", jsonData.body?.string())
            return parseJSON(jsonData.body!!.string())
        }
        catch (i: IOException){
            i.printStackTrace()
        }
        return false
    }

    fun getImage(code: String): ByteArray? {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        try{
            connection = URL(imgURL + code).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.doInput = true
            connection.doOutput = true

            inputStream = connection.inputStream
            val buffer = ByteArray(1024)
            val byteArrayOutputStream = ByteArrayOutputStream()

            while(inputStream.read(buffer) != -1){
                byteArrayOutputStream.write(buffer)
            }
            return byteArrayOutputStream.toByteArray()
        }
        catch (t: Throwable){
            t.printStackTrace()
        }
        finally {
            try {
                inputStream!!.close()
            }catch (t:Throwable){}
            try {
                connection!!.disconnect()
            }catch (t: Throwable){}
        }
        return null
    }

    private fun parseJSON(data: String): Boolean{
        try{
            val dataObj = JSONObject(data)
            //val dataObj = JSONObject(testJSONString)
            cityName = dataObj.getString("name")
            cityCountry = dataObj.getJSONObject("sys").getString("country")
            sunRise = dataObj.getJSONObject("sys").getString("sunrise")
            sunSet = dataObj.getJSONObject("sys").getString("sunset")
            temp = dataObj.getJSONObject("main").getString("temp")
            humidity = dataObj.getJSONObject("main").getString("humidity")
            wind = dataObj.getJSONObject("wind").getString("speed")
            timezone = dataObj.getString("timezone")
            //Log.i("Temp", temp)
            return true
        }
        catch (t: JSONException){
            t.printStackTrace()
        }
        return false
    }

}