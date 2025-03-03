package com.gk.kwikpass.screens.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VerifyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VerifyUiState())
    val uiState: StateFlow<VerifyUiState> = _uiState.asStateFlow()
    
    private var resendTimer: Job? = null

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
        // Don't start if max attempts reached
        if (_uiState.value.attempts >= _uiState.value.maxAttempts) return

        // Cancel any existing timer
        resendTimer?.cancel()

        // Update state immediately
        _uiState.update { currentState ->
            currentState.copy(
                isResendDisabled = true,
                resendSeconds = 30,
                attempts = currentState.attempts + 1,
                errors = emptyMap()
            )
        }

        // Start new timer
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
                // Enable resend after countdown
                _uiState.update { it.copy(isResendDisabled = false) }
            } catch (e: Exception) {
                // Enable resend if timer fails
                _uiState.update { it.copy(isResendDisabled = false) }
            }
        }
    }

    fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
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
    val maxAttempts: Int = 5
) 