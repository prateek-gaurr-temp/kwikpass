package com.gk.kwikpass.screens.login

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gk.kwikpass.initializer.ApplicationCtx
import com.gk.kwikpass.screens.login.ui.theme.KwikpassTheme
import com.gk.kwikpass.utils.ModifierWrapper
import com.gk.kwikpass.utils.applyStyles

@Composable
fun LoginHeader(
    logo: String? = null,
    bannerImage: String? = null,
    enableGuestLogin: Boolean = false,
    guestLoginButtonLabel: String = "Skip",
    onGuestLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    bannerImageStyle: ModifierWrapper? = null,
    logoStyle: ModifierWrapper? = null,
    imageContainerStyle: ModifierWrapper? = null,
    guestContainerStyle: ModifierWrapper? = null,
    guestButtonContainerColor: Color? = Color.Black,
    guestButtonContentColor: Color? = Color.White
) {

    var appContext = ApplicationCtx.get()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .applyStyles(imageContainerStyle),
        contentAlignment = Alignment.TopCenter
    ) {
        // Banner/Logo Section
        if (logo != null || bannerImage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .applyStyles(bannerImageStyle)
                    .let { mod ->
                        if (bannerImageStyle == null) {
                            mod.then(
                                if (bannerImage != null)
                                    Modifier.height(260.dp)
                                else
                                    Modifier.height(200.dp)
                            ).padding(bottom = 12.dp)
                        } else {
                            mod.padding(bottom = 12.dp)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    bannerImage != null -> {
                        if (bannerImage.startsWith("http")) {
                            AsyncImage(
                                model = bannerImage,
                                contentDescription = "Banner Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        } else {
                            Image(
                                painter = painterResource(id = appContext.resources.getIdentifier(bannerImage, "drawable", appContext.packageName)),
                                contentDescription = "Banner Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        }
                    }
                    logo != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .applyStyles(logoStyle),
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
                    .align(Alignment.TopEnd)
                    .applyStyles(guestContainerStyle),
                colors = ButtonDefaults.buttonColors(
                    containerColor = guestButtonContainerColor ?: Color.Black,
                    contentColor = guestButtonContentColor ?: Color.White
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
