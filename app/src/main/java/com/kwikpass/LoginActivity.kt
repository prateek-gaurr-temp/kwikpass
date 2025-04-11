package com.kwikpass

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gk.kwikpass.screens.KwikpassLoginFragment
import com.gk.kwikpass.screens.KwikpassConfig
import com.gk.kwikpass.screens.CreateUserConfig
import com.gk.kwikpass.screens.TextInputConfig
import com.gk.kwikpass.screens.KwikpassCallback
import com.gk.kwikpass.utils.ModifierWrapper
import android.content.Intent
import com.gk.kwikpass.screens.PhoneAuthScreenConfig

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val config = KwikpassConfig(
            bannerImage = "example",
            logo = "",
            footerText = "By continuing, you agree to our",
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
            bannerImageStyle = ModifierWrapper(
                """
                {
                    "padding": {
                        "top": 0,
                        "bottom": 0
                    },
                    "height": 250,
                    "width": "fill"
                }
            """.trimIndent()
            ),
            imageContainerStyle = ModifierWrapper(
                """
                {
                    "padding": 0,
                    "width": "fill"
                }
            """.trimIndent()
            ),
            inputProps = TextInputConfig(
                phoneAuthScreen = PhoneAuthScreenConfig(
                    title="Login/Signup"
                )
            )
        )

        val loginFragment = KwikpassLoginFragment.newInstance(
            config = config,
            callback = object : KwikpassCallback {
                override fun onGuestLogin() {
                    finish()
                }

                override fun onSuccess(data: MutableMap<String, Any?>?) {
                    val resultIntent = Intent()
                    resultIntent.putExtra("login_success", true)
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }

                override fun onError(error: String) {
                    if (error.contains("Failed to initialize") ||
                        error.contains("Network error") ||
                        error.contains("Authentication failed")
                    ) {
                        finish()
                    }
                }
            }
        )

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, loginFragment)
            .commit()
    }
} 