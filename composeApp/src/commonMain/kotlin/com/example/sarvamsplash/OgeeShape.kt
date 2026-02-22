package com.example.sarvamsplash

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Builds a Sarvam-style mandala layer: a vertical diamond with scalloped edges.
 *
 * 4 pointed ogee tips at top/right/bottom/left, with [lobesPerEdge] round
 * circular scallops between each pair of tips. Total lobes = 4 * lobesPerEdge.
 *
 * Construction: lobe circle centers are placed along each diamond edge,
 * offset outward. The outline traces circular arcs between cusps
 * (circle-circle intersections). Tips are naturally sharp because lobes
 * from adjacent edges approach from different angles.
 *
 * @param size Bounding box of the shape
 * @param lobesPerEdge Round scallops per edge. 3 matches the Sarvam logo.
 */
fun sarvamLayerPath(size: Size, lobesPerEdge: Int = 3): Path {
    val cx = size.width / 2f
    val cy = size.height / 2f

    // Diamond aspect ratio: taller than wide (~1.25:1)
    val hw = size.width * 0.44f    // half-width to left/right tips
    val hh = size.height * 0.50f   // half-height to top/bottom tips

    // 4 diamond tip points (clockwise from top)
    val tipX = floatArrayOf(cx, cx + hw, cx, cx - hw)
    val tipY = floatArrayOf(cy - hh, cy, cy + hh, cy)

    val totalLobes = lobesPerEdge * 4

    // Place lobe centers along each diamond edge, offset outward
    val lobeX = FloatArray(totalLobes)
    val lobeY = FloatArray(totalLobes)

    for (edge in 0 until 4) {
        val sx = tipX[edge]
        val sy = tipY[edge]
        val ex = tipX[(edge + 1) % 4]
        val ey = tipY[(edge + 1) % 4]

        val edx = ex - sx
        val edy = ey - sy
        val edgeLen = sqrt(edx * edx + edy * edy)

        // Outward normal: for CW winding in screen coords, (dy, -dx)
        val nx = edy / edgeLen
        val ny = -edx / edgeLen

        // How far lobe centers sit outside the diamond edge
        // Controls the "puffiness" of scallops
        val bulge = edgeLen / lobesPerEdge * 0.28f

        for (i in 0 until lobesPerEdge) {
            val t = (i + 0.5f) / lobesPerEdge
            val idx = edge * lobesPerEdge + i
            lobeX[idx] = sx + edx * t + nx * bulge
            lobeY[idx] = sy + edy * t + ny * bulge
        }
    }

    // Compute lobe radius: must overlap all adjacent pairs (same-edge AND cross-edge at tips).
    // Find min distance between any adjacent pair, then set radius for proper overlap.
    var minDist = Float.MAX_VALUE
    for (i in 0 until totalLobes) {
        val j = (i + 1) % totalLobes
        val dx = lobeX[i] - lobeX[j]
        val dy = lobeY[i] - lobeY[j]
        val d = sqrt(dx * dx + dy * dy)
        if (d < minDist) minDist = d
    }
    // Radius = 0.60 * minDist gives good overlap with deep but not extreme cusps
    val lobeRadius = minDist * 0.60f

    // Compute cusp points (outer circle-circle intersections)
    val cuspX = FloatArray(totalLobes)
    val cuspY = FloatArray(totalLobes)
    for (i in 0 until totalLobes) {
        val j = (i + 1) % totalLobes
        val (ix, iy) = circleOuterIntersection(
            lobeX[i], lobeY[i], lobeRadius,
            lobeX[j], lobeY[j], lobeRadius,
            cx, cy,
        )
        cuspX[i] = ix
        cuspY[i] = iy
    }

    // Build path: for each lobe, draw arc from incoming cusp to outgoing cusp
    return Path().apply {
        moveTo(cuspX[totalLobes - 1], cuspY[totalLobes - 1])
        for (i in 0 until totalLobes) {
            val prevCusp = (i - 1 + totalLobes) % totalLobes
            drawArcBezier(
                this,
                cuspX[prevCusp], cuspY[prevCusp],
                cuspX[i], cuspY[i],
                lobeX[i], lobeY[i],
            )
        }
        close()
    }
}

private fun circleOuterIntersection(
    x1: Float, y1: Float, r1: Float,
    x2: Float, y2: Float, r2: Float,
    cx: Float, cy: Float,
): Pair<Float, Float> {
    val dx = x2 - x1
    val dy = y2 - y1
    val d = sqrt(dx * dx + dy * dy)

    if (d > r1 + r2 || d < 0.001f) {
        // No intersection — return midpoint (fallback)
        return Pair((x1 + x2) / 2f, (y1 + y2) / 2f)
    }

    val a = (r1 * r1 - r2 * r2 + d * d) / (2f * d)
    val h = sqrt(maxOf(r1 * r1 - a * a, 0f))

    val px = x1 + a * dx / d
    val py = y1 + a * dy / d

    val ix1 = px + h * dy / d
    val iy1 = py - h * dx / d
    val ix2 = px - h * dy / d
    val iy2 = py + h * dx / d

    // Pick the one farthest from shape center (outer cusp)
    val d1 = (ix1 - cx) * (ix1 - cx) + (iy1 - cy) * (iy1 - cy)
    val d2 = (ix2 - cx) * (ix2 - cx) + (iy2 - cy) * (iy2 - cy)
    return if (d1 >= d2) Pair(ix1, iy1) else Pair(ix2, iy2)
}

private fun drawArcBezier(
    path: Path,
    sx: Float, sy: Float,
    ex: Float, ey: Float,
    ccx: Float, ccy: Float,
) {
    val v1x = sx - ccx
    val v1y = sy - ccy
    val v2x = ex - ccx
    val v2y = ey - ccy

    val dot = v1x * v2x + v1y * v2y
    val cross = v1x * v2y - v1y * v2x
    val angle = atan2(cross.toDouble(), dot.toDouble()).toFloat()

    if (kotlin.math.abs(angle) < 0.001f) {
        path.lineTo(ex, ey)
        return
    }

    val kappa = (4f / 3f) * tan(angle / 4f)

    path.cubicTo(
        sx - v1y * kappa, sy + v1x * kappa,
        ex + v2y * kappa, ey - v2x * kappa,
        ex, ey,
    )
}
