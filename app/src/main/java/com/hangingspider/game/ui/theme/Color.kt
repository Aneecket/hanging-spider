package com.hangingspider.game.ui.theme

import androidx.compose.ui.graphics.Color

// Crimson-noir palette. Marvel-inspired mood (dark red, black, chrome) without
// copying Marvel-specific marks. Gold kept only for coin balances.
object AppColors {
    val Coal          = Color(0xFF07050A)   // near-black
    val Ash           = Color(0xFF14090C)   // near-black with red undertone
    val Ink           = Color(0xFF1A0A0E)   // slightly lighter
    val Charcoal      = Color(0xFF211417)
    val CharcoalMid   = Color(0xFF2B1A1E)

    val Bloodstone    = Color(0xFF3D0A10)   // deep blood red
    val Ember         = Color(0xFF6B1116)   // dark red
    val Crimson       = Color(0xFFB01523)   // main brand red
    val Signal        = Color(0xFFFF1744)   // bright red, for eyes / danger
    val Rose          = Color(0xFFE85555)   // soft red, for secondary text

    val Silver        = Color(0xFFB8BCC4)   // chrome
    val ChromeBright  = Color(0xFFE8EBEF)   // brightest chrome
    val ChromeDim     = Color(0xFF6B6E75)   // muted chrome

    val GoldAntique   = Color(0xFFB8863A)
    val Gold          = Color(0xFFD4A54B)
    val GoldBright    = Color(0xFFFFD54F)   // still used for coin balances

    val Ivory         = Color(0xFFF2ECE4)
    val IvoryDim      = Color(0xFFB8B0A8)
    val MutedText     = Color(0xFF8A7F82)

    // Legacy names kept for old imports (mystical purple set), aliased to
    // approximate new-palette values to avoid touching every screen file.
    val PlumDeep      = Coal
    val PlumMid       = Ash
    val Aubergine     = Charcoal
    val Royal         = Bloodstone
    val Violet        = Ember
    val Mystic        = Crimson
    val Lavender      = Silver
    val CharcoalDeep  = Coal
    val EmberSoft     = Rose
}
