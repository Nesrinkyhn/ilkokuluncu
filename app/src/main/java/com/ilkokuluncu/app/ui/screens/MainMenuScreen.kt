package com.ilkokuluncu.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.ilkokuluncu.app.ads.AdConfig
import com.ilkokuluncu.app.ads.AdManager
import com.ilkokuluncu.app.data.GameModule
import com.ilkokuluncu.app.ui.components.BannerAdView
import com.ilkokuluncu.app.ui.components.GameCard

@Composable
fun MainMenuScreen(
    gameModules: List<GameModule>,
    onGameClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    isMusicMuted: Boolean = false,
    onToggleMusic: () -> Unit = {},
    adManager: AdManager? = null,
    modifier: Modifier = Modifier
) {
    var showFeedback by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "title")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = -10f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── Başlık + Ayarlar ──────────────────────────────────────────
            item {
                Spacer(Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Müzik açma/kapama butonu
                    IconButton(
                        onClick = onToggleMusic,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Text(
                            text     = if (isMusicMuted) "🔇" else "🔊",
                            fontSize = 22.sp
                        )
                    }
                    Text(
                        text = "📚 İlkokuluncu 📚",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .offset(y = offsetY.dp)
                    )
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ayarlar",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.height(36.dp))
            }

            // ── Oyun kartları ─────────────────────────────────────────────
            items(gameModules) { module ->
                GameCard(module = module, onClick = { onGameClick(module.id) })
                Spacer(Modifier.height(16.dp))
            }

            // ── Değerlendir butonu ────────────────────────────────────────
            item {
                RateUsButton(onClick = { showFeedback = true })
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Banner reklam (alt) ───────────────────────────────────────────
        if (adManager != null) {
            BannerAdView(adUnitId = AdConfig.BANNER_MAIN_MENU)
        }
    }

    if (showFeedback) {
        FeedbackDialog(onDismiss = { showFeedback = false })
    }
}

// ─── "Bizi Değerlendir" butonu ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RateUsButton(onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (pressed) 0.96f else 1f,
        spring(Spring.DampingRatioMediumBouncy), label = "sc")

    Card(
        modifier  = Modifier.fillMaxWidth().scale(sc),
        shape     = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        onClick   = { pressed = true; onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFFFF9800), Color(0xFFFFD600))))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("⭐⭐⭐⭐⭐", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                "Uygulamamızı değerlendir!",
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
        }
    }

    LaunchedEffect(pressed) {
        if (pressed) { delay(100); pressed = false }
    }
}

