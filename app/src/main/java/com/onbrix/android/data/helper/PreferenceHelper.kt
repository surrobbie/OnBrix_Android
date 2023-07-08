package com.onbrix.android.data.helper

import android.content.Context
import android.content.SharedPreferences
import com.onbrix.android.data.model.User
import com.onbrix.android.ext.log
import com.google.gson.Gson

object PreferenceHelper {

    private val PREF_FILE_NAME = "android_pref_file"
    private val PREF_KEY_TOKEN = "android_pref_key_token"
    private val PREF_KEY_USER = "android_pref_key_user"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
    }

    private fun getPreferencesEditor(context: Context): SharedPreferences.Editor {
        return getPreferences(
            context
        ).edit()
    }

    fun clearSharedPreferences(context: Context) {
        val editor =
            getPreferencesEditor(
                context
            )
        editor.remove(PREF_KEY_TOKEN)
        editor.apply()
    }

    fun setMessageToken(context: Context, token: String) {
        val editor =
            getPreferencesEditor(
                context
            )
        editor.putString(PREF_KEY_TOKEN, token)
        editor.apply()
    }

    fun getMessageToken(context: Context): String? {
        return getPreferences(
            context
        ).getString(PREF_KEY_TOKEN, "")
    }

    //  ====================================================================================

    fun getUser(context: Context): String {
        var value =  getPreferences(context).getString(PREF_KEY_USER, null);

        if(value != null) {
            return value
        }
        return Gson().toJson(User("", ""))
    }

    fun setUser(context: Context, id: String, password: String) {
        var user = User(id, password)
        var userToString = Gson().toJson(user)

        log("set user : " + userToString )

        val editor = getPreferencesEditor(context)
        editor.putString(PREF_KEY_USER, userToString)
        editor.apply()
    }

}