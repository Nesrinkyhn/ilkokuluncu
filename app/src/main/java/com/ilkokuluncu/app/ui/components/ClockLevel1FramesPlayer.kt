package com.ilkokuluncu.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.ilkokuluncu.app.R
import kotlinx.coroutines.delay

@Composable
fun ClockLevel1FramesPlayer(
    modifier: Modifier = Modifier,
    frameDurationMs: Long = 140L,
    onFinish: () -> Unit = {}
) {
    val frames = listOf(
        R.drawable.frame_001,
        R.drawable.frame_004,
        R.drawable.frame_007,
        R.drawable.frame_010,
        R.drawable.frame_013,
        R.drawable.frame_016,
        R.drawable.frame_019,
        R.drawable.frame_022,
        R.drawable.frame_025,
        R.drawable.frame_028,
        R.drawable.frame_031,
        R.drawable.frame_034,
        R.drawable.frame_037,
        R.drawable.frame_040
    )

    var currentFrame by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        for (i in frames.indices) {
            currentFrame = i
            delay(frameDurationMs)
        }
        onFinish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = painterResource(id = frames[currentFrame]),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}