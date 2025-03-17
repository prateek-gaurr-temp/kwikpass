package com.gk.kwikpass.screens.createaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
fun CreateAccountScreen(
    onSubmit: (CreateAccountData) -> Unit,
    isLoading: Boolean = false,
    errors: Map<String, String> = emptyMap(),
    title: String = "Submit your details",
    subTitle: String? = null,
    showEmail: Boolean = true,
    showUserName: Boolean = true,
    showGender: Boolean = false,
    showDob: Boolean = false
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }

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
        if (showEmail) {
            Column {
                Box {
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (errors["email"] != null) Color.Red else Color.Gray,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        placeholder = { Text("Enter your email", color = Color.Gray) },
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

                    errors["email"]?.let { error ->
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
        }

        // Username field
        if (showUserName) {
            Column {
                Box {
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (errors["username"] != null) Color.Red else Color.Gray,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        placeholder = { Text("Enter your name", color = Color.Gray) },
                        enabled = !isLoading,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = TextStyle(fontSize = 18.sp)
                    )

                    errors["username"]?.let { error ->
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
        }

        // Gender selection
        if (showGender) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Enter your gender",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    listOf("Male", "Female").forEach { option ->
                        Row(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { gender = option },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (gender == option) Color(0xFF007AFF) else Color.Gray,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            ) {
                                if (gender == option) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = Color(0xFF007AFF),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                    )
                                }
                            }
                            
                            Text(
                                text = option,
                                fontSize = 16.sp,
                                color = if (gender == option) Color(0xFF007AFF) else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Date of birth field
        if (showDob) {
            Column {
                Box {
                    TextField(
                        value = dob,
                        onValueChange = { dob = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (errors["dob"] != null) Color.Red else Color.Gray,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        placeholder = { Text("Enter your date of birth", color = Color.Gray) },
                        enabled = !isLoading,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = TextStyle(fontSize = 18.sp)
                    )

                    errors["dob"]?.let { error ->
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
        }

        // Submit Button
        Button(
            onClick = {
                onSubmit(
                    CreateAccountData(
                        email = if (showEmail) email else "",
                        username = if (showUserName) username else "",
                        gender = if (showGender) gender else "",
                        dob = if (showDob) dob else ""
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading && 
                     (!showEmail || email.isNotEmpty()) && 
                     (!showUserName || username.isNotEmpty()) && 
                     (!showGender || gender.isNotEmpty()) && 
                     (!showDob || dob.isNotEmpty()),
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
                    text = "Submit",
                    fontSize = 18.sp,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

data class CreateAccountData(
    val email: String,
    val username: String,
    val gender: String,
    val dob: String
) 