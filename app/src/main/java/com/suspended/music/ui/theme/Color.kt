package com.suspended.music.ui.theme

import androidx.compose.ui.graphics.Color

// ── Material 3 Expressive palette ──────────────────────────────────────
// The four brand hues are the single source of truth. Every semantic alias
// below references one of these, so re-tuning a hue updates the whole app —
// no more keeping AccentPurple and ExpressivePrimary in sync by hand.

// ── Apple Music palette ────────────────────────────────────────────────
// Primary accent is Apple Music red; secondary is its pink companion.

val ExpressivePrimary = Color(0xFFFA243C)
val ExpressivePrimaryLight = Color(0xFFFF8A9B)
val ExpressivePrimaryDark = Color(0xFFC8102E)
val ExpressivePrimaryContainer = Color(0xFFFFDCE1)
val ExpressiveOnPrimaryContainer = Color(0xFF680014)
val ExpressiveOnPrimary = Color(0xFFFFFFFF)

// Secondary: Apple pink companion
val ExpressiveSecondary = Color(0xFFFC3C44)
val ExpressiveSecondaryLight = Color(0xFFFF9AA2)
val ExpressiveSecondaryDark = Color(0xFFC8102E)
val ExpressiveSecondaryContainer = Color(0xFFFFDCE1)
val ExpressiveOnSecondaryContainer = Color(0xFF680014)
val ExpressiveOnSecondary = Color(0xFFFFFFFF)

// Tertiary: Apple orange for warm accents
val ExpressiveTertiary = Color(0xFFFF9F0A)
val ExpressiveTertiaryLight = Color(0xFFFFD191)
val ExpressiveTertiaryDark = Color(0xFFC77800)
val ExpressiveTertiaryContainer = Color(0xFFFFE9CC)
val ExpressiveOnTertiaryContainer = Color(0xFF663200)
val ExpressiveOnTertiary = Color(0xFFFFFFFF)

// Quaternary: Apple green for accent actions
val ExpressiveQuaternary = Color(0xFF30D158)
val ExpressiveQuaternaryDark = Color(0xFF1F9D43)
val ExpressiveOnQuaternary = Color(0xFF00381F)    // green → dark foreground

// ── Semantic aliases (reference the base hues — do not redefine) ────────
val AccentPurple = ExpressivePrimary
val AccentCyan = ExpressiveSecondary
val AccentOrange = ExpressiveTertiary
val AccentGreen = ExpressiveQuaternary

// Audio quality indicators
val HiResGold = Color(0xFFFFD700)
val LosslessBlue = ExpressiveSecondary
val StandardGray = Color(0xFF6B6B7B)

// Playback state
val PlayingGreen = ExpressiveQuaternary
val PausedOrange = ExpressiveTertiary

// Equalizer bands
val EqLow = ExpressiveSecondary
val EqMid = ExpressiveQuaternary
val EqHigh = ExpressiveTertiary

// ── Surfaces ───────────────────────────────────────────────────────────
// Dark (Apple system blacks)
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF101014)
val DarkSurfaceVariant = Color(0xFF1C1C1E)
val DarkSurfaceElevated = Color(0xFF2C2C2E)

// Dark surfaceContainer* tonal ramp
val DarkSurfaceBright = Color(0xFF3A3A3C)
val DarkSurfaceDim = Color(0xFF000000)
val DarkSurfaceContainerLowest = Color(0xFF0A0A0C)
val DarkSurfaceContainerLow = Color(0xFF101014)
val DarkSurfaceContainer = Color(0xFF1C1C1E)
val DarkSurfaceContainerHigh = Color(0xFF242426)
val DarkSurfaceContainerHighest = Color(0xFF2C2C2E)

// Dark inverse roles (used by e.g. snackbars on the near-black theme)
val DarkInverseSurface = Color(0xFFF2F2F7)
val DarkInverseOnSurface = Color(0xFF1C1C1E)
val DarkInversePrimary = Color(0xFFFA243C)

// Light (Apple system whites/grays)
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF2F2F7)
val LightSurfaceElevated = Color(0xFFE5E5EA)

// Light surfaceContainer* tonal ramp
val LightSurfaceBright = Color(0xFFFFFFFF)
val LightSurfaceDim = Color(0xFFE5E5EA)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF7F7F9)
val LightSurfaceContainer = Color(0xFFF2F2F7)
val LightSurfaceContainerHigh = Color(0xFFEBEBEF)
val LightSurfaceContainerHighest = Color(0xFFE5E5EA)

// ── Text ───────────────────────────────────────────────────────────────
// Dark theme (Apple system grays)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAEAEB2)
val TextTertiary = Color(0xFF8E8E93)

// Light theme (Apple system grays)
val TextPrimaryLight = Color(0xFF000000)
val TextSecondaryLight = Color(0xFF3C3C43)
val TextTertiaryLight = Color(0xFF8E8E93)

// ── Gradients (album art backgrounds) ──────────────────────────────────
val GradientStart = Color(0xFF3A3A3C)
val GradientEnd = Color(0xFF000000)
