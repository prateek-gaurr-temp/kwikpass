package com.gk.kwikpass.screens.verify

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gk.kwikpass.smsuserconsent.SmsUserConsentManager
import com.google.android.gms.auth.api.phone.SmsRetriever
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VerifyViewModel : ViewModel() {
    private val TAG = "VerifyViewModel"
    private val _uiState = MutableStateFlow(VerifyUiState())
    val uiState: StateFlow<VerifyUiState> = _uiState.asStateFlow()
    
    private var resendTimer: Job? = null
    private val _smsCode = MutableStateFlow<String>("")
    var smsCode: StateFlow<String> = _smsCode.asStateFlow()

    private var smsManager: SmsUserConsentManager? = null
    private var smsConsentLauncher: ActivityResultLauncher<Intent>? = null

    fun updateSmsCode(code: String) {
        _smsCode.value = code
    }

    fun resetSmsCode() {
        _smsCode.value = ""
    }

    fun resetOtpFlag() {
        _uiState.update { it.copy(shouldResetOtp = false) }
    }

    fun setResetOtpFlag() {
        _uiState.update { it.copy(shouldResetOtp = true) }
    }

    fun initializeSmsManager(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        Log.d(TAG, "Initializing SMS manager")
        smsConsentLauncher = launcher
        smsManager = SmsUserConsentManager(activity, object : SmsUserConsentManager.SmsConsentCallback {
            override fun onSmsReceived(sms: String) {
                Log.d(TAG, "SMS received in callback: $sms")
                viewModelScope.launch {
                    // Extract OTP from SMS (assuming SMS contains 4-digit OTP)
                    val otp = sms.replace(Regex("[^0-9]"), "").take(4)
                    Log.d(TAG, "Extracted OTP: $otp")
                    _smsCode.value = otp
                }
            }

            override fun onError(errorCode: SmsUserConsentManager.ErrorCode, errorMessage: String) {
                Log.e(TAG, "SMS Error: ${errorCode.getCode()} - $errorMessage")
                _uiState.update { it.copy(
                    errors = mapOf("otp" to errorMessage)
                )}
            }
        }, launcher)
    }

    fun startSmsListener() {
        try {
            Log.d(TAG, "Starting SMS listener")
            smsManager?.startSmsListener()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting SMS listener", e)
            _uiState.update { it.copy(
                errors = mapOf("otp" to "Failed to start SMS listener: ${e.message}")
            )}
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "Handling Activity Result - RequestCode: $requestCode, ResultCode: $resultCode")
        if (requestCode == SmsUserConsentManager.SMS_CONSENT_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val sms = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                Log.d(TAG, "SMS from consent dialog: $sms")
                if (sms != null) {
                    // Call handleSms in the SMS manager to trigger the callback
                    smsManager?.handleSms(sms)
                } else {
                    Log.e(TAG, "SMS message is null in activity result")
                    _uiState.update { it.copy(
                        errors = mapOf("otp" to "Failed to retrieve SMS message")
                    )}
                }
            } else {
                Log.e(TAG, "Activity result not OK or data is null - ResultCode: $resultCode")
                _uiState.update { it.copy(
                    errors = mapOf("otp" to "Failed to retrieve SMS message")
                )}
            }
        } else {
            Log.d(TAG, "Ignoring activity result with different request code: $requestCode")
        }
    }

    fun validateOTP(otp: String): Boolean {
        return if (otp.isEmpty()) {
            _uiState.update { it.copy(
                errors = mapOf("otp" to "OTP is required")
            )}
            false
        } else if (!otp.matches(Regex("^[0-9]{4}$"))) {
            _uiState.update { it.copy(
                errors = mapOf("otp" to "Enter a valid OTP")
            )}
            false
        } else {
            _uiState.update { it.copy(errors = emptyMap()) }
            true
        }
    }

    fun startResendTimer() {
        if (_uiState.value.attempts >= _uiState.value.maxAttempts) return

        resendTimer?.cancel()

        _uiState.update { currentState ->
            currentState.copy(
                isResendDisabled = true,
                resendSeconds = 30,
                attempts = currentState.attempts + 1,
                errors = emptyMap()
            )
        }

        resendTimer = viewModelScope.launch {
            try {
                repeat(30) { second ->
                    delay(1000)
                    _uiState.update { currentState ->
                        currentState.copy(
                            resendSeconds = 29 - second
                        )
                    }
                }
                _uiState.update { it.copy(isResendDisabled = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isResendDisabled = false) }
            }
        }
    }

    fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    fun setSuccess(success: Boolean) {
        _uiState.update { it.copy(isSuccess = success) }
    }

    override fun onCleared() {
        super.onCleared()
        resendTimer?.cancel()
    }
}

data class VerifyUiState(
    val isLoading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val resendSeconds: Int = 30,
    val isResendDisabled: Boolean = true,
    val attempts: Int = 0,
    val maxAttempts: Int = 5,
    val shouldResetOtp: Boolean = false,
    val isSuccess: Boolean = false
) 