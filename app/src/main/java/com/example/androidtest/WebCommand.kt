package com.example.androidtest

import android.annotation.SuppressLint
import android.os.Handler
import android.webkit.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

@SuppressLint("SetJavaScriptEnabled")
class WebCommandRunner(val webView: WebView, val commands: Array<WebCommand>) {
    var completed = false
    var currentUrl: String = ""
    val handler = Handler()
    val listeners: MutableList<WebViewClient> = mutableListOf()
    val jsListeners: MutableList<JsListener> = mutableListOf()

    var currentIndex = -1

    init {
        webView.webChromeClient = WebChromeClient()
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.allowContentAccess = true
        settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                currentUrl = url
                listeners.forEach { it.onPageFinished(view, url) }
            }
        }
        webView.addJavascriptInterface(JsReceiver(this), "receiver")

    }

    fun run() {
        currentIndex = -1
        next()
    }

    fun next() {
        if (completed) {
            return
        }

        currentIndex += 1

        if (currentIndex >= commands.size) {
            complete()
            return
        }

        handler.post {
            val executed = commands[currentIndex].execute(this@WebCommandRunner)
            if (executed) {
                next()
            }
        }
    }

    fun complete() {
        completed = true
    }


}

sealed class WebCommand {
    abstract fun execute(runner: WebCommandRunner): Boolean
}



class ScriptCommand(val script: String, val listener: JsListener? = null) : WebCommand() {
    override fun execute(runner: WebCommandRunner): Boolean {
        listener?.let {
            runner.jsListeners.add(it)
        }

        runner.webView.evaluateJavascript(script) {
            logi("[test]eval return :$it")
            runner.next()
            listener?.let {
                logi("[test] remove listener ")
                runner.jsListeners.remove(it)
            }
        }

        return false
    }
}

class LoadCommand(val url: String) : WebCommand() {
    override fun execute(runner: WebCommandRunner): Boolean {
        runner.webView.loadUrl(url)

        return true
    }
}

class WaitCommand(val urlPattern: String, val delayAfterUrl: Long, val timeout: Long) : WebCommand() {
    //todo check html tag exists or not and go to next
    var completed = AtomicBoolean(false)
    val urlRegex = try { Pattern.compile(urlPattern) } catch (e: Exception) {
        null
    }
    val listener = object: WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            perform(url?:"")
        }
    }
    lateinit var runner: WebCommandRunner

    override fun execute(runner: WebCommandRunner): Boolean {
        this.runner = runner
        runner.listeners.add(listener)
        runner.handler.postDelayed({ complete(runner) }, timeout)
        perform(runner.currentUrl)
        return false // go to next on complete()
    }

    fun perform(url: String) {
        if (isValidUrl(url)) {
            complete(runner)
        }
    }

    fun isValidUrl(url: String): Boolean =
        urlPattern == url || urlRegex?.matcher(url)?.matches()?:false

    fun complete(runner: WebCommandRunner) {
        if (completed.getAndSet(true)) {
            return
        }
        runner.listeners.remove(listener)
        runner.handler.postDelayed({
            runner.next()
        }, delayAfterUrl)

    }
}

class JsReceiver(val runner: WebCommandRunner) {
    @JavascriptInterface
    fun sendString(string: String) {
        logi("sendString=$string")
        runner.handler.post {
            runner.jsListeners.forEach {
                it.onStringReceived(string)
            }
        }
    }
}

interface JsListener {
    fun onStringReceived(string: String)
}