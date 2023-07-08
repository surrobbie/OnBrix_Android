package com.onbrix.android.ui.webview.browser

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.text.TextUtils
import android.webkit.*
import android.widget.Toast
import com.onbrix.android.R
import com.onbrix.android.data.Constants
import com.onbrix.android.ext.log
import java.io.UnsupportedEncodingException
import java.net.URISyntaxException
import java.net.URLDecoder


class BrowserWebviewClient(val context: Context, val bridge: Bridge): WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url.toString()

        url?.let {
            if (!URLUtil.isNetworkUrl(url) && !URLUtil.isJavaScriptUrl(url)) {
                val uri = try {
                    Uri.parse(url)
                } catch (e: Exception) {
                    return false
                }

                return when (uri.scheme) {
                    "intent" -> {
                        startSchemeIntent(it)
                    }
                    else -> {
                        return try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            true
                        } catch (e: java.lang.Exception) {
                            false
                        }
                    }
                }
            } else {
                return false
            }
        } ?: return false
    }

    private fun startSchemeIntent(url: String): Boolean {
        val schemeIntent: Intent = try {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        } catch (e: URISyntaxException) {
            return false
        }
        try {
            context.startActivity(schemeIntent)
            return true
        } catch (e: ActivityNotFoundException) {
            val packageName = schemeIntent.getPackage()

            if (!packageName.isNullOrBlank()) {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$packageName")
                    )
                )
                return true
            }
        }
        return false
    }

    private fun handleSMSLink(url: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:")

        if (url.contains("body=")) {
            var smsBody = url.split("body=").toTypedArray()[1]

            try {
                smsBody = URLDecoder.decode(smsBody, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
            if (!TextUtils.isEmpty(smsBody)) {
                intent.putExtra("sms_body", smsBody)
            }
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No SMS app found.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        super.onReceivedSslError(view, handler, error)

        val builder: AlertDialog.Builder = AlertDialog.Builder(view!!.context)
        builder.setMessage(R.string.ssl_error_message)
        builder.setPositiveButton(R.string.yes,
            DialogInterface.OnClickListener { dialog, which -> handler!!.proceed() })
        builder.setNegativeButton(R.string.no,
            DialogInterface.OnClickListener { dialog, which -> handler!!.cancel() })
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    @JavascriptInterface
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        CookieManager.getInstance().flush()

        if (url != null) {
            if (url.contains(Constants.EndPoint) && Constants.isFirst) {
                bridge.appVersionRequest()
            }
        }
    }

}