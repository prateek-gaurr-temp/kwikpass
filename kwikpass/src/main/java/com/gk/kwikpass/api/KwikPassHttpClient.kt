package com.gk.kwikpass.api
//
//import android.content.pm.PackageManager
//import com.gk.kwikpass.KwikPassSdk
//import com.gk.kwikpass.config.KwikPassCache
//import com.gk.kwikpass.config.KwikPassConfig
//import com.gk.kwikpass.config.KwikPassKeys
//import kotlinx.coroutines.runBlocking
//import okhttp3.OkHttpClient
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//
//object KwikPassHttpClient {
//    private var gokwikHttpClient: Retrofit? = null
//    private val cache by lazy { KwikPassCache.getInstance(KwikPassSdk.appContext) }
//
//    fun initializeHttpClient(environment: String) {
//        if (gokwikHttpClient != null) return
//
////        val appVersion = try {
////            KwikPassSdk.appContext.packageManager.getPackageInfo(
////                KwikPassSdk.appContext.packageName,
////                0
////            ).versionName
////        } catch (e: PackageManager.NameNotFoundException) {
////            "1.0.0"
////        }
//
//        val config = KwikPassConfig.getConfig(environment)
//
//        val okHttpClient = OkHttpClient.Builder()
//            .addInterceptor { chain ->
//                val original = chain.request()
//                val builder = original.newBuilder()
//
//                // Add common headers matching React Native implementation
//                builder.header("accept", "*/*")
//                builder.header("appplatform", "android")
//                builder.header("appversion", appVersion)
//                builder.header("source", "android-app")
//                builder.header("Content-Type", "application/json")
//
//                // Add dynamic headers from cache
//                runBlocking {
//                    cache.getValue(KwikPassKeys.GK_MERCHANT_ID)?.let {
//                        builder.header("x-gk-merchant-id", it)
//                    }
//                    cache.getValue(KwikPassKeys.GK_ACCESS_TOKEN)?.let {
//                        builder.header("x-gk-access-token", it)
//                    }
//                    cache.getValue(KwikPassKeys.GK_REQUEST_ID)?.let {
//                        builder.header("x-gk-request-id", it)
//                        builder.header("x-kp-request-id", it)
//                    }
//                    cache.getValue(KwikPassKeys.CHECKOUT_ACCESS_TOKEN)?.let {
//                        builder.header("x-checkout-access-token", it)
//                    }
//                }
//
//                chain.proceed(builder.build())
//            }
//            .build()
//
//        gokwikHttpClient = Retrofit.Builder()
//            .baseUrl(config.baseUrl)
//            .client(okHttpClient)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//    }
//
//    fun getHttpClient(environment: String? = null): Retrofit {
//        if (gokwikHttpClient == null) {
//            if (environment == null) {
//                throw IllegalStateException(
//                    "HTTP client not initialized. Please call initializeHttpClient first or provide environment."
//                )
//            }
//            initializeHttpClient(environment)
//        }
//        return gokwikHttpClient!!
//    }
//
//    inline fun <reified T> createService(environment: String? = null): T {
//        return getHttpClient(environment).create(T::class.java)
//    }
//}