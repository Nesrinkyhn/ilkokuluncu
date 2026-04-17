package com.ilkokuluncu.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.ilkokuluncu.app.data.Confetti
import kotlinx.coroutines.delay
import kotlin.random.Random


@Composable
fun CelebrationEffect(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    val confettiCount = 50
    val colors = listOf(
        Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFF45B7D1),
        Color(0xFFFFA07A), Color(0xFF98D8C8), Color(0xFFF7DC6F),
        Color(0xFFBB8FCE), Color(0xFF85C1E2), Color(0xFFF8B500)
    )

    var confettis by remember {
        mutableStateOf(
            List(confettiCount) {
                Confetti(
                    x = Random.nextFloat(),
                    y = -0.1f,
                    vx = (Random.nextFloat() - 0.5f) * 0.02f,
                    vy = Random.nextFloat() * 0.015f + 0.01f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = (Random.nextFloat() - 0.5f) * 10f,
                    color = colors.random(),
                    size = Random.nextFloat() * 10f + 5f
                )
            }
        )
    }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        val duration = 3000L // 3 saniye

        while (System.currentTimeMillis() - startTime < duration) {
            delay(16) // ~60 FPS

            confettis = confettis.map { confetti ->
                confetti.copy(
                    x = confetti.x + confetti.vx,
                    y = confetti.y + confetti.vy,
                    rotation = confetti.rotation + confetti.rotationSpeed
                )
            }
        }

        onComplete()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        confettis.forEach { confetti ->
            if (confetti.y <= 1.1f) {
                drawCircle(
                    color = confetti.color,
                    radius = confetti.size,
                    center = Offset(
                        x = confetti.x * width,
                        y = confetti.y * height
                    )
                )
            }
        }
    }
}