package com.docuvio.app.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.docuvio.app.theme.AlmostBlack

/**
 * The main tutorial overlay component.
 */
@Composable
fun TutorialOverlay(
    viewModel: TutorialViewModel,
    content: @Composable () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        
        if (uiState.isActive && uiState.currentTargetRect != null) {
            val rect = uiState.currentTargetRect!!
            
            // Scrim with hole
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.99f)
                    .pointerInput(uiState.currentStep) {
                        detectTapGestures { }
                    }
            ) {
                drawRect(color = AlmostBlack.copy(alpha = 0.7f))
                
                // Draw the highlight "hole"
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(rect.left - 8.dp.toPx(), rect.top - 8.dp.toPx()),
                    size = Size(
                        rect.width + 16.dp.toPx(),
                        rect.height + 16.dp.toPx()
                    ),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }
            
            // Tooltip
            TutorialTooltip(
                step = uiState.currentStep!!,
                targetRect = rect,
                onNext = { viewModel.nextStep() },
                onSkip = { viewModel.skipTutorial() }
            )
        }
    }
}
