package com.suspended.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material 3 Expressive shape system with larger, more bold corners
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Custom shapes for music player components (Expressive)
val AlbumArtShape = RoundedCornerShape(20.dp)
val MiniPlayerShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
val PlayerControlShape = RoundedCornerShape(50)
val CardShape = RoundedCornerShape(16.dp)
val ChipShape = RoundedCornerShape(12.dp)
val FloatingActionButtonShape = RoundedCornerShape(20.dp)
