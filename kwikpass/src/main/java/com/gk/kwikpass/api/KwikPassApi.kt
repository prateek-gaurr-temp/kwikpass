package com.gk.kwikpass.api

import android.app.Application
import android.content.Context
import com.gk.kwikpass.initializer.ApplicationCtx
import com.gk.kwikpass.config.KwikPassCache
import com.gk.kwikpass.config.KwikPassKeys
import com.google.gson.Gson
//import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class InitializeSdkArgs(
    val mid: String,
    val environment: String,
    val isSnowplowTrackingEnabled: Boolean = true,
    val application: Application
)

class KwikPassApi(private val context: Context) {
    private val cache = KwikPassCache.getInstance(context)
    private var apiService: KwikPassApiService? = null
    private val gson = Gson()

    suspend fun initializeSdk(args: InitializeSdkArgs): Result<String> = withContext(Dispatchers.IO) {
        try {
            val (mid, environment, isSnowplowTrackingEnabled, application) = args

            ApplicationCtx.instance = application

            // Initialize HTTP client and create API service
            apiService = KwikPassHttpClient.createService(environment)

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

            // Set tokens if they exist
            accessToken?.let {
                KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.GK_ACCESS_TOKEN to it))
            }
            checkoutAccessToken?.let {
                KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.CHECKOUT_ACCESS_TOKEN to it))
            }

            // Get browser token
            getBrowserToken()

            // Initialize merchant config
            val merchantConfig = initializeMerchant(mid, environment)

            // Store merchant type and URL
            merchantConfig.platform?.let {
                cache.setValue(KwikPassKeys.GK_MERCHANT_TYPE, it.lowercase())
            }
            merchantConfig.host?.let {
                cache.setValue(KwikPassKeys.GK_MERCHANT_URL, getHostName(it))
            }

            // Store merchant config
            cache.setValue(KwikPassKeys.GK_MERCHANT_CONFIG, gson.toJson(merchantConfig))

            // Initialize Snowplow client (you'll need to implement this)
            // initializeSnowplowClient(args)

            // Get device info
//            val deviceInfo = AppUtils.getDeviceInfo(context)
//            cache.setValue(KwikPassKeys.GK_DEVICE_INFO, gson.toJson(deviceInfo))

            // Set request ID if it exists
            requestId?.let {
                KwikPassHttpClient.setHeaders(
                    mapOf(
                        KwikPassKeys.KP_REQUEST_ID to it,
                        KwikPassKeys.GK_REQUEST_ID to it
                    )
                )
            }

            Result.success("Initialization Successful")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getBrowserToken() {
        try {
            val response = apiService?.getBrowserAuth()
            if (response?.isSuccessful == true) {
                val data = response.body()?.data
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun initializeMerchant(mid: String, environment: String): MerchantConfig {
        try {
            val response = apiService?.getMerchantConfig(mid)
            if (response?.isSuccessful == true) {
                val merchantConfig = response.body()?.data
                if (merchantConfig != null) {
                    return merchantConfig
                }
            }
            throw Exception("Failed to fetch merchant configuration")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun sendVerificationCode(phoneNumber: String, notifications: Boolean): Result<OtpSentResponse> {
        try {
            // Get browser token first
            getBrowserToken()

            // Set notification preference
            cache.setValue(KwikPassKeys.GK_NOTIFICATION_ENABLED, notifications.toString())
            cache.setValue(KwikPassKeys.GK_USER_PHONE, phoneNumber)

            val response = apiService?.sendVerificationCode(SendVerificationCodeRequest(phoneNumber))
                println("")
            if (response?.isSuccessful == true) {
                return Result.success(response.body()!!)
            }
            return Result.failure(Exception("Failed to send verification code"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun loginKpUser(): Result<LoginResponse> {
        try {
            val response = apiService?.loginKpUser()
            if (response?.isSuccessful == true) {
                return Result.success(response.body()!!)
            }
            return Result.failure(Exception("Failed to login user"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun createUser(email: String, name: String, dob: String? = null, gender: String? = null): Result<CreateUserResponse> {
        try {
            val response = apiService?.createUser(CreateUserRequest(email, name, dob, gender))
            if (response?.isSuccessful == true) {
                val data = response.body()?.data
                val user = data?.merchantResponse?.accountCreate?.user
                val errors = data?.merchantResponse?.accountCreate?.accountErrors

                if (user != null) {
                    val userData = gson.toJson(user)
                    cache.setValue(KwikPassKeys.GK_VERIFIED_USER, userData)
                    return Result.success(response.body()!!)
                } else if (!errors.isNullOrEmpty()) {
                    return Result.failure(Exception(errors[0]))
                }
            }
            return Result.failure(Exception("Failed to create user"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun validateUserToken(): Result<ValidateUserTokenResponse> {
        try {
            val accessToken = cache.getValue(KwikPassKeys.GK_ACCESS_TOKEN)
            val checkoutAccessToken = cache.getValue(KwikPassKeys.CHECKOUT_ACCESS_TOKEN)

            accessToken?.let {
                KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.GK_ACCESS_TOKEN to it))
            }
            checkoutAccessToken?.let {
                KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.CHECKOUT_ACCESS_TOKEN to it))
            }

            val response = apiService?.validateUserToken()
            if (response?.isSuccessful == true) {
                val responseData = gson.toJson(response.body()?.data)
                cache.setValue(KwikPassKeys.GK_VERIFIED_USER, responseData)
                return Result.success(response.body()!!)
            }
            return Result.failure(Exception("Failed to validate user token"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun verifyCode(phoneNumber: String, code: String): Result<VerifyCodeResponse> {
        try {
            val response = apiService?.verifyCode(VerifyCodeRequest(phoneNumber, code.toInt()))
            if (response?.isSuccessful == true) {
                val data = response.body()?.data
                if (data != null) {
                    // Handle Shopify specific logic
                    val merchantType = cache.getValue(KwikPassKeys.GK_MERCHANT_TYPE)
                    if (merchantType == "shopify") {
                        // Handle Shopify specific token and core token
                        data.token?.let {
                            KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.GK_ACCESS_TOKEN to it))
                            cache.setValue(KwikPassKeys.GK_ACCESS_TOKEN, it)
                        }
                        data.coreToken?.let {
                            KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.CHECKOUT_ACCESS_TOKEN to it))
                            cache.setValue(KwikPassKeys.CHECKOUT_ACCESS_TOKEN, it)
                        }

                        // Get customer intelligence data
                        val customerIntelligenceData = getCustomerIntelligence()
                        if (customerIntelligenceData != null) {
                            // Add customer intelligence data to response
                            // This part needs to be implemented based on your specific needs
                        }

                        // Handle Shopify specific state
                        if (data.state == "DISABLED") {
                            // Handle disabled state
                            // This part needs to be implemented based on your specific needs
                        }

                        return Result.success(response.body()!!)
                    }

                    // Handle non-Shopify case
                    data.token?.let {
                        cache.setValue(KwikPassKeys.GK_ACCESS_TOKEN, it)
                    }
                    data.coreToken?.let {
                        cache.setValue(KwikPassKeys.CHECKOUT_ACCESS_TOKEN, it)
                    }

                    // Validate token and login user
                    validateUserToken()
                    loginKpUser()

                    return Result.success(response.body()!!)
                }
            }
            return Result.failure(Exception("Failed to verify code"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private suspend fun getCustomerIntelligence(): List<String>? {
        try {
            val merchantConfigJson = cache.getValue(KwikPassKeys.GK_MERCHANT_CONFIG)
            if (merchantConfigJson != null) {
                val merchantConfig = gson.fromJson(merchantConfigJson, MerchantConfig::class.java)
                if (merchantConfig.customerIntelligenceEnabled == true) {
                    val metrics = merchantConfig.customerIntelligenceMetrics
                    if (metrics != null) {
                        val trueKeys = getTrueKeys(metrics)
                        val response = apiService?.getCustomerIntelligence(mapOf("cstmr-mtrcs" to trueKeys.joinToString(",")))
                        if (response?.isSuccessful == true) {
                            return response.body()?.data
                        }
                    }
                }
            }
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getTrueKeys(obj: Map<String, Any>): List<String> {
        val keys = mutableListOf<String>()
        obj.forEach { (key, value) ->
            when (value) {
                is Boolean -> if (value) keys.add(key)
                is Map<*, *> -> keys.addAll(getTrueKeys(value as Map<String, Any>))
            }
        }
        return keys.distinct()
    }

    suspend fun checkout(): Result<Boolean> {
        try {
            val userJson = cache.getValue(KwikPassKeys.GK_VERIFIED_USER) ?: "{}"
            val user = gson.fromJson(userJson, Map::class.java)
            val phone = user["phone"]?.toString() ?: "0"

            // Clear headers
            KwikPassHttpClient.clearHeaders()

            // Clear cache
            cache.clearCache()

            // Reinitialize SDK
            val env = cache.getValue(KwikPassKeys.GK_ENVIRONMENT) ?: "sandbox"
            val mid = cache.getValue(KwikPassKeys.GK_MERCHANT_ID) ?: ""
            val isSnowplowTrackingEnabled = cache.getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"

//            initializeSdk(InitializeSdkArgs(mid, env, isSnowplowTrackingEnabled, ""))

            return Result.success(true)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun kpLogout(): Result<Boolean> {
        try {
            val userJson = cache.getValue(KwikPassKeys.GK_VERIFIED_USER) ?: "{}"
            val user = gson.fromJson(userJson, Map::class.java)
            val phone = user["phone"]?.toString() ?: "0"

            // Clear headers
            KwikPassHttpClient.clearHeaders()

            // Clear cache
            cache.clearCache()

            // Reinitialize SDK
            val env = cache.getValue(KwikPassKeys.GK_ENVIRONMENT) ?: "sandbox"
            val mid = cache.getValue(KwikPassKeys.GK_MERCHANT_ID) ?: ""
            val isSnowplowTrackingEnabled = cache.getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"

//            initializeSdk(InitializeSdkArgs(mid, env, isSnowplowTrackingEnabled))

            return Result.success(true)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun getHostName(url: String): String {
        return url.replace(Regex("^(?:https?://)?(?:www\\.)?([^/]+).*$"), "$1")
    }
}