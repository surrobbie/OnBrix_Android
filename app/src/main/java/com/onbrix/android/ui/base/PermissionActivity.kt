package com.onbrix.android.ui.base

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.onbrix.android.BuildConfig
import com.onbrix.android.R
import com.onbrix.android.ext.haveAllPermission
import com.onbrix.android.ext.retryCheckPermission


open class PermissionActivity: AppCompatActivity() {

    companion object {
        val REQUEST_PERMISSION = 100
        val APPLICATION_DETAILS_SETTINGS = 1000
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val permissions_13 = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS)
    val permissions = arrayOf(Manifest.permission.CAMERA)

    open fun resultPermission(permissions: Array<out String>, grantResults: IntArray) {
        // TODO :
    }

    open fun resultActivity(requestCode: Int, resultCode: Int, data: Intent?) {
        // TODO :
    }

    fun checkPermission(): Boolean {
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU) {
            if (haveAllPermission(this, permissions_13)) { return true }
            ActivityCompat.requestPermissions(this, permissions_13, REQUEST_PERMISSION)
        } else{
            if (haveAllPermission(this, permissions)) { return true }
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION)
        }
        return false
    }

    private fun navigatePermissionSetting() {
        val intent: Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID))
        startActivityForResult(intent,
            APPLICATION_DETAILS_SETTINGS
        )
    }

    private fun showPermissionDialog() {
        val dlgBuilder = AlertDialog.Builder(this)
        dlgBuilder.setMessage(R.string.should_show_request_permission_rationale)
        dlgBuilder.setCancelable(false)
        dlgBuilder.setPositiveButton(R.string.yes) { _, _ -> navigatePermissionSetting() }
        dlgBuilder.create().show()
    }

    @Override
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_PERMISSION -> {
                if(grantResults.size == permissions.size ) {
                    grantResults.filter { it != PackageManager.PERMISSION_GRANTED }.map {
                        if(retryCheckPermission(this, permissions)) {
                            checkPermission()
                        } else {
                            showPermissionDialog()
                        }
                        return
                    }
                    resultPermission(permissions, grantResults)
                }
            }
        }
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            APPLICATION_DETAILS_SETTINGS -> {
                if (haveAllPermission(this, permissions)){
                    resultActivity(requestCode, resultCode, data)
                } else {
                    showPermissionDialog()
                }
            }
        }
    }

}