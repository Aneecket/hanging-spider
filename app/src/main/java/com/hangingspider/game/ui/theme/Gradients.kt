package com.hangingspider.game.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppGradients {
    // Auth: radial red-glow from center → deep black. Dramatic hero background.
    val authBackground: Brush get() = Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to AppColors.Bloodstone,
            0.45f to AppColors.Ash,
            1.0f to AppColors.Coal
        )
    )

    // Home: vertical crimson-to-black
    val homeBackground: Brush get() = Brush.verticalGradient(
        colors = listOf(AppColors.Ash, AppColors.Bloodstone, AppColors.Coal)
    )

    // Game: pure dark charcoal for stealth combat mood
    val gameBackground: Brush get() = Brush.verticalGradient(
        colors = listOf(AppColors.Coal, AppColors.Ash, AppColors.Charcoal)
    )

    // Coin card — kept metallic gold because money is universally gold.
    // Slightly deeper tone so it sits on crimson without clashing.
    val goldHero: Brush get() = Brush.linearGradient(
        colors = listOf(AppColors.GoldAntique, AppColors.GoldBright, AppColors.Gold),
        start = Offset(0f, 0f),
        end = Offset(600f, 400f)
    )

    // Primary CTA — crimson gradient (was mystical violet)
    val violetCta: Brush get() = Brush.linearGradient(
        colors = listOf(AppColors.Ember, AppColors.Crimson, AppColors.Signal),
        start = Offset(0f, 0f),
        end = Offset(600f, 200f)
    )

    // Danger vignette overlay for game
    val emberVignette: Brush get() = Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to Color.Transparent,
            0.7f to Color.Transparent,
            1.0f to AppColors.Signal.copy(alpha = 0.18f)
        )
    )

    // Chrome/silver metallic for accent surfaces
    val chromeMetal: Brush get() = Brush.linearGradient(
        colors = listOf(AppColors.ChromeDim, AppColors.ChromeBright, AppColors.Silver),
        start = Offset(0f, 0f),
        end = Offset(500f, 300f)
    )
}
