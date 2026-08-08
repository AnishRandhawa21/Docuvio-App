package com.docuvio.app.ui.auth

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuvio.app.R
import com.docuvio.app.theme.*
import com.docuvio.app.viewmodel.AuthViewModel
import com.docuvio.app.viewmodel.AuthViewModelFactory

@Composable
fun LoginScreen(
    viewModelFactory: AuthViewModelFactory,
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit
) {
    val viewModel: AuthViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            Log.d("AUTH", "Error: ${uiState.error}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // ── Soft decorative glow shapes (iOS-style ambient depth) ──
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-100).dp, y = (-80).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(LightGreen.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
                .align(Alignment.TopStart)
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = 100.dp, y = 60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SoftGreen.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
                .align(Alignment.BottomEnd)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                        slideInVertically(tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { -30 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Image(
                        painter = painterResource(id = R.drawable.docuvio_logo_png),
                        contentDescription = "Logo",
                        modifier = Modifier.size(110.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Welcome to",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MediumGray,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Docuvio",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = AlmostBlack,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, delayMillis = 120, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                        slideInVertically(tween(500, delayMillis = 120, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { 40 }
            ) {
                // ── Form card ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = AlmostBlack.copy(alpha = 0.08f)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(OffWhite)
                        .padding(20.dp)
                ) {
                    // Email field
                    Text(
                        text = "Email",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = AlmostBlack,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            viewModel.clearError()
                        },
                        placeholder = {
                            Text(
                                "Enter your email",
                                color = MediumGray.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = AlmostBlack.copy(alpha = 0.15f),
                            cursorColor = PrimaryGreen,
                            focusedTextColor = AlmostBlack,
                            unfocusedTextColor = AlmostBlack,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = AlmostBlack
                        )
                        TextButton(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://www.lovelyprints.co.in/forgot-password".toUri()
                                )
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = ForestGreen),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Forgot?",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
//                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            viewModel.clearError()
                        },
                        placeholder = {
                            Text(
                                "Enter your password",
                                color = MediumGray.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = MediumGray.copy(alpha = 0.6f)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = AlmostBlack.copy(alpha = 0.15f),
                            cursorColor = PrimaryGreen,
                            focusedTextColor = AlmostBlack,
                            unfocusedTextColor = AlmostBlack,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = uiState.error!!,
                                    color = Color(0xFFC62828),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            Log.d("AUTH", "LOGIN BUTTON CLICKED")
                            viewModel.login(email, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen,
                            disabledContainerColor = MediumGray.copy(alpha = 0.3f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 2.dp,
                            disabledElevation = 0.dp
                        ),
                        enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, delayMillis = 220))
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Don't have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediumGray
                    )
                    TextButton(
                        onClick = onNavigateToSignup,
                        colors = ButtonDefaults.textButtonColors(contentColor = ForestGreen),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Sign up",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}