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
fun RitmikSaymaMenuScreen(
    onLevel1Click: () -> Unit = {},
    onLevel2Click: () -> Unit = {},
    onLevel3Click: () -> Unit = {},
    onLevel4Click: () -> Unit = {},
    onBackPress:   () -> Unit
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF004D40), Color(0xFF00796B), Color(0xFF004D40))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        IconButton(
            onClick  = onBackPress,
            modifier = Modifier
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
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            Text("🔢", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text       = "Ritmik Sayma",
                fontSize   = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
            Text(
                text      = "İkişer, üçer, dörder sayarak\nçarpmanın sırrını keşfet!",
                fontSize  = 15.sp,
                color     = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(40.dp))

            // ── Level 1: 2'li Ritmik Sayma ────────────────────────────────────
            RitmikLevelCard(
                level       = "Level 1",
                emoji       = "2️⃣",
                title       = "2'li Ritmik Sayma",
                description = "2, 4, 6, 8, 10…\nİkişer ikişer saymayı öğren!",
                color1      = Color(0xFF00897B),
                color2      = Color(0xFF4DB6AC),
                comingSoon  = false,
                onClick     = onLevel1Click
            )

            Spacer(Modifier.height(20.dp))

            // ── Level 2: 3'ler ────────────────────────────────────────────────
            RitmikLevelCard(
                level       = "Level 2",
                emoji       = "☄️",
                title       = "3'ler",
                description = "3, 6, 9, 12, 15…\nKuyruklu yıldızları yakala!",
                color1      = Color(0xFF4A148C),
                color2      = Color(0xFF7B1FA2),
                comingSoon  = false,
                onClick     = onLevel2Click
            )

            Spacer(Modifier.height(20.dp))

            // ── Level 3: 4'ler Safari ─────────────────────────────────────────
            RitmikLevelCard(
                level       = "Level 3",
                emoji       = "🚗",
                title       = "4'ler Safari",
                description = "4, 8, 12, 16, 20…\nÇöl safarisinde doğru arabayı seç!",
                color1      = Color(0xFF6D4C1F),
                color2      = Color(0xFFB07D3A),
                comingSoon  = false,
                onClick     = onLevel3Click
            )

            Spacer(Modifier.height(20.dp))

            // ── Level 4: 5'ler Dondurma ───────────────────────────────────────
            RitmikLevelCard(
                level       = "Level 4",
                emoji       = "🍦",
                title       = "5'ler Dondurma",
                description = "5, 10, 15, 20, 25…\nSahilde doğru dondurma külahını bul!",
                color1      = Color(0xFF0097A7),
                color2      = Color(0xFF26C6DA),
                comingSoon  = false,
                onClick     = onLevel4Click
            )
        }
    }
}

@Composable
private fun RitmikLevelCard(
    level: String,
    emoji: String,
    title: String,
    description: String,
    color1: Color,
    color2: Color,
    comingSoon: Boolean = true,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(color1, color2)))
            .then(if (!comingSoon) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth()
        ) {
            Text(emoji, fontSize = 52.sp)

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier         = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Text(
                        text       = level,
                        fontSize   = 12.sp,
                        color      = Color.White,
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

            // Durum etiketi
            Box(
                modifier         = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.20f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = if (comingSoon) "Yakında\n🔒" else "Oyna!\n▶️",
                    fontSize   = 12.sp,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )
            }
        }
    }
}
