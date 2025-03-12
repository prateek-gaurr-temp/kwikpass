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
import com.gk.kwikpass.initializer.ApplicationCtx
import com.gk.kwikpass.screens.login.ui.theme.KwikpassTheme

@Composable
fun LoginHeader(
    logo: String? = null,
    bannerImage: String? = null,
    enableGuestLogin: Boolean = false,
    guestLoginButtonLabel: String = "Skip",
    onGuestLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    var appContext = ApplicationCtx.get()

    println(" EXAMPLE DRAWABLE ${appContext.resources.getIdentifier(logo,"drawable",appContext.packageName)}")

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
                        if (bannerImage.startsWith("http")) {
                            AsyncImage(
                                model = bannerImage,
                                contentDescription = "Banner Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else  {
                            Image(
                                painter = painterResource(id = appContext.resources.getIdentifier(bannerImage, "drawable", appContext.packageName)),
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
                            if (logo.startsWith("http")) {
                                AsyncImage(
                                    model = logo,
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else  {

                                Image(
                                    painter = painterResource(id = appContext.resources.getIdentifier(logo,"drawable",appContext.packageName)),
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
            logo = null,
            bannerImage = null,
            enableGuestLogin = true,
            guestLoginButtonLabel = "Skip",
            onGuestLoginClick = {}
        )
    }
}