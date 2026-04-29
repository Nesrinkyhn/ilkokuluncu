package com.ilkokuluncu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures

@Composable
fun RedBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
        .padding(12.dp)
        .statusBarsPadding(),
    shape: Shape = CircleShape,
    size: Int = 44
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(shape)
            .background(Color(0xFFE53935))
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "✕",
            fontSize   = (size * 0.40f).sp,
            color      = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
