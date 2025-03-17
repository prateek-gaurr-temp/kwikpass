package com.gk.kwikpass.screens.shopify

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShopifyEmailScreen(
    onSubmit: (String) -> Unit,
    isLoading: Boolean = false,
    errors: Map<String, String> = emptyMap(),
    title: String = "Submit your details",
    subTitle: String? = null,
    emailPlaceholder: String = "Enter your email",
    submitButtonText: String = "Submit"
) {
    var shopifyEmail by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            color = Color.Black
        )

        subTitle?.let {
            Text(
                text = it,
                fontSize = 18.sp,
                color = Color.Black
            )
        }

        // Email field
        Column {
            Box {
                TextField(
                    value = shopifyEmail,
                    onValueChange = { shopifyEmail = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (errors["shopifyEmail"] != null) Color.Red else Color.Gray,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    placeholder = { Text(emailPlaceholder, color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !isLoading,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = TextStyle(fontSize = 18.sp)
                )

                errors["shopifyEmail"]?.let { error ->
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

        // Submit Button
        Button(
            onClick = { onSubmit(shopifyEmail) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading,
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
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
} 