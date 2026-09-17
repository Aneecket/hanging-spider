package com.hangingspider.game.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients

object GameColors {
    val Correct = Color(0xFF3E9B4F)
    val Present = Color(0xFFC49A2C)
    val Absent = Color(0xFF55494D)
    val Tile = AppColors.Charcoal
    val TileSelected = AppColors.Crimson
    val Found = Color(0xFF2F7D5B)
    val Groups = listOf(Color(0xFFC49A2C), Color(0xFF3E9B4F), Color(0xFF3F6FB5), Color(0xFF8E4FB5))
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) AppGradients.violetCta else SolidColor(AppColors.CharcoalMid))
    ) {
        TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.align(Alignment.Center)) {
            Text(
                text,
                color = if (enabled) AppColors.Ivory else AppColors.MutedText,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Charcoal)
    ) {
        TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.align(Alignment.Center)) {
            Text(
                text,
                color = if (enabled) AppColors.Silver else AppColors.MutedText,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LetterTile(
    text: String,
    size: Dp,
    background: Color,
    modifier: Modifier = Modifier,
    textColor: Color = AppColors.Ivory,
    fontScale: Float = 0.5f
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.2f))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = textColor,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * fontScale).sp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * On-screen QWERTY keyboard. [keyColor] lets games tint keys (e.g. Five Letters feedback);
 * [onEnter] / [onDelete] add the extra keys to the bottom row when provided.
 */
@Composable
fun GameKeyboard(
    onLetter: (Char) -> Unit,
    modifier: Modifier = Modifier,
    disabled: Set<Char> = emptySet(),
    keyColor: (Char) -> Color? = { null },
    onEnter: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap = 3.dp
        val keyWidth = ((maxWidth - gap * 9) / 10).coerceAtLeast(24.dp)
        val keyHeight = keyWidth * 1.3f
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            rows.forEachIndexed { index, row ->
                Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    if (index == 2 && onEnter != null) {
                        WideKey("ENTER", keyWidth * 1.5f, keyHeight, onEnter)
                    }
                    row.forEach { c ->
                        val off = c in disabled
                        val tint = keyColor(c)
                        Box(
                            modifier = Modifier
                                .size(keyWidth, keyHeight)
                                .clip(RoundedCornerShape(8.dp))
                                .background(tint ?: if (off) AppColors.Charcoal.copy(alpha = 0.4f) else AppColors.Charcoal)
                        ) {
                            TextButton(
                                onClick = { if (!off) onLetter(c) },
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    c.toString(),
                                    color = when {
                                        tint != null -> AppColors.Ivory
                                        off -> AppColors.MutedText
                                        else -> AppColors.GoldBright
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp)
                                )
                            }
                        }
                    }
                    if (index == 2 && onDelete != null) {
                        WideKey("DEL", keyWidth * 1.5f, keyHeight, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun WideKey(label: String, width: Dp, height: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.CharcoalMid)
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(0.dp)) {
            Text(label, color = AppColors.Silver, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp))
        }
    }
}

/**
 * Reports which grid cell the finger is over while dragging. [onMove] only fires when the
 * finger is near a cell's centre, so diagonal moves don't clip neighbouring cells.
 */
@Composable
fun Modifier.gridDrag(
    rows: Int,
    cols: Int,
    onStart: (row: Int, col: Int) -> Unit,
    onMove: (row: Int, col: Int) -> Unit,
    onEnd: () -> Unit,
    enabled: Boolean = true
): Modifier {
    val start by rememberUpdatedState(onStart)
    val move by rememberUpdatedState(onMove)
    val end by rememberUpdatedState(onEnd)
    if (!enabled) return this
    return pointerInput(rows, cols) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val cellW = size.width / cols.toFloat()
            val cellH = size.height / rows.toFloat()
            fun cellAt(p: Offset, nearCentre: Boolean): Pair<Int, Int>? {
                val c = (p.x / cellW).toInt()
                val r = (p.y / cellH).toInt()
                if (r !in 0 until rows || c !in 0 until cols) return null
                if (nearCentre) {
                    val dx = p.x - (c + 0.5f) * cellW
                    val dy = p.y - (r + 0.5f) * cellH
                    if (dx * dx + dy * dy > (0.38f * minOf(cellW, cellH)).let { it * it }) return null
                }
                return r to c
            }
            cellAt(down.position, nearCentre = false)?.let { (r, c) -> start(r, c) }
            down.consume()
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (!change.pressed) break
                cellAt(change.position, nearCentre = true)?.let { (r, c) -> move(r, c) }
                change.consume()
            }
            end()
        }
    }
}

/** Small "Hint" button used by every game; the host decides whether it costs points or an ad. */
@Composable
fun HintButton(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Charcoal)
    ) {
        TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.align(Alignment.Center)) {
            Text(
                "✦ $label",
                color = if (enabled) AppColors.GoldBright else AppColors.MutedText,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

fun formatSeconds(totalSeconds: Int): String = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
