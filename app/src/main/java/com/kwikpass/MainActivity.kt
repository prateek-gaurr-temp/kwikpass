package com.kwikpass

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.gk.kwikpass.ImageViewModal.ImageViewModel
import com.gk.kwikpass.screens.OtpVerificationScreenConfig
import kotlinx.coroutines.launch
import android.widget.Toast
import android.graphics.Color
import android.view.WindowManager
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {
    private lateinit var loginButton: Button
    private lateinit var imageViewModel: ImageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure window to be edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Make status bar transparent and set light icons
        window.apply {
            statusBarColor = Color.TRANSPARENT
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
        
        // Set status bar icons to dark
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        setContentView(R.layout.activity_main)
        
        // Initialize Kwikpass in a coroutine
        lifecycleScope.launch {
            try {
                kwikpassInitializer.initialize(applicationContext, "19x8g5js05wj", "sandbox", true)
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
        imageViewModel = ViewModelProvider(this)[ImageViewModel::class.java]

        loginButton.setOnClickListener {
            // Hide the login button
            loginButton.visibility = View.GONE

                //bannerImage = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTBol7C9SAVHfOvelDy48J7bHXHuz6CcGLhXA&s",
            val config = KwikpassConfig(
                bannerImage = "example",
                logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQZB1ejyZyAZUGZMPEFq4iHD4YVmlAO7TbUkQ&s",
                footerText = "By continuing, you agree to our",
                footerUrls = listOf(
                    FooterUrl(
                        url = "https://google.com/",
                        label = "Privacy Policy"
                    ),
                    FooterUrl(
                        url = "https://google.com/",
                        label = "Terms of Service"
                    ),
                ),
                enableGuestLogin = true,
                guestLoginButtonLabel = "Continue as Guest",
                merchantType = "custom",
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
                    phoneAuthScreen = PhoneAuthScreenConfig(
                        title = "Login now from here",
//                        subTitle = "Login with your phone number",
                        phoneNumberPlaceholder = "Enter your phone here",
                        submitButtonText = "Continue",
                    ),
                    otpVerificationScreen = OtpVerificationScreenConfig(
                        title = "Enter your code here!",
//                        subTitle = "4 digit unique code",
                        submitButtonText = "Submit Code"
                    )
                )
            )

            // Create and show the KwikpassLoginFragment with configuration
            val loginFragment = KwikpassLoginFragment.newInstance(
                config = config,
                callback = object : KwikpassCallback {
                    override fun onSuccess(data: Any) {
                        println("SUCCESS LOGIN COMPLETE $data")
                        runOnUiThread {
                            // Handle successful login
                            Toast.makeText(
                                this@MainActivity,
                                "Login successful!",
                                Toast.LENGTH_SHORT
                            ).show()
                            loginButton.visibility = View.VISIBLE
                            supportFragmentManager.popBackStack()
                        }
                    }

                    override fun onError(error: String) {
                        runOnUiThread {
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
                }
            )

            // Replace the current fragment container with the login fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, loginFragment)
                .addToBackStack(null)
                .commit()
        }

        // Add back stack change listener to show button when fragment is popped
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                loginButton.visibility = View.VISIBLE
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
