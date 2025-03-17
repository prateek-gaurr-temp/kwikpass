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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

// Form state data class equivalent to React Native's LoginForm
data class LoginFormState(
    val phone: String = "",
    val notifications: Boolean = true,
    val otp: String = "",
    val otpSent: Boolean = false,
    val isNewUser: Boolean = false,
    val emailOtpSent: Boolean = false,
    val shopifyEmail: String = "",
    val shopifyOTP: String = "",
    val isSuccess: Boolean = false
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
    fun onSuccess(data: Any)
    fun onError(error: String)
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
    val submitButtonText: String? = null
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
    val merchantType: String = "custom"
)

class KwikpassLoginFragment : Fragment() {
    private val loginViewModel: LoginViewModel by viewModels()
    private val verifyViewModel: VerifyViewModel by viewModels()
    private val formViewModel: LoginFormViewModel by viewModels()
    private var config: KwikpassConfig? = null
    private lateinit var kwikPassApi: KwikPassApi
    private var merchantType: String = "custom"
    private var callback: KwikpassCallback? = null
    private lateinit var gson: Gson
    private lateinit var cache: KwikPassCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("LOGIN FRAGMENT LOADED")
        val context = ApplicationCtx.get()
        cache = KwikPassCache.getInstance(context)
        kwikPassApi = KwikPassApi(requireContext())
        gson = Gson()

        lifecycleScope.launch {
            try {
                val merchantConfigString = cache?.getValue(KwikPassKeys.GK_MERCHANT_CONFIG)
                println("Merchant Config from cache: ${merchantConfigString}")

                val regex = Regex("""platform=([A-Za-z0-9]+)""")
                val matchResult = merchantConfigString?.let { regex.find(it) }
                merchantType = matchResult?.groups?.get(1)?.value.toString().toLowerCase()
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
        // Parse configuration from arguments
        config = arguments?.let { parseConfig(it) }

        // Initialize Snowplow client
        viewLifecycleOwner.lifecycleScope.launch {
            val snowplowClientRes = SnowplowClient.getSnowplowClient(
                ApplicationCtx.get(),
                "sandbox",
                "19x8g5js05wj"
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
//        LaunchedEffect(Unit) {
//            val verifiedUser = cache?.getValue(KwikPassKeys.GK_VERIFIED_USER)
//            if (verifiedUser != null) {
//                val userData = gson.fromJson(verifiedUser, Map::class.java)
//                isUserLoggedIn = when (merchantType) {
//                    "shopify" -> {
//                        (userData["shopifyCustomerId"] != null &&
//                         userData["phone"] != null &&
//                         userData["email"] != null &&
//                         userData["multipassToken"] != null)
//                    }
//                    else -> {
//                        userData["phone"] != null && userData["email"] != null
//                    }
//                }
//            }
//        }

        var showVerifyScreen by remember { mutableStateOf(false) }
        var showCreateAccount by remember { mutableStateOf(false) }
        var showShopifyEmail by remember { mutableStateOf(false) }

        // Handle send verification code
        val handleSendVerificationCode = { phone: String, notifications: Boolean ->
            viewLifecycleOwner.lifecycleScope.launch {
                try {
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
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    verifyViewModel.setLoading(true)
                    val result = kwikPassApi.verifyCode(phone, otp)

                    println("RESULT IS PRINTED AS $result")

                    result.onSuccess { response ->
                        println("RESPONSE AFTER SUCCESS IS $response")
                        // First try to get the response body
                        val responseBody = when (response) {
                            is Map<*, *> -> response
                            else -> {
                                println("Response is not a Map, trying to parse from body")
                                gson.fromJson(gson.toJson(response), Map::class.java)
                            }
                        }

                        // Then try to get the data
                        val data = when (val rawData = responseBody?.get("data")) {
                            is Map<*, *> -> rawData
                            else -> {
                                println("Data is not a Map, trying to parse from JSON")
                                gson.fromJson(gson.toJson(rawData), Map::class.java)
                            }
                        }

                        // Handle Shopify merchant type
                        if (merchantType == "shopify" && data?.get("email") != null) {
                            // Create a copy with updated phone number
                            val cleanedData = data.toMutableMap().apply {
                                remove("state")
                                put("phone", get("phone") ?: phone)
                            }
                            
                            formViewModel.setSuccess(true)
                            callback?.onSuccess(cleanedData)
                            verifyViewModel.setLoading(false)
                            return@launch
                        }

                        // Handle multiple emails
                        if (data?.get("multipleEmail") != null) {
                            showShopifyEmail = true
                        }

                        // Handle new user requiring email
                        if (data?.get("emailRequired") == true && data["email"] == null) {
                            formViewModel.setNewUser(true)
                            showCreateAccount = true
                        }

                        // Handle merchant response with email
                        val merchantResponse = data?.get("merchantResponse") as? Map<*, *>
                        if (merchantResponse?.get("email") != null) {
                            val merchantDataMutable = merchantResponse.toMutableMap()
                            if (merchantDataMutable["phone"] == null) {
                                merchantDataMutable["phone"] = phone
                            }
                            formViewModel.setSuccess(true)
                            callback?.onSuccess(merchantDataMutable)
                        }

                        // Clear OTP
                        formViewModel.updateOtp("")
                    }.onFailure { error ->
                        formViewModel.setError("otp", error.message)
                        callback?.onError(error.message ?: "Failed to verify OTP")
                    }
                } catch (e: Exception) {
                    formViewModel.setError("otp", e.message)
                    callback?.onError(e.message ?: "Failed to verify OTP")
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
                    onGuestLoginClick = { /* Handle skip */ },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isUserLoggedIn) {
                    when {
                        showShopifyEmail -> {
                            ShopifyEmailScreen(
                                onSubmit = { email ->
                                    formViewModel.updateShopifyEmail(email)
                                },
                                isLoading = false,
                                title = config?.inputProps?.shopifyEmailScreen?.title ?: "Submit your details",
                                subTitle = config?.inputProps?.shopifyEmailScreen?.subTitle,
                                emailPlaceholder = config?.inputProps?.shopifyEmailScreen?.emailPlaceholder ?: "Enter your email",
                                submitButtonText = config?.inputProps?.shopifyEmailScreen?.submitButtonText ?: "Submit"
                            )
                        }
                        showCreateAccount -> {
                            CreateAccountScreen(
                                onSubmit = { accountData ->
                                    // Handle create account submission
                                },
                                isLoading = false,
                                errors = emptyMap(),
                                title = config?.inputProps?.createUserScreen?.title ?: "Submit your details",
                                subTitle = config?.inputProps?.createUserScreen?.subTitle,
                                showEmail = config?.createUserConfig?.showEmail ?: true,
                                showUserName = config?.createUserConfig?.showUserName ?: true,
                                showGender = config?.createUserConfig?.showGender ?: false,
                                showDob = config?.createUserConfig?.showDob ?: false
                            )
                        }
                        formState.otpSent -> {
                            VerifyScreen(
                                onVerify = { 
                                    handleVerifyOTP(formState.phone, formState.otp)
                                },
                                onEdit = {
                                    formViewModel.setOtpSent(false)
                                    showVerifyScreen = false
                                },
                                onResend = {
                                    handleSendVerificationCode(formState.phone, formState.notifications)
                                },
                                otpLabel = "+91 ${formState.phone}",
                                title = config?.inputProps?.otpVerificationScreen?.title ?: "OTP Verification",
                                subTitle = config?.inputProps?.otpVerificationScreen?.subTitle,
                                submitButtonText = config?.inputProps?.otpVerificationScreen?.submitButtonText ?: "Verify",
                                uiState = verifyUiState,
                                onOtpChange = { otp ->
                                    formViewModel.updateOtp(otp)
                                    verifyViewModel.validateOTP(otp)
                                    if (otp.length == 4) {
                                        verifyViewModel.setLoading(true)
                                        handleVerifyOTP(formState.phone, otp)
                                    }
                                }
                            )
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
                            config.footerUrls?.forEachIndexed { index, url ->
                                Text(
                                    text = url.label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.url))
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

    private fun parseConfig(bundle: Bundle): KwikpassConfig {
        return KwikpassConfig(
            bannerImage = bundle.getString(BANNER_IMAGE),
            logo = bundle.getString(LOGO),
            footerText = bundle.getString(FOOTER_TEXT),
            footerUrls = bundle.getString(FOOTER_URLS)?.let { urlsString ->
                urlsString.split("|").map { urlPair ->
                    val (label, url) = urlPair.split("::")
                    FooterUrl(label = label, url = url)
                }
            },
            enableGuestLogin = bundle.getBoolean(ENABLE_GUEST_LOGIN, false),
            guestLoginButtonLabel = bundle.getString(GUEST_LOGIN_LABEL),
            merchantType = bundle.getString(MERCHANT_TYPE, "custom"),
            createUserConfig = CreateUserConfig(
                isEmailRequired = bundle.getBoolean(IS_EMAIL_REQUIRED, false),
                isNameRequired = bundle.getBoolean(IS_NAME_REQUIRED, false),
                isGenderRequired = bundle.getBoolean(IS_GENDER_REQUIRED, false),
                isDobRequired = bundle.getBoolean(IS_DOB_REQUIRED, false),
                showEmail = bundle.getBoolean(SHOW_EMAIL, false),
                showUserName = bundle.getBoolean(SHOW_USERNAME, false),
                showGender = bundle.getBoolean(SHOW_GENDER, false),
                showDob = bundle.getBoolean(SHOW_DOB, false)
            ),
            inputProps = TextInputConfig(
                submitButtonStyle = bundle.getString(SUBMIT_BUTTON_STYLE)?.let { parseStyleMap(it) },
                inputContainerStyle = bundle.getString(INPUT_CONTAINER_STYLE)?.let { parseStyleMap(it) },
                inputStyle = bundle.getString(INPUT_STYLE)?.let { parseStyleMap(it) },
                titleStyle = bundle.getString(TITLE_STYLE)?.let { parseStyleMap(it) },
                subTitleStyle = bundle.getString(SUBTITLE_STYLE)?.let { parseStyleMap(it) },
                otpPlaceholder = bundle.getString(OTP_PLACEHOLDER),
                phoneAuthScreen = PhoneAuthScreenConfig(
                    title = bundle.getString(PHONE_AUTH_TITLE),
                    subTitle = bundle.getString(PHONE_AUTH_SUBTITLE),
                    phoneNumberPlaceholder = bundle.getString(PHONE_NUMBER_PLACEHOLDER),
                    updatesPlaceholder = bundle.getString(PHONE_UPDATES_PLACEHOLDER),
                    submitButtonText = bundle.getString(PHONE_SUBMIT_BUTTON_TEXT)
                ),
                otpVerificationScreen = OtpVerificationScreenConfig(
                    title = bundle.getString(OTP_VERIFY_TITLE),
                    subTitle = bundle.getString(OTP_VERIFY_SUBTITLE),
                    submitButtonText = bundle.getString(OTP_VERIFY_SUBMIT_TEXT)
                ),
                shopifyEmailScreen = ShopifyEmailScreenConfig(
                    title = bundle.getString(SHOPIFY_EMAIL_TITLE),
                    subTitle = bundle.getString(SHOPIFY_EMAIL_SUBTITLE),
                    emailPlaceholder = bundle.getString(SHOPIFY_EMAIL_PLACEHOLDER),
                    submitButtonText = bundle.getString(SHOPIFY_SUBMIT_TEXT)
                ),
                createUserScreen = CreateUserScreenConfig(
                    title = bundle.getString(CREATE_USER_TITLE),
                    subTitle = bundle.getString(CREATE_USER_SUBTITLE),
                    emailPlaceholder = bundle.getString(CREATE_USER_EMAIL_PLACEHOLDER),
                    namePlaceholder = bundle.getString(CREATE_USER_NAME_PLACEHOLDER),
                    dobPlaceholder = bundle.getString(CREATE_USER_DOB_PLACEHOLDER),
                    genderPlaceholder = bundle.getString(CREATE_USER_GENDER_PLACEHOLDER),
                    submitButtonText = bundle.getString(CREATE_USER_SUBMIT_TEXT),
                    dobFormat = bundle.getString(CREATE_USER_DOB_FORMAT),
                    genderTitle = bundle.getString(CREATE_USER_GENDER_TITLE)
                )
            )
        )
    }

    // Helper function to parse style maps from string
    private fun parseStyleMap(styleString: String): Map<String, Any> {
        return try {
            // Simple string to map parser - you might want to use a proper JSON parser
            styleString.trim('{', '}')
                .split(",")
                .map { it.split(":") }
                .associate { (key, value) ->
                    key.trim() to when {
                        value.trim().startsWith("\"") -> value.trim('"')
                        value.trim() == "true" -> true
                        value.trim() == "false" -> false
                        value.contains(".") -> value.toDouble()
                        else -> value.toInt()
                    }
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        // Configuration Keys
        private const val BANNER_IMAGE = "banner_image"
        private const val LOGO = "logo"
        private const val FOOTER_TEXT = "footer_text"
        private const val FOOTER_URLS = "footer_urls"
        private const val ENABLE_GUEST_LOGIN = "enable_guest_login"
        private const val GUEST_LOGIN_LABEL = "guest_login_label"
        private const val MERCHANT_TYPE = "merchant_type"

        // Create User Config Keys
        private const val IS_EMAIL_REQUIRED = "is_email_required"
        private const val IS_NAME_REQUIRED = "is_name_required"
        private const val IS_GENDER_REQUIRED = "is_gender_required"
        private const val IS_DOB_REQUIRED = "is_dob_required"
        private const val SHOW_EMAIL = "show_email"
        private const val SHOW_USERNAME = "show_username"
        private const val SHOW_GENDER = "show_gender"
        private const val SHOW_DOB = "show_dob"

        // Phone Auth Screen Config Keys
        private const val PHONE_AUTH_TITLE = "phone_auth_title"
        private const val PHONE_AUTH_SUBTITLE = "phone_auth_subtitle"
        private const val PHONE_NUMBER_PLACEHOLDER = "phone_number_placeholder"
        private const val PHONE_UPDATES_PLACEHOLDER = "phone_updates_placeholder"
        private const val PHONE_SUBMIT_BUTTON_TEXT = "phone_submit_button_text"

        // OTP Verification Screen Config Keys
        private const val OTP_VERIFY_TITLE = "otp_verify_title"
        private const val OTP_VERIFY_SUBTITLE = "otp_verify_subtitle"
        private const val OTP_VERIFY_SUBMIT_TEXT = "otp_verify_submit_text"
        private const val OTP_PLACEHOLDER = "otp_placeholder"

        // Shopify Email Screen Config Keys
        private const val SHOPIFY_EMAIL_TITLE = "shopify_email_title"
        private const val SHOPIFY_EMAIL_SUBTITLE = "shopify_email_subtitle"
        private const val SHOPIFY_EMAIL_PLACEHOLDER = "shopify_email_placeholder"
        private const val SHOPIFY_SUBMIT_TEXT = "shopify_submit_text"

        // Create User Screen Config Keys
        private const val CREATE_USER_TITLE = "create_user_title"
        private const val CREATE_USER_SUBTITLE = "create_user_subtitle"
        private const val CREATE_USER_EMAIL_PLACEHOLDER = "create_user_email_placeholder"
        private const val CREATE_USER_NAME_PLACEHOLDER = "create_user_name_placeholder"
        private const val CREATE_USER_DOB_PLACEHOLDER = "create_user_dob_placeholder"
        private const val CREATE_USER_GENDER_PLACEHOLDER = "create_user_gender_placeholder"
        private const val CREATE_USER_SUBMIT_TEXT = "create_user_submit_text"
        private const val CREATE_USER_DOB_FORMAT = "create_user_dob_format"
        private const val CREATE_USER_GENDER_TITLE = "create_user_gender_title"

        // Style Config Keys
        private const val SUBMIT_BUTTON_STYLE = "submit_button_style"
        private const val INPUT_CONTAINER_STYLE = "input_container_style"
        private const val INPUT_STYLE = "input_style"
        private const val TITLE_STYLE = "title_style"
        private const val SUBTITLE_STYLE = "subtitle_style"

        fun newInstance(config: KwikpassConfig, callback: KwikpassCallback): KwikpassLoginFragment {
            return KwikpassLoginFragment().apply {
                this.callback = callback
                arguments = Bundle().apply {
                    putString(BANNER_IMAGE, config?.bannerImage)
                    putString(LOGO, config?.logo)
                    putString(FOOTER_TEXT, config.footerText)
                    config.footerUrls?.let { urls ->
                        val urlsString = urls.joinToString("|") { "${it.label}::${it.url}" }
                        putString(FOOTER_URLS, urlsString)
                    }
                    putBoolean(ENABLE_GUEST_LOGIN, config.enableGuestLogin)
                    putString(GUEST_LOGIN_LABEL, config.guestLoginButtonLabel)
                    putString(MERCHANT_TYPE, config.merchantType)

                    // Create User Config
                    putBoolean(IS_EMAIL_REQUIRED, config.createUserConfig.isEmailRequired)
                    putBoolean(IS_NAME_REQUIRED, config.createUserConfig.isNameRequired)
                    putBoolean(IS_GENDER_REQUIRED, config.createUserConfig.isGenderRequired)
                    putBoolean(IS_DOB_REQUIRED, config.createUserConfig.isDobRequired)
                    putBoolean(SHOW_EMAIL, config.createUserConfig.showEmail)
                    putBoolean(SHOW_USERNAME, config.createUserConfig.showUserName)
                    putBoolean(SHOW_GENDER, config.createUserConfig.showGender)
                    putBoolean(SHOW_DOB, config.createUserConfig.showDob)

                    // Phone Auth Screen Config
                    config.inputProps?.phoneAuthScreen?.let { phoneAuth ->
                        putString(PHONE_AUTH_TITLE, phoneAuth.title)
                        putString(PHONE_AUTH_SUBTITLE, phoneAuth.subTitle)
                        putString(PHONE_NUMBER_PLACEHOLDER, phoneAuth.phoneNumberPlaceholder)
                        putString(PHONE_UPDATES_PLACEHOLDER, phoneAuth.updatesPlaceholder)
                        putString(PHONE_SUBMIT_BUTTON_TEXT, phoneAuth.submitButtonText)
                    }

                    // OTP Verification Screen Config
                    config.inputProps?.otpVerificationScreen?.let { otpVerify ->
                        putString(OTP_VERIFY_TITLE, otpVerify.title)
                        putString(OTP_VERIFY_SUBTITLE, otpVerify.subTitle)
                        putString(OTP_VERIFY_SUBMIT_TEXT, otpVerify.submitButtonText)
                        putString(OTP_PLACEHOLDER, config.inputProps?.otpPlaceholder)
                    }

                    // Shopify Email Screen Config
                    config.inputProps?.shopifyEmailScreen?.let { shopifyEmail ->
                        putString(SHOPIFY_EMAIL_TITLE, shopifyEmail.title)
                        putString(SHOPIFY_EMAIL_SUBTITLE, shopifyEmail.subTitle)
                        putString(SHOPIFY_EMAIL_PLACEHOLDER, shopifyEmail.emailPlaceholder)
                        putString(SHOPIFY_SUBMIT_TEXT, shopifyEmail.submitButtonText)
                    }

                    // Create User Screen Config
                    config.inputProps?.createUserScreen?.let { createUser ->
                        putString(CREATE_USER_TITLE, createUser.title)
                        putString(CREATE_USER_SUBTITLE, createUser.subTitle)
                        putString(CREATE_USER_EMAIL_PLACEHOLDER, createUser.emailPlaceholder)
                        putString(CREATE_USER_NAME_PLACEHOLDER, createUser.namePlaceholder)
                        putString(CREATE_USER_DOB_PLACEHOLDER, createUser.dobPlaceholder)
                        putString(CREATE_USER_GENDER_PLACEHOLDER, createUser.genderPlaceholder)
                        putString(CREATE_USER_SUBMIT_TEXT, createUser.submitButtonText)
                        putString(CREATE_USER_DOB_FORMAT, createUser.dobFormat)
                        putString(CREATE_USER_GENDER_TITLE, createUser.genderTitle)
                    }

                    // Style Configurations
                    config.inputProps?.let { props ->
                        putString(SUBMIT_BUTTON_STYLE, props.submitButtonStyle?.toString())
                        putString(INPUT_CONTAINER_STYLE, props.inputContainerStyle?.toString())
                        putString(INPUT_STYLE, props.inputStyle?.toString())
                        putString(TITLE_STYLE, props.titleStyle?.toString())
                        putString(SUBTITLE_STYLE, props.subTitleStyle?.toString())
                    }
                }
            }
        }
    }
} 