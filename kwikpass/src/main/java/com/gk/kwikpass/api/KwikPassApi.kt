package com.gk.kwikpass.api

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import com.gk.kwikpass.config.KwikPassCache
import com.gk.kwikpass.config.KwikPassKeys
//import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.*

data class InitializeSdkArgs(
    val mid: String,
    val environment: String,
    val isSnowplowTrackingEnabled: Boolean = true
)

class KwikPassApi(private val context: Context) {
    private val cache = KwikPassCache.getInstance(context)
//    private var apiService: KwikPassApiService? = null

    suspend fun initializeSdk(args: InitializeSdkArgs): Result<String> = withContext(Dispatchers.IO) {
        try {
            val (mid, environment, isSnowplowTrackingEnabled) = args
            
            // Initialize HTTP client and create API service
//            apiService = KwikPassHttpClient.createService(environment)

            // Set initial cache values in parallel
            coroutineScope {
                listOf(
                    async { cache.setValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED, isSnowplowTrackingEnabled.toString()) },
                    async { cache.setValue(KwikPassKeys.GK_ENVIRONMENT, environment) },
                    async { cache.setValue(KwikPassKeys.GK_MERCHANT_ID, mid) }
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

            // Get browser token
//            getBrowserToken()

            // Initialize merchant config
//            val merchantConfig = initializeSdk(mid, environment).getOrThrow()

            // Store merchant type and URL
//            merchantConfig.platform?.let {
//                cache.setValue(KwikPassKeys.GK_MERCHANT_TYPE, it.lowercase())
//            }
//            merchantConfig.host?.let {
//                cache.setValue(KwikPassKeys.GK_MERCHANT_URL, getHostName(it))
//            }

            // Initialize Snowplow client (you'll need to implement this)
            // initializeSnowplowClient(args)

            // Get device info
            val deviceInfo = getDeviceInfo()

            // Get advertising ID
//            try {
//                val adInfo = withContext(Dispatchers.IO) {
//                    AdvertisingIdClient.getAdvertisingIdInfo(context)
//                }
//                if (adInfo?.id != null) {
//                    deviceInfo[KwikPassKeys.GK_GOOGLE_AD_ID] = adInfo.id
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }

            // Store device info
            cache.setValue(KwikPassKeys.GK_DEVICE_INFO, deviceInfo.toString())

            Result.success("Initialization Successful")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDeviceInfo(): Map<String, String> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        
        return mapOf(
            KwikPassKeys.GK_DEVICE_MODEL to Build.MODEL,
            KwikPassKeys.GK_APP_DOMAIN to context.packageName,
            KwikPassKeys.GK_OPERATING_SYSTEM to "Android ${Build.VERSION.RELEASE}",
            KwikPassKeys.GK_DEVICE_ID to Build.ID,
            KwikPassKeys.GK_DEVICE_UNIQUE_ID to Build.ID,
            KwikPassKeys.GK_GOOGLE_ANALYTICS_ID to "", // You might want to implement Firebase Analytics ID here
            KwikPassKeys.GK_GOOGLE_AD_ID to "",
            KwikPassKeys.GK_APP_VERSION to context.packageManager.getPackageInfo(context.packageName, 0).versionName,
            KwikPassKeys.GK_APP_VERSION_CODE to context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toString(),
            KwikPassKeys.GK_SCREEN_RESOLUTION to "${metrics.widthPixels}x${metrics.heightPixels}",
            KwikPassKeys.GK_CARRIER_INFO to (telephonyManager.simOperatorName ?: ""),
            KwikPassKeys.GK_BATTERY_STATUS to "${batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)}",
            KwikPassKeys.GK_LANGUAGE to Locale.getDefault().language,
            KwikPassKeys.GK_TIME_ZONE to TimeZone.getDefault().id
        )
    }

    private fun getHostName(url: String): String {
        return url.replace(Regex("^(?:https?://)?(?:www\\.)?([^/]+).*$"), "$1")
    }

    // ... rest of your existing code ...
}