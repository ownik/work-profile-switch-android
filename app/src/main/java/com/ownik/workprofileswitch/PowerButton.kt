package com.ownik.workprofileswitch

import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
import android.graphics.RectF
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

import com.ownik.workprofileswitch.ui.theme.WorkProfileSwitchTheme

@Composable
fun PowerButton(
    enabled: Boolean,
    isOn: Boolean,
    onClick: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    // Smooth "off -> on" transition (0f..1f), drives both color and glow.
    val transition by animateFloatAsState(
        targetValue = if (isOn) 1f else 0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "transition"
    )

    val enabledAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.35f,
        animationSpec = tween(durationMillis = 250),
        label = "enabledAlpha"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()

    // Guards against a second tap starting a new onClick while the previous
    // coroutine is still running.
    var isToggling by remember { mutableStateOf(false) }

    // Fades the loading ring in/out around the button.
    val loadingAlpha by animateFloatAsState(
        targetValue = if (isToggling) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "loadingAlpha"
    )

    // Continuous rotation for the loading ring, only actually spinning while visible.
    val infiniteTransition = rememberInfiniteTransition(label = "loadingSpin")
    val loadingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadingRotation"
    )

    Box(
        modifier = modifier
            .background(Color.Transparent)
            .clickable(
                enabled = enabled && !isToggling,
                interactionSource = interactionSource,
                // Removes the default gray ripple/highlight on press.
                indication = null,
                onClick = {
                    if (isToggling) return@clickable
                    scope.launch {
                        isToggling = true
                        try {
                            onClick()
                        } finally {
                            isToggling = false
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val center = Offset(
                size.width / 2f,
                size.height / 2f
            )

            /*
             * ─────────────────────────────
             * Sizes
             * ─────────────────────────────
             */
            val outerRadius = size.minDimension * 0.40f
            val powerRadius = size.minDimension * 0.23f
            val outerStroke = size.minDimension * 0.075f
            val powerStroke = size.minDimension * 0.045f

            val activeColor = Color(0xFF00E5F0)
            val inactiveColor = Color(0xFF53616D)

            // Smoothly blend color between the off and on states.
            val color = lerp(inactiveColor, activeColor, transition)
            val iconAlpha = enabledAlpha
            val glowAlpha = transition * enabledAlpha

            /*
             * ─────────────────────────────
             * LOADING RING
             * ─────────────────────────────
             *
             * Spinning arc drawn just outside the outer ring while a click is
             * being processed (onClick coroutine still running).
             */
            if (loadingAlpha > 0f) {
                val loadingRadius = outerRadius + outerStroke * 1.0f
                val loadingStroke = outerStroke * 0.2f
                val loadingSweep = 100f

                val loadingTopLeft = Offset(
                    center.x - loadingRadius,
                    center.y - loadingRadius
                )
                val loadingSize = Size(
                    loadingRadius * 2f,
                    loadingRadius * 2f
                )

                drawNeonArc(
                    topLeft = loadingTopLeft,
                    arcSize = loadingSize,
                    startAngle = loadingRotation,
                    sweepAngle = loadingSweep,
                    strokeWidth = loadingStroke * 1.6f,
                    blurRadius = loadingStroke * 1.4f,
                    color = activeColor,
                    alpha = loadingAlpha * 0.8f
                )

                drawArc(
                    color = activeColor.copy(alpha = loadingAlpha),
                    startAngle = loadingRotation,
                    sweepAngle = loadingSweep,
                    useCenter = false,
                    topLeft = loadingTopLeft,
                    size = loadingSize,
                    style = Stroke(
                        width = loadingStroke,
                        cap = StrokeCap.Round
                    )
                )
            }

            /*
             * ─────────────────────────────
             * OUTER RING
             * ─────────────────────────────
             *
             * Full ring around the power icon.
             */
            val outerTopLeft = Offset(
                center.x - outerRadius,
                center.y - outerRadius
            )
            val outerSize = Size(
                outerRadius * 2f,
                outerRadius * 2f
            )

            // Real neon glow via BlurMaskFilter (two layers: wide soft + tight dense).
            drawNeonArc(
                topLeft = outerTopLeft,
                arcSize = outerSize,
                startAngle = 0f,
                sweepAngle = 360f,
                strokeWidth = outerStroke * 1.4f,
                blurRadius = outerStroke * 2.2f,
                color = activeColor,
                alpha = glowAlpha * 0.9f
            )
            drawNeonArc(
                topLeft = outerTopLeft,
                arcSize = outerSize,
                startAngle = 0f,
                sweepAngle = 360f,
                strokeWidth = outerStroke * 0.9f,
                blurRadius = outerStroke * 0.9f,
                color = activeColor,
                alpha = glowAlpha
            )

            /*
             * Outer ring (crisp stroke drawn on top of the glow).
             */
            drawArc(
                color = color.copy(alpha = iconAlpha),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = outerTopLeft,
                size = outerSize,
                style = Stroke(
                    width = outerStroke,
                    cap = StrokeCap.Round
                )
            )

            /*
             * ─────────────────────────────
             * POWER ICON
             * ─────────────────────────────
             */
            val powerTopLeft = Offset(
                center.x - powerRadius,
                center.y - powerRadius
            )
            val powerSize = Size(
                powerRadius * 2f,
                powerRadius * 2f
            )

            /*
             * Gap at the top of the power icon.
             */
            val ringStartAngle = -45f
            val ringSweepAngle = 270f

            val lineStart = Offset(
                center.x,
                center.y - powerRadius * 0.72f
            )
            val lineEnd = Offset(
                center.x,
                center.y
            )

            // Glow for the power symbol itself.
            drawNeonArc(
                topLeft = powerTopLeft,
                arcSize = powerSize,
                startAngle = ringStartAngle,
                sweepAngle = ringSweepAngle,
                strokeWidth = powerStroke * 1.3f,
                blurRadius = powerStroke * 1.6f,
                color = activeColor,
                alpha = glowAlpha
            )
            drawNeonLine(
                start = lineStart,
                end = lineEnd,
                strokeWidth = powerStroke * 1.3f,
                blurRadius = powerStroke * 1.6f,
                color = activeColor,
                alpha = glowAlpha
            )

            /*
             * Power ring (crisp).
             */
            drawArc(
                color = color.copy(alpha = iconAlpha),
                startAngle = ringStartAngle,
                sweepAngle = ringSweepAngle,
                useCenter = false,
                topLeft = powerTopLeft,
                size = powerSize,
                style = Stroke(
                    width = powerStroke,
                    cap = StrokeCap.Round
                )
            )

            /*
             * Power line (crisp).
             */
            drawLine(
                color = color.copy(alpha = iconAlpha),
                start = lineStart,
                end = lineEnd,
                strokeWidth = powerStroke,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Draws an arc with a real Gaussian blur (BlurMaskFilter) — produces a lively neon
 * halo instead of several stacked semi-transparent outlines.
 */
private fun DrawScope.drawNeonArc(
    topLeft: Offset,
    arcSize: Size,
    startAngle: Float,
    sweepAngle: Float,
    strokeWidth: Float,
    blurRadius: Float,
    color: Color,
    alpha: Float
) {
    if (alpha <= 0f) return

    val rect = RectF(
        topLeft.x,
        topLeft.y,
        topLeft.x + arcSize.width,
        topLeft.y + arcSize.height
    )

    drawIntoCanvas { canvas ->
        val paint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            style = NativePaint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = NativePaint.Cap.ROUND
            this.color = color.copy(alpha = alpha).toArgb()
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.drawArc(rect, startAngle, sweepAngle, false, paint)
    }
}

/**
 * Same idea as [drawNeonArc], but for a straight line (the vertical tick of the power symbol).
 */
private fun DrawScope.drawNeonLine(
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    blurRadius: Float,
    color: Color,
    alpha: Float
) {
    if (alpha <= 0f) return

    drawIntoCanvas { canvas ->
        val paint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            style = NativePaint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = NativePaint.Cap.ROUND
            this.color = color.copy(alpha = alpha).toArgb()
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.drawLine(start.x, start.y, end.x, end.y, paint)
    }
}

@Preview(showBackground = true)
@Composable
fun PowerButtonPreview() {
    WorkProfileSwitchTheme {
        PowerButton(
            enabled = false,
            isOn = true,
            onClick = {},
            modifier = Modifier.size(320.dp)
        )
    }
}