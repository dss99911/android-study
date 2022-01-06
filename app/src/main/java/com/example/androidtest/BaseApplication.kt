package com.example.androidtest

import android.app.Application
import android.os.Handler

val app: BaseApplication get() = BaseApplication.instance

class BaseApplication : Application() {
    companion object {
        lateinit var instance: BaseApplication
    }

    lateinit var handler: Handler

    override fun onCreate() {
        super.onCreate()
        instance = this
        handler = Handler()
    }
}

