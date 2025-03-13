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

object kwikpassInitializer {
    private var merchantId: String? = null
    private var environment: String? = null
    private var isSnowplowTrackingEnabled: Boolean = false
    private var applicationContext: Context? = null
    private var apiService: KwikPassApiService? = null
    private lateinit var kwikPassApi: KwikPassApi


    fun initialize(
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


//        cache.setValue(KwikPassKeys.GK_MERCHANT_ID, merchantId))

        // Initialize HTTP client and create API service
       // apiService = KwikPassHttpClient.createService(environment, merchantId)
        println("initialize: ${environment} ${merchantId}" )
        apiService = KwikPassHttpClient.createService(environment.toString(), merchantId.toString())

        kwikPassApi = KwikPassApi(context)
        kwikPassApi.setApiService(apiService!!)

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
