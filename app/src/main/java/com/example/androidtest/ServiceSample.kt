package com.example.androidtest

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.annotation.RequiresApi
import java.util.*
import kotlin.concurrent.schedule

class ForgroundServiceSample : Service() {
    val handler = Handler()
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = baseContext.getSystemService(NotificationManager::class.java)
            val serviceChannel = NotificationChannel(
                "CHANNEL_ID",
                "CHANNEL_NAME",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logi("test hyun")
        createNotificationChannel()

        val pendingIntent: PendingIntent =
            Intent(this, LocationActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val notification: Notification = Notification.Builder(this, "CHANNEL_ID")
            .setContentTitle("ddd")
            .setContentText("test")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("dd")
            .build()

        startForeground(1, notification)


        Timer("d").schedule(1000, 1000) {
            LocationSample.getInstance(this@ForgroundServiceSample).requestAccurateLocation()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}