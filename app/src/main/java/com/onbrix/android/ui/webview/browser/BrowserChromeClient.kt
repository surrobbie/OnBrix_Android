package com.onbrix.android.ui.webview.browser

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Message
import android.os.Parcelable
import android.provider.MediaStore
import android.view.*
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.onbrix.android.R
import com.onbrix.android.ext.log
import java.io.File


class BrowserChromeClient(val activity: Activity, val parent: FrameLayout): WebChromeClient() {

    private var mCustomView: View? = null
    private var mCustomViewCallback: CustomViewCallback? = null
    private var mOriginalOrientation = 0
    private var mFullscreenContainer: FrameLayout? = null
    private val COVER_SCREEN_PARAMS = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    companion object {
        val FILECHOOSER_REQ_CODE = 2002
        var filePathCallbackLollipop: ValueCallback<Array<Uri>>? = null
        var cameraImageUri: Uri? = null
    }

    @Override
    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
        val newView = Browser(activity)
        newView.webChromeClient = BrowserChromeClient(activity, parent)
        newView.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        parent.addView(newView)

        val transport = resultMsg.obj as WebViewTransport
        transport.webView = newView
        resultMsg.sendToTarget()
        return true
    }

    @Override
    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        parent.removeView(window)
    }

    @Override
    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        val finalRes = result!!
        val dlgBuilder = AlertDialog.Builder(view!!.context)
        dlgBuilder.setMessage(message)
        dlgBuilder.setCancelable(false)
        dlgBuilder.setPositiveButton(R.string.yes) { dialog, which -> finalRes.confirm() }

        val alertDlg: AlertDialog = dlgBuilder.create()
        alertDlg.show()
        return alertDlg.isShowing
    }

    @Override
    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        val dlgBuilder = AlertDialog.Builder(view!!.context)
        dlgBuilder.setMessage(message)
        dlgBuilder.setCancelable(false)
        dlgBuilder.setPositiveButton(R.string.yes) { dialog, which -> result!!.confirm() }
        dlgBuilder.setNegativeButton(R.string.no) { dialog, which -> result!!.cancel() }

        val alertDlg: AlertDialog = dlgBuilder.create()
        alertDlg.show()
        return alertDlg.isShowing
    }

    // 사진 촿영 및 앨범 호출
    @Override
    override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
        log("onShowFileChooser : " + fileChooserParams!!.isCaptureEnabled )
        if (filePathCallbackLollipop != null) {
            filePathCallbackLollipop!!.onReceiveValue(null)
            filePathCallbackLollipop = null
        }
        filePathCallbackLollipop = filePathCallback
        runCamera(fileChooserParams)
        return true
    }

    private fun runCamera(fileChooserParams: FileChooserParams?) {
        val intentCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val file = File(activity.externalCacheDir!!.absolutePath, "/sample.png")
        val isCapture = fileChooserParams!!.isCaptureEnabled

        cameraImageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(activity.applicationContext,activity.applicationContext.packageName + ".provider", file)
        } else {
            Uri.fromFile(file)
        }

        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)

        if (!isCapture) {
            val pickIntent = fileChooserParams?.createIntent()

            if( fileChooserParams?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE ) {
                pickIntent?.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }

            val chooserIntent = Intent.createChooser(pickIntent, activity.getString(R.string.pick_image))
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(intentCamera))
            activity.startActivityForResult(chooserIntent, FILECHOOSER_REQ_CODE)
        } else {
            activity.startActivityForResult(intentCamera, FILECHOOSER_REQ_CODE)
        }
    }

    // ================================================================================

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mCustomView != null) {
                callback.onCustomViewHidden()
                return
            }
            mOriginalOrientation = activity.getRequestedOrientation()
            val decor = activity.getWindow().getDecorView() as FrameLayout
            mFullscreenContainer = FullscreenHolder(activity)
            (mFullscreenContainer as FullscreenHolder).addView(view, COVER_SCREEN_PARAMS)
            decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS)
            mCustomView = view
            setFullscreen(true)
            mCustomViewCallback = callback
        }
        super.onShowCustomView(view, callback)
    }

    override fun onShowCustomView(
        view: View,
        requestedOrientation: Int,
        callback: CustomViewCallback
    ) {
        this.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        if (mCustomView == null) {
            return
        }

        setFullscreen(false)
        val decor = activity.window.decorView as FrameLayout
        decor.removeView(mFullscreenContainer)
        mFullscreenContainer = null
        mCustomView = null
        mCustomViewCallback?.onCustomViewHidden()
        activity.setRequestedOrientation(mOriginalOrientation)
    }

    private fun setFullscreen(enabled: Boolean) {
        val win: Window = activity.window
        val winParams: WindowManager.LayoutParams = win.getAttributes()
        val bits = WindowManager.LayoutParams.FLAG_FULLSCREEN
        if (enabled) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
            mCustomView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        win.attributes = winParams
    }

    private class FullscreenHolder(ctx: Context?) : FrameLayout(ctx!!) {
        override fun onTouchEvent(evt: MotionEvent): Boolean {
            return true
        }

        init {
            setBackgroundColor(ContextCompat.getColor(ctx!!, android.R.color.black))
        }
    }

}