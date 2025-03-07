package com.gk.kwikpass.screens

import LoginViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.gk.kwikpass.screens.login.LoginHeader
import com.gk.kwikpass.screens.login.LoginScreen
import com.gk.kwikpass.screens.verify.VerifyScreen
import com.gk.kwikpass.screens.verify.VerifyViewModel
import com.gk.kwikpass.screens.createaccount.CreateAccountScreen
import com.gk.kwikpass.screens.login.ImageSource
import com.gk.kwikpass.screens.shopify.ShopifyEmailScreen
import com.gk.kwikpass.ui.theme.KwikpassTheme

class KwikpassLoginFragment : Fragment() {
    private val loginViewModel: LoginViewModel by viewModels()
    private val verifyViewModel: VerifyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                KwikpassTheme {
                    KwikpassLoginContent(
                        loginViewModel = loginViewModel,
                        verifyViewModel = verifyViewModel,
                        arguments = arguments
                    )
                }
            }
        }
    }

    companion object {
        const val LOGIN_TITLE = "login_title"
        const val SUBMIT_BUTTON_TEXT = "submit_button_text"

        fun newInstance(title: String? = null, submitButtonText: String? = null): KwikpassLoginFragment {
            return KwikpassLoginFragment().apply {
                arguments = Bundle().apply {
                    putString(LOGIN_TITLE, title)
                    putString(SUBMIT_BUTTON_TEXT, submitButtonText)
                }
            }
        }
    }
}

@Composable
private fun KwikpassLoginContent(
    loginViewModel: LoginViewModel,
    verifyViewModel: VerifyViewModel,
    arguments: Bundle?
) {
    val title = arguments?.getString(KwikpassLoginFragment.LOGIN_TITLE) ?: "Login"
    val submitButtonText = arguments?.getString(KwikpassLoginFragment.SUBMIT_BUTTON_TEXT) ?: "Submit"

    val loginUiState by loginViewModel.uiState.collectAsState()
    val verifyUiState by verifyViewModel.uiState.collectAsState()
    
    // Track screen states
    var showVerifyScreen by remember { mutableStateOf(false) }
    var showCreateAccount by remember { mutableStateOf(false) }
    var showShopifyEmail by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }

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
            LoginHeader(
                logo = ImageSource(networkUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQZB1ejyZyAZUGZMPEFq4iHD4YVmlAO7TbUkQ&s"),
                bannerImage = ImageSource(networkUrl = "https://wiki.hollywoodinpixels.org/images/2/20/Placeholder-Banner.gif"),
                enableGuestLogin = true,
                guestLoginButtonLabel = "Skip",
                onGuestLoginClick = { /* Handle skip */ }
            )

            LoginScreen(
                onSubmit = {
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
                }
            )
        }
    }
} 