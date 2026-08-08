package com.docuvio.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PersonAddAlt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuvio.app.data.model.Organisation
import com.docuvio.app.theme.*
import com.docuvio.app.utils.isValidPassword
import com.docuvio.app.utils.openEmailApp
import com.docuvio.app.viewmodel.AuthViewModel
import com.docuvio.app.viewmodel.AuthViewModelFactory

@Composable
fun SignupScreen(
    viewModelFactory: AuthViewModelFactory,
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val viewModel: AuthViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordTouched by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    val isPasswordValid = isValidPassword(password)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && !showVerifyDialog) {
            showVerifyDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // ── Soft decorative glow shapes ──
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = 90.dp, y = (-90).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(colors = listOf(LightGreen.copy(alpha = 0.35f), Color.Transparent))
                )
                .align(Alignment.TopEnd)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-90).dp, y = 100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(colors = listOf(SoftGreen.copy(alpha = 0.4f), Color.Transparent))
                )
                .align(Alignment.BottomStart)
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
                    // Name field
                    Text(
                        text = "Name",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = AlmostBlack,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            viewModel.clearError()
                        },
                        placeholder = {
                            Text(
                                "Enter your full name",
                                color = MediumGray.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
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

                    // Organisation dropdown
                    Text(
                        text = "Organisation",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = AlmostBlack,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OrganisationDropdown(
                        organisations = uiState.organisations,
                        selectedOrganisation = uiState.selectedOrganisation,
                        onSelect = { viewModel.selectOrganisation(it) },
                        isLoading = uiState.isLoadingOrganisations
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = AlmostBlack,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordTouched = true
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
                        isError = passwordTouched && !isValidPassword(password),
                        supportingText = {
                            if (passwordTouched && !isValidPassword(password)) {
                                Text(
                                    "Password must be at least 8 characters",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC62828)
                                )
                            }
                        },
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
                            unfocusedContainerColor = Color.Transparent,
                            errorBorderColor = Color(0xFFC62828),
                            errorTextColor = AlmostBlack,
                            errorCursorColor = AlmostBlack
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
                            viewModel.signup(
                                name,
                                email,
                                password,
                                uiState.selectedOrganisation!!.id
                            )
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
                        enabled = !uiState.isLoading &&
                                name.isNotBlank() &&
                                email.isNotBlank() &&
                                isPasswordValid &&
                                uiState.selectedOrganisation != null
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Sign Up",
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
                        text = "Already have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediumGray
                    )
                    TextButton(
                        onClick = onNavigateToLogin,
                        colors = ButtonDefaults.textButtonColors(contentColor = ForestGreen),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Sign in",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = {}, // force user decision
            containerColor = Cream,
            title = {
                Text(
                    text = "Verify your email",
                    color = AlmostBlack,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "We’ve sent a verification link to:\n$email\n\n" +
                            "Please check your inbox (and spam) to continue.",
                    color = AlmostBlack,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        openEmailApp(context)
                        showVerifyDialog = false
                        onSignupSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Open email app")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showVerifyDialog = false
                        onSignupSuccess()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AlmostBlack)
                ) {
                    Text("Later")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganisationDropdown(
    organisations: List<Organisation>,
    selectedOrganisation: Organisation?,
    onSelect: (Organisation) -> Unit,
    isLoading: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (expanded) PrimaryGreen else AlmostBlack.copy(alpha = 0.15f)
        ),
        color = Color.Transparent
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if (!isLoading && organisations.isNotEmpty()) {
                    expanded = !expanded
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedOrganisation?.name
                        ?: if (!isLoading && organisations.isEmpty())
                            "No organisations available"
                        else
                            "Select your organisation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedOrganisation != null) AlmostBlack else MediumGray.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryGreen
                    )
                } else {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = AlmostBlack.copy(alpha = 0.6f)
                    )
                }
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(),
                containerColor = OffWhite
            ) {
                organisations.forEach { organisation ->
                    DropdownMenuItem(
                        text = {
                            val isSelected = selectedOrganisation?.id == organisation.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .background(
                                        color = if (isSelected) LightGreen.copy(alpha = 0.25f) else OffWhite,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = organisation.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AlmostBlack
                                )
                            }
                        },
                        onClick = {
                            onSelect(organisation)
                            expanded = false
                        }
                    )
                    if (organisation != organisations.last()) {
                        HorizontalDivider(thickness = 1.dp, color = MediumGray.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}