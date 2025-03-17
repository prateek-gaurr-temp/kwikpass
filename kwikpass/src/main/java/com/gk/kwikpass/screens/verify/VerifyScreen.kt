package com.gk.kwikpass.screens.verify

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
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onResend()
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
            onOtpChange = onOtpChange,
            error = uiState.errors["otp"],
            enabled = !uiState.isLoading,
            modifier = Modifier.padding(vertical = 2.dp)
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
            enabled = true,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007AFF)
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun OtpInput(
    onOtpChange: (String) -> Unit,
    error: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val cellCount = 4
    var otp by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val cellSize = with(LocalDensity.current) {
        (LocalConfiguration.current.screenWidthDp.dp - 100.dp) / 4
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Column {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(cellCount) { index ->
                val isFocused = otp.length == index
                val char = otp.getOrNull(index)?.toString() ?: ""
                val hasError = error != null && otp.length == cellCount

                Box(
                    modifier = Modifier
                        .size(cellSize)
                        .border(
                            width = 2.dp,
                            color = when {
                                hasError -> Color.Red
                                char.isNotEmpty() || isFocused -> Color.Black
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
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )
                    )
                }
            }
        }

        BasicTextField(
            value = otp,
            onValueChange = { newValue ->
                if (newValue.length <= cellCount && newValue.all { it.isDigit() }) {
                    otp = newValue
                    onOtpChange(newValue)
                    if (newValue.length == cellCount) {
                        keyboard?.hide()
                    }
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
            cursorBrush = SolidColor(Color.Transparent)
        )

        error?.let {
            Text(
                text = it,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
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
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OTP not received? ",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )

            val resendText = if (isResendDisabled) {
                "Resend in ${resendSeconds}s"
            } else {
                "Resend OTP"
            }
            
            Text(
                text = resendText,
                fontSize = 14.sp,
                color = Color(0xFF007AFF),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clickable(
                        enabled = !isResendDisabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onResend
                    )
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            )
        }
    }
} 