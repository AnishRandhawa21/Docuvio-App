package com.docuvio.app.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * CompositionLocal to access the TutorialController.
 */
val LocalTutorialController = staticCompositionLocalOf<TutorialController?> { null }

/**
 * A modifier that registers a UI component as a tutorial target.
 */
@Composable
fun Modifier.tutorialTarget(
    id: String
): Modifier {
    val controller = LocalTutorialController.current ?: return this
    
    return this.onGloballyPositioned { coordinates ->
        if (coordinates.isAttached) {
            val rect = Rect(
                offset = coordinates.positionInRoot(),
                size = Size(
                    coordinates.size.width.toFloat(),
                    coordinates.size.height.toFloat()
                )
            )
            controller.registerTarget(id, rect)
        }
    }
}
