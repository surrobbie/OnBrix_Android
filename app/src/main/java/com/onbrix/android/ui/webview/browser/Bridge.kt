package com.onbrix.android.ui.webview.browser

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import androidx.core.app.ActivityCompat
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.analytics.FirebaseAnalytics
import com.onbrix.android.BuildConfig
import com.onbrix.android.R
import com.onbrix.android.data.Constants
import com.onbrix.android.data.helper.PreferenceHelper
import com.onbrix.android.ext.getVersionInfo
import com.onbrix.android.ext.log


class Bridge(val context: Context, val browser: Browser) {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    companion object {
        val CALL_NAME = "android"
    }

    @JavascriptInterface
    fun transTrackingGA(name: String?, id: String?, quantity: String?, currency: String?, coupon: String?) {
        browser?.let {
            log("transTrackingGA > name : '$name', id : '$id', quantity : '$quantity', currency : '$currency', coupon : '$coupon'")

            firebaseAnalytics = FirebaseAnalytics.getInstance(context)

            if (name != null) {
                val params = Bundle()
                //  params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "product")
                params.putString(FirebaseAnalytics.Param.ITEM_ID, id)
                params.putString(FirebaseAnalytics.Param.QUANTITY, quantity)
                params.putString(FirebaseAnalytics.Param.CURRENCY, currency)
                params.putString(FirebaseAnalytics.Param.COUPON, coupon)

                firebaseAnalytics.logEvent(name, params)
            }

        }
    }

    @JavascriptInterface
    fun transTrackingFB(name: String?, id: String?, items: String?, currency: String?) {
        browser?.let {
            log("transTrackingFB > name : '$name', id : '$id', items : '$items', currency : '$currency'")

            if(name != null) {
                val params = Bundle()
                //  params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, "product");
                params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, id)
                params.putString(AppEventsConstants.EVENT_PARAM_NUM_ITEMS, items)
                params.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currency)

                val logger = AppEventsLogger.newLogger(context)
                logger.logEvent(name, params)
            }
        }
    }

    @JavascriptInterface
    fun getToken() {
        browser?.let {
            val token = PreferenceHelper.getMessageToken(context)
            it.callToJavascript("javascript:appBridgeDeviceToken('android','$token');")
        }
    }

    @JavascriptInterface
    fun getVersion() {
        browser?.let {
            it.callToJavascript("javascript:setVersion('${getVersionInfo(context)}');")
        }
    }

    @JavascriptInterface
    fun appVersionRequest() {
        browser?.let {
            it.callToJavascript("javascript:appVersionRequest('android');")
        }
    }

    @JavascriptInterface
    fun checkAppVersion(version: String, minVersion: String, message: String) {
        log("checkAppVersion : " + version + "  / " + minVersion)

        browser?.let {
            val appVersion = BuildConfig.VERSION_NAME
            val isAppStore = appVersion.compareTo(version)
            val isMin = appVersion.compareTo(minVersion)

            val dlgBuilder = AlertDialog.Builder(context)
            dlgBuilder.setTitle("버전 업데이트")
            dlgBuilder.setMessage(message)
            dlgBuilder.setPositiveButton(R.string.yes) { dialog, which -> navigatePermissionSetting() }

            log("version : " + version + "  / " + minVersion)

            if(isMin < 0) {
                dlgBuilder.create().show()
            } else if(isAppStore < 0) {
                dlgBuilder.setNegativeButton(R.string.no) { dialog, which -> { } }
                dlgBuilder.create().show()
            }

            Constants.isFirst = false
        }
    }

    private fun navigatePermissionSetting() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("market://details?id=com.onbrix.android")
        context.startActivity(intent)

        ActivityCompat.finishAffinity(context as Activity)
        System.exit(0)
    }

    @JavascriptInterface
    fun externalBrowser(url: String) {
        log("externalBrowser : " + url + " / " + context)

        val uri: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    @JavascriptInterface
    fun login(id: String, password: String) {
        log("login : " + id + " / " + password)
        PreferenceHelper.setUser(context, id, password)
    }

    @JavascriptInterface
    fun logout() {
        PreferenceHelper.setUser(context, "", "")
    }

    @JavascriptInterface
    fun getLoginInfo() {
        log("getLoginInfo : " + PreferenceHelper.getUser(context) )

        browser?.let {
            var userString = PreferenceHelper.getUser(context)
            var functionName = "javascript:setLoginInfo('${userString}');"
            it.callToJavascript(functionName)

            log("getLoginInfo callToJavascript : " + functionName)
        }
    }

}