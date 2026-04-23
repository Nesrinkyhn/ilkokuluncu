package com.ilkokuluncu.app.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import com.ilkokuluncu.app.ui.components.RedBackButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.data.*
import com.ilkokuluncu.app.ui.effects.GameBackgroundMusic
import com.ilkokuluncu.app.ui.effects.rememberSoundEffectPlayer


private val PLAYER_STRIPES = listOf(
    Color(0xFF9C27B0), Color(0xFF2196F3), Color(0xFF4CAF50),
    Color(0xFFFFEB3B), Color(0xFFFF3D00)
)

private val INTRO_STRIPES = listOf(
    Color(0xFFFF3D00), Color(0xFFFFD600), Color(0xFF4CAF50),
    Color(0xFF2979FF), Color(0xFFAA00FF)
)

private val STRIPE_SETS = listOf(
    listOf(Color(0xFF9C27B0), Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFFEB3B), Color(0xFFFF3D00)),
    listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFD600), Color(0xFF00C853), Color(0xFF00B0FF)),
    listOf(Color(0xFF6200EA), Color(0xFF00BCD4), Color(0xFFFFEB3B), Color(0xFFFF6D00), Color(0xFFE53935)),
    listOf(Color(0xFF00897B), Color(0xFFFFD600), Color(0xFFE53935), Color(0xFF7B1FA2), Color(0xFF1565C0)),
    listOf(Color(0xFF1565C0), Color(0xFF43A047), Color(0xFFFDD835), Color(0xFFFF7043), Color(0xFFAD1457)),
    listOf(Color(0xFFAD1457), Color(0xFF29B6F6), Color(0xFF66BB6A), Color(0xFFFFCA28), Color(0xFFEF5350)),
    listOf(Color(0xFF37474F), Color(0xFF00ACC1), Color(0xFF8BC34A), Color(0xFFFFB300), Color(0xFFFF5252)),
    listOf(Color(0xFF4A148C), Color(0xFF0288D1), Color(0xFF388E3C), Color(0xFFF9A825), Color(0xFFD84315)),
    listOf(Color(0xFF006064), Color(0xFF558B2F), Color(0xFFF57F17), Color(0xFFBF360C), Color(0xFF4527A0)),
    listOf(Color(0xFF880E4F), Color(0xFF01579B), Color(0xFF1B5E20), Color(0xFFF57F17), Color(0xFF4E342E))
)

private const val PLAYER_Y_NORM = 0.78f

// ── Ana ekran ─────────────────────────────────────────────────────────────────
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun PatternBalloonGameScreen(
    state: PatternGameState,
    onEvent: (PatternGameEvent) -> Unit,
    onBackPress: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    // ── Arka plan müziği ──────────────────────────────────────────────────────
    GameBackgroundMusic()

    // ── Ses efektleri ─────────────────────────────────────────────────────────
    val sfx = rememberSoundEffectPlayer()

    // Puan arttı → doğru balon yakalandı
    var prevScore by remember { mutableIntStateOf(state.score) }
    LaunchedEffect(state.score) {
        if (state.score > prevScore) sfx.playCorrect()
        prevScore = state.score
    }

    // Can düştü → yanlış balon
    var prevLives by remember { mutableIntStateOf(state.lives) }
    LaunchedEffect(state.lives) {
        if (state.lives < prevLives) sfx.playWrongWithVibration()
        prevLives = state.lives
    }

    // Oyun sonu sesleri
    LaunchedEffect(state.phase) {
        when (state.phase) {
            PatternGamePhase.VICTORY   -> sfx.playCorrect()
            PatternGamePhase.GAME_OVER -> sfx.playWrongWithVibration()
            else -> Unit
        }
    }

    val cloudDrift by rememberInfiniteTransition(label = "cloud").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val W = constraints.maxWidth.toFloat()
        val H = constraints.maxHeight.toFloat()

        // ── Canvas: gökyüzü + balonlar ────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.phase) {
                    if (state.phase == PatternGamePhase.PLAYING) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { ch ->
                                    if (ch.pressed) {
                                        onEvent(PatternGameEvent.PlayerMoved(ch.position.x / W))
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            // 1. Gökyüzü
            drawSkyBackground()

            // 2. Bulutlar
            drawClouds(cloudDrift, W, H)

            // 3. Düşen balonlar
            if (state.phase == PatternGamePhase.PLAYING) {
                state.fallingBalloons.forEach { fb ->
                    val stripes = STRIPE_SETS[fb.colorIndex % STRIPE_SETS.size]
                    drawSmallBalloon(
                        cx       = fb.x * W,
                        cy       = fb.y * H,
                        radius   = W * 0.072f,
                        stripes  = stripes,
                        number   = fb.value,
                        measurer = textMeasurer
                    )
                }
            }

            // 4. Oyuncu balonu (PLAYING)
            if (state.phase == PatternGamePhase.PLAYING) {
                drawHotAirBalloon(
                    cx      = state.playerX * W,
                    cy      = PLAYER_Y_NORM * H,
                    rX      = W * 0.11f,
                    rY      = W * 0.145f,
                    stripes = PLAYER_STRIPES,
                    label   = null,
                    measurer = textMeasurer
                )
            }

            // 5. Intro balonu
            if (state.phase == PatternGamePhase.INTRO) {
                drawHotAirBalloon(
                    cx      = state.introBalloonX * W,
                    cy      = H * 0.38f,
                    rX      = W * 0.15f,
                    rY      = W * 0.19f,
                    stripes = INTRO_STRIPES,
                    label   = "${state.currentPattern}",
                    measurer = textMeasurer
                )
            }
        }

        // ── HUD üst bar ───────────────────────────────────────────────────────
        PatternHUD(state = state, onBackPress = onBackPress)

        // ── Sıradaki sayı göstergesi ──────────────────────────────────────────
        if (state.phase == PatternGamePhase.PLAYING && state.nextIndex < state.sequence.size) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
                    .shadow(6.dp, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xCC1A237E))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text       = "Sıradaki: ${state.sequence[state.nextIndex]}",
                    color      = Color.White,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // ── INTRO bilgi metni ─────────────────────────────────────────────────
        if (state.phase == PatternGamePhase.INTRO) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "${state.currentPattern}'ler",
                        color      = Color.White,
                        fontSize   = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(6.dp))
                    val seqPreview = state.sequence.take(5).joinToString(", ")
                    Text(
                        text  = "$seqPreview …",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 18.sp
                    )
                }
            }
        }

        // ── GAME_OVER ─────────────────────────────────────────────────────────
        if (state.phase == PatternGamePhase.GAME_OVER) {
            PatternEndOverlay(
                emoji   = "😢",
                title   = "Canlar Bitti!",
                score   = state.score,
                bestScore = state.bestScore,
                isNewBest = state.isNewBest,
                onRetry = { onEvent(PatternGameEvent.RestartGame) },
                onBack  = onBackPress
            )
        }

        // ── VICTORY ───────────────────────────────────────────────────────────
        if (state.phase == PatternGamePhase.VICTORY) {
            PatternEndOverlay(
                emoji     = if (state.isNewBest) "🏆" else "🎉",
                title     = if (state.isNewBest) "Yeni Rekor!" else "Tebrikler!",
                score     = state.score,
                bestScore = state.bestScore,
                isNewBest = state.isNewBest,
                onRetry   = { onEvent(PatternGameEvent.RestartGame) },
                onBack    = onBackPress
            )
        }
    }
}

// ── HUD ───────────────────────────────────────────────────────────────────────
@Composable
private fun PatternHUD(state: PatternGameState, onBackPress: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick  = onBackPress,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.25f))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
        }

        Spacer(Modifier.width(8.dp))

        // Canlar: dolu 🎈 / boş = soluk balon + kırmızı ❌
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { i ->
                if (i < state.lives) {
                    Text("🎈", fontSize = 18.sp)
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎈", fontSize = 18.sp,
                            modifier = Modifier.graphicsLayer { alpha = 0.22f })
                        Text("❌", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Örüntü göstergesi
        if (state.phase == PatternGamePhase.PLAYING || state.phase == PatternGamePhase.INTRO) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${state.currentPattern}'ler  •  ${state.patternsDone + 1}/${state.patternOrder.size}",
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = "⭐ ${state.score}",
            color = Color(0xFFFFEA00), fontSize = 16.sp, fontWeight = FontWeight.Bold
        )
    }
}

// ── Son ekran overlay ─────────────────────────────────────────────────────────
@Composable
private fun PatternEndOverlay(
    emoji: String, title: String,
    score: Int, bestScore: Int, isNewBest: Boolean,
    onRetry: () -> Unit, onBack: () -> Unit
) {
    val bg = Brush.verticalGradient(listOf(Color(0xFF0D1B4B), Color(0xFF1A3575), Color(0xFF0D1B4B)))
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(16.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(bg)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 72.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("⭐ $score", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFEA00))
                    if (isNewBest) {
                        Spacer(Modifier.height(6.dp))
                        Text("Önceki rekor kırıldı! 🎯", fontSize = 14.sp, color = Color(0xFF00E676))
                    } else if (bestScore > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text("En iyi: $bestScore", fontSize = 15.sp, color = Color.White.copy(alpha = 0.65f))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            PatternBtn("🔄 Tekrar Oyna", Color(0xFF00897B), onRetry)
            Spacer(Modifier.height(12.dp))
            PatternBtn("◀ Menüye Dön", Color(0xFF37474F), onBack)
        }
    }
}

@Composable
private fun PatternBtn(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(color)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ── Canvas çizim fonksiyonları ────────────────────────────────────────────────

private fun DrawScope.drawSkyBackground() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF64B5F6), Color(0xFFBBDEFB)),
            startY = 0f, endY = size.height
        ),
        size = size
    )
    // Hafif güneş parıltısı sağ üstte
    drawCircle(
        brush  = Brush.radialGradient(
            colors = listOf(Color(0x33FFFFFF), Color(0x00FFFFFF)),
            center = Offset(size.width * 0.82f, size.height * 0.08f),
            radius = size.width * 0.25f
        ),
        radius = size.width * 0.25f,
        center = Offset(size.width * 0.82f, size.height * 0.08f)
    )
}

private fun DrawScope.drawClouds(drift: Float, W: Float, H: Float) {
    val cloudColor = Color.White.copy(alpha = 0.88f)
    val positions = listOf(
        Pair(0.18f, 0.09f),
        Pair(0.58f, 0.06f),
        Pair(0.40f, 0.17f),
        Pair(0.78f, 0.13f)
    )
    positions.forEachIndexed { i, (nx, ny) ->
        val offsetX = ((drift + i * 0.25f) % 1f) * W * 0.18f - W * 0.05f
        val cx = nx * W + offsetX
        val cy = ny * H
        val r  = W * 0.07f
        drawCircle(cloudColor, r * 0.75f, Offset(cx, cy))
        drawCircle(cloudColor, r * 0.65f, Offset(cx + r * 0.75f, cy + r * 0.12f))
        drawCircle(cloudColor, r * 0.65f, Offset(cx - r * 0.75f, cy + r * 0.12f))
        drawCircle(cloudColor, r * 0.85f, Offset(cx + r * 0.2f,  cy - r * 0.2f))
    }
}

// Gerçek uçan balon şekli için bezier path üretici
private fun balloonBodyPath(cx: Float, cy: Float, rX: Float, rY: Float): Path = Path().apply {
    moveTo(cx, cy - rY)
    // Sağ üst: tepe → en geniş nokta
    cubicTo(cx + rX * 0.52f, cy - rY * 1.02f,
            cx + rX * 1.22f, cy - rY * 0.42f,
            cx + rX * 1.16f, cy + rY * 0.18f)
    // Sağ alt: geniş → boğum
    cubicTo(cx + rX * 1.08f, cy + rY * 0.65f,
            cx + rX * 0.36f, cy + rY * 0.96f,
            cx,              cy + rY * 0.96f)
    // Sol alt (ayna)
    cubicTo(cx - rX * 0.36f, cy + rY * 0.96f,
            cx - rX * 1.08f, cy + rY * 0.65f,
            cx - rX * 1.16f, cy + rY * 0.18f)
    // Sol üst (ayna)
    cubicTo(cx - rX * 1.22f, cy - rY * 0.42f,
            cx - rX * 0.52f, cy - rY * 1.02f,
            cx,              cy - rY)
    close()
}

private fun DrawScope.drawHotAirBalloon(
    cx: Float, cy: Float,
    rX: Float, rY: Float,
    stripes: List<Color>,
    label: String?,
    measurer: androidx.compose.ui.text.TextMeasurer
) {
    val balloonPath = balloonBodyPath(cx, cy, rX, rY)
    val totalW  = rX * 2.38f
    val startX  = cx - rX * 1.19f

    // ── Renkli dikey paneller ──────────────────────────────────────────────
    clipPath(balloonPath) {
        val n  = stripes.size
        val sw = totalW / n
        stripes.forEachIndexed { i, color ->
            drawRect(color, topLeft = Offset(startX + i * sw, cy - rY), size = Size(sw, rY * 2f))
            // Her panelin sol kenarında beyaz parlaklık şeridi
            drawRect(
                Color.White.copy(alpha = 0.20f),
                topLeft = Offset(startX + i * sw, cy - rY),
                size    = Size(sw * 0.32f, rY * 2f)
            )
        }
        // Genel parlama — sol üst
        drawOval(
            brush   = Brush.radialGradient(
                colors  = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                center  = Offset(cx - rX * 0.52f, cy - rY * 0.52f),
                radius  = rX * 0.90f
            ),
            topLeft = Offset(cx - rX * 1.22f, cy - rY),
            size    = Size(rX * 2.44f, rY * 2f)
        )
    }

    // ── Enlem çizgileri (dekoratif) ───────────────────────────────────────
    clipPath(balloonPath) {
        for (b in 1..3) {
            val bandY = cy - rY + b * rY * 2f / 4f
            drawLine(Color.Black.copy(alpha = 0.10f),
                Offset(cx - rX * 1.25f, bandY), Offset(cx + rX * 1.25f, bandY), 1.5f)
        }
    }

    // ── Dış kontur ────────────────────────────────────────────────────────
    drawPath(balloonPath, Color.Black.copy(alpha = 0.22f), style = Stroke(2.5f))

    // ── İpler ─────────────────────────────────────────────────────────────
    val neckY    = cy + rY * 0.96f
    val neckHW   = rX * 0.17f
    val basketW  = rX * 0.72f
    val basketH  = rY * 0.46f
    val bTop     = neckY + rY * 0.10f

    drawLine(Color(0xFF6D4C41), Offset(cx - neckHW, neckY), Offset(cx - basketW * 0.38f, bTop), 2.5f)
    drawLine(Color(0xFF6D4C41), Offset(cx + neckHW, neckY), Offset(cx + basketW * 0.38f, bTop), 2.5f)

    // ── Sepet (trapez) ────────────────────────────────────────────────────
    val bL  = cx - basketW * 0.46f;  val bR  = cx + basketW * 0.46f
    val bBL = cx - basketW * 0.50f;  val bBR = cx + basketW * 0.50f
    val basketPath = Path().apply {
        moveTo(bL, bTop); lineTo(bR, bTop)
        lineTo(bBR, bTop + basketH); lineTo(bBL, bTop + basketH)
        close()
    }
    drawPath(basketPath, Color(0xFFBCAAA4))

    // Hasır ızgara
    for (c in 1..2) {
        val t = c / 3f
        drawLine(Color(0xFF8D6E63),
            Offset(bL + (bBL - bL) * t + (bR - bL) * t, bTop),
            Offset(bBL + (bBR - bBL) * t, bTop + basketH), 1.2f)
    }
    for (r in 1..2) {
        val t = r / 3f; val lineY = bTop + basketH * t
        val lx = bL + (bBL - bL) * t;  val rx = bR + (bBR - bR) * t
        drawLine(Color(0xFF8D6E63), Offset(lx, lineY), Offset(rx, lineY), 1.2f)
    }

    // Üst kasnak
    drawRoundRect(Color(0xFF4E342E),
        topLeft = Offset(cx - basketW * 0.48f, bTop - basketH * 0.10f),
        size = Size(basketW * 0.96f, basketH * 0.22f), cornerRadius = CornerRadius(4f))
    // Alt kasnak
    drawRoundRect(Color(0xFF4E342E),
        topLeft = Offset(bBL, bTop + basketH - basketH * 0.12f),
        size = Size(basketW, basketH * 0.14f), cornerRadius = CornerRadius(3f))
    // Brülör daireleri
    drawCircle(Color(0xFF3E2723), basketH * 0.10f, Offset(cx - basketW * 0.17f, bTop + basketH * 0.06f))
    drawCircle(Color(0xFF3E2723), basketH * 0.10f, Offset(cx + basketW * 0.17f, bTop + basketH * 0.06f))

    // ── Rakam etiketi (intro balonunda) ──────────────────────────────────
    if (label != null && cx > -rX * 1.3f && cx < size.width + rX * 1.3f) {
        val style = TextStyle(color = Color.White, fontSize = (rX * 0.60f).sp, fontWeight = FontWeight.ExtraBold)
        val m  = measurer.measure(label, style)
        val tw = m.size.width.toFloat().coerceAtLeast(1f)
        val th = m.size.height.toFloat().coerceAtLeast(1f)
        // Sayının arkasına yarı saydam daire
        drawCircle(Color.Black.copy(alpha = 0.30f), (tw.coerceAtLeast(th)) * 0.68f, Offset(cx, cy - rY * 0.08f))
        drawText(measurer, label, style = style,
            topLeft = Offset(cx - tw / 2f, cy - th / 2f - rY * 0.08f),
            size = Size(tw, th))
    }
}

private fun DrawScope.drawSmallBalloon(
    cx: Float, cy: Float,
    radius: Float,
    stripes: List<Color>,
    number: Int,
    measurer: androidx.compose.ui.text.TextMeasurer
) {
    val rX = radius
    val rY = radius * 1.18f
    val balloonPath = balloonBodyPath(cx, cy, rX, rY)
    val totalW = rX * 2.38f
    val startX = cx - rX * 1.19f

    // Renkli paneller
    clipPath(balloonPath) {
        val n  = stripes.size
        val sw = totalW / n
        stripes.forEachIndexed { i, color ->
            drawRect(color, topLeft = Offset(startX + i * sw, cy - rY), size = Size(sw, rY * 2f))
        }
        // Parlama
        drawOval(
            brush   = Brush.radialGradient(
                colors  = listOf(Color.White.copy(alpha = 0.32f), Color.Transparent),
                center  = Offset(cx - rX * 0.40f, cy - rY * 0.40f),
                radius  = rX * 0.72f
            ),
            topLeft = Offset(cx - rX * 1.22f, cy - rY),
            size    = Size(rX * 2.44f, rY * 2f)
        )
    }
    drawPath(balloonPath, Color.Black.copy(alpha = 0.16f), style = Stroke(1.5f))

    // Düğüm + ip
    val knotY = cy + rY * 0.96f
    drawCircle(Color(0xFF4E342E), radius * 0.09f, Offset(cx, knotY))
    drawLine(Color(0xFF795548), Offset(cx, knotY), Offset(cx, knotY + radius * 0.48f), 1.5f)

    // Sayı — canvas içindeyse
    if (cx > -rX * 1.3f && cx < size.width + rX * 1.3f && cy > -rY * 2f && cy < size.height + rY) {
        val style = TextStyle(color = Color.White, fontSize = (radius * 0.64f).sp, fontWeight = FontWeight.ExtraBold)
        val m  = measurer.measure("$number", style)
        val tw = m.size.width.toFloat().coerceAtLeast(1f)
        val th = m.size.height.toFloat().coerceAtLeast(1f)
        drawText(measurer, "$number", style = style,
            topLeft = Offset(cx - tw / 2f, cy - th / 2f - rY * 0.05f),
            size = Size(tw, th))
    }
}
