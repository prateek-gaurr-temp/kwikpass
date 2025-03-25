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
import com.gk.kwikpass.screens.KwikpassCallback
import com.gk.kwikpass.screens.FooterUrl
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast
import com.gk.kwikpass.api.KwikPassApi
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.gk.kwikpass.snowplow.Snowplow
import com.google.gson.Gson
import com.gk.kwikpass.utils.ModifierWrapper

class MainActivity : AppCompatActivity() {
    private lateinit var loginButton: Button
    private lateinit var actionButton: Button
    private lateinit var trackCartButton: Button
    private lateinit var trackCollectionButton: Button
    private lateinit var trackCustomEventButton: Button
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

        // Initialize buttons
        loginButton = findViewById(R.id.btnLogin)
        actionButton = findViewById(R.id.btnAction)
        trackCartButton = findViewById(R.id.btnTrackCart)
        trackCollectionButton = findViewById(R.id.btnTrackCollection)
        trackCustomEventButton = findViewById(R.id.btnTrackCustomEvent)

        // Initialize Kwikpass in a coroutine
        lifecycleScope.launch {
            try {
                kwikpassInitializer.initialize(applicationContext, "12wyqc2guqmkrw6406j", "production", true)
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to initialize Kwikpass: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        }

        loginButton.setOnClickListener {
            handleLogin()
        }

        actionButton.setOnClickListener {
            handleAction()
        }

        trackCartButton.setOnClickListener {
            handleTrackCart()
        }

        trackCollectionButton.setOnClickListener {
            handleTrackCollection()
        }

        trackCustomEventButton.setOnClickListener {
            handleTrackCustomEvent()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun handleTrackCart() {
        lifecycleScope.launch {
            try {
                // Cart event parameters
                val cartParams = Snowplow.TrackCartEventArgs(
                    cart_id = "gid://shopify/Cart/Z2NwLWFzaWEtc291dGhlYXN0MTowMUpIUFFTOVY5RFZSUjlESjVLM01DUEU1NA?key=5cb9ac020696ca061c38d3f8f56b5bb7",
                )

                // Track cart event using Snowplow
                Snowplow.trackCartEvent(cartParams)
                
                Toast.makeText(
                    this@MainActivity,
                    "Cart event tracked successfully",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error in cart event tracking: ${e.message}")
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleTrackCollection() {
        lifecycleScope.launch {
            try {
                // Collection event parameters
                val collectionParams = Snowplow.TrackCollectionsEventArgs(
                    cart_id = "gid://shopify/Cart/Z2NwLWFzaWEtc291dGhlYXN0MTowMUpIUFFTOVY5RFZSUjlESjVLM01DUEU1NA?key=5cb9ac020696ca061c38d3f8f56b5bb7",
                    collection_id = "478302175522",
                    name = "All Products",
                    handle = "all-products"
                )

                // Track collection event using Snowplow
                Snowplow.trackCollectionsEvent(collectionParams)
                
                Toast.makeText(
                    this@MainActivity,
                    "Collection event tracked successfully",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error in collection event tracking: ${e.message}")
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleAction() {
        lifecycleScope.launch {
            try {
                // Product event parameters from provided JSON
                val productParams = Snowplow.TrackProductEventArgs(
                    cart_id = "gid://shopify/Cart/Z2NwLWFzaWEtc291dGhlYXN0MTowMUpIUFFTOVY5RFZSUjlESjVLM01DUEU1NA?key=5cb9ac020696ca061c38d3f8f56b5bb7",
                    product_id = "8141118832930",
                    name = "April Ring",
                    pageUrl = "https://sandbox-plus-gokwik.myshopify.com/products/april-ring-pearl",
                    variant_id = "44554723066146",
                    img_url = "https://cdn.shopify.com/s/files/1/0727/0216/5282/products/2015-04-20_Accessories_32_10022_21392.jpg?v=1677584146",
                    price = "78.00",
                    handle = "april-ring-pearl"
                )

                // Track product event using Snowplow
                Snowplow.trackProductEvent(productParams)
                
                Toast.makeText(
                    this@MainActivity,
                    "Product event tracked successfully",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error in product event tracking: ${e.message}")
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleTrackCustomEvent() {
        lifecycleScope.launch {
            try {
                // Custom event parameters
                val customEventParams = Snowplow.StructuredProps(
                    category = "login_modal",
                    action = "click",
                    label = "open_login_modal"
                )

                // Convert StructuredProps to Map
                val eventMap = mapOf(
                    "category" to customEventParams.category,
                    "action" to customEventParams.action,
                    "label" to customEventParams.label,
                )

                // Track custom event using Snowplow
                Snowplow.sendCustomEventToSnowPlow(eventMap as Map<String, Any>)
                
                Toast.makeText(
                    this@MainActivity,
                    "Custom event tracked successfully",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error in custom event tracking: ${e.message}")
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
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
            logo = "",
            footerText = "By continuing, you agree to our",
//            footerUrls = listOf(
//                FooterUrl(
//                    url = "https://example.com/privacy",
//                    label = "Privacy Policy"
//                ),
//                FooterUrl(
//                    url = "https://example.com/terms",
//                    label = "Terms of Service"
//                )
//            ),
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
            bannerImageStyle = ModifierWrapper("""
                {
                    "padding": {
                        "top": 0,
                        "bottom": 0
                    },
                    "height": 250,
                    "width": "fill"
                }
            """.trimIndent()),
            imageContainerStyle = ModifierWrapper("""
                {
                    "padding": 0,
                    "width": "fill"
                }
            """.trimIndent()),
            inputProps = TextInputConfig(
//                submitButtonStyle = mapOf(
//                    "backgroundColor" to "#007AFF",
//                    "borderRadius" to 8,
//                    "height" to 48
//                ),
//                inputContainerStyle = mapOf(
//                    "marginBottom" to 16,
//                    "borderRadius" to 8,
//                    "borderWidth" to 1,
//                    "borderColor" to "#E5E5E5"
//                ),
//                inputStyle = mapOf(
//                    "fontSize" to 16,
//                    "color" to "#000000",
//                    "padding" to 12
//                ),
//                titleStyle = mapOf(
//                    "fontSize" to 24,
//                    "fontWeight" to "700",
//                    "color" to "#000000",
//                    "marginBottom" to 8
//                ),
//                subTitleStyle = mapOf(
//                    "fontSize" to 16,
//                    "color" to "#666666",
//                    "marginBottom" to 24
//                ),
//                otpPlaceholder = "Enter OTP",
//                phoneAuthScreen = PhoneAuthScreenConfig(
//                    title = "Login/Signup",
//                    subTitle = "Login with your phone number",
//                    phoneNumberPlaceholder = "Enter your phone number",
//                    updatesPlaceholder = "Get notifications on WhatsApp",
//                    submitButtonText = "Continue",
//                ),
//                otpVerificationScreen = OtpVerificationScreenConfig(
//                    title = "Verify OTP",
//                    subTitle = "Enter the 4-digit code sent to your phone",
//                    submitButtonText = "Verify",
//                    loadingText = "please wait..."
//                ),
//                createUserScreen = CreateUserScreenConfig(
//                    title = "Complete Profile setup",
////                    subTitle = "Tell us more about yourself",
//                    emailPlaceholder = "Enter your email",
//                    namePlaceholder = "Enter your name",
//                    dobPlaceholder = "Date of birth",
//                    genderPlaceholder = "Select gender",
//                    submitButtonText = "Complete",
//                    dobFormat = "DD/MM/YYYY",
//                    genderTitle = "Gender"
//                )
            )
        )

        // Create and show the KwikpassLoginFragment with configuration
        val loginFragment = KwikpassLoginFragment.newInstance(
            config = config,
            callback = object : KwikpassCallback {
                override fun onGuestLogin(){
                    supportFragmentManager.popBackStack()
                    loginButton.visibility = View.VISIBLE
                }
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
