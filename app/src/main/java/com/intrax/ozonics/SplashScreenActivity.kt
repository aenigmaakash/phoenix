package com.intrax.ozonics

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        val firstTimeFlag = sharedPref.getBoolean("first_time_flag", true)
        if(firstTimeFlag){
            with (sharedPref.edit()) {
                putBoolean("first_time_flag", false)
                apply()
            }

            GlobalScope.launch(Dispatchers.Main) {
                val intent = Intent(this@SplashScreenActivity, DeviceList::class.java)
                delay(400)
                intent.putExtra("first_time", true)
                startActivity(intent)
                finish()
            }


        }
        else {
            GlobalScope.launch(Dispatchers.Main) {
                val intent = Intent(this@SplashScreenActivity, DeviceList::class.java)
                delay(400)
                intent.putExtra("first_time", false)
                startActivity(intent)
                finish()
            }
        }



    }
}
