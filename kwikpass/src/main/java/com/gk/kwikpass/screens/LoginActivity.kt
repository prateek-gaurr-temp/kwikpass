package com.gk.kwikpass

import LoginViewModel
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import com.gk.kwikpass.screens.login.LoginHeader
import com.gk.kwikpass.screens.login.LoginScreen
import com.gk.kwikpass.screens.verify.VerifyScreen
import com.gk.kwikpass.screens.verify.VerifyViewModel
import com.gk.kwikpass.ui.theme.KwikpassTheme
import com.gk.kwikpass.screens.createaccount.CreateAccountScreen
import com.gk.kwikpass.screens.shopify.ShopifyEmailScreen

class LoginActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels()
    private val verifyViewModel: VerifyViewModel by viewModels()

    @SuppressLint("UnrememberedMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra(LOGIN_TITLE) ?: "Login"
        val submitButtonText = intent.getStringExtra(SUBMIT_BUTTON_TEXT) ?: "Submit"

        setContent {
            KwikpassTheme {
                val scrollState = rememberScrollState()
                val focusManager = LocalFocusManager.current
                val loginUiState by loginViewModel.uiState.collectAsState()
                val verifyUiState by verifyViewModel.uiState.collectAsState()
                
                // Track whether to show verify screen
                var showVerifyScreen by mutableStateOf(false)
                var phoneNumber by mutableStateOf("")

                // Add state for tracking create account screen
                var showCreateAccount by remember { mutableStateOf(false) }

                // Add state for tracking shopify email screen
                var showShopifyEmail by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        },
                    color = Color.White // Set surface color to white
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White) // Set column background to white
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .imePadding()
                            .verticalScroll(scrollState)
                    ) {
                        LoginHeader(
//                            logo = ImageSource(networkUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQZB1ejyZyAZUGZMPEFq4iHD4YVmlAO7TbUkQ&s"),
//                            bannerImage = ImageSource(networkUrl = "https://wiki.hollywoodinpixels.org/images/2/20/Placeholder-Banner.gif"),
                            enableGuestLogin = true,
                            guestLoginButtonLabel = "Skip",
                            onGuestLoginClick = { /* Handle skip */ }
                        )

                        when {
                            showShopifyEmail -> {
                                ShopifyEmailScreen(
                                    onSubmit = { email ->
                                        // Handle shopify email submission
                                    },
                                    isLoading = false,
                                    errors = emptyMap()
                                )
                            }
                            showCreateAccount -> {
                                CreateAccountScreen(
                                    onSubmit = { accountData ->
                                        // Navigate to Shopify email screen
                                        showCreateAccount = false
                                        showShopifyEmail = true
                                    },
                                    isLoading = false,
                                    errors = emptyMap()
                                )
                            }
                            showVerifyScreen -> {
                                VerifyScreen(
                                    onVerify = { 
                                        // Show create account screen after successful verification
                                        showCreateAccount = true
                                    },
                                    onEdit = {
                                        showVerifyScreen = false 
                                    },
                                    onResend = {
                                        // Handle resend OTP
                                    },
                                    otpLabel = "+91 $phoneNumber",
                                    title = "OTP Verification",
                                    uiState = verifyUiState,
                                    onOtpChange = { otp ->
                                        verifyViewModel.validateOTP(otp)
                                        // Auto-submit when OTP is complete
                                        if (otp.length == 4) {
                                            showCreateAccount = true
                                        }
                                    }
                                )
                            }
                            else -> {
                                LoginScreen(
                                    onSubmit = {
                                        focusManager.clearFocus()
                                        // Handle submit
                                    },
                                    title = title,
                                    submitButtonText = submitButtonText,
                                    errors = loginUiState.errors,
                                    isLoading = loginUiState.isLoading,
                                    onPhoneChange = { phone ->
                                        phoneNumber = phone
                                        if (phone.length == 10) {
                                            // Auto-submit when phone number is 10 digits
                                            if (loginViewModel.validatePhone(phone)) {
                                                showVerifyScreen = true
                                                // Trigger OTP send here
                                            }
                                        }
                                    },
                                    onNotificationsChange = { enabled ->
                                        // Handle notifications change if needed in activity
                                    },
                                    initialNotifications = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val LOGIN_TITLE = "login_title"
        const val SUBMIT_BUTTON_TEXT = "submit_button_text"

        fun getIntent(context: Context, title: String? = null, submitButtonText: String? = null): Intent {
            return Intent(context, LoginActivity::class.java).apply {
                putExtra(LOGIN_TITLE, title)
                putExtra(SUBMIT_BUTTON_TEXT, submitButtonText)
            }
        }
    }
}