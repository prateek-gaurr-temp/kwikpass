package com.gk.kwikpass.initializer

import android.content.Context
import com.gk.kwikpass.utils.AppUtils

object kwikpassInitializer {
    private var merchantId: String? = null
    private var environment: String? = null
    private var isSnowplowTrackingEnabled: Boolean = false
    private var applicationContext: Context? = null

    fun initialize(
        context: Context,
        merchantId: String,
        environment: String,
        isSnowplowTrackingEnabled: Boolean
    ) {
        this.applicationContext = context
        ApplicationCtx.instance = context

        this.merchantId = merchantId
        this.environment = environment
        this.isSnowplowTrackingEnabled = isSnowplowTrackingEnabled

        val appVersion = AppUtils.getHostAppVersion()
        println("APP VERSION $appVersion")

        val deviceInfo = AppUtils.getDeviceInfo()
        println("DEVICE INFO FROM MODULE $deviceInfo")

        // Example: Perform initialization tasks
        println("CustomModule Initialized")
        println("Merchant ID: $merchantId")
        println("Environment: $environment")
        println("Snowplow Tracking: $isSnowplowTrackingEnabled")

        // You can add more setup logic here (e.g., logging, network setup)
    }

    fun getMerchantId(): String? = merchantId
    fun getEnvironment(): String? = environment
    fun isTrackingEnabled(): Boolean = isSnowplowTrackingEnabled
    fun getAppContext(): Context? = applicationContext
}
