package com.gk.kwikpass.api

import com.gk.kwikpass.config.KwikPassConfig
import com.gk.kwikpass.utils.AppUtils
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object KwikPassHttpClient {
    private var gokwikHttpClient: Retrofit? = null
    private var okHttpClient: OkHttpClient? = null

    fun initializeHttpClient(environment: String) {
        if (gokwikHttpClient != null) return
       
        val config = KwikPassConfig.getConfig(environment)


        val appVersion = AppUtils.getHostAppVersion()
        println("APP VERSION $appVersion")

        okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()

                // Add common headers matching React Native implementation
                builder.header("accept", "*/*")
                builder.header("appplatform", "android")
                builder.header("appversion", appVersion)
                builder.header("source", "android-app")

                chain.proceed(builder.build())
            }
            .build()

        gokwikHttpClient = Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClient!!)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getHttpClient(environment: String? = null): Retrofit {
        if (gokwikHttpClient == null) {
            if (environment == null) {
                throw IllegalStateException(
                    "HTTP client not initialized. Please call initializeHttpClient first or provide environment."
                )
            }
            initializeHttpClient(environment)
        }
        return gokwikHttpClient!!
    }

    inline fun <reified T> createService(environment: String? = null): T {
        return getHttpClient(environment).create(T::class.java)
    }

    fun setHeaders(headers: Map<String, String>) {
        val currentClient = okHttpClient ?: return
        okHttpClient = currentClient.newBuilder()
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()

                // Add all headers
                headers.forEach { (key, value) ->
                    builder.header(key, value)
                }

                chain.proceed(builder.build())
            }
            .build()

        // Rebuild Retrofit with new client
        gokwikHttpClient = gokwikHttpClient?.newBuilder()
            ?.client(okHttpClient!!)
            ?.build()
    }

    fun clearHeaders() {
        val currentClient = okHttpClient ?: return
        okHttpClient = currentClient.newBuilder()
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()

                // Only keep the accept header
                builder.header("accept", "*/*")

                chain.proceed(builder.build())
            }
            .build()

        // Rebuild Retrofit with new client
        gokwikHttpClient = gokwikHttpClient?.newBuilder()
            ?.client(okHttpClient!!)
            ?.build()
    }
}