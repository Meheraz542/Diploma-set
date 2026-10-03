package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Warm studio ambient background matching the Liquid Glass reference design
val BgCanvas = Color(0xFFF3EFEA)
val BgCanvasGradientStart = Color(0xFFF8F5F1)
val BgCanvasGradientEnd = Color(0xFFECE7E1)

// Ambient caustics & chromatic blooms
val CausticPurple = Color(0x359378FF)
val CausticCyan = Color(0x354AE4D6)
val CausticPeach = Color(0x25FBA673)
val CausticBlue = Color(0x3060A5FA)

// Liquid Glass Surfaces
val CardGlass = Color(0xFFFFFFFF).copy(alpha = 0.72f)
val CardGlassBorder = Color.White.copy(alpha = 0.85f)
val CardGlassSoft = Color(0xFFFFFFFF).copy(alpha = 0.50f)
val GlassHighlightWhite = Color.White.copy(alpha = 0.90f)

// Primary Purple Liquid Glass (matching "Setvittley" button in reference design)
val PrimaryPurple = Color(0xFF6756F6)
val PrimaryPurpleEnd = Color(0xFF8371FC)
val PrimaryPurpleLight = Color(0xFFEEEAFE)
val PrimaryPurpleGlow = Color(0x556756F6)
val PrimaryPurpleInnerShadow = Color(0x451A1054)

// Secondary Mint / Cyan Liquid Glass (matching "Secondary" button in reference design)
val SecondaryCyan = Color(0xFF5EEAD4)
val SecondaryCyanEnd = Color(0xFF2DD4BF)
val SecondaryCyanGlow = Color(0x4D14B8A6)
val SecondaryCyanInnerShadow = Color(0x380F766E)
val SecondaryCyanText = Color(0xFF0F2926)

// Frosted White / Silver Liquid Glass (matching "Upgrade plan" / "Invite member" in design)
val FrostedWhiteStart = Color(0xFFFFFFFF).copy(alpha = 0.92f)
val FrostedWhiteEnd = Color(0xFFF1F5F9).copy(alpha = 0.75f)
val FrostedWhiteGlow = Color(0x221E293B)
val FrostedWhiteInnerShadow = Color(0x20000000)

// Accent & Amber (matching toggle knob and accents in design)
val AccentBlue = Color(0xFF3B82F6)
val AccentOrange = Color(0xFFF97316)
val AccentOrangeEnd = Color(0xFFFB923C)
val AccentOrangeGlow = Color(0x4DF97316)

// Text tokens
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextMuted = Color(0xFF7E8EA5)

// Status Badges
val BadgeGoodBg = Color(0x3310B981)
val BadgeGoodText = Color(0xFF065F46)
val BadgeLikeNewBg = Color(0x3314B8A6)
val BadgeLikeNewText = Color(0xFF0F766E)
val BadgeUsedBg = Color(0x33F59E0B)
val BadgeUsedText = Color(0xFF92400E)
val BadgeHeavilyUsedBg = Color(0x33EF4444)
val BadgeHeavilyUsedText = Color(0xFF991B1B)

// Neumorphic Shadows
val SoftShadowColor = Color(0x1F1E293B)
val DeepShadowColor = Color(0x2E0F172A)

// Specular Rim & Gradient Brushes
val GlassRimBrush = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.95f),
        Color.White.copy(alpha = 0.40f),
        Color(0x6067E8F9),
        Color(0x60C084FC)
    )
)

val PurpleGradient = Brush.horizontalGradient(
    listOf(PrimaryPurple, PrimaryPurpleEnd)
)

val PurpleButtonBrush = Brush.linearGradient(
    listOf(PrimaryPurple, PrimaryPurpleEnd)
)

val CyanButtonBrush = Brush.linearGradient(
    listOf(Color(0xFF86EFAC).copy(alpha = 0.85f), Color(0xFF5EEAD4).copy(alpha = 0.85f))
)

val FrostedWhiteBrush = Brush.verticalGradient(
    listOf(FrostedWhiteStart, FrostedWhiteEnd)
)


