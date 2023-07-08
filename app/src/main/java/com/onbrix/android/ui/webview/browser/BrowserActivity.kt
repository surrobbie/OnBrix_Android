package com.onbrix.android.ui.webview.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.onbrix.android.databinding.ActivityBrowserBinding
import com.onbrix.android.ext.log
import com.onbrix.android.ui.base.PermissionActivity

open class BrowserActivity: PermissionActivity() {

    protected lateinit var binding: ActivityBrowserBinding
    protected lateinit var frameLayout: FrameLayout
    protected lateinit var browser: Browser

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        frameLayout = binding.frameLayout

        browser = Browser(this)
        browser.webChromeClient = BrowserChromeClient(this, frameLayout)
        browser.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        frameLayout.addView(browser)
    }

    @Override
    override fun onPause() {
        super.onPause()
        browser.onPause()
    }

    @Override
    override fun onResume() {
        super.onResume()
        browser.onResume()
    }

    fun loadUrl(url: String) {
        val finalUrl: String = UrlQuerySanitizer.getUrlAndSpaceLegal().sanitize(url)
        browser.loadUrl(finalUrl)
    }

    private fun fileChooser(resultCode: Int, resultData: Intent?) {
        var data = resultData

        if (data == null)
            data = Intent()

        if (data.data == null)
            data.data = BrowserChromeClient.cameraImageUri

        var params: Array<Uri>? = null
        if (data.clipData != null) { // handle multiple-selected files
            val list = mutableListOf<Uri>()
            val numSelectedFiles = data.clipData!!.itemCount
            for (i in 0 until numSelectedFiles) {
                list.add(data.clipData!!.getItemAt(i).uri)
            }
            params = list.toTypedArray()
        } else if (data.data != null) { // handle single-selected file
            params = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
        }

        BrowserChromeClient.filePathCallbackLollipop?.onReceiveValue(params)
        BrowserChromeClient.filePathCallbackLollipop = null
    }

    @Override
    override fun resultPermission(permissions: Array<out String>, grantResults: IntArray) {
        super.resultPermission(permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        } else {
            checkPermission()
        }
    }

    @Override
    override fun resultActivity(requestCode: Int, resultCode: Int, data: Intent?) {
        log("browser result : " + requestCode + " / " + resultCode )
    }

    @Override
    override fun onBackPressed() {
        val childCount = frameLayout.childCount

        if(childCount > 1) {
            var newBrowser = frameLayout.getChildAt(childCount - 1) as Browser

            if (newBrowser.canGoBack())
                newBrowser.goBack()
            else {
                frameLayout.removeViewAt(childCount - 1)
                newBrowser.destroy()
            }
            return
        }

        if (browser.canGoBack()) browser.goBack() else super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            BrowserChromeClient.FILECHOOSER_REQ_CODE -> {

                if(resultCode != Activity.RESULT_OK) {
                    BrowserChromeClient.filePathCallbackLollipop?.onReceiveValue(null)
                    BrowserChromeClient.filePathCallbackLollipop = null
                    return
                }

                if (BrowserChromeClient.filePathCallbackLollipop == null)
                    return

                fileChooser(resultCode, data)
            }
        }
    }


}

