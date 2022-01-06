package com.example.androidtest

import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest


object Logger {
    const val TAG = "HYUN"
    fun logInfo(text: String) {
        Log.i(TAG, text)
    }
}

fun Any.logi(text: String) {
    Logger.logInfo("[${this::class.simpleName}] $text")
}
fun Any.logToast(text: String) {
    app.handler.post {
        logi(text)
        Toast.makeText(app, "[${this::class.simpleName}] $text", Toast.LENGTH_LONG).show()
    }
}

fun Intent.toLogString(): String {
    return extras?.keySet()?.map { key ->
        key to extras!!.get(key)
    }.toString()
}


fun Any.logSlack(text: String) {
    val slack = Slack.getInstance()
    // Load an env variable
// If the token is a bot token, it starts with `xoxb-` while if it's a user token, it starts with `xoxp-`
    val token = BuildConfig.SLACK_TOKEN
// Initialize an API Methods client with the given token
    val methods: MethodsClient = slack.methods(token)

// Build a request object
    val request = ChatPostMessageRequest.builder()
        .channel("@hyun") // Use a channel ID `C1234567` is preferrable
        .text(text)
        .build()

// Get a response as a Java object
    var response = methods.chatPostMessage(request)

}
