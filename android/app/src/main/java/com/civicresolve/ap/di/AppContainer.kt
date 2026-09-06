package com.civicresolve.ap.di

import android.content.Context
import com.civicresolve.ap.data.ThemePreferences
import com.civicresolve.ap.data.remote.ApiService
import com.civicresolve.ap.data.remote.CookieJarImpl
import com.civicresolve.ap.data.repository.AuthRepository
import com.civicresolve.ap.data.repository.ComplaintRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val authRepository: AuthRepository
    val complaintRepository: ComplaintRepository
    val cookieJar: CookieJarImpl
    val moshi: Moshi
    val baseUrl: String
    val socketUrl: String
    val themePreferences: ThemePreferences
    val apiService: ApiService
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val socketUrl = "https://ankle-paper-magnify.ngrok-free.dev"
    override val baseUrl = "$socketUrl/api/"

    override val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    override val cookieJar = CookieJarImpl(context)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val noCacheInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("Cache-Control", "no-cache, no-store")
            .header("Pragma", "no-cache")
            .build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(noCacheInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .build()

    override val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepository(apiService, cookieJar)
    }

    override val complaintRepository: ComplaintRepository by lazy {
        ComplaintRepository(apiService)
    }

    override val themePreferences: ThemePreferences by lazy {
        ThemePreferences(context)
    }
}
