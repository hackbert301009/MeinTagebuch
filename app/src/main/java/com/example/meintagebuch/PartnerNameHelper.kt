package com.example.meintagebuch

import android.content.Context
import android.content.SharedPreferences

object PartnerNameHelper {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_MY_NAME = "my_name"

    fun getMyName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_MY_NAME, null) ?: ""
    }

    fun setMyName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MY_NAME, name).apply()
    }

    fun hasMyName(context: Context): Boolean {
        return getMyName(context).isNotBlank()
    }
}