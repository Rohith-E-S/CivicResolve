package com.civicresolve.ap

import android.app.Application
import com.civicresolve.ap.di.AppContainer
import com.civicresolve.ap.di.DefaultAppContainer

class CivicResolveApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
