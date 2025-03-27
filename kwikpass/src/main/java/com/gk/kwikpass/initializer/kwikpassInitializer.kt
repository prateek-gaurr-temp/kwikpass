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
import com.gk.kwikpass.utils.CoroutineUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val TAG = "KwikPassInitializer"

object kwikpassInitializer {
    private var merchantId: String? = null
    private var environment: String? = null
    private var isSnowplowTrackingEnabled: Boolean = false
    private var applicationContext: Context? = null
    private var apiService: KwikPassApiService? = null
    private var idfaAidModule: IdfaAidModule? = null

    fun initialize(
        context: Context,
        merchantId: String,
        environment: String,
        isSnowplowTrackingEnabled: Boolean
    ) {
        val initializer = this
        CoroutineUtils.coroutine.launch {
            try {
                initializer.applicationContext = context
                ApplicationCtx.instance = context
                val cache = KwikPassCache.getInstance(context)

                initializer.merchantId = merchantId
                initializer.environment = environment
                initializer.isSnowplowTrackingEnabled = isSnowplowTrackingEnabled

                // Set initial cache values in parallel
                coroutineScope {
                    listOf(
                        async {
                            cache.setValue(
                                KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED,
                                isSnowplowTrackingEnabled.toString()
                            )
                        },
                        async { cache.setValue(KwikPassKeys.GK_ENVIRONMENT, environment) },
                        async { cache.setValue(KwikPassKeys.GK_MERCHANT_ID, merchantId) }
                    ).map { it.await() }
                }

                // Get cached values in parallel
                val (requestId, accessToken, checkoutAccessToken) = coroutineScope {
                    val requestIdDeferred = async { cache.getValue(KwikPassKeys.GK_REQUEST_ID) }
                    val accessTokenDeferred = async { cache.getValue(KwikPassKeys.GK_ACCESS_TOKEN) }
                    val checkoutTokenDeferred =
                        async { cache.getValue(KwikPassKeys.CHECKOUT_ACCESS_TOKEN) }
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

                if (initializer.apiService == null) {
                    // Initialize HTTP client and get browser token
                    initializer.apiService = KwikPassHttpClient.getApiService(environment, merchantId)
                }

                val browserAuthResponse = initializer.apiService?.getBrowserAuth()
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
                                async {
                                    cache.setValue(
                                        KwikPassKeys.GK_REQUEST_ID,
                                        data.requestId
                                    )
                                },
                                async {
                                    cache.setValue(
                                        KwikPassKeys.KP_REQUEST_ID,
                                        data.requestId
                                    )
                                },
                                async { cache.setValue(KwikPassKeys.GK_AUTH_TOKEN, data.token) }
                            ).map { it.await() }
                        }
                    }
                }

                // Get merchant config
                val merchantConfigResponse = initializer.apiService?.getMerchantConfig(merchantId)
                if (merchantConfigResponse?.isSuccessful == true) {
                    val data = merchantConfigResponse.body()?.data
                    data?.platform?.toString()?.let {
                        cache.setValue(KwikPassKeys.GK_MERCHANT_TYPE, it.lowercase())
                    }
                    data?.host?.let {
                        cache.setValue(KwikPassKeys.GK_MERCHANT_URL, AppUtils.getHostName(it))
                    }
                    cache.setValue(KwikPassKeys.GK_MERCHANT_CONFIG, data.toString())
                }

                if (initializer.idfaAidModule == null) {
                    initializer.idfaAidModule = IdfaAidModule.getInstance(context)
                }

                // Get device info and update with ad ID
                val deviceInfoDetails = AppUtils.getDeviceInfo().toMutableMap()

                Log.d(TAG, "Device Info Details: $deviceInfoDetails")
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

                Log.i(TAG, "KwikPass SDK Initialized Successfully")
                Log.i(TAG, "Merchant ID: $merchantId")
                Log.i(TAG, "Environment: $environment")
                Log.i(TAG, "Snowplow Tracking: $isSnowplowTrackingEnabled")

            } catch (e: Exception) {
                Log.e(TAG, "Error initializing KwikPass SDK", e)
                throw e
            }
        }
    }

    fun getMerchantId(): String? = merchantId
    fun getEnvironment(): String? = environment
    fun isTrackingEnabled(): Boolean = isSnowplowTrackingEnabled
    fun getAppContext(): Context? = applicationContext
}
