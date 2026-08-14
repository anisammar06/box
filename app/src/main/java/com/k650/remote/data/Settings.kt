package com.k650.remote.data

import android.content.Context
import com.k650.remote.BuildConfig

/** Persists the soundbar endpoint. The app points a fixed IP (static DHCP). */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("k650", Context.MODE_PRIVATE)

    var host: String
        get() = prefs.getString("host", BuildConfig.DEFAULT_HOST) ?: BuildConfig.DEFAULT_HOST
        set(value) = prefs.edit().putString("host", value).apply()

    var port: Int
        get() = prefs.getInt("port", BuildConfig.DEFAULT_PORT)
        set(value) = prefs.edit().putInt("port", value).apply()
}
