package com.docuvio.app.ui.profile

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*


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
            Log.e("FEEDBACK_FORM", "Request failed", e)
            onResult(false)
        }

        override fun onResponse(call: Call, response: Response) {
            Log.d("FEEDBACK_FORM", "Form submitted")
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

    var message by remember { mutableStateOf("") }
    var rating by remember { mutableIntStateOf(5) }
    var type by remember { mutableStateOf("General Feedback") }

    var loading by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

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

    val optionIcons = mapOf(
        "Bug / App Crash" to Icons.Default.BugReport,
        "Feature Request" to Icons.Default.AutoAwesome,
        "Improvement Suggestion" to Icons.Default.Lightbulb,
        "General Feedback" to Icons.Default.Chat
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

            /* ---------- HEADER ---------- */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
//                IconButton(
//                    onClick = { onBack() },
//                    modifier = Modifier
//                        .size(36.dp)
//                        .clip(CircleShape)
//                        .background(AlmostBlack.copy(alpha = 0.06f))
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.ArrowBack,
//                        contentDescription = "Back",
//                        tint = AlmostBlack,
//                        modifier = Modifier.size(17.dp)
//                    )
//                }
//
//                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Share Your Thoughts",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = AlmostBlack,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Help us make Docuvio better for you",
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

                /* -- STAR RATING SECTION -- */
                SectionHeader(text = "Rate Your Experience")
                SectionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                                            GoldenYellow
                                        else
                                            LightGray,
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
                            fontWeight = FontWeight.Bold,
                            color = GoldenYellow
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                /* -- FEEDBACK TYPE SECTION -- */
                SectionHeader(text = "Feedback Type")
                SectionCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        options.forEach { option ->
                            val isSelected = type == option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
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
                                            LightGray,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { type = option },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = LimeGreen,
                                        unselectedColor = MediumGray
                                    )
                                )
                                Spacer(Modifier.width(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = optionIcons[option]!!,
                                        contentDescription = option,
                                        tint = if (isSelected) AlmostBlack else DarkGray,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) AlmostBlack else DarkGray
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                /* -- MESSAGE SECTION -- */
                SectionHeader(text = "Your Message")
                SectionCard {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = { Text("Describe the issue or suggestion…", color = MediumGray) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LimeGreen,
                            unfocusedBorderColor = LightGray,
                            focusedContainerColor = SurfaceCream,   // was White
                            unfocusedContainerColor = SurfaceCream
                        )
                    )
                }

                Spacer(Modifier.height(24.dp))

                /* -- EMAIL SECTION -- */
                SectionHeader(text = "Reply To")
                SectionCard {
                    OutlinedTextField(
                        value = email ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = LightGray,
                            disabledContainerColor = SurfaceCream,
                            disabledTextColor = DarkGray
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

                Spacer(Modifier.height(32.dp))

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
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            "Submit Feedback",
                            fontWeight = FontWeight.ExtraBold,
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(NewGreen.copy(alpha = 0.10f))
                            .border(
                                1.dp,
                                NewGreen.copy(alpha = 0.3f),
                                RoundedCornerShape(14.dp)
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
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCream,   // was White
        border = BorderStroke(1.dp, AlmostBlack.copy(alpha = 0.06f))
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

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