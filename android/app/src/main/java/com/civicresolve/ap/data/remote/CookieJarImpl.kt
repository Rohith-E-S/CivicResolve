package com.civicresolve.ap.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CookieJarImpl(context: Context, backendUrl: HttpUrl) : CookieJar {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        "SecureCookiePrefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        // Legacy bare tokens have no trustworthy origin or expiry: require sign-in.
        preferences.edit().remove("token").apply()
    }

    private val delegate = PersistentCookieJar(object : CookieStorage {
        override fun read(): Set<String> = preferences.getStringSet("cookies_v2", emptySet())!!.toSet()
        override fun write(cookies: Set<String>) {
            preferences.edit().putStringSet("cookies_v2", cookies).apply()
        }
    }, backendUrl)

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = delegate.saveFromResponse(url, cookies)
    override fun loadForRequest(url: HttpUrl): List<Cookie> = delegate.loadForRequest(url)
    fun setToken(token: String) = delegate.setToken(token)
    fun getToken(): String? = delegate.getToken()
    fun clearCookies() = delegate.clearCookies()
}
