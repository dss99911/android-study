package com.example.androidtest

import android.content.Context
import android.widget.ScrollView

class ViewSample {

    fun scrollFillViewport(context: Context) {
        val scroll = ScrollView(context)
        scroll.isFillViewport = true
    }
}