package com.faridganj.upazila

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    companion object {
        private const val HOME_URL =
            "https://faridganjapp.rf.gd/"

        private const val SITE_HOST = "faridganjapp.rf.gd"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cookie and Login Session support
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(
            WebView(this),
            true
        )

        webView = WebView(this)

        webView.setBackgroundColor(android.graphics.Color.WHITE)

        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadsImagesAutomatically = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(false)

        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode =
            WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        // Remove WebView identifier from User Agent
        settings.userAgentString =
            settings.userAgentString.replace("; wv", "")

        webView.webChromeClient = WebChromeClient()

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return handleUrl(view, request.url)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {
                return handleUrl(view, Uri.parse(url))
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)

                if (request.isForMainFrame) {
                    Toast.makeText(
                        this@MainActivity,
                        "ওয়েবসাইট লোড হয়নি। ইন্টারনেট পরীক্ষা করে আবার চেষ্টা করুন।",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        setContentView(webView)

        if (savedInstanceState == null) {
            webView.loadUrl(HOME_URL)
        } else {
            val restored = webView.restoreState(savedInstanceState)

            if (restored == null) {
                webView.loadUrl(HOME_URL)
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    private fun handleUrl(
        view: WebView,
        uri: Uri
    ): Boolean {

        val scheme = uri.scheme?.lowercase() ?: return true
        val host = uri.host?.lowercase() ?: ""

        val isInternalSite =
            host == SITE_HOST || host.endsWith(".$SITE_HOST")

        if (isInternalSite) {

            if (scheme == "https") {
                return false
            }

            if (scheme == "http") {
                val secureUrl = uri.buildUpon()
                    .scheme("https")
                    .build()
                    .toString()

                view.loadUrl(secureUrl)
                return true
            }
        }

        if (scheme == "tel" ||
            scheme == "mailto" ||
            scheme == "sms") {
            return openExternal(uri)
        }

        if (scheme == "https" || scheme == "http") {
            return openExternal(uri)
        }

        return true
    }

    private fun openExternal(uri: Uri): Boolean {
        return try {
            startActivity(
                Intent(Intent.ACTION_VIEW, uri)
            )
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "এই লিংক খোলার অ্যাপ পাওয়া যায়নি।",
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }

    override fun onPause() {
        CookieManager.getInstance().flush()
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
