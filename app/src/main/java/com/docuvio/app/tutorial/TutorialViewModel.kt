package com.docuvio.app.tutorial

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel to bridge the controller and the UI.
 */
class TutorialViewModel(private val controller: TutorialController) : ViewModel() {
    val uiState: StateFlow<TutorialState> = controller.uiState

    fun nextStep() {
        controller.nextStep()
    }

    fun skipTutorial() {
        controller.skipTutorial()
    }
}
