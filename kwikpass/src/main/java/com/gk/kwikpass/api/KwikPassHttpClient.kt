package com.gk.kwikpass.api

import com.gk.kwikpass.config.KwikPassConfig
import com.gk.kwikpass.config.KwikPassKeys
import com.gk.kwikpass.initializer.kwikpassInitializer
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
    private var apiService: KwikPassApiService? = null
    private var currentHeaders: MutableMap<String, String> = mutableMapOf()

    @Synchronized
    fun getClient(environment: String, mid: String): Retrofit {
        if (retrofit == null) {
            retrofit = createRetrofit(environment, mid)
        }
        return retrofit!!
    }

    @Synchronized
    fun getApiService(environment: String, mid: String): KwikPassApiService {
        if (apiService == null) {
            apiService = createService<KwikPassApiService>(environment, mid)
        }
        return apiService!!
    }

    fun getKwikPassClient(environment: String, mid: String): KwikPassHttpClient {
        getClient(environment, mid)
        return this
    }

    @Synchronized
    private fun createRetrofit(environment: String, mid: String): Retrofit {
        val config = KwikPassConfig.getConfig(environment)
        val appVersion = AppUtils.getHostAppVersion()

        // Create logging interceptor
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Create dynamic header interceptor
        val headerInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
                .header("accept", "*/*")
                .header("appplatform", "android")
                .header("appversion", appVersion)
                .header("source", "android-app")
                .header(KwikPassKeys.GK_MERCHANT_ID, mid.toString())
            
            // Add all current headers
            currentHeaders.forEach { (key, value) ->
                builder.header(key, value)
            }
            
            chain.proceed(builder.build())
        }

        // Create OkHttpClient with timeouts and interceptors
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(headerInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(config?.baseUrl.toString())
            .client(okHttpClient!!)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Synchronized
    fun setHeaders(headers: Map<String, String>) {
        // Update current headers
        currentHeaders.putAll(headers)
        
        // Recreate the client and retrofit instance with new headers
        val environment = kwikpassInitializer.getEnvironment() ?: "sandbox"
        val merchantId = kwikpassInitializer.getMerchantId()
        val mid = currentHeaders[KwikPassKeys.GK_MERCHANT_ID] ?: merchantId
        
        // Clear existing instances to force recreation
        okHttpClient = null
        retrofit = null
        apiService = null
        
        // Recreate instances with updated headers
        retrofit = createRetrofit(environment, mid.toString())
        apiService = retrofit?.create(KwikPassApiService::class.java)
    }

    @Synchronized
    fun clearHeaders() {
        currentHeaders.clear()
        // Force recreation of clients with cleared headers
        val environment = kwikpassInitializer.getEnvironment() ?: "sandbox"
        val mid = kwikpassInitializer.getMerchantId()
        
        okHttpClient = null
        retrofit = null
        apiService = null
        
        retrofit = createRetrofit(environment, mid.toString())
        apiService = retrofit?.create(KwikPassApiService::class.java)
    }

    inline fun <reified T> createService(environment: String, mid: String): T {
        return getClient(environment, mid).create(T::class.java)
    }
}