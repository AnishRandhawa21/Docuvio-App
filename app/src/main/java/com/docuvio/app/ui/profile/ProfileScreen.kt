package com.docuvio.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.viewmodel.ProfileViewModel
import com.docuvio.app.theme.AlmostBlack // ADDED: Import theme colors
import com.docuvio.app.theme.Cream // ADDED: Import theme colors
import com.docuvio.app.theme.LimeGreen // ADDED: Import theme colors
import com.docuvio.app.theme.MediumGray // ADDED: Import theme colors
import androidx.compose.foundation.shape.RoundedCornerShape // ADDED: For rounded corners
import com.docuvio.app.theme.CoralRed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext

import com.docuvio.app.theme.DarkBlue
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb
import com.docuvio.app.BuildConfig

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    tokenManager: TokenManager,
    onLogout: () -> Unit,
    onDeleteClick: () -> Unit,
    onFeedbackClick: () -> Unit
) {
    val userName by tokenManager.userNameFlow.collectAsState(initial = "User")
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { },

            // 🔲 popup background
            containerColor = Cream, // CHANGED: from Color(0xFF2B2B2B) to Color.White

            // 📝 text colors
            titleContentColor = AlmostBlack, // CHANGED: from Color.White to AlmostBlack
            textContentColor = MediumGray, // CHANGED: from Color(0xFFCCCCCC) to MediumGray

            title = {
                Text(
                    text = "Logout",
                    color = AlmostBlack, // CHANGED: from Color(0xFFFFFFFF) to AlmostBlack
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Text(
                    text = "Are you sure you want to logout?",
                    color = MediumGray // CHANGED: from Color(0xFF878787) to MediumGray
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.logout(onLogout)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = CoralRed // CHANGED: from Color(0xFFFF9500) to red for logout action
                    )
                ) {
                    Text("Logout")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = { },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AlmostBlack // CHANGED: from Color.White to AlmostBlack
                    )
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp) // CHANGED: Added rounded corners for modern look
        )
    }
    val activity = LocalContext.current as Activity
    val window = activity.window

    SideEffect {
        window.statusBarColor = Cream.toArgb()

        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream) // CHANGED: from gradient dark to Cream solid color
    ) {
        IconButton(
            onClick = {
                onDeleteClick()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Data",
                tint = CoralRed
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 0.dp), // CHANGED: Adjusted padding to match other screens
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp)) // CHANGED: from 32.dp to 48.dp for better spacing

            // Profile Avatar
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(10.dp), // CHANGED: from MaterialTheme.shapes.large to circular
                color = Color(0xFFE8ED95), // CHANGED: from Color(0xFFFFB88F) to SoftYellow
                shadowElevation = 2.dp // ADDED: shadow for depth
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = userName?.firstOrNull()?.uppercase() ?: "U",
                        style = MaterialTheme.typography.displayLarge.copy( // CHANGED: Added bold
                            fontWeight = FontWeight.Bold
                        ),
                        color = LimeGreen // CHANGED: from Color(0xFFFF8840) to DeepAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Username
            Text(
                text = userName ?: "User",
                style = MaterialTheme.typography.headlineMedium.copy( // CHANGED: Added bold
                    fontWeight = FontWeight.Bold
                ),
                color = AlmostBlack // CHANGED: from Color(0xFFFFFFFF) to AlmostBlack
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Account Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White // CHANGED: from Color(0xFF363636) to Color.White
                ),
                shape = RoundedCornerShape(16.dp), // CHANGED: Added rounded corners
                elevation = CardDefaults.cardElevation(4.dp) // CHANGED: Added elevation
            ) {
                Column(modifier = Modifier.padding(20.dp)) { // CHANGED: from 16.dp to 20.dp
                    Text(
                        text = "Account Settings",
                        style = MaterialTheme.typography.titleMedium.copy( // CHANGED: Added bold
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = AlmostBlack // CHANGED: from Color(0xFFFFFFFF) to AlmostBlack
                    )

                    HorizontalDivider( // CHANGED: from Divider to HorizontalDivider
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MediumGray.copy(alpha = 0.3f), // CHANGED: from Color(0xFF878787) to MediumGray with transparency
                        thickness = 1.dp // ADDED: explicit thickness
                    )

                    // Logout Button
                    Button( // CHANGED: from TextButton to Button for better visual
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp), // ADDED: fixed height
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEBEE), // CHANGED: Light red background
                            contentColor = CoralRed, // CHANGED: Red text for logout
                            disabledContainerColor = MediumGray.copy(alpha = 0.2f),
                            disabledContentColor = MediumGray
                        ),
                        shape = RoundedCornerShape(12.dp) // ADDED: rounded corners
                    ) {
                        Text(
                            text = "Logout",
                            style = MaterialTheme.typography.titleSmall.copy( // CHANGED: Better text style
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onFeedbackClick() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE3F2FD), // light green
                            contentColor = DarkBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Send Feedback",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Version text
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MediumGray, // CHANGED: from Color(0xFF878787) to MediumGray
                modifier = Modifier.padding(bottom = 16.dp) // ADDED: bottom padding
            )
        }
    }
}

