package com.nuvio.tv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.ui.theme.NuvioColors

/**
 * Animated preview of the classic horizontal row layout.
 * Shows 3 rows with colored placeholder rectangles scrolling horizontally.
 */
@Composable
fun ClassicLayoutPreview(
    modifier: Modifier = Modifier,
    accentColor: Color = NuvioColors.Primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "classicPreview")
    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "classicScroll"
    )

    val textMeasurer = rememberTextMeasurer()
    val bgColor = NuvioColors.Background
    val cardColor = accentColor.copy(alpha = 0.6f)
    val cardColorDim = accentColor.copy(alpha = 0.3f)
    val textColor = Color.White.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rowHeight = h / 4.2f
            val cardWidth = w / 5.5f
            val cardHeight = rowHeight * 0.65f
            val gap = w / 40f
            val labelHeight = rowHeight * 0.25f

            val labels = listOf("Trending", "Action", "Comedy")

            labels.forEachIndexed { rowIndex, label ->
                val rowY = rowHeight * 0.3f + rowIndex * (rowHeight + h * 0.02f)

                // Row label
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(gap * 2, rowY),
                    style = TextStyle(
                        color = textColor,
                        fontSize = (h * 0.055f).sp
                    )
                )

                // Cards - middle row scrolls
                val numCards = 7
                val baseOffset = if (rowIndex == 1) {
                    -scrollOffset * cardWidth * 2
                } else {
                    0f
                }

                for (i in 0 until numCards) {
                    val cardX = gap * 2 + i * (cardWidth + gap) + baseOffset
                    if (cardX + cardWidth > -cardWidth && cardX < w + cardWidth) {
                        drawRoundRect(
                            color = if (rowIndex == 1) cardColor else cardColorDim,
                            topLeft = Offset(cardX, rowY + labelHeight),
                            size = Size(cardWidth, cardHeight),
                            cornerRadius = CornerRadius(h * 0.02f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated preview of the grid layout with hero banner and vertical grid.
 * Shows a hero area at top, then a grid scrolling upward with sticky header transitions.
 */
@Composable
fun GridLayoutPreview(
    modifier: Modifier = Modifier,
    accentColor: Color = NuvioColors.Primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gridPreview")
    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gridScroll"
    )

    // Crossfade between section names
    val sectionAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sectionAlpha"
    )

    val textMeasurer = rememberTextMeasurer()
    val bgColor = NuvioColors.Background
    val heroColor = accentColor.copy(alpha = 0.4f)
    val cardColor = accentColor.copy(alpha = 0.5f)
    val cardColorAlt = accentColor.copy(alpha = 0.3f)
    val headerBg = NuvioColors.Background.copy(alpha = 0.9f)
    val textColor = Color.White.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Hero section at top (~30% of height)
            val heroHeight = h * 0.28f
            drawRoundRect(
                color = heroColor,
                topLeft = Offset(w * 0.04f, h * 0.04f),
                size = Size(w * 0.92f, heroHeight),
                cornerRadius = CornerRadius(h * 0.02f)
            )

            // Hero text placeholder
            drawText(
                textMeasurer = textMeasurer,
                text = "Featured",
                topLeft = Offset(w * 0.08f, heroHeight * 0.6f + h * 0.04f),
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = (h * 0.05f).sp
                )
            )

            // Sticky header area
            val headerY = heroHeight + h * 0.08f
            val headerHeight = h * 0.06f
            drawRect(
                color = headerBg,
                topLeft = Offset(0f, headerY),
                size = Size(w, headerHeight)
            )

            // Crossfading header text
            val headerText = if (sectionAlpha > 0.5f) "Trending" else "Action"
            drawText(
                textMeasurer = textMeasurer,
                text = headerText,
                topLeft = Offset(w * 0.06f, headerY + headerHeight * 0.15f),
                style = TextStyle(
                    color = textColor,
                    fontSize = (h * 0.04f).sp
                )
            )

            // Grid of cards
            val gridStartY = headerY + headerHeight + h * 0.02f
            val cols = 5
            val cardGap = w * 0.02f
            val cardW = (w - cardGap * (cols + 1)) / cols
            val cardH = cardW * 1.4f
            val totalScrollY = scrollOffset * cardH * 1.5f

            for (row in 0..5) {
                for (col in 0 until cols) {
                    val cardX = cardGap + col * (cardW + cardGap)
                    val cardY = gridStartY + row * (cardH + cardGap) - totalScrollY

                    if (cardY + cardH > gridStartY && cardY < h) {
                        val color = if (row < 2) cardColor else cardColorAlt
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(cardX, cardY),
                            size = Size(cardW, cardH),
                            cornerRadius = CornerRadius(h * 0.015f)
                        )
                    }
                }
            }
        }
    }
}
