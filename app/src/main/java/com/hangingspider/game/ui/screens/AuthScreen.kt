package com.hangingspider.game.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients
import com.hangingspider.game.viewmodel.AuthViewModel

@Composable
fun AuthScreen(authVm: AuthViewModel, onSignedIn: (String) -> Unit) {
    val state by authVm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.uid) { state.uid?.let(onSignedIn) }

    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradients.authBackground)
    ) {
        // Hex honeycomb texture
        HexBackground(alpha = 0.08f, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(24.dp))

            HeroSpider(pulse = pulse, modifier = Modifier.size(260.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "HANGING\nSPIDER",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 36.sp,
                        lineHeight = 42.sp,
                        letterSpacing = 6.sp
                    ),
                    color = AppColors.ChromeBright,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "unleash the hunter within",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.5.sp
                    ),
                    color = AppColors.Signal.copy(alpha = 0.85f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val err = state.error
                if (err != null) {
                    SignInPill(onClick = { authVm.signIn() })
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = AppColors.Signal.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            err,
                            color = AppColors.Rose,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    CircularProgressIndicator(color = AppColors.Signal)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Preparing your hunt...",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.MutedText
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SignInPill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(AppColors.Ember, AppColors.Crimson, AppColors.Signal)
                )
            )
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                "Retry",
                color = AppColors.Ivory,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
            )
        }
    }
}

/** Aggressive hero spider — abstract silhouette, chrome body, red glow. */
@Composable
private fun HeroSpider(pulse: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h * 0.55f
        val red = AppColors.Signal
        val redDim = AppColors.Crimson
        val body = AppColors.Coal
        val chrome = AppColors.ChromeBright
        val chromeDim = AppColors.Silver

        // Red radial glow behind the spider
        for (i in 0..5) {
            val r = w * (0.30f + i * 0.05f)
            drawCircle(
                red.copy(alpha = (0.10f - i * 0.015f) * pulse),
                radius = r,
                center = Offset(cx, cy)
            )
        }

        // Silk strand from top
        drawLine(chromeDim.copy(alpha = 0.55f), Offset(cx, 0f), Offset(cx, cy - h * 0.20f), 1.6f, StrokeCap.Round)

        // Eight aggressive legs (four per side, angled outward and slightly down)
        // Each leg has a knee bend for a menacing pose.
        val legLen = w * 0.28f
        val legs = listOf(
            -0.9f to -0.35f, -0.55f to -0.05f, -0.20f to 0.20f, 0.15f to 0.45f
        )
        legs.forEachIndexed { i, (bendAngle, drop) ->
            val bendX = kotlin.math.cos(bendAngle) * legLen * 0.55f
            val bendY = kotlin.math.sin(bendAngle) * legLen * 0.55f + drop * legLen
            val endX = kotlin.math.cos(bendAngle + 0.3f) * legLen * 1.15f
            val endY = kotlin.math.sin(bendAngle + 0.3f) * legLen * 1.15f + drop * legLen + 20f

            // Left leg (mirrored)
            drawLine(redDim, Offset(cx - w * 0.05f, cy), Offset(cx - bendX, cy + bendY), 4.5f, StrokeCap.Round)
            drawLine(redDim, Offset(cx - bendX, cy + bendY), Offset(cx - endX, cy + endY), 4.5f, StrokeCap.Round)
            // Right leg
            drawLine(redDim, Offset(cx + w * 0.05f, cy), Offset(cx + bendX, cy + bendY), 4.5f, StrokeCap.Round)
            drawLine(redDim, Offset(cx + bendX, cy + bendY), Offset(cx + endX, cy + endY), 4.5f, StrokeCap.Round)
        }

        // Body — abdomen (large teardrop-ish; use overlapped circles for depth)
        val abd = w * 0.11f
        drawCircle(body, radius = abd, center = Offset(cx, cy + abd * 0.15f))
        drawCircle(redDim.copy(alpha = 0.65f), radius = abd, center = Offset(cx, cy + abd * 0.15f), style = Stroke(width = 2.2f))
        // Red web accent on abdomen (a few crossed lines)
        for (i in -2..2) {
            drawLine(red.copy(alpha = 0.35f),
                start = Offset(cx + i * 6f, cy + abd * 0.15f - abd * 0.85f),
                end = Offset(cx + i * 4f, cy + abd * 0.15f + abd * 0.85f),
                strokeWidth = 0.9f
            )
        }

        // Head — smaller circle above
        val head = w * 0.07f
        drawCircle(body, radius = head, center = Offset(cx, cy - head * 1.3f))
        drawCircle(redDim.copy(alpha = 0.7f), radius = head, center = Offset(cx, cy - head * 1.3f), style = Stroke(width = 2f))

        // Chrome highlight on the head (specular)
        drawCircle(chrome.copy(alpha = 0.20f), radius = head * 0.35f, center = Offset(cx - head * 0.25f, cy - head * 1.5f))

        // Fangs
        drawLine(chromeDim, Offset(cx - 5f, cy - head * 0.35f), Offset(cx - 7f, cy - head * 0.05f), 2.2f, StrokeCap.Round)
        drawLine(chromeDim, Offset(cx + 5f, cy - head * 0.35f), Offset(cx + 7f, cy - head * 0.05f), 2.2f, StrokeCap.Round)

        // Red glowing eyes — main + halo
        val eyeY = cy - head * 1.45f
        listOf(-6f, 6f).forEach { dx ->
            drawCircle(red.copy(alpha = 0.35f * pulse), radius = 8f, center = Offset(cx + dx, eyeY))
            drawCircle(red, radius = 3.2f, center = Offset(cx + dx, eyeY))
        }
    }
}

/** Sparse hex honeycomb overlay for that Spider-Man wallpaper feel. */
@Composable
private fun HexBackground(alpha: Float, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = AppColors.Crimson.copy(alpha = alpha)
        val r = 22f
        val hexH = r * kotlin.math.sqrt(3f)
        val stepX = r * 1.5f
        var row = 0
        var y = -hexH
        while (y < h + hexH) {
            val xOffset = if (row % 2 == 0) 0f else stepX * 0.5f
            var x = -stepX + xOffset - stepX
            while (x < w + stepX) {
                drawHex(Offset(x, y), r, c)
                x += stepX * 2f
            }
            y += hexH * 0.5f
            row++
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHex(
    center: Offset, r: Float, color: Color
) {
    val path = androidx.compose.ui.graphics.Path()
    for (i in 0..5) {
        val angle = (i * 60f) * (Math.PI.toFloat() / 180f)
        val px = center.x + r * kotlin.math.cos(angle)
        val py = center.y + r * kotlin.math.sin(angle)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color, style = Stroke(width = 0.8f))
}
