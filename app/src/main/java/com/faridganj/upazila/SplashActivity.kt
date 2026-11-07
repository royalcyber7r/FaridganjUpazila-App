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
                        padding: 24px;
                    }

                    img {
                        width: 130px;
                        height: 130px;
                        object-fit: contain;
                        margin-bottom: 20px;
                    }

                    h1 {
                        color: #173B57;
                        font-size: 26px;
                        font-weight: 800;
                        margin: 0;
                        letter-spacing: 0.3px;
                    }

                    .welcome {
                        color: #536B7C;
                        font-size: 15px;
                        margin-top: 12px;
                        font-weight: 500;
                    }

                    .tagline {
                        color: #16845B;
                        font-size: 12px;
                        margin-top: 9px;
                        letter-spacing: 1px;
                    }
                </style>
            </head>
            <body>
                <div class="content">
                    <img
                        src="https://faridganjapp.rf.gd/assets/logo.png"
                        alt="Faridganj Upazila Logo">

                    <h1>Faridganj Upazila</h1>

                    <div class="welcome">
                        Welcome to Faridganj Upazila
                    </div>

                    <div class="tagline">
                        YOUR DIGITAL UPAZILA
                    </div>
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
