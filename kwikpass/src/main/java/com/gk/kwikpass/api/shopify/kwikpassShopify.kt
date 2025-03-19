package com.gk.kwikpass.api.shopify

import com.gk.kwikpass.api.*
import com.gk.kwikpass.config.KwikPassCache
import com.gk.kwikpass.config.KwikPassKeys
import com.gk.kwikpass.initializer.ApplicationCtx
import com.gk.kwikpass.initializer.kwikpassInitializer
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

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

    suspend fun shopifySendEmailVerificationCode(email: String): Result<ShopifyResponse> = withContext(Dispatchers.IO) {
        try {
            val phoneNumber = cache.getValue(KwikPassKeys.GK_USER_PHONE)

            // Send Snowplow event
            phoneNumber?.let {
//                sendCustomEventToSnowPlow(
//                    category = "login_screen",
//                    action = "click",
//                    label = "email_filled",
//                    property = "email",
//                    value = it.toLong()
//                )
            }

            // Make API call
            val response = apiService?.sendShopifyEmailVerificationCode(
                ShopifyEmailVerificationRequest(email)
            )

            if (response?.isSuccessful == true) {
                return@withContext Result.success(response.body()!!)
            }
            Result.failure(Exception("Failed to send email verification code"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shopifyVerifyEmail(
        email: String,
        otp: String
    ): Result<ShopifyResponse> = withContext(Dispatchers.IO) {
        try {
            val notifications = cache.getValue(KwikPassKeys.GK_NOTIFICATION_ENABLED)

            // Make API call
            val response = apiService?.verifyShopifyEmail(
                ShopifyEmailVerifyRequest(
                    email = email,
                    otp = otp,
                    isMarketingEventSubscribed = notifications == "true"
                )
            )

            if (response?.isSuccessful == true) {
                // Get existing user data
                val userDataJson = cache.getValue(KwikPassKeys.GK_VERIFIED_USER)
                val userData = userDataJson?.let { gson.fromJson(it, ShopifyUserData::class.java) }

                // Update user data
                val updatedData = response.body()?.data?.data?.copy(
                    phone = userData?.phone
                )

                // Store updated user data
                updatedData?.let {
                    cache.setValue(KwikPassKeys.GK_VERIFIED_USER, gson.toJson(it))
                }

                // Send Snowplow event
                userData?.phone?.let {
//                    sendCustomEventToSnowPlow(
//                        category = "login_screen",
//                        action = "logged_in",
//                        label = "otp_verified",
//                        property = "kwik_pass",
//                        value = it.toLong()
//                    )
                }

                return@withContext Result.success(response.body()!!)
            }
            Result.failure(Exception("Failed to verify email"))
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
//                sendCustomEventToSnowPlow(
//                    category = "login_screen",
//                    action = "logged_in",
//                    label = "shopify_logged_in",
//                    property = "kwik_pass",
//                    value = phone.toLong()
//                )

                return@withContext Result.success(response.body()!!)
            }
            Result.failure(Exception("Failed to get checkout multipass token"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
