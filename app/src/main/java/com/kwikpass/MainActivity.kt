package com.kwikpass

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gk.kwikpass.initializer.kwikpassInitializer
import com.gk.kwikpass.screens.KwikpassLoginFragment
import com.gk.kwikpass.screens.KwikpassConfig
import com.gk.kwikpass.screens.CreateUserConfig
import com.gk.kwikpass.screens.TextInputConfig
import com.gk.kwikpass.screens.PhoneAuthScreenConfig
import com.gk.kwikpass.screens.KwikpassCallback
import com.gk.kwikpass.screens.FooterUrl
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.gk.kwikpass.screens.OtpVerificationScreenConfig
import kotlinx.coroutines.launch
import android.widget.Toast
import com.gk.kwikpass.screens.CreateUserScreenConfig
import com.gk.kwikpass.api.KwikPassApi
import android.content.SharedPreferences
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {
    private lateinit var loginButton: Button
    private lateinit var kwikPassApi: KwikPassApi
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "KwikpassAppPrefs"
        private const val KEY_USER_LOGGED_IN = "user_logged_in"
        private const val KEY_USER_DATA = "user_data"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_main)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        
        // Initialize Kwikpass in a coroutine
        lifecycleScope.launch {
            try {
                // kwikpassInitializer.initialize(applicationContext, "19g6jle2d5p3n", "sandbox", true)
                kwikpassInitializer.initialize(applicationContext, "cedmlkex5kserdawp", "sandbox", true)
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to initialize Kwikpass: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        }

        loginButton = findViewById(R.id.btnLogin)
//        updateButtonState()

        loginButton.setOnClickListener {
            handleLogin()
//            if (isUserLoggedIn()) {
//                // Handle logout
//                handleLogout()
//            } else {
//                // Handle login
//                handleLogin()
//            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_USER_LOGGED_IN, false)
    }

    private fun updateButtonState() {
        loginButton.text = if (isUserLoggedIn()) "Logout" else "Login"
    }

    private fun handleLogout() {
        lifecycleScope.launch {
            try {
                val result = kwikPassApi.checkout()
                result.fold(
                    onSuccess = {
                        // Clear user data from SharedPreferences
                        sharedPreferences.edit()
                            .remove(KEY_USER_LOGGED_IN)
                            .remove(KEY_USER_DATA)
                            .apply()
                        
                        Toast.makeText(
                            this@MainActivity,
                            "Logged out successfully",
                            Toast.LENGTH_SHORT
                        ).show()
//                        updateButtonState()
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to logout: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error during logout: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun handleLogin() {
        // Hide the login button
        loginButton.visibility = View.GONE

        val config = KwikpassConfig(
            bannerImage = "example",
            footerText = "By continuing, you agree to our",
            footerUrls = listOf(
                FooterUrl(
                    url = "https://example.com/privacy",
                    label = "Privacy Policy"
                ),
                FooterUrl(
                    url = "https://example.com/terms",
                    label = "Terms of Service"
                )
            ),
            enableGuestLogin = true,
            guestLoginButtonLabel = "Skip",
            createUserConfig = CreateUserConfig(
                isEmailRequired = true,
                isNameRequired = true,
                isGenderRequired = false,
                isDobRequired = false,
                showEmail = true,
                showUserName = true,
                showGender = false,
                showDob = false
            ),
            inputProps = TextInputConfig(
                submitButtonStyle = mapOf(
                    "backgroundColor" to "#007AFF",
                    "borderRadius" to 8,
                    "height" to 48
                ),
                inputContainerStyle = mapOf(
                    "marginBottom" to 16,
                    "borderRadius" to 8,
                    "borderWidth" to 1,
                    "borderColor" to "#E5E5E5"
                ),
                inputStyle = mapOf(
                    "fontSize" to 16,
                    "color" to "#000000",
                    "padding" to 12
                ),
                titleStyle = mapOf(
                    "fontSize" to 24,
                    "fontWeight" to "700",
                    "color" to "#000000",
                    "marginBottom" to 8
                ),
                subTitleStyle = mapOf(
                    "fontSize" to 16,
                    "color" to "#666666",
                    "marginBottom" to 24
                ),
                otpPlaceholder = "Enter OTP",
                phoneAuthScreen = PhoneAuthScreenConfig(
                    title = "Login/Signup",
                    subTitle = "Login with your phone number",
                    phoneNumberPlaceholder = "Enter your phone number",
                    updatesPlaceholder = "Get notifications on WhatsApp",
                    submitButtonText = "Continue",
                ),
                otpVerificationScreen = OtpVerificationScreenConfig(
                    title = "Verify OTP",
                    subTitle = "Enter the 4-digit code sent to your phone",
                    submitButtonText = "Verify",
                    loadingText = "please wait..."
                ),
                createUserScreen = CreateUserScreenConfig(
                    title = "Complete Profile setup",
//                    subTitle = "Tell us more about yourself",
                    emailPlaceholder = "Enter your email",
                    namePlaceholder = "Enter your name",
                    dobPlaceholder = "Date of birth",
                    genderPlaceholder = "Select gender",
                    submitButtonText = "Complete",
                    dobFormat = "DD/MM/YYYY",
                    genderTitle = "Gender"
                )
            )
        )

        // Create and show the KwikpassLoginFragment with configuration
        val loginFragment = KwikpassLoginFragment.newInstance(
            config = config,
            callback = object : KwikpassCallback {
                override fun onSuccess(data: MutableMap<String, Any?>?) {
                    println("SUCCESS LOGIN COMPLETE $data")
                    // Store user data in SharedPreferences
                    sharedPreferences.edit()
                        .putBoolean(KEY_USER_LOGGED_IN, true)
                        .putString(KEY_USER_DATA, gson.toJson(data))
                        .apply()
                    
                    // Handle successful login
                    Toast.makeText(
                        this@MainActivity,
                        "Login successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    loginButton.visibility = View.VISIBLE
//                    updateButtonState()
                    supportFragmentManager.popBackStack()
                }

                override fun onError(error: String) {
                    // Handle login error with proper error message
                    Toast.makeText(
                        this@MainActivity,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Optionally show the login button again on critical errors
                    if (error.contains("Failed to initialize") || 
                        error.contains("Network error") ||
                        error.contains("Authentication failed")) {
                        loginButton.visibility = View.VISIBLE
                        supportFragmentManager.popBackStack()
                    }
                }
            }
        )

        // Replace the current fragment container with the login fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, loginFragment)
            .addToBackStack(null)
            .commit()
    }
}
