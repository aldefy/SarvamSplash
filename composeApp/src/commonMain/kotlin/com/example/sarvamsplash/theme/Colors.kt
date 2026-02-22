package com.example.sarvamsplash.theme

import androidx.compose.ui.graphics.Color

object SarvamColors {
    // Layer gradients from innermost (0) to outermost (7).
    // Each pair: (center highlight, edge color).
    // From the video: warm golden center → uniform warm orange outer.
    val layerGradients: List<Pair<Color, Color>> = listOf(
        Color(0xFFF5C84A) to Color(0xFFEBB040), // 0: bright warm gold
        Color(0xFFF0B840) to Color(0xFFE5A035), // 1: golden
        Color(0xFFEBA838) to Color(0xFFDE9430), // 2: gold-orange
        Color(0xFFE59A32) to Color(0xFFD8882A), // 3: warm orange
        Color(0xFFE08E2C) to Color(0xFFD27E25), // 4: orange
        Color(0xFFDA8228) to Color(0xFFCC7220), // 5: deeper orange
        Color(0xFFD47822) to Color(0xFFC6681C), // 6: amber-orange
        Color(0xFFCE6E1E) to Color(0xFFC06018), // 7: deep amber
    )

    val background = Color.White

    // Final splash screen gradient (from video frame 9-10)
    // Vertical gradient: darker orange top → warm golden center → warm peach bottom
    val splashTop = Color(0xFFCC7020)
    val splashCenter = Color(0xFFE0A040)
    val splashBottom = Color(0xFFE8B858)
}
