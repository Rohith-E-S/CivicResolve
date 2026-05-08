package com.example.complaintportal.di

import android.content.Context
import com.example.complaintportal.data.notification.NotificationApiService
import com.example.complaintportal.data.remote.ApiService
import com.example.complaintportal.data.remote.CookieJarImpl
import com.example.complaintportal.data.repository.AuthRepository
import com.example.complaintportal.data.repository.ComplaintRepository
import com.example.complaintportal.data.repository.MessageRepository
import com.example.complaintportal.data.repository.AnalyticsRepository
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
    val messageRepository: MessageRepository
    val analyticsRepository: AnalyticsRepository
    val notificationApiService: NotificationApiService
    val cookieJar: CookieJarImpl
    val moshi: Moshi
    val baseUrl: String
    val socketUrl: String
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    // For Emulator: http://10.0.2.2:4000
    // For Physical Device: http://YOUR_IP_ADDRESS:4000
    override val socketUrl = "https://nonadjacent-unsurnamed-lizabeth.ngrok-free.dev"
    //override val socketUrl = "http://10.0.2.2:4000"
    
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

    private val retrofitService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepository(retrofitService, cookieJar)
    }

    override val complaintRepository: ComplaintRepository by lazy {
        ComplaintRepository(retrofitService)
    }

    override val messageRepository: MessageRepository by lazy {
        MessageRepository(retrofitService)
    }

    override val analyticsRepository: AnalyticsRepository by lazy {
        AnalyticsRepository(retrofitService)
    }

    override val notificationApiService: NotificationApiService by lazy {
        retrofit.create(NotificationApiService::class.java)
    }
}
