package com.docuvio.app.ui.terms

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.theme.*
import com.docuvio.app.R
@Composable
fun TermsScreen(
    modifier: Modifier = Modifier,
    onAccept: () -> Unit = {}
) {
    var isChecked by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.docuvio_logo_png),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Header text
            Text(
                text = "Terms & Conditions",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = Montserrat,
                    fontSize = 26.sp
                ),
                color = AlmostBlack
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please read and accept to continue",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MediumGray
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Terms card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Policies list
                    val policies = listOf(
                        "Orders, once paid, are final and cannot be cancelled or refunded.",
                        "All uploaded files are subject to automated content screening. Uploading prohibited or inappropriate content may result in immediate account suspension or permanent termination.",
                        "Customers must present the valid pickup OTP at the time of collection.",
                        "Orders must be collected within the selected time slot. Failure to do so may result in delays or cancellation of service.",
                        "Scheduled orders for the next day cannot be placed via the app between 12:00 AM and 6:00 AM. Additional charges will be applied for scheduled orders.",
                        "All orders must be collected on the same day of printing before the closing time of the shop. Uncollected orders will be discarded after the end of the day without prior notice.",
                        "Neither the print shop nor Docuvio shall be liable for any loss, damage, or claims arising from uncollected or discarded orders.",
                        "Print shops are not responsible for errors resulting from incorrect, incomplete, or low-quality files submitted by users.",
                        "Any misuse of the platform, including policy violations or abusive behavior, may lead to suspension or permanent termination of the account.",
                        "Refunds shall be issued solely in the event of verified technical failures attributable to our system."
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(policies) { policy ->
                            PolicyItem(text = policy)
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = Color(0xFFF0F0F0)
                    )

                    // Checkbox row
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF9F9F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isChecked = !isChecked }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { isChecked = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = NewGreen,
                                    uncheckedColor = Color(0xFFD1D5DB),
                                    checkmarkColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "I accept the Terms & Conditions",
                                color = AlmostBlack,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Accept button
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = isChecked,
                        onClick = onAccept,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NewGreen,
                            disabledContainerColor = Color(0xFFC1C1C1),
                            contentColor = Color.White,
                            disabledContentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Accept & Continue",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PolicyItem(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF9F9F9),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, end = 12.dp)
                    .size(6.dp)
                    .background(color = AccentYellow, shape = CircleShape)
            )

            Text(
                text = text,
                color = AlmostBlack,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TermsScreenPreview() {
    TermsScreen()
}
