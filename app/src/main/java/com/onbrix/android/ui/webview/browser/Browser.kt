package com.onbrix.android.ui.webview.browser

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.AttributeSet
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import com.onbrix.android.BuildConfig
import com.onbrix.android.R
import com.onbrix.android.ext.log
import com.onbrix.android.ui.base.PermissionActivity

class Browser: WebView {

    constructor(context: Context): super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize()
    }

    private fun initialize() {
        setWebViewSettings()
        setProperties()
        addDownloadListener()
    }

    private fun setWebViewSettings() {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.builtInZoomControls = true // 확대
        settings.displayZoomControls = false
        settings.setSupportZoom(true)
        settings.setSupportMultipleWindows(true)

        // 파일 허용
        settings.allowContentAccess = true
        settings.allowFileAccess = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.loadsImagesAutomatically = true

        // 웹뷰 생성시 useragent에 uniq 값을 전달하여 앱 종료 여부를 웹에서 판단할 수 있도록 한다.
        var appVersion = BuildConfig.VERSION_NAME
        val userAgentString = settings.userAgentString + "&app=onbrix&os=android&appVersion=" + appVersion
        settings.userAgentString = userAgentString

        setAllowCookie()
    }

    // 쿠키 허용 필수 : 나이스 인증 관련
    private fun setAllowCookie() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)
    }

    private fun setProperties() {
        isVerticalFadingEdgeEnabled = false
        isHorizontalScrollBarEnabled = false
        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val bridge: Bridge = Bridge(context, this)
        addJavascriptInterface(bridge, Bridge.CALL_NAME)
        webViewClient = BrowserWebviewClient(context, bridge)
    }

    // =======================================================

    fun setBackground(color: Int) {
        setBackgroundColor(color)
    }

    fun callToJavascript(func: String) {
        log("callToJavascript: $func")
        try {
            post {
                evaluateJavascript(func
                ) { value -> log("callToJavascript receive : $value") }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addDownloadListener() {
        setDownloadListener(fun(
            url: String,
            userAgent: String,
            contentDisposition: String,
            mistypes: String,
            _: Long
        ) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) === PackageManager.PERMISSION_GRANTED) {
                    downloadDialog(url, userAgent, contentDisposition, mistypes)
                } else {
                    ActivityCompat.requestPermissions(
                        context as Activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PermissionActivity.REQUEST_PERMISSION
                    )
                }
            } else {
                downloadDialog(url, userAgent, contentDisposition, mistypes)
            }
        })
    }

    private fun downloadDialog(url: String?, userAgent: String?, contentDisposition: String?, mistypes: String?) {
        val filename = URLUtil.guessFileName(url, contentDisposition, mistypes)
        val builder = AlertDialog.Builder(context)
        builder.setMessage(context.getString(R.string.download_file))
        builder.setPositiveButton(
            context.getString(R.string.yes), fun(dialog: DialogInterface, which: Int) {
                val request = DownloadManager.Request(Uri.parse(url))
                val cookie = CookieManager.getInstance().getCookie(url)
                request.addRequestHeader("Cookie", cookie)
                request.addRequestHeader("User-Agent", userAgent)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                val downloadManager =
                    context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                downloadManager.enqueue(request)
            }
        )
        builder.setNegativeButton(R.string.no, { dialog, which -> dialog.cancel() })
        builder.show()
    }

}
