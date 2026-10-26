package com.vastobb.alpmegateway

import android.app.Application

class AlpmeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
