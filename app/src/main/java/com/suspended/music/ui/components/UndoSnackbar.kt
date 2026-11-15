package com.suspended.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Interface for managing undo operations
 */
interface UndoStateManager {
    fun showUndo(
        message: String,
        action: String = "Undo",
        duration: Long = 5000L,
        onUndo: () -> Unit,
        onDismiss: () -> Unit = {}
    )
    fun dismissUndo()
    fun getUndoAction(): State<UndoAction?>
}

data class UndoAction(
    val message: String,
    val action: String = "Undo",
    val duration: Long = 5000L, // 5 seconds default
    val onUndo: () -> Unit,
    val onDismiss: () -> Unit = {}
)

/**
 * Reusable undo snackbar with auto-dismiss
 *
 * @param undoAction The action to perform on undo, with timeout
 * @param modifier Modifier for the snackbar
 */
@Composable
fun UndoSnackbar(
    undoAction: UndoAction?,
    modifier: Modifier = Modifier
) {
    val isVisible = remember(undoAction) { mutableStateOf(false) }
    // Track the latest action — a new undoAction replaces the old one instead
    // of being ignored (the old `remember { undoAction }` captured only the
    // first value forever).
    var currentAction by remember { mutableStateOf<UndoAction?>(null) }
    // (delegate resolved via getValue/setValue imports below)

    LaunchedEffect(undoAction) {
        if (undoAction != null) {
            currentAction = undoAction
            isVisible.value = true

            delay(undoAction.duration)
            isVisible.value = false
            delay(300) // Allow animation to finish
            // Only fire the dismissal callback if this action is still the
            // current one — a replacement action's timer may still be running.
            if (currentAction == undoAction) {
                currentAction = null
                undoAction.onDismiss()
            }
        } else {
            // The host cleared the action (e.g. dismissUndo) — hide promptly
            // instead of lingering until the timeout.
            isVisible.value = false
            currentAction = null
        }
    }
    
    AnimatedVisibility(
        visible = isVisible.value,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        currentAction?.let { action ->
            Snackbar(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                action = {
                    TextButton(onClick = {
                        action.onUndo()
                        isVisible.value = false
                        currentAction = null
                    }) {
                        Text(
                            action.action,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actionOnNewLine = false,
                shape = RoundedCornerShape(8.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                dismissAction = {
                    TextButton(onClick = {
                        isVisible.value = false
                        currentAction = null
                    }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(action.message)
            }
        }
    }
}

/**
 * Composable for managing undo state and snackbar visibility
 * Use this in a screen to handle undo logic
 */
@Composable
fun rememberUndoState(): UndoStateManager {
    val undoAction = remember { mutableStateOf<UndoAction?>(null) }
    return remember {
        object : UndoStateManager {
            override fun showUndo(
                message: String,
                action: String,
                duration: Long,
                onUndo: () -> Unit,
                onDismiss: () -> Unit
            ) {
                undoAction.value = UndoAction(
                    message = message,
                    action = action,
                    duration = duration,
                    onUndo = onUndo,
                    onDismiss = onDismiss
                )
            }
            
            override fun dismissUndo() {
                undoAction.value = null
            }
            
            override fun getUndoAction() = undoAction
        }
    }
}

/**
 * Generic undo manager that can be used in any screen
 * Stores deleted items and provides undo capability
 */
class UndoManager<T> {
    private val deletedItems = mutableListOf<Pair<Int, T>>()
    
    fun markDeleted(index: Int, item: T) {
        deletedItems.add(Pair(index, item))
    }
    
    fun canUndo(): Boolean = deletedItems.isNotEmpty()
    
    fun undo(): Pair<Int, T>? {
        return if (deletedItems.isNotEmpty()) {
            deletedItems.removeAt(deletedItems.lastIndex)
        } else {
            null
        }
    }
    
    fun clearDeleted() {
        deletedItems.clear()
    }
}
