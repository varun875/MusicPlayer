package com.suspended.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.min

data class ContextMenuOption(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit
)

/**
 * Enhanced card with long-press gesture feedback
 * Features: Scale + elevation increase, haptic pulse, context menu
 *
 * @param onLongPress Callback for long press
 * @param modifier Modifier for the card
 * @param content The card content
 */
@Composable
fun GestureEnhancedCard(
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "card_scale"
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 12f else 4f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "card_elevation"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(dampingRatio = 0.7f),
        label = "card_bg_color"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                        isPressed = false
                    }
                )
            }
    ) {
        content()
    }
}

/**
 * Card with long-press context menu
 *
 * @param options Context menu options to display
 * @param modifier Modifier for the card
 * @param onMenuDismiss Callback when menu is dismissed
 * @param content The card content
 */
@Composable
fun LongPressContextMenuCard(
    options: List<ContextMenuOption>,
    modifier: Modifier = Modifier,
    onMenuDismiss: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var isPressed by remember { mutableStateOf(false) }
    var isMenuVisible by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "menu_card_scale"
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isPressed || isMenuVisible) 12f else 4f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "menu_card_elevation"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed || isMenuVisible) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(dampingRatio = 0.7f),
        label = "menu_card_bg_color"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // `offset` is in pixels — convert to dp with the actual
                        // density, not the mdpi default (offset.x.dp assumes 1).
                        menuOffset = with(density) {
                            DpOffset(offset.x.toDp(), offset.y.toDp())
                        }
                        isMenuVisible = true
                    },
                    onPress = { offset ->
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
    ) {
        content()
        
        // Context menu
        if (isMenuVisible) {
            Box {
                DropdownMenu(
                    expanded = isMenuVisible,
                    onDismissRequest = {
                        isMenuVisible = false
                        onMenuDismiss()
                    },
                    offset = menuOffset
                ) {
                    options.forEachIndexed { index, option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                option.onClick()
                                isMenuVisible = false
                                onMenuDismiss()
                            },
                            leadingIcon = option.icon?.let {
                                {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = MenuDefaults.itemColors()
                        )
                        if (index < options.size - 1) {
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Smooth context menu that appears with fade and slide animation
 *
 * @param items Menu items to display
 * @param onItemClick Callback when menu item is clicked
 * @param isVisible Whether the menu should be visible
 * @param onDismiss Callback when menu is dismissed
 */
@Composable
fun SmoothContextMenu(
    items: List<ContextMenuOption>,
    onItemClick: (Int) -> Unit = {},
    isVisible: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    if (isVisible) {
        DropdownMenu(
            expanded = isVisible,
            onDismissRequest = onDismiss
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        onItemClick(index)
                        item.onClick()
                        onDismiss()
                    },
                    leadingIcon = item.icon?.let {
                        {
                            Icon(
                                imageVector = it,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        }
    }
}
