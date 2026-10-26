package com.vastobb.alpmegateway.net

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

object HttpClients {

    val data: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun upstream(timeoutSeconds: Int): OkHttpClient = data.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}
