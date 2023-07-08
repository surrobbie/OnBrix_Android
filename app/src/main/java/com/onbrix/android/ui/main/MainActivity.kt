package com.onbrix.android.ui.main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import com.facebook.FacebookSdk
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.onbrix.android.data.Constants
import com.onbrix.android.data.event.BusEvent
import com.onbrix.android.data.event.BusProvider
import com.onbrix.android.data.helper.PreferenceHelper
import com.onbrix.android.ext.log
import com.onbrix.android.ui.webview.browser.BrowserActivity
import com.squareup.otto.Subscribe

class MainActivity: BrowserActivity() {

    private final val EndPoint = Constants.EndPoint
    private val SPLASH_TIME_OUT: Long = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BusProvider.get()?.register(this)

        FacebookSdk.setAutoLogAppEventsEnabled(true)
        FacebookSdk.setAutoInitEnabled(true)
        FacebookSdk.fullyInitialize()

        // 앱 배경색을 투명으로 처리
        browser.setBackground(Color.TRANSPARENT)

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }

            task.result?.let {
                PreferenceHelper.setMessageToken(this, it)
                log("push onbrix token : " + it )
            }
        })

        if(checkPermission()) {
            startWebView()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BusProvider.get()?.unregister(this)
    }

    override fun resultActivity(requestCode: Int, resultCode: Int, data: Intent?) {
        startWebView()
    }

    override fun resultPermission(permissions: Array<out String>, grantResults: IntArray) {
        startWebView()
    }

    private fun startWebView() {
        Handler().postDelayed({
            intent?.let {
                // push notification을 통해 유입된 경우 url
                var url = intent.getStringExtra(Constants.URL)

                // scheme를 통해 앱으로 유입된 경우 해당 url을 main 전환시 전달한다.
                if (Intent.ACTION_VIEW == intent.action) {
                    intent.data?.let {
                        it.getQueryParameter(Constants.URL)?.let { url = it }
                    }
                }

                // push or scheme 관련 처리로 인한 진입이 아닌 일반 진입의 경우 home으로 이동한다.
                if(url.isNullOrEmpty()) {
                    url = EndPoint
                }

                loadUrl(url!!)

                browser?.let {
                    it.setBackgroundColor(Color.WHITE)
                }
            }
        }, SPLASH_TIME_OUT)
    }

    @Subscribe
    open fun onBusEvent(event: BusEvent) {
        browser?.let {
            try {
                val token = PreferenceHelper.getMessageToken(this)
                it.callToJavascript("javascript:setToken('$token');")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}