package com.ilkokuluncu.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.ilkokuluncu.app.ui.components.RedBackButton
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
fun CarpmaOyunlarScreen(
    onBalloonGameClick: () -> Unit,
    onGoldRunClick: () -> Unit,
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            Text("🎮", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text       = "Oyunlar",
                fontSize   = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
            Text(
                text      = "Bir oyun seç ve çarpmayı öğren!",
                fontSize  = 15.sp,
                color     = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(40.dp))

            // ── Level 1: Çarpışan Balonlar ────────────────────────────────────
            OyunLevelCard(
                level       = "Level 1",
                emoji       = "🎈",
                title       = "Çarpışan Balonlar",
                description = "Soruyu gör, doğru cevabı\ntaşıyan balonu patlat!",
                color1      = Color(0xFFFF6D00),
                color2      = Color(0xFFFFAB40),
                onClick     = onBalloonGameClick
            )

            Spacer(Modifier.height(20.dp))

            // ── Level 2: Altınları Topla ──────────────────────────────────────
            OyunLevelCard(
                level       = "Level 2",
                emoji       = "⚽",
                title       = "Altınları Topla",
                description = "Koş, zıpla, doğru cevabı\ngösteren altını topla!",
                color1      = Color(0xFF1565C0),
                color2      = Color(0xFF42A5F5),
                onClick     = onGoldRunClick
            )
        }
    }
}

@Composable
private fun OyunLevelCard(
    level: String,
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
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth()
        ) {
            // Emoji + level badge
            Box(contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 52.sp)
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier         = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Text(
                        text      = level,
                        fontSize  = 12.sp,
                        color     = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text       = title,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = description,
                    fontSize   = 13.sp,
                    color      = Color.White.copy(alpha = 0.90f),
                    lineHeight = 19.sp
                )
            }

            Spacer(Modifier.width(12.dp))
            Text("▶", fontSize = 22.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}
