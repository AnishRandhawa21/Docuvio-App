package com.docuvio.app.ui.order.schedulecomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.theme.*

@Composable
fun OrderStepCard(
    stepNumber: Int,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCream)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (enabled) PrimaryGreen else MediumGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stepNumber.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) AlmostBlack else AlmostBlack.copy(alpha = 0.4f)
                )
                subtitle?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = MediumGray
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}