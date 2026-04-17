
package com.ilkokuluncu.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SparkleBurst(
    trigger: Int,
    modifier: Modifier = Modifier
) {
    val visible = remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            visible.value = true
            delay(520)
            visible.value = false
        }
    }

    if (!visible.value) return

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        SparkleParticle("✨", (-88).dp, (-70).dp, trigger + 1)
        SparkleParticle("⭐", 92.dp, (-62).dp, trigger + 2)
        SparkleParticle("✨", (-104).dp, 36.dp, trigger + 3)
        SparkleParticle("🌟", 100.dp, 44.dp, trigger + 4)
        SparkleParticle("⭐", 0.dp, (-108).dp, trigger + 5)
        SparkleParticle("✨", 0.dp, 112.dp, trigger + 6)
    }
}

@Composable
private fun SparkleParticle(
    emoji: String,
    x: androidx.compose.ui.unit.Dp,
    y: androidx.compose.ui.unit.Dp,
    keyValue: Int
) {
    val alpha = remember(keyValue) { Animatable(0f) }
    val scale = remember(keyValue) { Animatable(0.2f) }

    LaunchedEffect(keyValue) {
        alpha.snapTo(0f)
        scale.snapTo(0.2f)
        alpha.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        alpha.animateTo(0f, tween(240))
    }

    Text(
        text = emoji,
        modifier = Modifier
            .offset(x = x, y = y)
            .size(28.dp)
            .alpha(alpha.value)
            .scale(scale.value)
    )
}
