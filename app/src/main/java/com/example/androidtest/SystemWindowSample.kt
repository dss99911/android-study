package com.example.androidtest

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button


class SystemWindowSample {
    val root = getView()
    var isShown = false
    fun showSystemWindow() {
//        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        val windowManager = app.getSystemService(WINDOW_SERVICE) as WindowManager
        val lparam = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            flags,
            PixelFormat.TRANSLUCENT
        )

        lparam.gravity = Gravity.TOP or Gravity.START
        lparam.x = 0
        lparam.y = 0
        lparam.width = 200
        lparam.height = 200
        windowManager.addView(root, lparam)
        isShown = true
    }

    fun toggle() {
        if (isShown) {
            removeWindw()
        } else {
            showSystemWindow()
        }
    }

    fun removeWindw() {
        val windowManager = app.getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.removeView(root)
        isShown = false
    }

    @SuppressLint("MissingPermission")
    fun getView(): View {
        val view = Button(app)
        view.text = "click"
        view.setOnClickListener {
            logToast("test")
        }
        return view
    }

    fun requestSystemAlertPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + app.packageName)
                )
                context.startActivity(intent)
            }
        }
    }
}