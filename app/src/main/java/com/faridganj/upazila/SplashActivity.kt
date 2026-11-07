package com.faridganj.upazila

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private var splashWebView: WebView? = null

    private val handler = Handler(Looper.getMainLooper())

    private val openApp = Runnable {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        val webView = WebView(this)
        splashWebView = webView

        webView.setBackgroundColor(Color.WHITE)
        webView.settings.javaScriptEnabled = false
        webView.settings.domStorageEnabled = false

        webView.webViewClient = WebViewClient()

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport"
                    content="width=device-width, initial-scale=1.0">
                <style>
                    * {
                        box-sizing: border-box;
                    }

                    html, body {
                        margin: 0;
                        width: 100%;
                        height: 100%;
                        background: #ffffff;
                        font-family: Arial, sans-serif;
                    }

                    body {
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        text-align: center;
                    }

                    .content {
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                    }

                    img {
                        width: 120px;
                        height: 120px;
                        object-fit: contain;
                    }

                    h2 {
                        color: #172d40;
                        font-size: 23px;
                        margin-top: 22px;
                        font-weight: 700;
                    }

                    p {
                        color: #777777;
                        font-size: 13px;
                        margin-top: 2px;
                    }
                </style>
            </head>
            <body>
                <div class="content">
                    <img
                        src="https://faridganjapp.rf.gd/assets/logo.png"
                        alt="Faridganj Upazila Logo">
                    <h2>Faridganj Upazila</h2>
                    <p>Welcome</p>
                </div>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(
            "https://faridganjapp.rf.gd/",
            html,
            "text/html",
            "UTF-8",
            null
        )

        setContentView(webView)

        handler.postDelayed(openApp, 2000)
    }

    override fun onDestroy() {
        handler.removeCallbacks(openApp)

        splashWebView?.stopLoading()
        splashWebView?.destroy()
        splashWebView = null

        super.onDestroy()
    }
}
