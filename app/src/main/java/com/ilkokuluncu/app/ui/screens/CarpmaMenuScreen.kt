package com.ilkokuluncu.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.ilkokuluncu.app.ui.components.RedBackButton
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
    onRitmikSaymaClick: () -> Unit,
    onOyunlarClick: () -> Unit,
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
        RedBackButton(
            onClick = onBackPress,
            modifier = Modifier
                .padding(12.dp)
                .statusBarsPadding()
                .align(Alignment.TopStart)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // ── Başlık ────────────────────────────────────────────────────────
            Text("✖️", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text       = "Çarpma",
                fontSize   = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
            Text(
                text      = "Ne öğrenmek istiyorsun?",
                fontSize  = 16.sp,
                color     = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(40.dp))

            // ── Kart 1: Ritmik Sayma ──────────────────────────────────────────
            CarpmaTopCard(
                emoji       = "🔢",
                title       = "Ritmik Sayma",
                description = "2'ler, 3'ler, 4'ler…\nRitmik sayarak çarpmanın temelini at!",
                levelInfo   = "2 bölüm • 2'ler · 3'ler",
                color1      = Color(0xFF00897B),
                color2      = Color(0xFF26C6DA),
                onClick     = onRitmikSaymaClick
            )

            Spacer(Modifier.height(20.dp))

            // ── Kart 2: Oyunlar ───────────────────────────────────────────────
            CarpmaTopCard(
                emoji       = "🎮",
                title       = "Oyunlar",
                description = "Çarpma sorularını eğlenceli\noyunlarla çöz!",
                levelInfo   = "2 bölüm • Balonlar · Altınlar",
                color1      = Color(0xFFFF6D00),
                color2      = Color(0xFFFFAB40),
                onClick     = onOyunlarClick
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CarpmaTopCard(
    emoji: String,
    title: String,
    description: String,
    levelInfo: String,
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.fillMaxWidth()
        ) {
            Text(emoji, fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text       = title,
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text       = description,
                fontSize   = 15.sp,
                color      = Color.White.copy(alpha = 0.92f),
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(16.dp))
            // Level chip
            Box(
                modifier         = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = levelInfo,
                    fontSize  = 13.sp,
                    color     = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier         = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.25f))
                    .padding(horizontal = 32.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "Başla! 🚀",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
        }
    }
}
