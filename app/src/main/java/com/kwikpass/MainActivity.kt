package com.kwikpass

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gk.kwikpass.initializer.kwikpassInitializer
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
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.gk.kwikpass.initializer.ApplicationCtx

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

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val loginSuccess = result.data?.getBooleanExtra("login_success", false) ?: false
            if (loginSuccess) {
                sharedPreferences.edit()
                    .putBoolean(KEY_USER_LOGGED_IN, true)
                    .apply()
                updateButtonState()
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        // Initialize KwikPassApi
        kwikPassApi = KwikPassApi(applicationContext)

        // Initialize buttons
        loginButton = findViewById(R.id.btnLogin)
        actionButton = findViewById(R.id.btnAction)
        trackCartButton = findViewById(R.id.btnTrackCart)
        trackCollectionButton = findViewById(R.id.btnTrackCollection)
        trackCustomEventButton = findViewById(R.id.btnTrackCustomEvent)

        try {
//            kwikpassInitializer.initialize(
//                applicationContext,
//                "19x8g5js05wj",
//                "sandbox",
//                true
//            )
            kwikpassInitializer.initialize(
                applicationContext,
                "12wyqc2guqmkrw6406j",
                "production",
                true
            )
        } catch (e: Exception) {
            Toast.makeText(
                this@MainActivity,
                "Failed to initialize Kwikpass: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }

        loginButton.setOnClickListener {
            if (isUserLoggedIn()) {
                handleLogout()
            } else {
                val intent = Intent(this, LoginActivity::class.java)
                loginLauncher.launch(intent)
            }
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

        updateButtonState()
    }

    private fun handleTrackCart() {
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

    private fun handleTrackCollection() {
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

    private fun handleAction() {
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

    private fun handleTrackCustomEvent() {
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
                        // Clear local preferences
                        sharedPreferences.edit()
                            .remove(KEY_USER_LOGGED_IN)
                            .remove(KEY_USER_DATA)
                            .apply()
                        
                        // Update UI
                        updateButtonState()
                        Toast.makeText(
                            this@MainActivity,
                            "Logged out successfully",
                            Toast.LENGTH_SHORT
                        ).show()
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
}
