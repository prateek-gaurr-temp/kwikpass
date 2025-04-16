package com.gk.kwikpass.screens

import LoginViewModel
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.gk.kwikpass.api.KwikPassApi
import com.gk.kwikpass.config.KwikPassCache
import com.gk.kwikpass.config.KwikPassKeys
import com.gk.kwikpass.initializer.ApplicationCtx
import com.gk.kwikpass.screens.createaccount.CreateAccountScreen
import com.gk.kwikpass.screens.login.LoginHeader
import com.gk.kwikpass.screens.login.LoginScreen
import com.gk.kwikpass.screens.shopify.ShopifyEmailScreen
import com.gk.kwikpass.screens.verify.VerifyScreen
import com.gk.kwikpass.screens.verify.VerifyViewModel
import com.gk.kwikpass.snowplow.SnowplowClient
import com.gk.kwikpass.ui.theme.KwikpassTheme
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.gk.kwikpass.initializer.kwikpassInitializer
import com.gk.kwikpass.screens.createaccount.CreateAccountData
import com.gk.kwikpass.utils.CoroutineUtils
import com.gk.kwikpass.utils.ModifierWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update

// Form state data class equivalent to React Native's LoginForm
data class LoginFormState(
    val phone: String = "",
    val notifications: Boolean = true,
    var otp: String = "",
    val otpSent: Boolean = false,
    val isNewUser: Boolean = false,
    val emailOtpSent: Boolean = false,
    var shopifyEmail: String = "",
    val shopifyOTP: String = "",
    val isSuccess: Boolean = false,
    val multipleEmail: List<MultipleEmail> = emptyList()
)

data class MultipleEmail(
    val label: String,
    val value: String
)

// Form validation errors
data class LoginFormErrors(
    val phone: String? = null,
    val otp: String? = null,
    val shopifyEmail: String? = null,
    val shopifyOTP: String? = null
)

// Callback interface for Kwikpass events
interface KwikpassCallback {
    fun onSuccess(data: MutableMap<String, Any?>?)
    fun onError(error: String)
    fun onGuestLogin()
}

// ViewModel for managing form state
class LoginFormViewModel : androidx.lifecycle.ViewModel() {
    private val _formState = MutableStateFlow(LoginFormState())
    val formState: StateFlow<LoginFormState> = _formState.asStateFlow()

    private val _formErrors = MutableStateFlow(LoginFormErrors())
    val formErrors: StateFlow<LoginFormErrors> = _formErrors.asStateFlow()

    fun updatePhone(phone: String) {
        _formState.value = _formState.value.copy(phone = phone)
    }

    fun updateNotifications(enabled: Boolean) {
        _formState.value = _formState.value.copy(notifications = enabled)
    }

    fun updateOtp(otp: String) {
        _formState.value = _formState.value.copy(otp = otp)
    }

    fun setOtpSent(sent: Boolean) {
        _formState.value = _formState.value.copy(otpSent = sent)
    }

    fun setNewUser(isNew: Boolean) {
        _formState.value = _formState.value.copy(isNewUser = isNew)
    }

    fun updateShopifyEmail(email: String) {
        _formState.value = _formState.value.copy(shopifyEmail = email)
    }

    fun updateShopifyOTP(otp: String) {
        _formState.value = _formState.value.copy(shopifyOTP = otp)
    }

    fun setEmailOtpSent(sent: Boolean) {
        _formState.value = _formState.value.copy(emailOtpSent = sent)
    }

    fun setSuccess(success: Boolean) {
        _formState.value = _formState.value.copy(isSuccess = success)
    }

    fun setError(field: String, error: String?) {
        _formErrors.value = when (field) {
            "phone" -> _formErrors.value.copy(phone = error)
            "otp" -> _formErrors.value.copy(otp = error)
            "shopifyEmail" -> _formErrors.value.copy(shopifyEmail = error)
            "shopifyOTP" -> _formErrors.value.copy(shopifyOTP = error)
            else -> _formErrors.value
        }
    }

    fun resetForm() {
        _formState.value = LoginFormState()
        _formErrors.value = LoginFormErrors()
    }

    fun updateMultipleEmail(emails: List<MultipleEmail>) {
        _formState.value = _formState.value.copy(multipleEmail = emails)
    }
}

// Data classes for configuration
data class CreateAccountForm(
    val email: String = "",
    val username: String = "",
    val gender: String = "",
    val dob: String = ""
)

data class CreateUserConfig(
    val isEmailRequired: Boolean = false,
    val isNameRequired: Boolean = false,
    val isGenderRequired: Boolean = false,
    val isDobRequired: Boolean = false,
    val showEmail: Boolean = false,
    val showUserName: Boolean = false,
    val showGender: Boolean = false,
    val showDob: Boolean = false
)

data class TextInputConfig(
    val submitButtonStyle: Map<String, Any>? = null,
    val inputContainerStyle: Map<String, Any>? = null,
    val inputStyle: Map<String, Any>? = null,
    val titleStyle: Map<String, Any>? = null,
    val subTitleStyle: Map<String, Any>? = null,
    val otpPlaceholder: String? = null,
    val phoneAuthScreen: PhoneAuthScreenConfig? = null,
    val otpVerificationScreen: OtpVerificationScreenConfig? = null,
    val createUserScreen: CreateUserScreenConfig? = null,
    val shopifyEmailScreen: ShopifyEmailScreenConfig? = null
)

data class PhoneAuthScreenConfig(
    val title: String? = null,
    val subTitle: String? = null,
    val phoneNumberPlaceholder: String? = null,
    val updatesPlaceholder: String? = null,
    val submitButtonText: String? = null
)

data class OtpVerificationScreenConfig(
    val title: String? = null,
    val subTitle: String? = null,
    val submitButtonText: String? = null,
    val loadingText: String? = null,
    val loadingTextStyle: Map<String, Any>? = null
)

data class CreateUserScreenConfig(
    val title: String? = null,
    val subTitle: String? = null,
    val emailPlaceholder: String? = null,
    val namePlaceholder: String? = null,
    val dobPlaceholder: String? = null,
    val genderPlaceholder: String? = null,
    val submitButtonText: String? = null,
    val dobFormat: String? = null,
    val genderTitle: String? = null
)

data class ShopifyEmailScreenConfig(
    val title: String? = null,
    val subTitle: String? = null,
    val emailPlaceholder: String? = null,
    val submitButtonText: String? = null
)

data class FooterUrl(
    val label: String,
    val url: String
)

data class KwikpassConfig(
    val bannerImage: String? = null,
    val logo: String? = null,
    val footerText: String? = null,
    val footerUrls: List<FooterUrl>? = null,
    val createUserConfig: CreateUserConfig = CreateUserConfig(),
    val enableGuestLogin: Boolean = false,
    val guestLoginButtonLabel: String? = null,
    val inputProps: TextInputConfig? = null,
    val merchantType: String = "custom",

    // header styles
    val bannerImageStyle: ModifierWrapper? = null,
    val logoStyle: ModifierWrapper? = null,
    val imageContainerStyle: ModifierWrapper? = null,
    val guestContainerStyle: ModifierWrapper? = null,
    val guestButtonContainerColor: Color? = Color.Black,
    val guestButtonContentColor: Color? = Color.White
) {
    companion object {
        private val defaultFooterUrls = listOf(
            FooterUrl("Privacy Policy", "https://google.com/"),
            FooterUrl("Terms & Conditions", "https://google.com/")
        )

        fun getFooterUrls(customUrls: List<FooterUrl>?): List<FooterUrl> {
            return customUrls ?: defaultFooterUrls
        }
    }
}

class KwikpassLoginFragment : Fragment() {
    private val loginViewModel: LoginViewModel by viewModels()
    private val verifyViewModel: VerifyViewModel by viewModels()
    private val formViewModel: LoginFormViewModel by viewModels()
    private var config: KwikpassConfig? = null
    private lateinit var kwikPassApi: KwikPassApi
    private var merchantType: String = "custom"
    var callback: KwikpassCallback? = null
    private lateinit var gson: Gson
    private lateinit var cache: KwikPassCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("LOGIN FRAGMENT LOADED")
        val context = ApplicationCtx.get()
        cache = KwikPassCache.getInstance(context)
        kwikPassApi = KwikPassApi(ApplicationCtx.get())
        gson = Gson()

        CoroutineUtils.coroutine.launch {
            try {
                val merchantTypeFromCahce = cache?.getValue(KwikPassKeys.GK_MERCHANT_TYPE)
                merchantType = merchantTypeFromCahce.toString().toLowerCase()
                println("MERCHANT TYPE FROM API $merchantType")
            } catch (e: Exception) {
                println("Error getting merchant config: ${e.message}")
                e.printStackTrace()
                merchantType = "custom"
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Parse configuration from arguments using Gson
        config = arguments?.getString(CONFIG_KEY)?.let {
            Gson().fromJson(it, KwikpassConfig::class.java)
        }

        // Initialize Snowplow client
        CoroutineUtils.coroutine.launch(Dispatchers.IO) {
            val snowplowClientRes = SnowplowClient.getSnowplowClient(
                ApplicationCtx.get(),
                kwikpassInitializer.getEnvironment().toString(),
                kwikpassInitializer.getMerchantId().toString()
            )
        }

        return ComposeView(requireContext()).apply {
            setContent {
                KwikpassTheme {
                    KwikpassLoginContent(
                        loginViewModel = loginViewModel,
                        verifyViewModel = verifyViewModel,
                        formViewModel = formViewModel,
                        config = config
                    )
                }
            }
        }
    }

    @Composable
    private fun KwikpassLoginContent(
        loginViewModel: LoginViewModel,
        verifyViewModel: VerifyViewModel,
        formViewModel: LoginFormViewModel,
        config: KwikpassConfig?
    ) {
        val loginUiState by loginViewModel.uiState.collectAsState()
        val verifyUiState by verifyViewModel.uiState.collectAsState()
        val formState by formViewModel.formState.collectAsState()
        val formErrors by formViewModel.formErrors.collectAsState()
        var isUserLoggedIn by remember { mutableStateOf(false) }

        // Check user logged in state
        LaunchedEffect(Unit) {
            val verifiedUser = cache?.getValue(KwikPassKeys.GK_VERIFIED_USER)
            if (verifiedUser != null) {
                val userData = gson.fromJson(verifiedUser, Map::class.java)
                println("USER DATA IS $userData")
                isUserLoggedIn = when (merchantType) {
                    "shopify" -> {
                        (userData["shopifyCustomerId"] != null &&
                         userData["phone"] != null &&
                         userData["email"] != null &&
                         userData["multipassToken"] != null)
                    }
                    else -> {
                        userData?.get("phone") != null && userData?.get("email") != null
                    }
                }
            }
        }

        var showVerifyScreen by remember { mutableStateOf(false) }
        var showCreateAccount by remember { mutableStateOf(false) }
        var showShopifyEmail by remember { mutableStateOf(false) }
        var shouldResetOtp by remember { mutableStateOf(false) }

        // Handle send verification code
        val handleSendVerificationCode = { phone: String, notifications: Boolean ->
            CoroutineUtils.coroutine.launch(Dispatchers.IO) {
                try {
                    // Validate phone number before making API call
                    if (phone.length != 10 || !phone.all { it.isDigit() }) {
                        formViewModel.setError("phone", "Please enter a valid 10-digit phone number")
                        callback?.onError("Please enter a valid 10-digit phone number")
                        return@launch
                    }

                    println("PHONE $phone")
                    println("NOTIFI $notifications")
                    loginViewModel.setLoading(true)
                    val result = kwikPassApi.sendVerificationCode(phone, notifications)
                    println("RESULT FROM API $result")

                    result.onSuccess {
                        formViewModel.setOtpSent(true)
                        showVerifyScreen = true
                    }.onFailure { error ->
                        formViewModel.setError("phone", error.message)
                        callback?.onError(error.message ?: "Failed to send verification code")
                    }
                } catch (e: Exception) {
                    println("ERrorr : $e" )
                    formViewModel.setError("phone", e.message)
                    callback?.onError(e.message ?: "Failed to send verification code")
                } finally {
                    loginViewModel.setLoading(false)
                }
            }
        }

        // Handle verify OTP
        val handleVerifyOTP = { phone: String, otp: String ->
            CoroutineUtils.coroutine.launch(Dispatchers.IO) {
                try {
                    verifyViewModel.clearErrors() // Clear any existing errors
                    verifyViewModel.setLoading(true)
                    val result = kwikPassApi.verifyCode(phone, otp)
                    shouldResetOtp = true

                    println("RESULT IS PRINTED AS $result")

                    result.onSuccess { response ->
                        println("RESPONSE TYPE: ${response::class.java.simpleName}")
                        println("RESPONSE AFTER SUCCESS IS $response")

                        // Convert the response to JSON string first
                        val responseJson = gson.toJson(response)
                        println("RESPONSE AS JSON: $responseJson")

                        // Parse the nested structure
                        val responseMap = gson.fromJson(responseJson, Map::class.java)
                        if(merchantType == "shopify") {
                            // Get the data from the nested structure
                            val responseData = (responseMap["value"] as? Map<*, *>)?.get("data") as? Map<*, *>
                            
                            println("EXTRACTED DATA: $responseData")

                            // Handle multiple email case
                            if (responseData?.containsKey("multipleEmail") == true) {
                                val multipleEmailStr = responseData["multipleEmail"] as? String
                                if (multipleEmailStr != null) {
                                    val emails = multipleEmailStr.split(",").map { email ->
                                        MultipleEmail(
                                            label = email.trim(),
                                            value = email.trim()
                                        )
                                    }
                                    formViewModel.updateMultipleEmail(emails)
                                }
                                return@onSuccess
                            }

                            // Handle email required case
                            if (responseData?.get("emailRequired") == true && responseData["email"] == null) {
                                verifyViewModel.setLoading(false)
                                formViewModel.setNewUser(true)
                                formState.otp = ""
                                formViewModel.setOtpSent(false)
                                return@launch
                            }

                            // Create cleaned data with required fields
                            val cleanedData = mutableMapOf<String, Any?>()
                            cleanedData["shopifyCustomerId"] = responseData?.get("shopifyCustomerId")
                            cleanedData["email"] = responseData?.get("email")
                            cleanedData["phone"] = phone

                            val password = responseData?.get("password")
                            if(password != null) {
                                cleanedData["password"] = password
                            }

                            val token = responseData?.get("multipassToken")
                            if(token != null) {
                                cleanedData["multipassToken"] = token
                            }

                            println("FINAL CLEAN DATA IS $cleanedData")

                            formViewModel.setSuccess(true)
                            verifyViewModel.setSuccess(true)
                            callback?.onSuccess(cleanedData)
                            verifyViewModel.setLoading(false)
                            return@onSuccess
                        } else {
                            // Handle non-shopify case
                            val data = (responseMap["value"] as? Map<String, Any?>)?.get("data") as? Map<String, Any?>
                            
                            if(data?.containsKey("emailRequired") == true) {
                                if(data["emailRequired"] == true && data["email"] == null) {
                                    formViewModel.setNewUser(true)
                                    formState.otp = ""
                                    formViewModel.setOtpSent(false)
                                    verifyViewModel.setLoading(false)
                                    return@launch
                                }
                            }

                            if(data?.containsKey("email") == true && data["email"] != null) {
                                // Create a mutable copy of the data map
                                val mutableData = data.toMutableMap()
                                
                                if(!mutableData.containsKey("phone")) {
                                    mutableData["phone"] = formState.phone
                                }

                                println("DATA FOR CUSTOM MERCHANTS $mutableData")
                                cache.setValue(KwikPassKeys.GK_VERIFIED_USER, gson.toJson(mutableData))
                                formViewModel.setSuccess(true)
                                verifyViewModel.setSuccess(true)
                                callback?.onSuccess(mutableData)
                                verifyViewModel.setLoading(false)
                            }
                            return@onSuccess
                        }
                    }.onFailure { error ->
                        verifyViewModel.setError("otp", error.message)
                        callback?.onError(error.message ?: "Failed to verify OTP")
                    }
                } catch (e: Exception) {
                    verifyViewModel.setError("otp", e.message)
                    callback?.onError(e.message ?: "Failed to verify OTP")
                } finally {
                    verifyViewModel.setLoading(false)
                }
            }
        }

        // Handle create account
        val handleCreateAccount = { accountData: CreateAccountData ->
            println("ACCOUNT CREATION DATA $accountData")

            CoroutineUtils.coroutine.launch(Dispatchers.IO) {
                try {
                    verifyViewModel.setLoading(true)
                    val result = kwikPassApi.createUser(
                        accountData.email,
                        accountData.username,
                        accountData.dob,
                        accountData.gender
                    )

                    result.onSuccess { response ->
                        val responseJson = gson.toJson(response)
                        val responseMap = gson.fromJson(responseJson, Map::class.java)
                        val data = responseMap["data"] as? Map<*, *>
                        val merchantResponse = data?.get("merchantResponse") as? Map<*, *>
                        val accountCreate = merchantResponse?.get("accountCreate") as? Map<*, *>
                        val user = accountCreate?.get("user") as? Map<*, *>
                        val accountErrors = accountCreate?.get("accountErrors") as? List<*>

                        println("USER RESPONSE IS $user")
                        println("ACCOUNT ERRORS $accountErrors")

                        if (user != null && (accountErrors == null || accountErrors.isEmpty())) {
                            // Create user data map with all necessary fields
                            val userData = mutableMapOf<String, Any?>()
                            userData["id"] = user["id"]
                            userData["token"] = user["token"]
                            userData["refreshToken"] = user["refreshToken"]
                            userData["csrfToken"] = user["csrfToken"]
                            userData["phone"] = formState.phone
                            userData["email"] = accountData.email

                            // Store user data in cache
                            cache.setValue(KwikPassKeys.GK_VERIFIED_USER, gson.toJson(userData))

                            // Set success state and trigger callback
                            formViewModel.setSuccess(true)
                            verifyViewModel.setSuccess(true)
                            callback?.onSuccess(userData)
                        } else if (!accountErrors.isNullOrEmpty()) {
                            val errorMessage = accountErrors.firstOrNull()?.toString() ?: "Failed to create account"
                            formViewModel.setError("createAccount", errorMessage)
                            callback?.onError(errorMessage)
                        }
                    }.onFailure { error ->
                        formViewModel.setError("createAccount", error.message)
                        callback?.onError(error.message ?: "Failed to create account")
                    }
                } catch (e: Exception) {
                    formViewModel.setError("createAccount", e.message)
                    callback?.onError(e.message ?: "Failed to create account")
                } finally {
                    verifyViewModel.setLoading(false)
                }
            }
        }

        // Handle Shopify email submission
        val handleShopifyEmailSubmit = { email: String ->
            formState.shopifyEmail = email
            CoroutineUtils.coroutine.launch(Dispatchers.IO) {
                try {
                    loginViewModel.setLoading(true)
                    val result = kwikPassApi.shopifySendEmailVerificationCode(email)

                    println("RESULT FROM API FOR EMAIL VERIFICATION FLOW $result")

                    result.onSuccess {
                        formViewModel.setEmailOtpSent(true)
                        formViewModel.setNewUser(false)
                    }.onFailure { error ->
                        formViewModel.setError("shopifyEmail", error.message)
                        callback?.onError(error.message ?: "Failed to send email verification code")
                    }
                } catch (e: Exception) {
                    formViewModel.setError("shopifyEmail", e.message)
                    callback?.onError(e.message ?: "Failed to send email verification code")
                } finally {
                    loginViewModel.setLoading(false)
                }
            }
        }

        // Handle resend email OTP
        val handleResendEmailOTP = {
            CoroutineUtils.coroutine.launch(Dispatchers.IO) {
                try {
                    verifyViewModel.setLoading(true)
                    val result = kwikPassApi.shopifySendEmailVerificationCode(formState.shopifyEmail)

                    result.onSuccess {
                        formViewModel.setEmailOtpSent(true)
                        formViewModel.setNewUser(false)
                    }.onFailure { error ->
                        formViewModel.setError("shopifyOTP", error.message)
                        callback?.onError(error.message ?: "Failed to resend email verification code")
                    }
                } catch (e: Exception) {
                    formViewModel.setError("shopifyOTP", e.message)
                    callback?.onError(e.message ?: "Failed to resend email verification code")
                } finally {
                    verifyViewModel.setLoading(false)
                }
            }
        }

        // Handle resend phone OTP
        val handleResendPhoneOTP = {
            CoroutineUtils.coroutine.launch(Dispatchers.IO) {
                try {
                    verifyViewModel.setLoading(true)
                    val result = kwikPassApi.sendVerificationCode(formState.phone, true)

                    result.onSuccess {
                        formViewModel.setOtpSent(true)
                    }.onFailure { error ->
                        formViewModel.setError("otp", error.message)
                        callback?.onError(error.message ?: "Failed to resend verification code")
                    }
                } catch (e: Exception) {
                    formViewModel.setError("otp", e.message)
                    callback?.onError(e.message ?: "Failed to resend verification code")
                } finally {
                    verifyViewModel.setLoading(false)
                }
            }
        }

        // Handle Shopify email OTP verification
        val handleShopifyEmailOTPVerification = { email: String, otp: String ->
            println("EMAIL AND OTP FOR EMAIL VERIFICATION $email and $otp")
            CoroutineUtils.coroutine.launch(Dispatchers.IO) {
                try {
                    verifyViewModel.setLoading(true)
                    val result = kwikPassApi.shopifyVerifyEmail(email.toString(), otp.toString())
                    shouldResetOtp = true

                    result.onSuccess { response ->
                        println("SHOPIFY EMAIL VERIFICATION RESPONSE: $response")

                        // Convert response to JSON string
                        val responseJson = gson.toJson(response)
                        val responseMap = gson.fromJson(responseJson, Map::class.java)
                        val responseData = responseMap["data"] as? Map<String, Any>

                        if (responseData != null) {
                            // Create a mutable copy of the response data
                            val userData = responseData.toMutableMap()

                            // Add phone number if not present
                            if (!userData.containsKey("phone")) {
                                userData["phone"] = formState.phone
                            }

                            // Store verified user data in cache
                            val userJson = gson.toJson(userData)
                            cache?.setValue(KwikPassKeys.GK_VERIFIED_USER, userJson)

                            // Set success state
                            formViewModel.setSuccess(true)
                            verifyViewModel.setSuccess(true)

                            // Call success callback with user data
                            callback?.onSuccess(userData as MutableMap<String, Any?>?)
                        }
                    }.onFailure { error ->
                        verifyViewModel.setError("shopifyOTP", error.message)
                        callback?.onError(error.message ?: "Failed to verify email OTP")
                    }
                } catch (e: Exception) {
                    verifyViewModel.setError("shopifyOTP", e.message)
                    callback?.onError(e.message ?: "Failed to verify email OTP")
                } finally {
                    verifyViewModel.setLoading(false)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Header Section
                LoginHeader(
                    logo = config?.logo,
                    bannerImage = config?.bannerImage,
                    enableGuestLogin = config?.enableGuestLogin ?: false,
                    guestLoginButtonLabel = config?.guestLoginButtonLabel ?: "Skip",
                    onGuestLoginClick = { callback?.onGuestLogin() },
                    modifier = Modifier.fillMaxWidth(),
                    bannerImageStyle = config?.bannerImageStyle,
                    logoStyle = config?.logoStyle,
                    imageContainerStyle = config?.imageContainerStyle,
                    guestContainerStyle = config?.guestContainerStyle,
                    guestButtonContentColor = config?.guestButtonContentColor,
                    guestButtonContainerColor = config?.guestButtonContainerColor
                )

                if (!isUserLoggedIn) {
                    when {
                        formState.emailOtpSent -> {
                            VerifyScreen(
                                onVerify = {
                                    handleShopifyEmailOTPVerification(formState.shopifyEmail, formState.shopifyOTP)
                                },
                                onEdit = {
                                    formViewModel.setEmailOtpSent(false)
                                    formViewModel.setNewUser(true)
                                },
                                onResend = {
                                    handleResendEmailOTP()
                                },
                                otpLabel = formState.shopifyEmail,
                                uiState = verifyUiState,
                                onOtpChange = { otp ->
                                    formViewModel.updateShopifyOTP(otp)
                                    if (otp.length == 4) {
                                        handleShopifyEmailOTPVerification(formState.shopifyEmail, otp)
                                    }
                                },
                                resetOtp = verifyUiState.shouldResetOtp,
                                currentOtp = formState.shopifyOTP,
                                loadingText = config?.inputProps?.otpVerificationScreen?.loadingText ?: "Signing you in...",
//                                loadingTextStyle = TextStyle(
//                                    color = Color.White,
//                                    fontSize = (config?.inputProps?.otpVerificationScreen?.loadingTextStyle?.fontSize?.toIntOrNull() ?: 16).sp,
//                                    fontWeight = FontWeight.Medium
//                                )
                            )
                        }
                        formState.otpSent -> {
                            VerifyScreen(
                                onVerify = {
                                    handleVerifyOTP(formState.phone, formState.otp)
                                },
                                onEdit = {
                                    formViewModel.setOtpSent(false)
                                },
                                onResend = {
                                    handleResendPhoneOTP()
                                },
                                otpLabel = "+91 ${formState.phone}",
                                title = config?.inputProps?.otpVerificationScreen?.title ?: "OTP Verification",
                                subTitle = config?.inputProps?.otpVerificationScreen?.subTitle,
                                submitButtonText = config?.inputProps?.otpVerificationScreen?.submitButtonText ?: "Verify",
                                uiState = verifyUiState,
                                onOtpChange = { otp ->
                                    formViewModel.updateOtp(otp)
                                    if (otp.length == 4) {
                                        handleVerifyOTP(formState.phone, otp)
                                    }
                                },
                                resetOtp = verifyUiState.shouldResetOtp,
                                currentOtp = formState.otp,
                                loadingText = config?.inputProps?.otpVerificationScreen?.loadingText ?: "Signing you in...",
//                                loadingTextStyle = TextStyle(
//                                    color = Color.White,
//                                    fontSize = (config?.inputProps?.otpVerificationScreen?.loadingTextStyle?.fontSize?.toIntOrNull() ?: 16).sp,
//                                    fontWeight = FontWeight.Medium
//                                )
                            )
                        }
                        formState.isNewUser -> {
                            if (merchantType == "custom") {
                                CreateAccountScreen(
                                    onSubmit = { accountData ->
                                        handleCreateAccount(accountData)
                                    },
                                    isLoading = verifyUiState.isLoading,
//                                    errors = formErrors,
                                    title = config?.inputProps?.createUserScreen?.title ?: "Submit your details",
                                    subTitle = config?.inputProps?.createUserScreen?.subTitle,
                                    showEmail = config?.createUserConfig?.showEmail ?: true,
                                    showUserName = config?.createUserConfig?.showUserName ?: true,
                                    showGender = config?.createUserConfig?.showGender ?: false,
                                    showDob = config?.createUserConfig?.showDob ?: false
                                )
                            } else {
                                ShopifyEmailScreen(
                                    onSubmit = { email ->
                                        handleShopifyEmailSubmit(email)
                                    },
                                    isLoading = verifyUiState.isLoading,
                                    title = config?.inputProps?.shopifyEmailScreen?.title ?: "Submit your details",
                                    subTitle = config?.inputProps?.shopifyEmailScreen?.subTitle,
                                    emailPlaceholder = config?.inputProps?.shopifyEmailScreen?.emailPlaceholder ?: "Enter your email",
                                    submitButtonText = config?.inputProps?.shopifyEmailScreen?.submitButtonText ?: "Submit",
                                    multipleEmail = formState.multipleEmail
                                )
                            }
                        }
                        else -> {
                            LoginScreen(
                                onSubmit = {
                                    handleSendVerificationCode(formState.phone, formState.notifications)
                                },
                                title = config?.inputProps?.phoneAuthScreen?.title,
                                subTitle = config?.inputProps?.phoneAuthScreen?.subTitle,
                                submitButtonText = config?.inputProps?.phoneAuthScreen?.submitButtonText ?: "Submit",
                                placeholderText = config?.inputProps?.phoneAuthScreen?.phoneNumberPlaceholder ?: "Enter your phone",
                                updateText = config?.inputProps?.phoneAuthScreen?.updatesPlaceholder ?: "Get updates on WhatsApp",
                                errors = mapOf("phone" to (formErrors.phone ?: "")),
                                isLoading = loginUiState.isLoading,
                                initialPhoneNumber = formState.phone,
                                initialNotifications = formState.notifications,
                                onNotificationsChange = { enabled ->
                                    formViewModel.updateNotifications(enabled)
                                },
                                onPhoneChange = { phone ->
                                    formViewModel.updatePhone(phone)
                                    if (phone.length == 10) {
                                        if (loginViewModel.validatePhone(phone)) {
                                            handleSendVerificationCode(phone, formState.notifications)
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "You are already logged in",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                // Footer Section
                if (config?.footerText?.isNotEmpty() == true) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 22.dp, vertical = 16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = config.footerText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            KwikpassConfig.getFooterUrls(config?.footerUrls).forEachIndexed { index, url ->
                                Text(
                                    text = url.label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clickable {
                                            val intent =
                                                Intent(Intent.ACTION_VIEW, Uri.parse(url.url))
                                            startActivity(intent)
                                        },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val CONFIG_KEY = "config"

        fun newInstance(config: KwikpassConfig, callback: KwikpassCallback): KwikpassLoginFragment {
            return KwikpassLoginFragment().apply {
                this.callback = callback
                arguments = Bundle().apply {
                    putString(CONFIG_KEY, Gson().toJson(config))
                }
            }
        }
    }
}
