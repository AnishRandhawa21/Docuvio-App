package com.docuvio.app.ui.profile

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.theme.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import okhttp3.*

/* ---------------- GOOGLE FORM SUBMIT FUNCTION ---------------- */

fun submitFeedback(
    rating: String,
    type: String,
    message: String,
    email: String,
    onResult: (Boolean) -> Unit
) {

    val client = OkHttpClient()

    val formBody = FormBody.Builder()
        .add("entry.160507102", rating)
        .add("entry.1228100078", type)
        .add("entry.1379615479", message)
        .add("entry.605999872", email)
        .build()

    val request = Request.Builder()
        .url("https://docs.google.com/forms/d/e/1FAIpQLSeknhasmaYc5dpi8rczO8oRV_JXMFNCwgjpxEmO6qAFll6s-Q/formResponse")
        .post(formBody)
        .addHeader("User-Agent", "Mozilla/5.0")
        .build()

    client.newCall(request).enqueue(object : Callback {

        override fun onFailure(call: Call, e: java.io.IOException) {
            android.util.Log.e("FEEDBACK_FORM", "Request failed", e)
            onResult(false)
        }

        override fun onResponse(call: Call, response: Response) {
            android.util.Log.d("FEEDBACK_FORM", "Form submitted")
            val body = response.body?.string()
            Log.d("FEEDBACK_FORM", "Response: $body")
            onResult(true)
            response.close()
        }
    })
}

/* ---------------- FEEDBACK SCREEN ---------------- */

@Composable
fun FeedbackScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit
) {

    val email by tokenManager.userEmailFlow.collectAsState(initial = "")
    val username by tokenManager.userNameFlow.collectAsState(initial = "")

    var message by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(5) }
    var type by remember { mutableStateOf("General Feedback") }

    var loading by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    // Keep status bar matching cream throughout
    val systemUiController = rememberSystemUiController()
    DisposableEffect(Unit) {
        systemUiController.setStatusBarColor(
            color = Cream,
            darkIcons = true
        )
        onDispose {
            systemUiController.setStatusBarColor(
                color = Cream,
                darkIcons = true
            )
        }
    }

    val options = listOf(
        "Bug / App Crash",
        "Feature Request",
        "Improvement Suggestion",
        "General Feedback"
    )

    val optionEmojis = mapOf(
        "Bug / App Crash" to "🐛",
        "Feature Request" to "✨",
        "Improvement Suggestion" to "💡",
        "General Feedback" to "💬"
    )

    val starLabels = mapOf(
        1 to "Poor",
        2 to "Fair",
        3 to "Good",
        4 to "Great",
        5 to "Excellent"
    )

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
                // Back button — subtle pill on cream
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
                    text = "Share Your Thoughts",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AlmostBlack,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Help us make Docuvio better for you",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AlmostBlack.copy(alpha = 0.45f)
                )

                Spacer(Modifier.height(20.dp))

                // Subtle divider
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

                /* -- STAR RATING SECTION -- */
                SectionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionLabel(text = "Rate your experience")
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..5) {
                                IconButton(
                                    onClick = { rating = i },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (i <= rating)
                                            Icons.Default.Star
                                        else
                                            Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (i <= rating)
                                            Color(0xFFFFC107)
                                        else
                                            Color(0xFFD1CBBD),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = starLabels[rating] ?: "",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFC107)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                /* -- FEEDBACK TYPE SECTION -- */
                SectionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionLabel(text = "Feedback Type")
                        Spacer(Modifier.height(12.dp))

                        options.forEach { option ->
                            val isSelected = type == option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { type = option }
                                    .background(
                                        if (isSelected)
                                            LimeGreen.copy(alpha = 0.12f)
                                        else
                                            Color.Transparent
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected)
                                            LimeGreen.copy(alpha = 0.5f)
                                        else
                                            Color(0xFFE5DFD4),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { type = option },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = LimeGreen,
                                        unselectedColor = Color(0xFFB5AFA5)
                                    )
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${optionEmojis[option]} $option",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) AlmostBlack else Color(0xFF6B6560)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                /* -- MESSAGE SECTION -- */
                SectionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionLabel(text = "Your Message")
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            placeholder = { Text("Describe the issue or suggestion…", color = Color(0xFFB5AFA5)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LimeGreen,
                                unfocusedBorderColor = Color(0xFFE0D9CF),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFFAF8F5)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                /* -- EMAIL SECTION -- */
                SectionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionLabel(text = "Reply To")
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = email ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color(0xFFE0D9CF),
                                disabledContainerColor = Color(0xFFF3F0EB),
                                disabledTextColor = Color(0xFF8A847C)
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

                Spacer(Modifier.height(28.dp))

                /* -- SUBMIT BUTTON -- */
                Button(
                    onClick = {
                        loading = true
                        submitFeedback(
                            rating.toString(),
                            type,
                            message,
                            email ?: ""
                        ) { success ->
                            loading = false
                            submitted = success
                        }
                    },
                    enabled = message.isNotBlank() && !loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeGreen,
                        disabledContainerColor = LimeGreen.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
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
                            "Submit Feedback",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.2.sp
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
                            .background(NewGreen.copy(alpha = 0.10f))
                            .border(
                                1.dp,
                                NewGreen.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NewGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Thanks for your feedback!",
                            color = NewGreen,
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
private fun SectionCard(content: @Composable () -> Unit) {
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF8A847C),
        letterSpacing = 0.4.sp
    )
}