package com.suspended.music.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material 3 baseline shape scale. Functional components (buttons, text fields,
// dialogs, menus, snackbars) inherit these, so they stay restrained; expressive
// surfaces opt in to larger radii through the aliases below.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Full/pill shape — the M3 "full" token. Use for FABs, chips, play controls,
// and any fully rounded surface.
//
// NOTE: this is CircleShape (== RoundedCornerShape(50.percent)). Do NOT write
// RoundedCornerShape(50) — the Int argument binds to `topStartPercent` and
// rounds only one corner, not a pill.
val FullShape = CircleShape

// Apple Music radii: list art 10dp, cards/pills 12dp, player art 16dp.
val AlbumArtShape = RoundedCornerShape(16.dp)
val MiniPlayerShape = RoundedCornerShape(12.dp)
val PlayerControlShape = FullShape
val CardShape = RoundedCornerShape(12.dp)
val ListArtShape = RoundedCornerShape(10.dp)
val ChipShape = RoundedCornerShape(10.dp)
val FloatingActionButtonShape = RoundedCornerShape(16.dp)
