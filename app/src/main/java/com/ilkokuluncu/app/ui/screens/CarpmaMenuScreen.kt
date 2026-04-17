package com.ilkokuluncu.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CarpmaMenuScreen(
    onGameClick: () -> Unit,
    onBackPress: () -> Unit
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A0050), Color(0xFF4A0080), Color(0xFF1A0050))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {

        // ── Geri butonu ───────────────────────────────────────────────────────
        IconButton(
            onClick   = onBackPress,
            modifier  = Modifier
                .padding(12.dp)
                .statusBarsPadding()
                .align(Alignment.TopStart)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
        }

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(72.dp))

            // ── Başlık ────────────────────────────────────────────────────────
            Text(
                text       = "✖️",
                fontSize   = 64.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text       = "Çarpma",
                fontSize   = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
            Text(
                text       = "Bir oyun seç ve çarpmayı öğren!",
                fontSize   = 16.sp,
                color      = Color.White.copy(alpha = 0.75f),
                textAlign  = TextAlign.Center,
                modifier   = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(48.dp))

            // ── Oyun kartı: Çarpışan Balonlar ─────────────────────────────────
            CarpmaGameCard(
                emoji       = "🎈",
                title       = "Çarpışan Balonlar",
                description = "Uçan balonlara dokunarak çarpma işlemlerini çöz!",
                color1      = Color(0xFFFF6D00),
                color2      = Color(0xFFFFAB40),
                onClick     = onGameClick
            )
        }
    }
}

@Composable
private fun CarpmaGameCard(
    emoji: String,
    title: String,
    description: String,
    color1: Color,
    color2: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(color1, color2)))
            .clickable(onClick = onClick)
            .padding(28.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(emoji, fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text       = title,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text       = description,
                fontSize   = 14.sp,
                color      = Color.White.copy(alpha = 0.9f),
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier            = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.25f))
                    .padding(horizontal = 32.dp, vertical = 10.dp),
                contentAlignment    = Alignment.Center
            ) {
                Text(
                    text       = "Oyna! 🚀",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
        }
    }
}
