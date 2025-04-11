package com.gk.kwikpass.api.shopify

import com.gk.kwikpass.api.*
import com.gk.kwikpass.config.KwikPassCache
import com.gk.kwikpass.config.KwikPassKeys
import com.gk.kwikpass.initializer.ApplicationCtx
import com.gk.kwikpass.initializer.kwikpassInitializer
import com.gk.kwikpass.snowplow.Snowplow
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KwikpassShopify {
    private val cache = KwikPassCache.getInstance(ApplicationCtx.get())
    private var apiService: KwikPassApiService? = null
    private val gson = Gson()

    suspend fun getShopifyMultipassToken(
        phone: String,
        email: String,
        id: String? = null,
        state: String? = null
    ): Result<ShopifyData> = withContext(Dispatchers.IO) {
        try {

            if (apiService == null) {
                apiService = KwikPassHttpClient.getApiService(kwikpassInitializer.getEnvironment().toString(), kwikpassInitializer.getMerchantId().toString())
            }

            // Get tokens from cache
            val accessToken = cache.getValue(KwikPassKeys.GK_ACCESS_TOKEN)
            val checkoutAccessToken = cache.getValue(KwikPassKeys.CHECKOUT_ACCESS_TOKEN)
            val notifications = cache.getValue(KwikPassKeys.GK_NOTIFICATION_ENABLED)

            // Set headers
            accessToken?.let {
                KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.GK_ACCESS_TOKEN to it))
            }
            checkoutAccessToken?.let {
                KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.CHECKOUT_ACCESS_TOKEN to it))
            }

            // Make API call
            val response = apiService?.getShopifyMultipassToken(
                ShopifyMultipassRequest(
                    id = id ?: "",
                    email = email,
                    isMarketingEventSubscribed = notifications == "true",
                    state = state ?: ""
                )
            )

            if (response?.isSuccessful == true) {
                val responseBody = response.body()
                if (responseBody != null) {
                    // Store user data
                    val user = responseBody.data
                    val userData = responseBody.data?.copy(phone = phone)
                    userData?.let {
                        val userDataJson = gson.toJson(it)
                        println("STORING USER DATA: $userDataJson")
                        cache.setValue(KwikPassKeys.GK_VERIFIED_USER, userDataJson.toString())
                    }

                    return@withContext Result.success(response.body()!!)
                } else {
                    println("Response body is null")
                    return@withContext Result.failure(Exception("Empty response body"))
                }
            }
            Result.failure(Exception("Failed to get multipass token"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCheckoutMultiPassToken(
        phone: String,
        email: String,
        gkAccessToken: String,
        id: String? = null,
        notifications: Boolean = false
    ): Result<ShopifyData> = withContext(Dispatchers.IO) {
        try {
            // Set access token header
            KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.GK_ACCESS_TOKEN to gkAccessToken))

            // Make API call
            val response = apiService?.getShopifyMultipassToken(
                ShopifyMultipassRequest(
                    id = id ?: "",
                    email = email,
                    isMarketingEventSubscribed = notifications,
                    skipEmailOtp = true
                )
            )

            if (response?.isSuccessful == true) {
                // Store user data
                val userData = response.body()?.data?.copy(phone = phone)
                userData?.let {
                    cache.setValue(KwikPassKeys.GK_VERIFIED_USER, gson.toJson(it))
                }

                // Send Snowplow event
                Snowplow.sendCustomEventToSnowPlow(
                    mapOf(
                        "category" to "sso_login",
                        "action" to "logged_in",
                        "label" to "checkout_sso_logged_in",
                        "property" to "phone_number",
                        "value" to (phone.toIntOrNull() ?: 0)
                    )
                )

                return@withContext Result.success(response.body()!!)
            }
            Result.failure(Exception("Failed to get checkout multipass token"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateUserToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiService == null) {
                apiService = KwikPassHttpClient.getApiService(kwikpassInitializer.getEnvironment().toString(), kwikpassInitializer.getMerchantId().toString())
            }

            // Get tokens from cache
            val accessToken = cache.getValue(KwikPassKeys.GK_ACCESS_TOKEN)
            val checkoutAccessToken = cache.getValue(KwikPassKeys.CHECKOUT_ACCESS_TOKEN)

            // Set headers
            accessToken?.let {
                KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.GK_ACCESS_TOKEN to it))
            }
            checkoutAccessToken?.let {
                KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.CHECKOUT_ACCESS_TOKEN to it))
            }

            // Make API call
            val response = apiService?.validateUserToken()

            if (response?.isSuccessful == true) {
                val responseData = response.body()
                if (responseData?.data != null) {
                    // Convert response data to JSON string
                    val responseDataJson = gson.toJson(responseData.data)
                    
                    // Store in cache
                    cache.setValue(KwikPassKeys.GK_VERIFIED_USER, responseDataJson)
                    
                    return@withContext Result.success(responseDataJson)
                }
                return@withContext Result.failure(Exception("Empty response data"))
            }
            
            Result.failure(Exception("Failed to validate user token"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginKpUser(): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            if (apiService == null) {
                apiService = KwikPassHttpClient.getApiService(kwikpassInitializer.getEnvironment().toString(), kwikpassInitializer.getMerchantId().toString())
            }

            // Make API call
            val response = apiService?.loginKpUser()

            if (response?.isSuccessful == true) {
                val responseData = response.body()
                if (responseData != null) {
                    return@withContext Result.success(responseData)
                }
                return@withContext Result.failure(Exception("Empty response data"))
            }
            
            Result.failure(Exception("Failed to login user"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
