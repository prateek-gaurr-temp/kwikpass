package com.gk.kwikpass.screens.shopify

import androidx.compose.foundation.border
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gk.kwikpass.screens.MultipleEmail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopifyEmailScreen(
    onSubmit: (String) -> Unit,
    isLoading: Boolean = false,
    errors: Map<String, String> = emptyMap(),
    title: String = "Submit your details",
    subTitle: String? = null,
    emailPlaceholder: String = "Enter your email",
    submitButtonText: String = "Submit",
    multipleEmail: List<MultipleEmail> = emptyList()
) {
    var shopifyEmail by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedEmail by remember { mutableStateOf<MultipleEmail?>(null) }
    val colorScheme = MaterialTheme.colorScheme

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

        // Email field with dropdown support
        Column {
            Box {
                if (multipleEmail.isNotEmpty()) {
                    // Dropdown menu box
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedEmail?.label ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .border(
                                    width = 1.dp,
                                    color = if (errors["shopifyEmail"] != null) Color.Red else Color.Gray,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            placeholder = { Text(emailPlaceholder, color = Color.Gray) },
                            enabled = !isLoading,
                            trailingIcon = { 
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expanded
                                )
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                disabledTextColor = Color.Black,
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray,
                                disabledPlaceholderColor = Color.Gray,
                                focusedTrailingIconColor = Color.Gray,
                                unfocusedTrailingIconColor = Color.Gray,
                                disabledTrailingIconColor = Color.Gray
                            ),
                            textStyle = TextStyle(fontSize = 18.sp)
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            multipleEmail.forEach { email ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = email.label,
                                            fontSize = 16.sp,
                                            color = colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        selectedEmail = email
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Regular text field
                    TextField(
                        value = shopifyEmail,
                        onValueChange = { shopifyEmail = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (errors["shopifyEmail"] != null) Color.Red else Color.Black,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        placeholder = { Text(emailPlaceholder, color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isValidEmail(shopifyEmail)) {
                                    onSubmit(shopifyEmail)
                                }
                            }
                        ),
                        enabled = !isLoading,
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            disabledTextColor = Color.Black
                        ),
                        textStyle = TextStyle(fontSize = 18.sp)
                    )
                }

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
            onClick = { 
                if (multipleEmail.isNotEmpty()) {
                    selectedEmail?.let { onSubmit(it.value) }
                } else {
                    onSubmit(shopifyEmail)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
//            enabled = !isLoading &&
//                     ((multipleEmail.isNotEmpty() && selectedEmail != null) ||
//                      (multipleEmail.isEmpty() && shopifyEmail.isNotEmpty())),
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

private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}