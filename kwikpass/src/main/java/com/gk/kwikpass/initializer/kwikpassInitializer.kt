package com.gk.kwikpass.initializer

import android.content.Context
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import com.gk.kwikpass.api.KwikPassApi
import com.gk.kwikpass.api.KwikPassApiService
import com.gk.kwikpass.api.KwikPassHttpClient
import com.gk.kwikpass.config.KwikPassCache
import com.gk.kwikpass.config.KwikPassKeys
import com.gk.kwikpass.snowplow.SnowplowClient
import com.gk.kwikpass.utils.AppUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object kwikpassInitializer {
    private var merchantId: String? = null
    private var environment: String? = null
    private var isSnowplowTrackingEnabled: Boolean = false
    private var applicationContext: Context? = null
    private var apiService: KwikPassApiService? = null
    private lateinit var kwikPassApi: KwikPassApi


    suspend fun initialize(
        context: Context,
        merchantId: String,
        environment: String,
        isSnowplowTrackingEnabled: Boolean
    ) {
        this.applicationContext = context
        ApplicationCtx.instance = context

        val cache = KwikPassCache.getInstance(context)

        this.merchantId = merchantId
        this.environment = environment
        this.isSnowplowTrackingEnabled = isSnowplowTrackingEnabled


         cache.setValue(KwikPassKeys.GK_MERCHANT_ID, merchantId)
        // Initialize HTTP client and get API service
        apiService = KwikPassHttpClient.getApiService(environment, merchantId)
        val response = apiService?.getBrowserAuth()
        if (response?.isSuccessful == true) {
            println("response?.isSuccessful")
            val data = response.body()?.data

            println("DATA FROM RESPONSE $data")

            if (data != null) {
                // Set headers in HTTP client
               KwikPassHttpClient.setHeaders(
                    mapOf(
                        KwikPassKeys.GK_REQUEST_ID to data.requestId,
                        KwikPassKeys.KP_REQUEST_ID to data.requestId,
                        "Authorization" to data.token
                    )
               )

                // Set cache values in parallel
                coroutineScope {
                    listOf(
                        async { cache.setValue(KwikPassKeys.GK_REQUEST_ID, data.requestId) },
                        async { cache.setValue(KwikPassKeys.KP_REQUEST_ID, data.requestId) },
                        async { cache.setValue(KwikPassKeys.GK_AUTH_TOKEN, data.token) }
                    ).map { it.await() }
                }
            }
        }

        val apiresponse = apiService?.getMerchantConfig(merchantId.toString())

        if (apiresponse?.isSuccessful == true) {
            println("response?.isSuccessful")
            val data = apiresponse.body()?.data
            data?.platform?.toString()
                ?.let { cache.setValue(KwikPassKeys.GK_MERCHANT_TYPE, it.toLowerCase()) }
            cache.setValue(KwikPassKeys.GK_MERCHANT_CONFIG, data.toString())
        }

        val appVersion = AppUtils.getHostAppVersion()
        println("APP VERSION $appVersion")

        val deviceInfo = AppUtils.getDeviceInfo()
        println("DEVICE INFO FROM MODULE $deviceInfo")

        // Example: Perform initialization tasks
        println("CustomModule Initialized")
        println("Merchant ID: $merchantId")
        println("Environment: $environment")
        println("Snowplow Tracking: $isSnowplowTrackingEnabled")
    }

    fun getMerchantId(): String? = merchantId
    fun getEnvironment(): String? = environment
    fun isTrackingEnabled(): Boolean = isSnowplowTrackingEnabled
    fun getAppContext(): Context? = applicationContext
}
