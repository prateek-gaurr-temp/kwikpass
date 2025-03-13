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
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.gk.kwikpass.ImageViewModal.ImageViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var loginButton: Button
    private lateinit var imageViewModel: ImageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        kwikpassInitializer.initialize(applicationContext, "19x8g5js05wj", "sandbox", true)

        loginButton = findViewById(R.id.btnLogin)
        imageViewModel = ViewModelProvider(this)[ImageViewModel::class.java]

        loginButton.setOnClickListener {
            // Hide the login button
            loginButton.visibility = View.GONE
            
            // Create configuration for the login fragment
            imageViewModel.setBannerImage("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTBol7C9SAVHfOvelDy48J7bHXHuz6CcGLhXA&s")
            imageViewModel.setLogo("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQZB1ejyZyAZUGZMPEFq4iHD4YVmlAO7TbUkQ&s")
            val config = KwikpassConfig(
                footerText = "By continuing, you agree to our Terms",
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
                        phoneNumberPlaceholder = "Enter your phone number",
                        submitButtonText = "Sign-In"
                    )
                )
            )

            // Create and show the KwikpassLoginFragment with configuration
            val loginFragment = KwikpassLoginFragment.newInstance(config)

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
