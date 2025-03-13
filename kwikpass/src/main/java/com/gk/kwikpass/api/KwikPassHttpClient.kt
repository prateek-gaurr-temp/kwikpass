package com.gk.kwikpass.api

import com.gk.kwikpass.config.KwikPassConfig
import com.gk.kwikpass.config.KwikPassKeys
import com.gk.kwikpass.utils.AppUtils
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object KwikPassHttpClient {
    private var retrofit: Retrofit? = null
    private var okHttpClient: OkHttpClient? = null

    fun getClient(environment: String, mid: String): Retrofit {
        if (retrofit == null) {
            retrofit = createRetrofit(environment, mid)
        }
        return retrofit!!
    }

    private fun createRetrofit(environment: String, mid: String): Retrofit {
        val config = KwikPassConfig.getConfig(environment)
        val appVersion = AppUtils.getHostAppVersion()

        // Create logging interceptor
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Create header interceptor
        val headerInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val requestWithHeader = originalRequest.newBuilder()
                .header("accept", "*/*")
                .header("appplatform", "android")
                .header("appversion", appVersion)
                .header("source", "android-app")
                .header(KwikPassKeys.GK_MERCHANT_ID, mid.toString())
                .build()
            chain.proceed(requestWithHeader)
        }

        // Create OkHttpClient with timeouts and interceptors
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(headerInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(config?.baseUrl.toString())
            .client(okHttpClient!!)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun setHeaders(headers: Map<String, String>) {
        val currentClient = okHttpClient ?: return
        okHttpClient = currentClient.newBuilder()
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                headers.forEach { (key, value) ->
                    builder.header(key, value)
                }
                chain.proceed(builder.build())
            }
            .build()
    }

    fun clearHeaders() {
        val currentClient = okHttpClient ?: return
        okHttpClient = currentClient.newBuilder()
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                builder.header("accept", "*/*")
                chain.proceed(builder.build())
            }
            .build()
    }

    inline fun <reified T> createService(environment: String, mid: String): T {
        return getClient(environment, mid).create(T::class.java)
    }
}