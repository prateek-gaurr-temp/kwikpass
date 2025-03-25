package com.gk.kwikpass.screens.login

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager

@SuppressLint("RememberReturnType")
@Composable
fun LoginScreen(
    onSubmit: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    title: String? = null,
    subTitle: String? = null,
    errors: Map<String, String> = emptyMap(),
    submitButtonText: String = "Continue",
    isLoading: Boolean = false,
    placeholderText: String = "Enter your phone",
    updateText: String = "Get updates on WhatsApp",
    initialPhoneNumber: String = "",
    initialNotifications: Boolean = true,
    modifier: Modifier = Modifier
) {
    var phoneNumber by remember { mutableStateOf(initialPhoneNumber) }
    var notifications by remember { mutableStateOf(initialNotifications) }
    val focusManager = LocalFocusManager.current

    // Phone number validation
    val isValidPhoneNumber = phoneNumber.length == 10 && phoneNumber.all { it.isDigit() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title
        title?.let {
            Text(
                text = it,
                fontSize = 20.sp,
                color = Color.Black
            )
        }

        subTitle?.let {
            Text(
                text = it,
                fontSize = 18.sp,
                color = Color.Black
            )
        }

        // Phone Input
        Column {
            Box {
                TextField(
                    value = phoneNumber,
                    onValueChange = { 
                        if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                            phoneNumber = it
                            onPhoneChange(it)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (errors["phone"] != null && errors["phone"]?.isNotEmpty() == true) Color.Red else Color.Black,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    placeholder = { 
                        Text(
                            text = placeholderText, 
                            color = Color.Gray.copy(alpha = 0.6f)
                        ) 
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    enabled = !isLoading,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        disabledTextColor = Color.Black,
                        disabledContainerColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        disabledPlaceholderColor = Color.Gray.copy(alpha = 0.6f),
                        cursorColor = Color(0xFF007AFF)
                    ),
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                )

                // Error message
                errors["phone"]?.let { error ->
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 8.dp, bottom = 6.dp)
                    )
                }
            }
        }

        // Notifications checkbox
        Row(
            modifier =  Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { 
                    notifications = !notifications
                    onNotificationsChange(notifications)
                }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(
                        width = 1.5.dp,
                        color = Color.Gray,
                        shape = RoundedCornerShape(5.dp)
                    )
            ) {
                if (notifications) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Gray,
                                shape = RoundedCornerShape(5.dp)
                            )
                    )
                }
            }
            
            Text(
                text = updateText,
                fontSize = 16.sp,
                color = Color.Gray
            )
        }

        // Submit Button
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = true,  // Always keep button enabled
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007AFF)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = submitButtonText,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
    }
}