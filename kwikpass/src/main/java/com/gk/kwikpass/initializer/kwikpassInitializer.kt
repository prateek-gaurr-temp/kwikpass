package com.gk.kwikpass.initializer

import android.content.Context
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import com.gk.kwikpass.IdfaAid.IdfaAidModule
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
    private var idfaAidModule: IdfaAidModule? = null

    suspend fun initialize(
        context: Context,
        merchantId: String,
        environment: String,
        isSnowplowTrackingEnabled: Boolean
    ) {
        try {
            this.applicationContext = context
            ApplicationCtx.instance = context
            val cache = KwikPassCache.getInstance(context)

            this.merchantId = merchantId
            this.environment = environment
            this.isSnowplowTrackingEnabled = isSnowplowTrackingEnabled

            // Set initial cache values in parallel
            coroutineScope {
                listOf(
                    async { cache.setValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED, isSnowplowTrackingEnabled.toString()) },
                    async { cache.setValue(KwikPassKeys.GK_ENVIRONMENT, environment) },
                    async { cache.setValue(KwikPassKeys.GK_MERCHANT_ID, merchantId) }
                ).map { it.await() }
            }

            // Get cached values in parallel
            val (requestId, accessToken, checkoutAccessToken) = coroutineScope {
                val requestIdDeferred = async { cache.getValue(KwikPassKeys.GK_REQUEST_ID) }
                val accessTokenDeferred = async { cache.getValue(KwikPassKeys.GK_ACCESS_TOKEN) }
                val checkoutTokenDeferred = async { cache.getValue(KwikPassKeys.CHECKOUT_ACCESS_TOKEN) }
                Triple(
                    requestIdDeferred.await(),
                    accessTokenDeferred.await(),
                    checkoutTokenDeferred.await()
                )
            }

            // Set tokens if they exist
            accessToken?.let {
                KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.GK_ACCESS_TOKEN to it))
            }
            checkoutAccessToken?.let {
                KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.CHECKOUT_ACCESS_TOKEN to it))
            }

            if(apiService== null) {
                // Initialize HTTP client and get browser token
                apiService = KwikPassHttpClient.getApiService(environment, merchantId)
            }

            val browserAuthResponse = apiService?.getBrowserAuth()
            if (browserAuthResponse?.isSuccessful == true) {
                val data = browserAuthResponse.body()?.data
                if (data != null) {
                    // Set headers and cache values
                    KwikPassHttpClient.setHeaders(
                        mapOf(
                            KwikPassKeys.GK_REQUEST_ID to data.requestId,
                            KwikPassKeys.KP_REQUEST_ID to data.requestId,
                            "Authorization" to data.token
                        )
                    )
                    coroutineScope {
                        listOf(
                            async { cache.setValue(KwikPassKeys.GK_REQUEST_ID, data.requestId) },
                            async { cache.setValue(KwikPassKeys.KP_REQUEST_ID, data.requestId) },
                            async { cache.setValue(KwikPassKeys.GK_AUTH_TOKEN, data.token) }
                        ).map { it.await() }
                    }
                }
            }

            // Get merchant config
            val merchantConfigResponse = apiService?.getMerchantConfig(merchantId)
            if (merchantConfigResponse?.isSuccessful == true) {
                val data = merchantConfigResponse.body()?.data
                data?.platform?.toString()?.let { 
                    cache.setValue(KwikPassKeys.GK_MERCHANT_TYPE, it.lowercase())
                }
                data?.host?.let {
                    cache.setValue(KwikPassKeys.GK_MERCHANT_URL, getHostName(it))
                }
                cache.setValue(KwikPassKeys.GK_MERCHANT_CONFIG, data.toString())
            }



            if(idfaAidModule == null){
                idfaAidModule = IdfaAidModule.getInstance(context)
            }

            val advertisingInfo = idfaAidModule?.getAdvertisingInfo()

            // Collect device info
            val deviceInfoDetails = mutableMapOf<String, String>().apply {
                put(KwikPassKeys.GK_DEVICE_MODEL, android.os.Build.MODEL)
                put(KwikPassKeys.GK_APP_DOMAIN, context.packageName)
                put(KwikPassKeys.GK_OPERATING_SYSTEM, "Android ${android.os.Build.VERSION.RELEASE}")
                put(KwikPassKeys.GK_DEVICE_ID, android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID))
                put(KwikPassKeys.GK_DEVICE_UNIQUE_ID, android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID))
                put(KwikPassKeys.GK_APP_VERSION, context.packageManager.getPackageInfo(context.packageName, 0).versionName)
                put(KwikPassKeys.GK_APP_VERSION_CODE, context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toString())
                put(KwikPassKeys.GK_GOOGLE_AD_ID, advertisingInfo?.id ?: "" )

                val displayMetrics = context.resources.displayMetrics
                put(KwikPassKeys.GK_SCREEN_RESOLUTION, "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}")
                
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
                put(KwikPassKeys.GK_CARRIER_INFO, telephonyManager.networkOperatorName ?: "")
                
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                put(KwikPassKeys.GK_BATTERY_STATUS, batteryLevel.toString())
                
                put(KwikPassKeys.GK_LANGUAGE, java.util.Locale.getDefault().toString())
                put(KwikPassKeys.GK_TIME_ZONE, java.util.TimeZone.getDefault().id)
            }

            println("DEVICE INFO DETAILS: $deviceInfoDetails")

            // Store device info
            cache.setValue(KwikPassKeys.GK_DEVICE_INFO, deviceInfoDetails.toString())

            // Set request ID if it exists
            requestId?.let {
                KwikPassHttpClient.setHeaders(
                    mapOf(
                        KwikPassKeys.KP_REQUEST_ID to it,
                        KwikPassKeys.GK_REQUEST_ID to it
                    )
                )
            }

            println("CustomModule Initialized Successfully")
            println("Merchant ID: $merchantId")
            println("Environment: $environment")
            println("Snowplow Tracking: $isSnowplowTrackingEnabled")

        } catch (e: Exception) {
            println("Error in initialize sdk: ${e.message}")
            throw e
        }
    }

    private fun getHostName(url: String): String {
        return url.replace(Regex("^(?:https?://)?(?:www\\.)?([^/]+).*$"), "$1")
    }

    fun getMerchantId(): String? = merchantId
    fun getEnvironment(): String? = environment
    fun isTrackingEnabled(): Boolean = isSnowplowTrackingEnabled
    fun getAppContext(): Context? = applicationContext
}
