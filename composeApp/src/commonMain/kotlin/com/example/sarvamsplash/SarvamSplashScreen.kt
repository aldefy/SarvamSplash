package com.example.sarvamsplash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.sarvamsplash.theme.SarvamColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LAYER_COUNT = 8
private const val STAGGER_DELAY_MS = 60L
private const val EXPAND_DURATION_MS = 1000
private const val OVERLAY_FADE_IN_MS = 500
private const val HOLD_DURATION_MS = 800L
private const val OVERLAY_FADE_OUT_MS = 400
private const val PAUSE_BEFORE_REPEAT_MS = 300L

// Start overlay fade when expansion is this % done (overlaps with tail end)
private const val OVERLAY_START_DELAY_MS = 600L

@Composable
fun SarvamSplashScreen() {
    var restartKey by remember { mutableIntStateOf(0) }

    val layerScales = remember(restartKey) {
        List(LAYER_COUNT) { Animatable(0f) }
    }
    val overlayAlpha = remember(restartKey) { Animatable(0f) }

    val baseSize = remember { Size(200f, 200f) }
    val shapePath = remember { sarvamLayerPath(baseSize, lobesPerEdge = 3) }

    LaunchedEffect(restartKey) {
        while (true) {
            // --- EXPAND PHASE ---
            // Launch layer expansions AND overlay fade concurrently.
            // Overlay starts partway through expansion so the transition is seamless.

            val expandJobs = layerScales.mapIndexed { index, animatable ->
                launch {
                    delay(index * STAGGER_DELAY_MS)
                    animatable.animateTo(
                        targetValue = 1.0f + (index.toFloat() / (LAYER_COUNT - 1)) * 9f,
                        animationSpec = tween(
                            durationMillis = EXPAND_DURATION_MS,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                }
            }

            // Overlay starts fading in while outer layers are still expanding
            val overlayJob = launch {
                delay(OVERLAY_START_DELAY_MS)
                overlayAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = OVERLAY_FADE_IN_MS,
                        easing = LinearEasing,
                    ),
                )
            }

            expandJobs.forEach { it.join() }
            overlayJob.join()

            // --- HOLD ---
            delay(HOLD_DURATION_MS)

            // --- RESET BEHIND OVERLAY, THEN FADE OUT ---
            // Snap layers to 0 while overlay is still fully opaque (hidden reset)
            layerScales.forEach { it.snapTo(0f) }

            // Now fade out the overlay to reveal the clean white canvas
            overlayAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = OVERLAY_FADE_OUT_MS,
                    easing = LinearEasing,
                ),
            )

            delay(PAUSE_BEFORE_REPEAT_MS)
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                restartKey++
            },
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Draw mandala layers: outermost first, inner on top
        for (i in (LAYER_COUNT - 1) downTo 0) {
            val currentScale = layerScales[i].value
            if (currentScale <= 0.001f) continue

            val colors = SarvamColors.layerGradients[i]

            drawMandalaLayer(
                path = shapePath,
                pathSize = baseSize,
                scale = currentScale,
                center = Offset(centerX, centerY),
                centerColor = colors.first,
                edgeColor = colors.second,
            )
        }

        // Full-screen gradient overlay
        val alpha = overlayAlpha.value
        if (alpha > 0.001f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SarvamColors.splashTop.copy(alpha = alpha),
                        SarvamColors.splashCenter.copy(alpha = alpha),
                        SarvamColors.splashBottom.copy(alpha = alpha),
                    ),
                ),
                size = size,
            )
        }
    }
}

private fun DrawScope.drawMandalaLayer(
    path: Path,
    pathSize: Size,
    scale: Float,
    center: Offset,
    centerColor: Color,
    edgeColor: Color,
) {
    withTransform({
        translate(
            left = center.x - pathSize.width / 2f,
            top = center.y - pathSize.height / 2f,
        )
        scale(scale, scale, Offset(pathSize.width / 2f, pathSize.height / 2f))
    }) {
        val brush = Brush.radialGradient(
            colors = listOf(centerColor, edgeColor),
            center = Offset(pathSize.width / 2f, pathSize.height / 2f),
            radius = pathSize.height * 0.6f,
        )
        drawPath(path = path, brush = brush)
    }
}
