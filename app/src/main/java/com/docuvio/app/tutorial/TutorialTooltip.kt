package com.docuvio.app.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.theme.*

/**
 * Tooltip that appears near the target.
 */
@Composable
fun TutorialTooltip(
    step: TutorialStep,
    targetRect: Rect,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    
    // Determine tooltip position
    // If target is lower than 40% of the screen, show tooltip above to avoid getting cut off by bottom nav.
    val showAbove = targetRect.top > screenHeightPx * 0.4f
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .align(if (showAbove) Alignment.BottomCenter else Alignment.TopCenter)
                .offset(y = if (showAbove) 
                    -with(density) { (screenHeightPx - targetRect.top + 20.dp.toPx()).toDp() }
                    else 
                    with(density) { (targetRect.bottom + 20.dp.toPx()).toDp() }
                )
                .widthIn(max = 300.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = AlmostBlack
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediumGray,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) {
                        Text("Skip", color = MediumGray, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (step.id == "orders") "Finish" else "Next",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
