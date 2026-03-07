package com.suspended.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material 3 Expressive shape scale — bolder, more characterful corners
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Full/pill shape — M3 Expressive "full" token
// Use for FABs, chips, play controls, and any fully rounded surfaces
val FullShape = RoundedCornerShape(50)

// Music player component shapes
val AlbumArtShape = RoundedCornerShape(28.dp)         // Expressive — bold, intentional rounding
val MiniPlayerShape = RoundedCornerShape(20.dp)        // All corners for smooth animation to full player
val PlayerControlShape = FullShape                     // Pill — play/pause, skip buttons
val CardShape = RoundedCornerShape(20.dp)              // Slightly bolder than medium token
val ChipShape = FullShape                              // M3 Expressive chips are fully pill-shaped
val FloatingActionButtonShape = RoundedCornerShape(28.dp) // Matches extraLarge token per M3 Expressive FAB spec