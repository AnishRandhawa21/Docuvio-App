package com.docuvio.app.tutorial

import androidx.compose.ui.geometry.Rect

/**
 * UI State for the tutorial overlay.
 */
data class TutorialState(
    val isActive: Boolean = false,
    val currentStepIndex: Int = 0,
    val currentStep: TutorialStep? = null,
    val currentTargetRect: Rect? = null,
    val currentRoute: String? = null
)
