package com.faridganj.upazila

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private var downloadId: Long = -1L
    private var receiverRegistered = false
    private var pendingInstallPermission = false

    private val preferences by lazy {
        getSharedPreferences("app_update_preferences", MODE_PRIVATE)
    }

    companion object {
        private const val HOME_URL = "https://faridganjapp.rf.gd/"
        private const val SITE_HOST = "faridganjapp.rf.gd"

        private const val GITHUB_API =
            "https://api.github.com/repos/royalcyber7r/FaridganjUpazila-App/releases/latest"

        private const val APK_NAME = "Faridganjapp.apk"
    }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                return
            }

            val completedId = intent.getLongExtra(
                DownloadManager.EXTRA_DOWNLOAD_ID,
                -1L
            )

            if (completedId != downloadId) return

            val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)

            manager.query(query).use { cursor ->

                if (cursor != null && cursor.moveToFirst()) {

                    val status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_STATUS
                        )
                    )

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {

                        Toast.makeText(
                            this@MainActivity,
                            "আপডেট ডাউনলোড সম্পন্ন হয়েছে।",
                            Toast.LENGTH_LONG
                        ).show()

                        showInstallDialog()

                    } else {

                        Toast.makeText(
                            this@MainActivity,
                            "APK ডাউনলোড ব্যর্থ হয়েছে। আবার চেষ্টা করুন।",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cookie and Login Session support
        webView = WebView(this)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

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

        // Register download completion receiver
        registerDownloadReceiver()

        if (savedInstanceState == null) {
            webView.loadUrl(HOME_URL)
        } else {
            val restored = webView.restoreState(savedInstanceState)

            if (restored == null) {
                webView.loadUrl(HOME_URL)
            }
        }

        // Check latest GitHub release
        checkForUpdate()

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

    private fun registerDownloadReceiver() {

        if (receiverRegistered) return

        val filter = IntentFilter(
            DownloadManager.ACTION_DOWNLOAD_COMPLETE
        )

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                downloadReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(downloadReceiver, filter)
        }

        receiverRegistered = true

        downloadId = preferences.getLong("download_id", -1L)
    }

    // Check GitHub Latest Release
    private fun checkForUpdate() {

        Thread {

            var connection: HttpURLConnection? = null

            try {
                connection = URL(GITHUB_API)
                    .openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty(
                    "Accept",
                    "application/vnd.github+json"
                )
                connection.setRequestProperty(
                    "User-Agent",
                    "FaridganjUpazila-Android-App"
                )

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@Thread
                }

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                val release = JSONObject(response)

                if (release.optBoolean("draft", false) ||
                    release.optBoolean("prerelease", false)) {
                    return@Thread
                }

                val latestVersion = release.optString("tag_name")
                    .removePrefix("v")
                    .removePrefix("V")

                val currentVersion = packageManager
                    .getPackageInfo(packageName, 0)
                    .versionName ?: "1.0.0"

                if (compareVersions(latestVersion, currentVersion) <= 0) {
                    return@Thread
                }

                val assets = release.optJSONArray("assets")
                    ?: return@Thread

                var apkUrl: String? = null

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)

                    if (asset.optString("name") == APK_NAME) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }

                if (apkUrl.isNullOrBlank()) return@Thread

                runOnUiThread {
                    showUpdateDialog(latestVersion, apkUrl)
                }

            } catch (_: Exception) {
                // Keep the app usable if update checking fails.
            } finally {
                connection?.disconnect()
            }

        }.start()
    }

    private fun compareVersions(
        latest: String,
        current: String
    ): Int {

        val latestParts = latest.split(".")
        val currentParts = current.split(".")

        val count = maxOf(latestParts.size, currentParts.size)

        for (i in 0 until count) {

            val latestNumber =
                latestParts.getOrNull(i)?.toIntOrNull() ?: 0

            val currentNumber =
                currentParts.getOrNull(i)?.toIntOrNull() ?: 0

            if (latestNumber > currentNumber) return 1
            if (latestNumber < currentNumber) return -1
        }

        return 0
    }

    private fun showUpdateDialog(
        version: String,
        apkUrl: String
    ) {

        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle("New Update Available")
            .setMessage(
                "Faridganj Upazila অ্যাপের নতুন ভার্সন $version পাওয়া গেছে।\n\n" +
                "নতুন ভার্সন ব্যবহার করতে Update চাপুন।"
            )
            .setPositiveButton("Update") { _, _ ->
                downloadUpdate(apkUrl)
            }
            .setNegativeButton("Later", null)
            .setCancelable(false)
            .show()
    }

    // Download Faridganjapp.apk
    private fun downloadUpdate(apkUrl: String) {

        try {

            val request = DownloadManager.Request(Uri.parse(apkUrl))

            request.setTitle("Faridganj Upazila Update")
            request.setDescription("Faridganjapp.apk ডাউনলোড হচ্ছে...")
            request.setMimeType("application/vnd.android.package-archive")

            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )

            request.setDestinationInExternalFilesDir(
                this,
                Environment.DIRECTORY_DOWNLOADS,
                APK_NAME
            )

            val manager = getSystemService(DOWNLOAD_SERVICE)
                as DownloadManager

            val oldId = preferences.getLong("download_id", -1L)

            if (oldId != -1L) {
                manager.remove(oldId)
            }

            downloadId = manager.enqueue(request)

            preferences.edit()
                .putLong("download_id", downloadId)
                .apply()

            Toast.makeText(
                this,
                "নতুন আপডেট ডাউনলোড শুরু হয়েছে।",
                Toast.LENGTH_LONG
            ).show()

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "ডাউনলোড শুরু করা যায়নি। আবার চেষ্টা করুন।",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showInstallDialog() {

        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle("Download Complete")
            .setMessage(
                "নতুন আপডেট ডাউনলোড সম্পন্ন হয়েছে। এখন Install চাপুন।"
            )
            .setPositiveButton("Install") { _, _ ->
                installApk()
            }
            .setNegativeButton("Later", null)
            .setCancelable(false)
            .show()
    }

    private fun installApk() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !packageManager.canRequestPackageInstalls()
        ) {

            pendingInstallPermission = true

            AlertDialog.Builder(this)
                .setTitle("Install Permission Required")
                .setMessage(
                    "অ্যাপ আপডেট ইনস্টল করতে এই অ্যাপের জন্য " +
                    "Install unknown apps অনুমতি চালু করুন।"
                )
                .setPositiveButton("Settings") { _, _ ->

                    try {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:$packageName")
                        )

                        startActivity(intent)

                    } catch (_: ActivityNotFoundException) {
                        pendingInstallPermission = false

                        Toast.makeText(
                            this,
                            "সেটিংস খোলা যায়নি।",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    pendingInstallPermission = false
                }
                .show()

            return
        }

        openApkInstaller()
    }

    private fun openApkInstaller() {

        try {

            val apkFile = File(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                APK_NAME
            )

            if (!apkFile.exists()) {
                Toast.makeText(
                    this,
                    "APK ফাইল পাওয়া যায়নি। আবার ডাউনলোড করুন।",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val apkUri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    apkUri,
                    "application/vnd.android.package-archive"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            startActivity(intent)

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "ইনস্টলার চালু করা যায়নি।",
                Toast.LENGTH_LONG
            ).show()
        }
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
            scheme == "sms"
        ) {
            return openExternal(uri)
        }

        if (scheme == "https" || scheme == "http") {
            return openExternal(uri)
        }

        return true
    }

    private fun openExternal(uri: Uri): Boolean {

        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
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

    override fun onResume() {
        super.onResume()

        webView.onResume()

        if (pendingInstallPermission) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                packageManager.canRequestPackageInstalls()
            ) {
                pendingInstallPermission = false
                openApkInstaller()
            }
        }
    }

    override fun onPause() {
        CookieManager.getInstance().flush()
        webView.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {

        if (receiverRegistered) {
            unregisterReceiver(downloadReceiver)
            receiverRegistered = false
        }

        webView.stopLoading()
        webView.destroy()

        super.onDestroy()
    }
}
