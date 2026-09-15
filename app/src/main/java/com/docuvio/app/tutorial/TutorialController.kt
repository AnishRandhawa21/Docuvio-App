package com.docuvio.app.tutorial

import androidx.compose.ui.geometry.Rect
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.ui.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Controller to manage tutorial state, steps, and target registration.
 */
class TutorialController(
    private val tokenManager: TokenManager,
    private val scope: CoroutineScope,
    private val onNavigate: (String) -> Unit
) {
    private val _uiState = MutableStateFlow(TutorialState())
    val uiState: StateFlow<TutorialState> = _uiState.asStateFlow()

    private val targets = mutableMapOf<String, Rect>()

    private val steps = listOf(
        TutorialStep(
            id = "shop_intro",
            title = "Discover Print Shops",
            description = "This is a Shop Card. You can see the shop's name, its location in the college, and whether it's currently open for orders.",
            targetId = "shop_card",
            targetRoute = Routes.Home.route
        ),
        TutorialStep(
            id = "filter",
            title = "Find Online Shops",
            description = "Filter shops that are online and ready to accept your orders instantly.",
            targetId = "filter_row",
            targetRoute = Routes.Home.route
        ),
        TutorialStep(
            id = "qr",
            title = "Quick Scan",
            description = "At a shop? Scan the Docuvio QR code to start an instant live print session.",
            targetId = "qr_fab",
            targetRoute = Routes.Home.route
        ),
        TutorialStep(
            id = "schedule",
            title = "Schedule a Print",
            description = "Want to skip the queue? Schedule a print order and pick it up later at your convenience.",
            targetId = "schedule_button",
            targetRoute = Routes.Home.route,
            interactionType = InteractionType.REAL_INTERACTION
        ),
        TutorialStep(
            id = "upload",
            title = "Upload Document",
            description = "Select the file you want to print. We support PDF, DOCX, and images.",
            targetId = "upload_step",
            targetRoute = "createOrder" // Partial match for route
        ),
        TutorialStep(
            id = "cv_mode",
            title = "CV Mode",
            description = "Printing a resume? Turn on CV Mode to automatically select high-quality bond paper.",
            targetId = "cv_toggle",
            targetRoute = "createOrder"
        ),
        TutorialStep(
            id = "orders",
            title = "Track Orders",
            description = "Keep track of your active orders and see your printing history here.",
            targetId = "orders_tab",
            targetRoute = Routes.Home.route
        )
    )

    init {
        scope.launch {
            if (!tokenManager.isTutorialCompletedBlocking()) {
                startTutorial()
            }
        }
    }

    fun startTutorial() {
        _uiState.value = TutorialState(
            isActive = true,
            currentStepIndex = 0,
            currentStep = steps[0]
        )
    }

    fun registerTarget(id: String, rect: Rect) {
        targets[id] = rect
        val currentStep = _uiState.value.currentStep
        if (currentStep?.targetId == id) {
            _uiState.value = _uiState.value.copy(currentTargetRect = rect)
        }
    }

    fun nextStep() {
        val nextIndex = _uiState.value.currentStepIndex + 1
        if (nextIndex < steps.size) {
            val nextStep = steps[nextIndex]
            
            // Check if we need to navigate
            if (nextStep.targetRoute != null && !_uiState.value.currentRoute?.startsWith(nextStep.targetRoute)!!) {
                 // Navigation handling is complex across screens, for now we assume simple tab switching or direct navigation
                 // In Docuvio, we might need to actually trigger navigation via the callback
                 if (nextStep.id == "orders") {
                     onNavigate(Routes.Orders.route)
                 }
            }

            _uiState.value = _uiState.value.copy(
                currentStepIndex = nextIndex,
                currentStep = nextStep,
                currentTargetRect = targets[nextStep.targetId]
            )
        } else {
            finishTutorial()
        }
    }

    fun skipTutorial() {
        finishTutorial()
    }

    private fun finishTutorial() {
        _uiState.value = TutorialState(isActive = false)
        scope.launch {
            tokenManager.setTutorialCompleted(true)
        }
    }

    fun onRouteChanged(route: String?) {
        _uiState.value = _uiState.value.copy(currentRoute = route)
    }
    
    fun onRealInteraction(targetId: String) {
        val currentStep = _uiState.value.currentStep
        if (currentStep?.targetId == targetId && currentStep.interactionType == InteractionType.REAL_INTERACTION) {
            nextStep()
        }
    }
}
