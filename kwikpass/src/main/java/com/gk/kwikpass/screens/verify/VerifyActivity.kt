package com.gk.kwikpass.screens.verify

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VerifyActivity : ComponentActivity() {
    private val viewModel: VerifyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            VerifyScreen(
                onVerify = { /* Handle verification */ },
                onEdit = { /* Handle edit */ },
                onResend = {
                    lifecycleScope.launch {
                        try {
                            // Start timer first
                            viewModel.startResendTimer()
                            
                            // Then make API call
                            // api.resendOTP()
                        } catch (e: Exception) {
                            // Handle error
                        }
                    }
                },
                otpLabel = "+91 ${intent.getStringExtra(PHONE_NUMBER)}",
                title = "OTP Verification",
                uiState = uiState,
                onOtpChange = { otp ->
                    viewModel.validateOTP(otp)
                }
            )
        }
    }

    companion object {
        const val OTP_LABEL = "otp_label"
        const val TITLE = "title"
        const val PHONE_NUMBER = "phone_number"

        fun getIntent(context: Context, otpLabel: String? = null, title: String? = null): Intent {
            return Intent(context, VerifyActivity::class.java).apply {
                putExtra(OTP_LABEL, otpLabel)
                putExtra(TITLE, title)
            }
        }
    }
} 