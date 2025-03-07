package com.gk.kwikpass.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body

interface KwikPassApiService {
    @GET("auth/browser")
    suspend fun getBrowserAuth(): Response<BrowserAuthResponse>

    @GET("configurations/{mid}")
    suspend fun getMerchantConfig(@Path("mid") mid: String): Response<MerchantConfigResponse>

    @POST("auth/otp/send")
    suspend fun sendVerificationCode(@Body body: SendVerificationCodeRequest): Response<OtpSentResponse>

    @GET("customer/custom/login")
    suspend fun loginKpUser(): Response<LoginResponse>

    @POST("customer/custom/create-user")
    suspend fun createUser(@Body body: CreateUserRequest): Response<CreateUserResponse>

    @GET("auth/validate-token")
    suspend fun validateUserToken(): Response<ValidateUserTokenResponse>

    @POST("auth/otp/verify")
    suspend fun verifyCode(@Body body: VerifyCodeRequest): Response<VerifyCodeResponse>

    @GET("customer-intelligence")
    suspend fun getCustomerIntelligence(@Body params: Map<String, String>): Response<CustomerIntelligenceResponse>
}

// Request classes
data class SendVerificationCodeRequest(
    val phone: String
)

data class CreateUserRequest(
    val email: String,
    val name: String,
    val dob: String? = null,
    val gender: String? = null
)

data class VerifyCodeRequest(
    val phone: String,
    val otp: Int
)

// Response classes
data class BrowserAuthResponse(
    val data: BrowserAuthData
)

data class BrowserAuthData(
    val requestId: String,
    val token: String
)

data class MerchantConfigResponse(
    val data: MerchantConfig
)

data class MerchantConfig(
    val platform: String?,
    val host: String?,
    val name: String?,
    val logo: String?,
    val theme: MerchantTheme?,
    val features: List<String>?,
    val settings: MerchantSettings?,
    val customerIntelligenceEnabled: Boolean? = false,
    val customerIntelligenceMetrics: Map<String, Any>? = null
)

data class MerchantTheme(
    val primaryColor: String?,
    val secondaryColor: String?,
    val backgroundColor: String?
)

data class MerchantSettings(
    val enableGuestCheckout: Boolean?,
    val enableSocialLogin: Boolean?,
    val enablePhoneVerification: Boolean?
)

data class OtpSentResponse(
    val data: OtpSentData
)

data class OtpSentData(
    val message: String?,
    val status: String?
)

data class LoginResponse(
    val data: LoginData
)

data class LoginData(
    val merchantResponse: MerchantResponse?
)

data class MerchantResponse(
    val email: String?,
    val csrfToken: String?,
    val id: String?,
    val token: String?,
    val refreshToken: String?
)

data class CreateUserResponse(
    val data: CreateUserData
)

data class CreateUserData(
    val merchantResponse: CreateUserMerchantResponse
)

data class CreateUserMerchantResponse(
    val accountCreate: AccountCreate
)

data class AccountCreate(
    val user: AccountUser?,
    val accountErrors: List<String>?
)

data class AccountUser(
    val csrfToken: String?,
    val id: String?,
    val token: String?,
    val refreshToken: String?
)

data class ValidateUserTokenResponse(
    val data: ValidateUserTokenData
)

data class ValidateUserTokenData(
    val merchantResponse: ValidateUserTokenMerchantResponse
)

data class ValidateUserTokenMerchantResponse(
    val email: String?,
    val csrfToken: String?,
    val id: String?,
    val token: String?,
    val refreshToken: String?
)

data class VerifyCodeResponse(
    val data: VerifyCodeData
)

data class VerifyCodeData(
    val token: String?,
    val coreToken: String?,
    val state: String?,
    val email: String?,
    val shopifyCustomerId: String?
)

data class CustomerIntelligenceResponse(
    val data: List<String>?
) 