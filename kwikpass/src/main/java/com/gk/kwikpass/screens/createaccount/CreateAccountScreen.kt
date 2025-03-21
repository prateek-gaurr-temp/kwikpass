package com.gk.kwikpass.screens.createaccount

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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalFocusManager

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current

    // Focus requesters for keyboard actions
    val emailFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }

    // Validation functions
    val isEmailValid = remember(email) {
        if (!showEmail) true
        else email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    val isUsernameValid = remember(username) {
        if (!showUserName) true
        else username.isNotEmpty() && username.length >= 3
    }

    val isGenderValid = remember(gender) {
        if (!showGender) true
        else gender.isNotEmpty() && (gender == "Male" || gender == "Female")
    }

    val isDobValid = remember(selectedDate) {
        if (!showDob) true
        else selectedDate != null
    }

    // Date formatter for display
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay()?.toInstant(java.time.ZoneOffset.UTC)?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = LocalDate.ofEpochDay(utcTimeMillis / 86400000)
                    return date.isBefore(LocalDate.now()) && date.isAfter(LocalDate.of(1900, 1, 1))
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = LocalDate.ofEpochDay(millis / 86400000)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

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
                            .focusRequester(emailFocusRequester)
                            .border(
                                width = 1.dp,
                                color = if (errors["email"] != null) Color.Red else Color.Gray,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        placeholder = { Text("Enter your email", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                if (showUserName) {
                                    usernameFocusRequester.requestFocus()
                                }
                            }
                        ),
                        enabled = !isLoading,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
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
                            .focusRequester(usernameFocusRequester)
                            .border(
                                width = 1.dp,
                                color = if (errors["username"] != null) Color.Red else Color.Gray,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        placeholder = { Text("Enter your name", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (isEmailValid && isUsernameValid && isGenderValid && isDobValid) {
                                    onSubmit(
                                        CreateAccountData(
                                            email = if (showEmail) email else "",
                                            username = if (showUserName) username else "",
                                            gender = if (showGender) gender else "",
                                            dob = if (showDob) selectedDate?.format(dateFormatter) ?: "" else ""
                                        )
                                    )
                                }
                            }
                        ),
                        enabled = !isLoading,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showDatePicker = true }
                            .border(
                                width = 1.dp,
                                color = if (errors["dob"] != null) Color.Red else Color.Gray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (selectedDate != null) {
                            Text(
                                text = selectedDate!!.format(dateFormatter),
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                        } else {
                            Text(
                                text = "Select your date of birth",
                                fontSize = 18.sp,
                                color = Color.Gray
                            )
                        }
                    }

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
                        dob = if (showDob) selectedDate?.format(dateFormatter) ?: "" else ""
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
//            enabled = !isLoading && isEmailValid && isUsernameValid && isGenderValid && isDobValid,
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