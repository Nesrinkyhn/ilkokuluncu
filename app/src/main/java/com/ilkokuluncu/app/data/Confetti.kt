package com.ilkokuluncu.app.data

import androidx.compose.ui.graphics.Color

data class Confetti(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    val color: Color,
    val size: Float
)
