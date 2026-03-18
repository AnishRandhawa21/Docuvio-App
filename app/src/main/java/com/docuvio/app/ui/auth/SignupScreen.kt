package com.docuvio.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuvio.app.viewmodel.AuthViewModel
import com.docuvio.app.viewmodel.AuthViewModelFactory
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation
import com.docuvio.app.utils.isValidPassword
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import com.docuvio.app.data.model.Organisation
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.docuvio.app.theme.AlmostBlack
import com.docuvio.app.theme.Cream
import com.docuvio.app.theme.MediumGray
import com.docuvio.app.theme.OffWhite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.docuvio.app.theme.Blue
import com.docuvio.app.theme.SoftBlue
import com.docuvio.app.utils.openEmailApp

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Signup heading
            Text(
                text = "Create Your Account",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = AlmostBlack,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Name field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Name",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
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
                            color = MediumGray.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AlmostBlack,
                        unfocusedBorderColor = AlmostBlack.copy(alpha = 0.6f),
                        cursorColor = AlmostBlack,
                        focusedTextColor = AlmostBlack,
                        unfocusedTextColor = AlmostBlack,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Email field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Email",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
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
                            color = MediumGray.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AlmostBlack,
                        unfocusedBorderColor = AlmostBlack.copy(alpha = 0.6f),
                        cursorColor = AlmostBlack,
                        focusedTextColor = AlmostBlack,
                        unfocusedTextColor = AlmostBlack,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Organisation dropdown
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Organisation",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = AlmostBlack,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OrganisationDropdown(
                    organisations = uiState.organisations,
                    selectedOrganisation = uiState.selectedOrganisation,
                    onSelect = { viewModel.selectOrganisation(it) },
                    isLoading = uiState.isLoadingOrganisations
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
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
                            color = MediumGray.copy(alpha = 0.4f),
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
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible }
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = MediumGray.copy(alpha = 0.6f)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AlmostBlack,
                        unfocusedBorderColor = AlmostBlack.copy(alpha = 0.6f),
                        cursorColor = AlmostBlack,
                        focusedTextColor = AlmostBlack,
                        unfocusedTextColor = AlmostBlack,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorBorderColor = Color(0xFFC62828),
                        errorTextColor = AlmostBlack,
                        errorCursorColor = AlmostBlack
                    )
                )
            }

            // Error message
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

            Spacer(modifier = Modifier.height(32.dp))

            // Signup button
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
                    containerColor = AlmostBlack,
                    disabledContainerColor = MediumGray.copy(alpha = 0.4f),
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
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login prompt
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediumGray
                )
                TextButton(
                    onClick = onNavigateToLogin,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Blue
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Sign in",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
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
                        containerColor = Blue,
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
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AlmostBlack
                    )
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
            color = if (expanded) AlmostBlack else AlmostBlack.copy(alpha = 0.6f)
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
                    color = if (selectedOrganisation != null)
                        AlmostBlack
                    else
                        MediumGray.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = AlmostBlack
                    )
                } else {
                    Icon(
                        imageVector = if (expanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
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
            )
            {
                organisations.forEach { organisation ->
                    DropdownMenuItem(
                        text = {
                            val isSelected = selectedOrganisation?.id == organisation.id

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .background(
                                        color = if (isSelected) SoftBlue else OffWhite,
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
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MediumGray.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}