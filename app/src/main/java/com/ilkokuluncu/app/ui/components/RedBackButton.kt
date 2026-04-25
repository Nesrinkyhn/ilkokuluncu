package com.ilkokuluncu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun RedBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
        .padding(12.dp)
        .statusBarsPadding(),
    shape: Shape = CircleShape,
    size: Int = 72
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size.dp)
            .clip(shape)
            .background(Color(0xFFDA0E0A).copy(alpha = 0.90f))
    ) {
        Icon(
            Icons.Default.ArrowBack,
            contentDescription = "Geri",
            tint = Color.White,
            modifier = Modifier.size((size / 2).dp)
        )
    }
}
