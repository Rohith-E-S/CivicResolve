package com.civicresolve.ap.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CookieJarImpl(context: Context) : CookieJar {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "SecureCookiePrefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val editor = sharedPreferences.edit()
        cookies.forEach { cookie ->
            if (cookie.name == "token") {
                editor.putString(cookie.name, cookie.value)
            }
        }
        editor.apply()
    }

    fun setToken(token: String) {
        sharedPreferences.edit().putString("token", token).apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = mutableListOf<Cookie>()
        val token = sharedPreferences.getString("token", null)
        if (token != null) {
            val cookie = Cookie.Builder()
                .name("token")
                .value(token)
                .domain(url.host)
                .build()
            cookies.add(cookie)
        }
        return cookies
    }

    fun clearCookies() {
        sharedPreferences.edit().clear().apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString("token", null)
    }
}
