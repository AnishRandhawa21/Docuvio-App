package com.docuvio.app.tutorial

/**
 * Defines a single step in the tutorial.
 */
data class TutorialStep(
    val id: String,
    val title: String,
    val description: String,
    val targetId: String,
    val targetRoute: String? = null,
    val interactionType: InteractionType = InteractionType.NEXT_BUTTON
)

enum class InteractionType {
    NEXT_BUTTON,
    REAL_INTERACTION
}
