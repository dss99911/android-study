package com.example.androidtest

import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LocationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocationSample.getInstance(this@LocationActivity).requestAccurateLocation(this@LocationActivity, 1000, object : LocationSample.LocationListener {
            override fun onSuccess(location: Location?) {
                logToast("success = $location")
            }

            override fun onFailure(e: Exception?) {
                logToast("failure $e")
            }
        })

    }
}