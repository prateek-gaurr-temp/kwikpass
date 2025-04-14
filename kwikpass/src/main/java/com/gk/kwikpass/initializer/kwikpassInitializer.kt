package com.gk.kwikpass.initializer

import android.content.Context
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import com.gk.kwikpass.IdfaAid.IdfaAidModule
import com.gk.kwikpass.api.KwikPassApi
import com.gk.kwikpass.api.KwikPassApiService
import com.gk.kwikpass.api.KwikPassHttpClient
import com.gk.kwikpass.config.KwikPassCache
import com.gk.kwikpass.config.KwikPassEnvironment
import com.gk.kwikpass.config.KwikPassKeys
import com.gk.kwikpass.snowplow.SnowplowClient
import com.gk.kwikpass.utils.AppUtils
import com.gk.kwikpass.utils.CoroutineUtils
import com.google.gson.Gson
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
    private val gson = Gson()

    fun initialize(
        context: Context,
        merchantId: String,
        environment: String,
        isSnowplowTrackingEnabled: Boolean
    ) {
        val initializer = this
        CoroutineUtils.coroutine.launch {
            try {
                initializeCoreComponents(
                    context,
                    merchantId,
                    environment,
                    isSnowplowTrackingEnabled
                )
                handleTokensAndAuthentication()
                setupMerchantConfiguration()
                initializeDeviceTracking()


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

    private suspend fun initializeCoreComponents(
        context: Context,
        merchantId: String,
        environment: String,
        isSnowplowTrackingEnabled: Boolean
    ) {
        applicationContext = context
        ApplicationCtx.init(context)
        val cache = KwikPassCache.getInstance(context)
        SnowplowClient.initializeSnowplowClient(context, environment, merchantId)


        this.merchantId = merchantId
        this.environment = environment
        this.isSnowplowTrackingEnabled = isSnowplowTrackingEnabled

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
    }

    private suspend fun handleTokensAndAuthentication() {
        val cache = KwikPassCache.getInstance(applicationContext!!)

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

        if (apiService == null) {
            apiService = KwikPassHttpClient.getApiService(environment!!, merchantId!!)
        }

        handleBrowserAuthentication(cache, requestId)
    }

    private suspend fun handleBrowserAuthentication(cache: KwikPassCache, requestId: String?) {
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

        // Set request ID if it exists
        requestId?.let {
            KwikPassHttpClient.setHeaders(
                mapOf(
                    KwikPassKeys.KP_REQUEST_ID to it,
                    KwikPassKeys.GK_REQUEST_ID to it
                )
            )
        }
    }

    private suspend fun setupMerchantConfiguration() {
        val merchantConfigResponse = apiService?.getMerchantConfig(merchantId!!)
        if (merchantConfigResponse?.isSuccessful == true) {
            val data = merchantConfigResponse.body()?.data
            val cache = KwikPassCache.getInstance(applicationContext!!)

            data?.platform?.toString()?.let {
                cache.setValue(KwikPassKeys.GK_MERCHANT_TYPE, it.lowercase())
            }
            data?.host?.let {
                cache.setValue(KwikPassKeys.GK_MERCHANT_URL, AppUtils.getHostName(it))
            }
            cache.setValue(KwikPassKeys.GK_MERCHANT_CONFIG, data.toString())
        }
    }

    private fun initializeDeviceTracking() {
        if (idfaAidModule == null) {
            idfaAidModule = IdfaAidModule.getInstance(applicationContext!!)
        }

        CoroutineUtils.coroutine.launch {
            // Get device info and update with ad ID
            val deviceInfoDetails = AppUtils.getDeviceInfo().toMutableMap()
            Log.d(TAG, "Device Info Details: $deviceInfoDetails")

            val cache = KwikPassCache.getInstance(applicationContext!!)
            val jsonString = gson.toJson(deviceInfoDetails)
            cache.setValue(KwikPassKeys.GK_DEVICE_INFO, jsonString)
        }
    }

    fun getMerchantId(): String? = merchantId
    fun getEnvironment(): String? = environment
    fun isTrackingEnabled(): Boolean = isSnowplowTrackingEnabled
    fun getAppContext(): Context? = applicationContext
}
