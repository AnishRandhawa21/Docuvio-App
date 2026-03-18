package com.docuvio.app.ui.profile

import androidx.compose.animation.*
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

    // Keep status bar matching the cream background throughout
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

            /* ---------- HEADER (cream, no dark background) ---------- */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 52.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
            ) {
                // Back button — subtle, pill-shaped
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AlmostBlack.copy(alpha = 0.07f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = AlmostBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Delete Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AlmostBlack,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "This action is permanent and cannot be undone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AlmostBlack.copy(alpha = 0.45f)
                )

                Spacer(Modifier.height(20.dp))

                // Subtle divider replacing the heavy header block
                HorizontalDivider(
                    color = AlmostBlack.copy(alpha = 0.08f),
                    thickness = 1.dp
                )
            }

            /* ---------- FORM CONTENT ---------- */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {

                /* -- WARNING CARD -- */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CoralRed.copy(alpha = 0.08f))
                        .border(
                            width = 1.dp,
                            color = CoralRed.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(14.dp)
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
                            fontWeight = FontWeight.Bold,
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

                Spacer(Modifier.height(20.dp))

                /* -- ACCOUNT INFO SECTION -- */
                DeleteSectionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        DeleteSectionLabel(text = "Account Details")
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = email ?: "",
                            onValueChange = {},
                            label = { Text("Email") },
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties { canFocus = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color(0xFFE0D9CF),
                                disabledContainerColor = Color(0xFFF3F0EB),
                                disabledTextColor = Color(0xFF8A847C),
                                disabledLabelColor = Color(0xFFB5AFA5)
                            ),
                            enabled = false,
                            trailingIcon = {
                                Text(
                                    text = "Auto-filled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB5AFA5),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = username ?: "",
                            onValueChange = {},
                            label = { Text("Username") },
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties { canFocus = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color(0xFFE0D9CF),
                                disabledContainerColor = Color(0xFFF3F0EB),
                                disabledTextColor = Color(0xFF8A847C),
                                disabledLabelColor = Color(0xFFB5AFA5)
                            ),
                            enabled = false,
                            trailingIcon = {
                                Text(
                                    text = "Auto-filled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB5AFA5),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                /* -- REASON SECTION -- */
                DeleteSectionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        DeleteSectionLabel(text = "Reason for Leaving")
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            placeholder = { Text("Tell us why you're leaving (optional)…", color = Color(0xFFB5AFA5)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LimeGreen,
                                unfocusedBorderColor = Color(0xFFE0D9CF),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFFAF8F5),
                                focusedLabelColor = LimeGreen,
                                cursorColor = LimeGreen
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

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
                                checkmarkColor = Color.White,
                                uncheckedColor = Color(0xFFB5AFA5)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "I confirm that I want to delete my Docuvio account",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (confirmed) AlmostBlack else Color(0xFF8A847C),
                            fontWeight = if (confirmed) FontWeight.SemiBold else FontWeight.Normal,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

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
                        disabledContainerColor = CoralRed.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = "Submit Request",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.2.sp,
                            color = Color.White
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(LimeGreen.copy(alpha = 0.10f))
                            .border(
                                1.dp,
                                LimeGreen.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
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
                            fontWeight = FontWeight.SemiBold,
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
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun DeleteSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF8A847C),
        letterSpacing = 0.4.sp
    )
}