package com.hangingspider.game.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

private val AppColorScheme = darkColorScheme(
    primary = AppColors.Mystic,
    onPrimary = AppColors.Ivory,
    primaryContainer = AppColors.Violet,
    onPrimaryContainer = AppColors.Ivory,
    secondary = AppColors.Gold,
    onSecondary = AppColors.PlumDeep,
    secondaryContainer = AppColors.GoldAntique,
    onSecondaryContainer = AppColors.PlumDeep,
    tertiary = AppColors.Lavender,
    background = AppColors.PlumDeep,
    onBackground = AppColors.Ivory,
    surface = AppColors.PlumMid,
    onSurface = AppColors.Ivory,
    surfaceVariant = AppColors.Aubergine,
    onSurfaceVariant = AppColors.IvoryDim,
    error = AppColors.Ember,
    outline = AppColors.Lavender,
    outlineVariant = AppColors.MutedText
)

@Composable
fun HangingSpiderTheme(
    background: Brush? = null,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        shapes = AppShapes
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppColors.PlumDeep
        ) {
            if (background != null) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize().background(background)
                ) { content() }
            } else {
                content()
            }
        }
    }
}
