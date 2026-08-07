package com.docuvio.app.ui.profile

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.theme.*
import okhttp3.*
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb

fun submitDeleteRequest(email: String, username: String, reason: String, onResult: (Boolean) -> Unit) {

    val client = OkHttpClient()

    val formBuilder = FormBody.Builder()
        .add("entry.321931227", email)
        .add("entry.52087131", username)
        .add(
            "entry.1581256086",
            "I confirm that I want to delete my Docuvio account and associated data."
        )

    if (reason.isNotBlank()) {
        formBuilder.add("entry.1095865272", reason)
    }

    val formBody = formBuilder.build()

    val request = Request.Builder()
        .url("https://docs.google.com/forms/d/e/1FAIpQLSeKHJ7sgPc4ap5_8tbkD0eD3KRNOSYwYZcZuxq4qaP885BMzQ/formResponse")
        .post(formBody)
        .addHeader("Content-Type", "application/x-www-form-urlencoded")
        .build()

    client.newCall(request).enqueue(object : Callback {

        override fun onFailure(call: Call, e: java.io.IOException) {
            android.util.Log.e("DELETE_FORM", "Request failed", e)
            onResult(false)
        }

        override fun onResponse(call: Call, response: Response) {

            val body = response.body?.string() ?: ""

            if (body.contains("Your response has been recorded")) {
                android.util.Log.d("DELETE_FORM", "Form submitted successfully")
                onResult(true)
            } else {
                android.util.Log.e("DELETE_FORM", "Form submission failed")
                onResult(false)
            }

            response.close()
        }
    })
}

@Composable
fun DeleteAccountScreen(
    tokenManager: TokenManager,
    activity: Activity,
    onBack: () -> Unit
) {

    val email by tokenManager.userEmailFlow.collectAsState(initial = "")
    val username by tokenManager.userNameFlow.collectAsState(initial = "")

    var reason by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    val window = activity.window

    SideEffect {
        window.statusBarColor = Cream.toArgb()
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true
    }

    DisposableEffect(Unit) {
        onDispose {
            window.statusBarColor = Cream.toArgb()
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Cream
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            /* ---------- HEADER ---------- */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 52.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AlmostBlack.copy(alpha = 0.06f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = AlmostBlack,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Delete Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = AlmostBlack,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "This action is permanent and cannot be undone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkGray.copy(alpha = 0.75f)
                )
            }

            /* ---------- FORM CONTENT ---------- */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp)
            ) {

                /* -- WARNING CARD -- */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CoralRed.copy(alpha = 0.08f))
                        .border(
                            width = 1.dp,
                            color = CoralRed.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CoralRed,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Warning",
                            fontWeight = FontWeight.ExtraBold,
                            color = CoralRed,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Deleting your account will permanently remove your data.",
                            color = AlmostBlack.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                /* -- ACCOUNT INFO SECTION -- */
                SectionHeader(text = "Account Details")
                DeleteSectionCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = email ?: "",
                            onValueChange = {},
                            label = { Text("Email") },
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties { canFocus = false },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = LightGray,
                                disabledContainerColor = SurfaceCream,
                                disabledTextColor = DarkGray,
                                disabledLabelColor = MediumGray
                            ),
                            enabled = false,
                            trailingIcon = {
                                Text(
                                    text = "Auto-filled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MediumGray,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        )

                        OutlinedTextField(
                            value = username ?: "",
                            onValueChange = {},
                            label = { Text("Username") },
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties { canFocus = false },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = LightGray,
                                disabledContainerColor = SurfaceCream,
                                disabledTextColor = DarkGray,
                                disabledLabelColor = MediumGray
                            ),
                            enabled = false,
                            trailingIcon = {
                                Text(
                                    text = "Auto-filled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MediumGray,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                /* -- REASON SECTION -- */
                SectionHeader(text = "Reason for Leaving")
                DeleteSectionCard {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        placeholder = { Text("Tell us why you're leaving (optional)…", color = MediumGray) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LimeGreen,
                            unfocusedBorderColor = LightGray,
                            focusedContainerColor = White,
                            unfocusedContainerColor = SurfaceCream,
                            focusedLabelColor = LimeGreen,
                            cursorColor = LimeGreen
                        )
                    )
                }

                Spacer(Modifier.height(24.dp))

                /* -- CONFIRMATION SECTION -- */
                DeleteSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = confirmed,
                            onCheckedChange = { confirmed = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = DarkGreen,
                                checkmarkColor = White,
                                uncheckedColor = MediumGray
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "I confirm that I want to delete my Docuvio account",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (confirmed) AlmostBlack else DarkGray,
                            fontWeight = if (confirmed) FontWeight.Bold else FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                /* -- SUBMIT BUTTON -- */
                Button(
                    onClick = {
                        loading = true
                        submitDeleteRequest(
                            email ?: "",
                            username ?: "",
                            reason
                        ) { success ->
                            loading = false
                            submitted = success
                        }
                    },
                    enabled = confirmed && !loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralRed,
                        disabledContainerColor = CoralRed.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = "Submit Request",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 0.2.sp,
                            color = White
                        )
                    }
                }

                /* -- SUCCESS MESSAGE -- */
                AnimatedVisibility(
                    visible = submitted,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
                ) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(LimeGreen.copy(alpha = 0.10f))
                            .border(
                                1.dp,
                                LimeGreen.copy(alpha = 0.3f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = LimeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Request submitted successfully.",
                            color = DarkGreen,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/* ---------------- HELPER COMPOSABLES ---------------- */

@Composable
private fun DeleteSectionCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),   // single consistent radius across all cards
        color = White,
        border = BorderStroke(1.dp, AlmostBlack.copy(alpha = 0.06f))
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

// iOS-style grouped-list section header: uppercase, small, muted, left-inset to match card padding
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = DarkGray.copy(alpha = 0.55f),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}