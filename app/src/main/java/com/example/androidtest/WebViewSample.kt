package com.example.androidtest

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.webkit.*


class WebViewSample {

    fun view(context: Context) {
        val webView = WebView(context)
        val settings = webView.settings
        settings.setJavaScriptEnabled(true)
        settings.setAllowContentAccess(true)
        settings.setDomStorageEnabled(true)
        WebView.setWebContentsDebuggingEnabled(true)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype: String, contentLength ->
            //only support GET method.
            //also if it's POST method, as we don't know payload of POST, we can't download
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimetype)
            val cookies: String = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("cookie", cookies)
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading file...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(context.getExternalFilesDir(null)!!.absolutePath, URLUtil.guessFileName(url, contentDisposition, mimetype))
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
            dm!!.enqueue(request)
        }
        webView.webChromeClient = WebChromeClient() //this is required for javascript,

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
            }
        }
        webView.addJavascriptInterface(JsInterface(), "interface")
        webView.loadUrl("https://netbanking.hdfcbank.com/netbanking/")
    }


    class JsInterface {
        @JavascriptInterface
        fun data(json: String) { //support only primitive. use json and parse it.

        }
    }

    fun interceptPostData() {
        //https://github.com/KeejOow/android-post-webview
        //https://github.com/KonstantinSchubert/request_data_webviewclient
    }
}