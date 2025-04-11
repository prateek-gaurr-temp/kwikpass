package com.gk.kwikpass.api

import android.app.Application
import android.content.Context
import com.gk.kwikpass.api.shopify.KwikpassShopify
import com.gk.kwikpass.config.KwikPassCache
import com.gk.kwikpass.config.KwikPassKeys
import com.gk.kwikpass.config.KwikPassEnvironment
import com.gk.kwikpass.config.KwikPassMerchantType
import com.gk.kwikpass.config.KwikPassUserState
import com.gk.kwikpass.initializer.kwikpassInitializer
import com.gk.kwikpass.snowplow.Snowplow
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.net.URLEncoder
import okhttp3.MediaType.Companion.toMediaTypeOrNull

data class ApiErrorResponse(
    val message: String,
    val requestId: String = "N/A",
    val result: Boolean = false,
    val errorCode: Int? = null,
    val response: Response<*>? = null
)

class KwikPassApi(private val context: Context) {
    @Volatile
    private var apiService: KwikPassApiService? = null
    private val cache = KwikPassCache.getInstance(context)
    private val gson = Gson()
    
    private val apiServiceLock = Any()

    private fun getOrCreateApiService(environment: KwikPassEnvironment, mid: String): KwikPassApiService {
        return apiService ?: synchronized(apiServiceLock) {
            apiService ?: KwikPassHttpClient.getApiService(environment.toString().lowercase(), mid).also { 
                apiService = it 
            }
        }
    }

    private fun handleApiError(error: Throwable): ApiErrorResponse {
        return when (error) {
            is retrofit2.HttpException -> {
                val response = error.response()
                val errorBody = response?.errorBody()?.string()
                val errorData = try {
                    gson.fromJson(errorBody, Map::class.java)
                } catch (e: Exception) {
                    null
                }
                
                val message = when {
                    errorData?.get("error") != null -> errorData["error"].toString()
                    errorData?.get("data")?.toString()?.contains("error") == true -> {
                        val dataMap = gson.fromJson(errorData["data"].toString(), Map::class.java)
                        dataMap["error"].toString()
                    }
                    else -> "Unexpected error with status: ${response?.code()}"
                }

                ApiErrorResponse(
                    message = message,
                    requestId = response?.headers()?.get("request-id") ?: "N/A",
                    result = false,
                    errorCode = response?.code(),
                    response = response
                )
            }
            else -> ApiErrorResponse(
                message = error.message ?: "An unknown error occurred",
                result = false
            )
        }
    }

    private suspend fun getBrowserToken() {
        try {
            val environment = KwikPassEnvironment.fromString(kwikpassInitializer.getEnvironment() as String)
            val mid = kwikpassInitializer.getMerchantId() ?: ""

            println("ENVIRONMENT $environment MERCHANT $mid")
            
            val service = getOrCreateApiService(environment, mid)
            val response = service.getBrowserAuth()
            
            println("RESPONSE FROM GET BROWSER TOKEN ${response?.isSuccessful}")

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

//    private suspend fun initializeMerchant(mid: String, environment: String): MerchantConfig {
//        try {
//            val service = getOrCreateApiService(environment, mid)
//            val response = service.getMerchantConfig(mid)
//            if (response?.isSuccessful == true) {
//                val merchantConfig = response.body()?.data
//                if (merchantConfig != null) {
//                    return merchantConfig
//                }
//            }
//            throw Exception("Failed to fetch merchant configuration")
//        } catch (e: Exception) {
//            e.printStackTrace()
//            throw e
//        }
//    }

    suspend fun sendVerificationCode(phoneNumber: String, notifications: Boolean): Result<Any> {
        return try {
            getBrowserToken()
            cache.setValue(KwikPassKeys.GK_NOTIFICATION_ENABLED, notifications.toString())
            cache.setValue(KwikPassKeys.GK_USER_PHONE, phoneNumber)

            // Send Snowplow events for phone number tracking
            Snowplow.sendCustomEventToSnowPlow(
                mapOf(
                    "category" to "login_modal",
                    "action" to "click",
                    "label" to "phone_number_filled",
                    "property" to "phone_number",
                    "value" to (phoneNumber.toIntOrNull() ?: 0)
                )
            )

            Snowplow.sendCustomEventToSnowPlow(
                mapOf(
                    "category" to "login_modal",
                    "action" to "click",
                    "label" to "phone_number_entered",
                    "property" to "phone_number",
                    "value" to (phoneNumber.toIntOrNull() ?: 0)
                )
            )

            val response = apiService?.sendVerificationCode(SendVerificationCodeRequest(phoneNumber))

            if(response?.isSuccessful == true) {
                Snowplow.sendCustomEventToSnowPlow(
                    mapOf(
                        "category" to "login_modal",
                        "action" to "automated",
                        "label" to "otp_sent_successfully",
                        "property" to "phone_number",
                        "value" to (phoneNumber.toIntOrNull() ?: 0)
                    )
                )
                Result.success(response.body()!!)
            } else {
                val error = handleApiError(retrofit2.HttpException(response!!))
                Result.failure(Exception(error.message))
            }
        } catch (e: Exception) {
            val error = handleApiError(e)
            Result.failure(Exception(error.message))
        }
    }

    suspend fun loginKpUser(): Result<LoginResponse> {
        try {
            val response = apiService?.loginKpUser()
            println("RESPONSE FROM LOGIN KWIKPASS USER ${response?.body()}")
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
            println("RESPONSE FROM VALIDATE USER TOKEN $response")
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

    suspend fun verifyCode(phoneNumber: String, code: String): Result<Any> {
        return try {
            getBrowserToken()
            
            // Send Snowplow event for OTP submission
            Snowplow.sendCustomEventToSnowPlow(
                mapOf(
                    "category" to "login_modal",
                    "action" to "automated",
                    "label" to "submit_otp",
                    "property" to "phone_number",
                    "value" to (phoneNumber.toIntOrNull() ?: 0)
                )
            )

            val response = apiService?.verifyCode(VerifyCodeRequest(phoneNumber, code.toInt()))
            if (response?.isSuccessful == true) {
                val data = response.body()?.data
                println("DATA FROM RESPONSE $data")

                if (data != null) {
                    val merchantType = KwikPassMerchantType.fromString(cache.getValue(KwikPassKeys.GK_MERCHANT_TYPE) ?: "")
                    println("MERCHANT TYPE AT TIME OF VERIFY CODE $merchantType")
                    
                    // Handle Shopify merchant type
                    if (merchantType is KwikPassMerchantType.SHOPIFY) {
                        // Set access token
                        data.token?.let { token ->
                            KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.GK_ACCESS_TOKEN to token))
                            cache.setValue(KwikPassKeys.GK_ACCESS_TOKEN, token)
                            data.token = null
                        }

                        // Set core token
                        data.coreToken?.let { coreToken ->
                            KwikPassHttpClient.setHeaders(mapOf(KwikPassKeys.CHECKOUT_ACCESS_TOKEN to coreToken))
                            cache.setValue(KwikPassKeys.CHECKOUT_ACCESS_TOKEN, coreToken)
                            data.coreToken = null
                        }

                        // Set KP token
                        data.kpToken?.let { kpToken ->
                            cache.setValue(KwikPassKeys.GK_KP_TOKEN, kpToken)
                            data.kpToken = null
                        }

                        val shopify = KwikpassShopify()

                        // Handle disabled state
                        if (KwikPassUserState.fromString(data.state ?: "") is KwikPassUserState.DISABLED) {
                            val multipassResult = shopify.getShopifyMultipassToken(
                                phoneNumber,
                                data?.email.toString(),
                                data.shopifyCustomerId,
                                state = data.state
                            )

                            multipassResult.fold(
                                onSuccess = { multipassResponse ->
                                    // Handle account activation
                                    multipassResponse.data?.let { shopifyData ->
                                        val activationUrl = shopifyData.accountActivationUrl
                                        val customerId = shopifyData.shopifyCustomerId
                                        if (activationUrl != null && customerId != null) {
                                            val accountActivationUrl = activationUrl.split("/")
                                            val token = accountActivationUrl.last()
                                            val url = extractDomain(activationUrl)

                                            activateUserAccount(
                                                customerId = customerId,
                                                url = url,
                                                password = shopifyData.password,
                                                token = token
                                            )
                                        }
                                    }

                                    // Send Snowplow event for successful login
                                    Snowplow.sendCustomEventToSnowPlow(
                                        mapOf(
                                            "category" to "login_modal",
                                            "action" to "logged_in",
                                            "label" to "phone_Number_logged_in",
                                            "property" to "phone_number",
                                            "value" to (phoneNumber.toIntOrNull() ?: 0)
                                        )
                                    )

                                    Result.success(multipassResponse)
                                },
                                onFailure = { e ->
                                    Result.failure(e)
                                }
                            )
                        }

                        println("EMAIL DATA PRESENT? ${data.email}")

                        // Handle email exists case
                        if (data.email != null) {
                            val multipassResult = shopify.getShopifyMultipassToken(
                                phoneNumber,
                                data.email,
                                data.shopifyCustomerId
                            )

                            println("MULTIPASS RESPONSE $multipassResult")

                            // Send Snowplow event for successful login
                            Snowplow.sendCustomEventToSnowPlow(
                                mapOf(
                                    "category" to "login_modal",
                                    "action" to "logged_in",
                                    "label" to "phone_Number_logged_in",
                                    "property" to "phone_number",
                                    "value" to (phoneNumber.toIntOrNull() ?: 0)
                                )
                            )

                            return Result.success(multipassResult)
                        }

                        // Store user data
                        val userData = data.copy(phone = phoneNumber)
                        cache.setValue(KwikPassKeys.GK_VERIFIED_USER, gson.toJson(userData))
                        println("userData before return $userData")

                        // Send Snowplow event for successful login
                        Snowplow.sendCustomEventToSnowPlow(
                            mapOf(
                                "category" to "login_modal",
                                "action" to "logged_in",
                                "label" to "phone_Number_logged_in",
                                "property" to "phone_number",
                                "value" to (phoneNumber.toIntOrNull() ?: 0)
                            )
                        )

                        return Result.success(response.body()!!)
                    }

                    // Handle non-Shopify case
                    data.token?.let { token ->
                        cache.setValue(KwikPassKeys.GK_ACCESS_TOKEN, token)
                        data.token = null
                    }
                    data.coreToken?.let { coreToken ->
                        cache.setValue(KwikPassKeys.CHECKOUT_ACCESS_TOKEN, coreToken)
                        data.coreToken = null
                    }

                    // Validate token and login user
                    validateUserToken()
                    val loginResponse = loginKpUser()
                    loginResponse.onSuccess { response ->
                        response.data?.merchantResponse?.email?.let {
                            cache.setValue(KwikPassKeys.GK_VERIFIED_USER, gson.toJson(response.data))
                        }

                        println("RESPONSE AFTER LOGIN IS SUCCESSFUL $response")

                        // Send Snowplow event for successful login
                        Snowplow.sendCustomEventToSnowPlow(
                            mapOf(
                                "category" to "login_modal",
                                "action" to "logged_in",
                                "label" to "phone_Number_logged_in",
                                "property" to "phone_number",
                                "value" to (phoneNumber.toIntOrNull() ?: 0)
                            )
                        )

                        return Result.success(response)
                    }

                    return Result.success(response.body()!!)
                }
            }
            val error = handleApiError(HttpException(response!!))
            Result.failure(Exception(error.message))
        } catch (e: Exception) {
            val error = handleApiError(e)
            Result.failure(Exception(error.message))
        }
    }

    private fun extractDomain(url: String?): String {
        if (url == null) return ""
        val regex = """^(?:https?://)?(?:www\.)?([^/]+)""".toRegex()
        return regex.find(url)?.groupValues?.get(1) ?: ""
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
            // Get phone number from cache
            val phoneNumber = cache.getValue(KwikPassKeys.GK_USER_PHONE)

            // Send Snowplow event for logout
            Snowplow.sendCustomEventToSnowPlow(
                mapOf(
                    "category" to "logged_in_page",
                    "action" to "logged_out",
                    "label" to "logout_button_click",
                    "property" to "phone_number",
                    "value" to (phoneNumber?.toIntOrNull() ?: 0)
                )
            )

            // Clear headers
            KwikPassHttpClient.clearHeaders()

            // Clear cache
            cache.clearCache()

            // Clear all stored values
            cache.removeValue(KwikPassKeys.GK_ACCESS_TOKEN)
            cache.removeValue(KwikPassKeys.CHECKOUT_ACCESS_TOKEN)
            cache.removeValue(KwikPassKeys.GK_KP_TOKEN)
            cache.removeValue(KwikPassKeys.GK_VERIFIED_USER)
            cache.removeValue(KwikPassKeys.GK_REQUEST_ID)
            cache.removeValue(KwikPassKeys.KP_REQUEST_ID)
            cache.removeValue(KwikPassKeys.GK_AUTH_TOKEN)
            cache.removeValue(KwikPassKeys.GK_USER_PHONE)
            cache.removeValue(KwikPassKeys.GK_NOTIFICATION_ENABLED)

            return Result.success(true)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun getHostName(url: String): String {
        return url.replace(Regex("^(?:https?://)?(?:www\\.)?([^/]+).*$"), "$1")
    }

    private suspend fun activateUserAccount(
        customerId: String?,
        url: String,
        password: String?,
        token: String
    ): Result<Any> = withContext(Dispatchers.IO) {
        try {
            if (customerId == null || password == null) {
                return@withContext Result.failure(Exception("Missing required parameters"))
            }

            // Get phone number from cache for event tracking
            val phoneNumber = cache.getValue(KwikPassKeys.GK_USER_PHONE)

            // Create form data similar to qs.stringify in React Native
            val formData = mapOf(
                "form_type" to "activate_customer_password",
                "utf8" to "✓",
                "customer[password]" to password,
                "customer[password_confirmation]" to password,
                "token" to token,
                "id" to customerId
            ).map { (key, value) -> 
                "${key.encodeUrl()}=${value.encodeUrl()}"
            }.joinToString("&")

            // Create a custom OkHttp client for this specific request
            val client = OkHttpClient.Builder()
                .followRedirects(false) // equivalent to maxRedirects: 0
                .build()

            val request = Request.Builder()
                .url("https://$url/account/activate")
                .post(RequestBody.create(
                    "application/x-www-form-urlencoded".toMediaTypeOrNull(),
                    formData
                ))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            // Send Snowplow event for account activation
            Snowplow.sendCustomEventToSnowPlow(
                mapOf(
                    "category" to "login_modal",
                    "action" to "automated",
                    "label" to "account_activated",
                    "property" to "phone_number",
                    "value" to (phoneNumber?.toIntOrNull() ?: 0)
                )
            )

            val response = client.newCall(request).execute()
            println("response from activate user account $response")

            return@withContext if (response.isSuccessful) {
                Result.success(response)
            } else {
                Result.failure(Exception("Failed to activate user account"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper function to URL encode parameters
    private fun String.encodeUrl(): String {
        return URLEncoder.encode(this, "UTF-8")
    }

    suspend fun shopifySendEmailVerificationCode(email: String): Result<Any> {
        return try {
            getBrowserToken()
            
            // Get phone number from cache
            val phoneNumber = cache.getValue(KwikPassKeys.GK_USER_PHONE)
            
            // Send Snowplow event
            Snowplow.sendCustomEventToSnowPlow(
                mapOf(
                    "category" to "login_modal",
                    "action" to "click",
                    "label" to "email_filled",
                    "property" to "email",
                    "value" to (phoneNumber?.toIntOrNull() ?: 0)
                )
            )

            val response = apiService?.sendShopifyEmailVerificationCode(
                ShopifyEmailVerificationRequest(email)
            )

            println("RESPONSE FOR HANDLING EMAIL OTP API $response")

            if (response?.isSuccessful == true) {
                return Result.success(response.body()!!)
            }
            
            val error = handleApiError(HttpException(response!!))
            Result.failure(Exception(error.message))
        } catch (e: Exception) {
            val error = handleApiError(e)
            Result.failure(Exception(error.message))
        }
    }

    suspend fun shopifyVerifyEmail(email: String, otp: String): Result<Any> {
        return try {
            getBrowserToken()
            
            // Get notifications preference from cache
            val notifications = cache.getValue(KwikPassKeys.GK_NOTIFICATION_ENABLED) == "true"
            
            // Create request body using the new request class
            val requestBody = ShopifyEmailVerifyRequest(
                email = email,
                otp = otp,
                redirectUrl = "/",
                isMarketingEventSubscribed = notifications
            )

            // Make API call
            val response = apiService?.verifyShopifyEmail(requestBody)
            println("RESPONSE FOR EMAIL VERIFICATION $response")
            
            if (response?.isSuccessful == true) {
                val responseData = response.body()
                println("RESPONSE DATA FROM API FOR EMAIL VERIFICATION $responseData")

                // Get existing user data from cache
//                val userDataJson = cache.getValue(KwikPassKeys.GK_VERIFIED_USER)
//                val userData = if (userDataJson != null) {
//                    gson.fromJson(userDataJson, Map::class.java)
//                } else {
//                    emptyMap<String, Any>()
//                }
//
//                // Create merged user data
//                val mergedData = responseData?.data?.toMutableMap() ?: mutableMapOf()
//
//                // Add phone number if not present in response but exists in user data
//                if (!mergedData.containsKey("phone") && userData.containsKey("phone")) {
//                    mergedData["phone"] = userData["phone"]
//                }
//
//                // Store updated user data in cache
//                val updatedUserJson = gson.toJson(mergedData)
//                cache.setValue(KwikPassKeys.GK_VERIFIED_USER, updatedUserJson)

                // Send Snowplow event
                // TODO: Implement Snowplow event tracking
                val phone = cache.getValue(KwikPassKeys.GK_USER_PHONE)
                Snowplow.sendCustomEventToSnowPlow(
                    mapOf(
                        "category" to "login_modal",
                        "action" to "logged_in",
                        "label" to "otp_verified",
                        "property" to "phone_number",
                        "value" to (phone?.toIntOrNull() ?: 0)
                    )
                )

                return Result.success(response.body()!!)
            }

            val error = handleApiError(HttpException(response!!))
            Result.failure(Exception(error.message))
        } catch (e: Exception) {
            val error = handleApiError(e)
            println("ERROR FROM EMAIL VALIDATION $error")
            Result.failure(Exception(error.message))
        }
    }
}