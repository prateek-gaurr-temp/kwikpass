package com.gk.kwikpass.screens.verify

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gk.kwikpass.smsuserconsent.SmsUserConsentManager
import kotlinx.coroutines.delay

@Composable
fun VerifyScreen(
    onVerify: () -> Unit,
    onEdit: () -> Unit,
    onResend: () -> Unit,
    otpLabel: String,
    title: String = "OTP Verification",
    subTitle: String? = null,
    submitButtonText: String = "Verify",
    uiState: VerifyUiState,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerifyViewModel = viewModel(),
    resetOtp: Boolean = false,
    currentOtp: String = "",
    loadingText: String = "Signing you in...",
    loadingTextStyle: TextStyle = TextStyle(
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )
) {
    val TAG = "VerifyScreen"
    val context = LocalContext.current
    val activity = context as Activity
    val smsCode by viewModel.smsCode.collectAsState()

    // Reset OTP when screen is first composed or when resetOtp changes
    LaunchedEffect(Unit, resetOtp) {
        if (resetOtp) {
            viewModel.resetSmsCode()
            viewModel.resetOtpFlag()
        }
    }

    // Handle activity result for SMS consent
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Activity result received - ResultCode: ${result.resultCode}")
        viewModel.handleActivityResult(
            SmsUserConsentManager.SMS_CONSENT_REQUEST,
            result.resultCode,
            result.data
        )
    }

    // Initialize SMS manager when the screen is first created
    LaunchedEffect(Unit) {
        Log.d(TAG, "Initializing SMS manager")
        viewModel.initializeSmsManager(activity, launcher)
        viewModel.startSmsListener()
    }

    // Auto-verify when OTP is filled (either from SMS or manual input)
    LaunchedEffect(smsCode, currentOtp) {
        if (smsCode.length == 4 && !uiState.isLoading) {
            Log.d(TAG, "Auto-verifying OTP from SMS: $smsCode")
            onOtpChange(smsCode)  // Update the form state with SMS code
            viewModel.updateSmsCode("")
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            color = Color.Black,
            fontWeight = FontWeight.Medium
        )

        subTitle?.let {
            Text(
                text = it,
                fontSize = 18.sp,
                color = Color.Black
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = otpLabel,
                fontSize = 20.sp,
                color = Color(0xFF999999)
            )
            
            Text(
                text = "Edit",
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEdit
                )
            )
        }

        OtpInput(
            onOtpChange={ newValue ->
                onOtpChange(newValue)
            },
            error = uiState.errors["otp"],
            enabled = true,
            modifier = Modifier.padding(vertical = 2.dp),
            value = currentOtp
        )

        ResendSection(
            attempts = uiState.attempts,
            maxAttempts = uiState.maxAttempts,
            isResendDisabled = uiState.isResendDisabled,
            resendSeconds = uiState.resendSeconds,
            onResend = onResend
        )

        Button(
            onClick = onVerify,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(top = 2.dp),
//            enabled = !uiState.isLoading && !uiState.isSuccess,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007AFF)
            )
        ) {
            when {
                uiState.isSuccess -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = loadingText,
                            style = loadingTextStyle
                        )
                    }
                }
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Text(
                        text = submitButtonText,
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun OtpInput(
    onOtpChange: (String) -> Unit,
    error: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    value: String = ""
) {
    val cellCount = 4
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val cellSize = 60.dp

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(cellCount) { index ->
                val isFocused = value.length == index
                val char = value.getOrNull(index)?.toString() ?: ""
                val hasError = error != null && value.length == cellCount

                Box(
                    modifier = Modifier
                        .size(cellSize)
                        .border(
                            width = 2.dp,
                            color = when {
                                hasError -> Color.Red
                                isFocused -> Color.Black
                                else -> Color.Black.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = enabled) {
                            focusRequester.requestFocus()
                            keyboard?.show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char,
                        style = TextStyle(
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center,
                            color = if (hasError) Color.Red else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.length <= cellCount && newValue.all { it.isDigit() }) {
                    println("VALUE ON OTP CHANGE $newValue")
                    onOtpChange(newValue)
                }
            },
            modifier = Modifier
                .focusRequester(focusRequester)
                .size(1.dp)
                .background(Color.Transparent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            enabled = enabled
        )

        error?.let {
            Text(
                text = it,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ResendSection(
    attempts: Int,
    maxAttempts: Int,
    isResendDisabled: Boolean,
    resendSeconds: Int,
    onResend: () -> Unit
) {
    if (attempts < maxAttempts) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OTP not received? ",
                fontSize = 14.sp,
                color = Color.Black
            )

            val resendText = if (isResendDisabled) {
                "Resend in ${resendSeconds}s"
            } else {
                "Resend OTP"
            }
            
            Text(
                text = resendText,
                fontSize = 14.sp,
                color = Color(0xFF0964C5),
                modifier = Modifier
                    .clickable(
                        enabled = !isResendDisabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onResend
                    )
            )
        }
    }
} 