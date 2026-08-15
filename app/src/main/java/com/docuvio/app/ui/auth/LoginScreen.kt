package com.docuvio.app.ui.auth

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onNavigateToSignup: () -> Unit,
    onNavigateToQRScanner: () -> Unit,
    onResumeSession: (String) -> Unit
) {
    val viewModel: AuthViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    // 🔥 Reactively observe the saved email and active session
    val savedEmail = remember { viewModel.getSavedEmail() }
    val activeSessionToken = uiState.activeSessionToken

    var email by remember { mutableStateOf(savedEmail ?: "") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onLoginSuccess()
    }
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) Log.d("AUTH", "Error: ${uiState.error}")
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

        // ── TOP: Header — pinned, never moves ──────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -30 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.docuvio_logo_png),
                    contentDescription = "Logo",
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Welcome to",
                        style = MaterialTheme.typography.labelMedium,
                        color = MediumGray
                    )
                    Text(
                        text = "Docuvio",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = AlmostBlack,
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                }
            }
        }

        // ── BOTTOM: Guest option ────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 12.dp),
                color = AlmostBlack.copy(alpha = 0.05f),
                thickness = 1.dp
            )

            Text(
                text = "Just want to print?",
                style = MaterialTheme.typography.titleSmall,
                color = AlmostBlack,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Scan shop QR to start a print session without an account.",
                style = MaterialTheme.typography.bodySmall,
                color = MediumGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // QR Button
                OutlinedButton(
                    onClick = onNavigateToQRScanner,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, PrimaryGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(20.dp))
                    if (activeSessionToken.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text("Scan QR & Print", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }

                // Resume Button (Conditional)
                if (!activeSessionToken.isNullOrBlank()) {
                    Button(
                        onClick = { onResumeSession(activeSessionToken) },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("Resume Session", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // ── CENTER: Login form ──────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, delayMillis = 120)) + slideInVertically(tween(500, delayMillis = 120)) { 20 },
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = AlmostBlack.copy(alpha = 0.08f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(OffWhite)
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.clearError() },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = AlmostBlack.copy(alpha = 0.15f),
                            cursorColor = PrimaryGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.clearError() },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = MediumGray.copy(alpha = 0.6f)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = AlmostBlack.copy(alpha = 0.15f),
                            cursorColor = PrimaryGreen
                        )
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, "https://www.lovelyprints.co.in/forgot-password".toUri())
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = ForestGreen),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Forgot Password?", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                        }
                    }

                    if (uiState.error != null) {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("⚠️", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 6.dp))
                                Text(uiState.error!!, color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.login(email, password) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = Color.White)
                        } else {
                            Text("Sign In", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Don't have an account? ", style = MaterialTheme.typography.bodyMedium, color = MediumGray)
                    TextButton(onClick = onNavigateToSignup, colors = ButtonDefaults.textButtonColors(contentColor = ForestGreen), contentPadding = PaddingValues(0.dp)) {
                        Text("Sign up", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
