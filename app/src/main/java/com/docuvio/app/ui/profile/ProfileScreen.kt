package com.docuvio.app.ui.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.BuildConfig
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.theme.*
import com.docuvio.app.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    tokenManager: TokenManager,
    onLogout: () -> Unit,
    onDeleteClick: () -> Unit,
    onFeedbackClick: () -> Unit
) {
    val isLoggingOut = viewModel.isLoggingOut
    val userName by tokenManager.userNameFlow.collectAsState(initial = "User")
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Cream,
            titleContentColor = AlmostBlack,
            textContentColor = AlmostBlack.copy(alpha = 0.7f),
            title = {
                Text(
                    text = "Logout",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to logout? You'll need to sign in again to access your orders.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(onLogout)
                    },
                    enabled = !isLoggingOut,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = AlmostBlack)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        containerColor = Cream,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineLarge,
                    color = AlmostBlack,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Account",
                        tint = CoralRed.copy(alpha = 0.8f)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Flat Profile Header
            ProfileHeader(userName = userName ?: "User")

            Spacer(modifier = Modifier.height(36.dp))

            // Support Section
            ProfileSection(title = "Support") {
                ProfileItem(
                    icon = Icons.Outlined.Feedback,
                    title = "Send Feedback",
                    subtitle = "Help us improve Docuvio",
                    iconColor = Blue,
                    onClick = onFeedbackClick
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Account Section
            ProfileSection(title = "Account") {
                ProfileItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Logout",
                    subtitle = "Sign out of your account",
                    iconColor = CoralRed,
                    showArrow = false,
                    onClick = { showLogoutDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // App Version Footer
            Text(
                text = "Docuvio v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = AlmostBlack.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileHeader(userName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar - Flat design
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.dp, AlmostBlack.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SoftBlue.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = AlmostBlack
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = userName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = AlmostBlack
        )
    }
}

@Composable
fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = AlmostBlack.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.5.dp, AlmostBlack.copy(alpha = 0.08f))
            // Removed shadowElevation for flat look
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun ProfileItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = iconColor.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AlmostBlack
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AlmostBlack.copy(alpha = 0.5f)
            )
        }

        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AlmostBlack.copy(alpha = 0.3f)
            )
        }
    }
}
