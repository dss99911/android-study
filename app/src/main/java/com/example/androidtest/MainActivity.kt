package com.example.androidtest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.karumi.dexter.Dexter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        android.Manifest.permission
        Dexter.withContext(this)
            .withPermission(permission)
            .withListener(listener)
            .check();

    }
}