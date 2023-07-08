package com.onbrix.android.ext

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


fun Any.getClassTag(): String = this.javaClass.name
fun Any.getClassSimpleTag(): String = this.javaClass.simpleName

@Suppress("NOTHING_TO_INLINE")
inline fun Any.getMethodTag(): String =
    getClassTag() + "::" + object : Any() {}.javaClass.enclosingMethod?.name

fun Any.log(message: String) =
    Log.i("rrobbie", message)


fun getVersionInfo(context: Context): String? {
    try {
        val packageInfo = context.applicationContext
            .packageManager
            .getPackageInfo(context.applicationContext.packageName, 0)
        return packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return ""
}

fun haveAllPermission(context: Context, permissions: Array<out String>): Boolean {
    permissions.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }.map {
        return false
    }
    return true
}

fun retryCheckPermission(context: Context, permissions: Array<out String>): Boolean {
    permissions.filter { ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, it) }.map {
        return true
    }
    return false
}
