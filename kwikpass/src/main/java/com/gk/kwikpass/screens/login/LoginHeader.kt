package com.gk.kwikpass.screens.login

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gk.kwikpass.screens.login.ui.theme.KwikpassTheme

// Data class to handle both local and network images
data class ImageSource(
    val localResourceId: Int? = null,
    val networkUrl: String? = null
)

@Composable
fun LoginHeader(
    logo: ImageSource? = null,
    bannerImage: ImageSource? = null,
    enableGuestLogin: Boolean = false,
    guestLoginButtonLabel: String = "Skip",
    onGuestLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Banner/Logo Section
        if (logo != null || bannerImage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (bannerImage != null) 300.dp else 200.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    bannerImage != null -> {
                        if (bannerImage.networkUrl != null) {
                            AsyncImage(
                                model = bannerImage.networkUrl,
                                contentDescription = "Banner Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (bannerImage.localResourceId != null) {
                            Image(
                                painter = painterResource(id = bannerImage.localResourceId),
                                contentDescription = "Banner Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    logo != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp), // 40% of 200dp
                            contentAlignment = Alignment.Center
                        ) {
                            if (logo.networkUrl != null) {
                                AsyncImage(
                                    model = logo.networkUrl,
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else if (logo.localResourceId != null) {
                                Image(
                                    painter = painterResource(id = logo.localResourceId),
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }

        // Skip Button
        if (enableGuestLogin) {
            Button(
                onClick = onGuestLoginClick,
                modifier = Modifier
                    .padding(top = 20.dp, end = 20.dp)
                    .align(Alignment.TopEnd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 6.dp)
            ) {
                Text(
                    text = guestLoginButtonLabel,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginHeaderPreview() {
    KwikpassTheme {
        LoginHeader(
            logo = ImageSource(networkUrl = "https://cdn.freebiesupply.com/logos/large/2x/puma-3-logo-png-transparent.png"),
            bannerImage = ImageSource(networkUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTJOpmCDZYo06eSxeL-dqddnW4xIMu26haXoA&s"),
            enableGuestLogin = true,
            guestLoginButtonLabel = "Skip",
            onGuestLoginClick = {}
        )
    }
}