package com.ilkokuluncu.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.R
import com.ilkokuluncu.app.data.QuarterGameEvent
import com.ilkokuluncu.app.data.QuarterGameState
import com.ilkokuluncu.app.data.QuarterPhase
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─── Arka plan renkleri ───────────────────────────────────────────────────────
private val qBgColors = listOf(
    listOf(Color(0xFF7C3AED), Color(0xFF0EA5E9)),
    listOf(Color(0xFF059669), Color(0xFF0EA5E9)),
    listOf(Color(0xFFDC2626), Color(0xFF7C3AED)),
    listOf(Color(0xFFD97706), Color(0xFF059669)),
    listOf(Color(0xFF1D4ED8), Color(0xFF7C3AED)),
    listOf(Color(0xFF0F766E), Color(0xFF1D4ED8)),
    listOf(Color(0xFF7C3AED), Color(0xFFDC2626)),
    listOf(Color(0xFF0369A1), Color(0xFF059669))
)
private val qClockColors = listOf(
    Color(0xFF7C3AED), Color(0xFF059669), Color(0xFFDC2626), Color(0xFFD97706),
    Color(0xFF1D4ED8), Color(0xFF0F766E), Color(0xFF9333EA), Color(0xFF0369A1)
)

// ─── Ana Ekran ────────────────────────────────────────────────────────────────
@Composable
fun QuarterGameScreen(
    state: QuarterGameState,
    onEvent: (QuarterGameEvent) -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.phase == QuarterPhase.SUCCESS) {
        QuarterSuccessScreen(
            score       = state.score,
            bestScore   = state.bestScore,
            isNewRecord = state.newRecord,
            onRetry     = { onEvent(QuarterGameEvent.StartFresh) },
            onBack      = onBackPress
        )
        return
    }

    val bgColors    = qBgColors[state.bgIndex % qBgColors.size]
    val clockColor  = qClockColors[state.bgIndex % qClockColors.size]
    val timerFrac   = (state.timeLeft / 10f).coerceIn(0f, 1f)
    val timerColor  = when {
        timerFrac > 0.6f -> Color(0xFF00E676)
        timerFrac > 0.3f -> Color(0xFFFFD600)
        else             -> Color(0xFFFF5252)
    }
    val timerAnim by animateFloatAsState(timerFrac, tween(90), label = "timerQ")

    // feedback overlay animasyonu
    val fbAlpha by animateFloatAsState(
        targetValue   = if (state.phase == QuarterPhase.FEEDBACK) 1f else 0f,
        animationSpec = tween(160),
        label         = "fbAlpha"
    )
    val fbCorrect = state.lastAnswerCorrect == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgColors))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackPress,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                }
                Text(
                    "🕐 Çeyrek Geçe / Kala",
                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                )
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.9f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏆", fontSize = 14.sp)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            if (state.bestScore > 0) "${state.bestScore}" else "—",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Skor + hedef çubuğu (0 → 300) ───────────────────────────
            val progressFrac = (state.score / 300f).coerceIn(0f, 1f)
            val progressAnim by animateFloatAsState(progressFrac, tween(200), label = "prog")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "⭐ ${state.score}",
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                )
                Text(
                    "Hedef: 300",
                    fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f)
                )
            }
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressAnim)
                        .background(Color(0xFFFFD600), RoundedCornerShape(6.dp))
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Soru metni kartı ─────────────────────────────────────────
            AnimatedContent(
                targetState = state.questionText(),
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.85f)) togetherWith
                    fadeOut(tween(150))
                },
                label = "qText"
            ) { txt ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text       = txt,
                        fontSize   = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Saat kadranı ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawQuarterClock(
                        hour        = state.clockHour,
                        minute      = state.clockMinute,
                        accentColor = clockColor
                    )
                }

                // Feedback overlay
                if (fbAlpha > 0f) {
                    val fbColor = if (fbCorrect) Color(0xFF00C853) else Color(0xFFD50000)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(fbColor.copy(alpha = 0.78f * fbAlpha), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (fbCorrect) "✅" else "❌",
                                fontSize = 52.sp
                            )
                            val delta = state.pointsDelta
                            Text(
                                if (delta >= 0) "+$delta" else "$delta",
                                fontSize   = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Doğru / Yanlış butonları ─────────────────────────────────
            val buttonsEnabled = state.phase == QuarterPhase.ANSWERING
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Doğru butonu
                AnswerButton(
                    label   = "✅  Doğru",
                    bgColor = Color(0xFF00C853),
                    enabled = buttonsEnabled,
                    modifier = Modifier.weight(1f)
                ) { onEvent(QuarterGameEvent.AnswerTrue) }

                // Yanlış butonu
                AnswerButton(
                    label   = "❌  Yanlış",
                    bgColor = Color(0xFFD50000),
                    enabled = buttonsEnabled,
                    modifier = Modifier.weight(1f)
                ) { onEvent(QuarterGameEvent.AnswerFalse) }
            }

            Spacer(Modifier.height(18.dp))

            // ── Timer çubuğu ─────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%.1f s".format(state.timeLeft.coerceAtLeast(0f)),
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(13.dp)
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(7.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(timerAnim)
                            .background(timerColor, RoundedCornerShape(7.dp))
                    )
                }
            }
        }

        // ── Doğru cevap konfetisi — Column'ın üstünde, tüm ekranı kaplar ──
        if (state.phase == QuarterPhase.FEEDBACK && state.lastAnswerCorrect == true) {
            key(state.questionCount) {          // her soru için taze başlat
                ConfettiEffect(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// ─── Doğru / Yanlış buton bileşeni ───────────────────────────────────────────
@Composable
private fun AnswerButton(
    label: String,
    bgColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "btnScale"
    )
    LaunchedEffect(pressed) { if (pressed) { delay(120); pressed = false } }

    Button(
        onClick  = { if (enabled) { pressed = true; onClick() } },
        enabled  = enabled,
        shape    = RoundedCornerShape(18.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = bgColor,
            disabledContainerColor = bgColor.copy(alpha = 0.4f)
        ),
        modifier = modifier.height(58.dp).scale(scale)
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

// ─── Başarı ekranı ────────────────────────────────────────────────────────────
@Composable
private fun QuarterSuccessScreen(
    score: Int,
    bestScore: Int,
    isNewRecord: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    // Frame animasyonu
    val frames = remember {
        listOf(
            R.drawable.frame_001, R.drawable.frame_004, R.drawable.frame_007,
            R.drawable.frame_010, R.drawable.frame_013, R.drawable.frame_016,
            R.drawable.frame_019, R.drawable.frame_022, R.drawable.frame_025,
            R.drawable.frame_028, R.drawable.frame_031, R.drawable.frame_034,
            R.drawable.frame_037, R.drawable.frame_040
        )
    }
    var frameIdx by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { delay(80); frameIdx = (frameIdx + 1) % frames.size }
    }

    // Pulsating yazı
    val inf = rememberInfiniteTransition(label = "pulse")
    val textScale by inf.animateFloat(
        initialValue  = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label         = "ts"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A0040), Color(0xFF003366)))),
        contentAlignment = Alignment.Center
    ) {
        // ── Havai fişek ──────────────────────────────────────────────────
        FireworksEffect(modifier = Modifier.fillMaxSize())

        // ── İçerik ───────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Animasyonlu frame karakteri
            Image(
                painter           = painterResource(frames[frameIdx]),
                contentDescription = null,
                modifier          = Modifier.size(180.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Başarı mesajı
            Text(
                text       = "🎉 Başardın Dostum! 🎉",
                fontSize   = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color(0xFFFFD600),
                textAlign  = TextAlign.Center,
                modifier   = Modifier.scale(textScale)
            )

            Spacer(Modifier.height(16.dp))

            // Skor kutusu
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.12f)
            ) {
                Column(
                    modifier            = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "⭐ Skor: $score",
                        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                    )
                    if (isNewRecord) {
                        Spacer(Modifier.height(6.dp))
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFD600)) {
                            Text(
                                "🏆 Yeni Rekor!",
                                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A237E),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    } else if (bestScore > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "En iyi: $bestScore",
                            fontSize = 15.sp, color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))

            Button(
                onClick  = onRetry,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(18.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text("Tekrar Oyna 🔄", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onBack) {
                Text("Menüye Dön", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
            }
        }
    }
}

// ─── Konfeti (doğru cevap) ────────────────────────────────────────────────────
@Composable
private fun ConfettiEffect(modifier: Modifier = Modifier) {

    data class Piece(
        val x: Float,          // 0..1
        val y: Float,          // 0..1
        val vy: Float,         // düşme hızı
        val vx: Float,         // yatay sürüklenme
        val rotation: Float,
        val rotSpeed: Float,
        val color: Color,
        val w: Float, val h: Float  // genişlik & yükseklik (px)
    )

    val confettiColors = listOf(
        Color(0xFFFF5252), Color(0xFFFFD600), Color(0xFF69F0AE),
        Color(0xFF40C4FF), Color(0xFFE040FB), Color(0xFFFF6E40),
        Color(0xFFFFFFFF), Color(0xFF00E5FF), Color(0xFFFF4081)
    )

    // Başlangıçta 60 parça oluştur, yukarıdan dağıt
    val initialPieces = remember {
        List(60) {
            Piece(
                x        = Random.nextFloat(),
                y        = -0.05f - Random.nextFloat() * 0.4f,   // ekranın üstünden başla
                vy       = 0.004f + Random.nextFloat() * 0.005f,
                vx       = (Random.nextFloat() - 0.5f) * 0.003f,
                rotation = Random.nextFloat() * 360f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 8f,
                color    = confettiColors.random(),
                w        = 8f + Random.nextFloat() * 10f,
                h        = 4f + Random.nextFloat() * 6f
            )
        }
    }

    var pieces by remember { mutableStateOf(initialPieces) }

    LaunchedEffect(Unit) {
        while (pieces.isNotEmpty()) {
            delay(16)
            pieces = pieces
                .map { p ->
                    p.copy(
                        x        = p.x + p.vx,
                        y        = p.y + p.vy,
                        rotation = (p.rotation + p.rotSpeed) % 360f,
                        vy       = p.vy + 0.00008f   // hafif yerçekimi
                    )
                }
                .filter { it.y < 1.15f }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        for (p in pieces) {
            val cx = p.x * w; val cy = p.y * h
            withTransform({
                translate(cx, cy)
                rotate(p.rotation, Offset.Zero)
            }) {
                drawRect(
                    color   = p.color.copy(alpha = ((1f - p.y).coerceIn(0.3f, 1f))),
                    topLeft = Offset(-p.w / 2f, -p.h / 2f),
                    size    = Size(p.w, p.h)
                )
            }
        }
    }
}

// ─── Havai Fişek (particle system) ───────────────────────────────────────────
@Composable
private fun FireworksEffect(modifier: Modifier = Modifier) {

    data class Particle(
        val x: Float, val y: Float,
        val vx: Float, val vy: Float,
        val color: Color,
        val life: Float,
        val size: Float
    )

    val colors = listOf(
        Color(0xFFFF5252), Color(0xFFFFD600), Color(0xFF69F0AE),
        Color(0xFF40C4FF), Color(0xFFE040FB), Color(0xFFFF6E40), Color(0xFFFFFFFF)
    )

    var particles by remember { mutableStateOf(emptyList<Particle>()) }

    LaunchedEffect(Unit) {
        var tick = 0
        while (true) {
            delay(16)
            tick++
            val list = particles.toMutableList()

            // Her 28 karede bir patlama
            if (tick % 28 == 0 || list.size < 10) {
                val bx = 0.12f + Random.nextFloat() * 0.76f
                val by = 0.08f + Random.nextFloat() * 0.50f
                repeat(22) { i ->
                    val angleDeg = i * (360f / 22f) + Random.nextFloat() * 10f
                    val speed    = 0.003f + Random.nextFloat() * 0.006f
                    val rad      = Math.toRadians(angleDeg.toDouble())
                    list.add(
                        Particle(
                            x     = bx,
                            y     = by,
                            vx    = (cos(rad) * speed).toFloat(),
                            vy    = (sin(rad) * speed).toFloat(),
                            color = colors.random(),
                            life  = 1f,
                            size  = 4f + Random.nextFloat() * 6f
                        )
                    )
                }
            }

            particles = list
                .map { p -> p.copy(x = p.x + p.vx, y = p.y + p.vy, vy = p.vy + 0.00025f, life = p.life - 0.022f) }
                .filter { it.life > 0f }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        for (p in particles) {
            drawCircle(
                color  = p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                radius = p.size,
                center = Offset(p.x * w, p.y * h)
            )
        }
    }
}

// ─── Statik saat kadranı çizimi ───────────────────────────────────────────────
private fun DrawScope.drawQuarterClock(hour: Int, minute: Int, accentColor: Color) {
    val radius = minOf(size.width, size.height) / 2f * 0.88f
    val center = Offset(size.width / 2f, size.height / 2f)

    drawCircle(Color.White, radius, center)
    drawCircle(accentColor.copy(alpha = 0.08f), radius, center)
    drawCircle(accentColor, radius, center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(6f))

    // Saat tikleri
    for (h in 0 until 12) {
        val a   = Math.toRadians((h * 30 - 90).toDouble())
        val out = radius * 0.94f; val inn = radius * 0.78f
        drawLine(accentColor,
            Offset(center.x + (inn * cos(a)).toFloat(), center.y + (inn * sin(a)).toFloat()),
            Offset(center.x + (out * cos(a)).toFloat(), center.y + (out * sin(a)).toFloat()),
            6f, StrokeCap.Round)
    }

    // Dakika tikleri
    for (m in 0 until 60) {
        if (m % 5 == 0) continue
        val a = Math.toRadians((m * 6 - 90).toDouble())
        drawLine(accentColor.copy(alpha = 0.22f),
            Offset(center.x + (radius * 0.88f * cos(a)).toFloat(), center.y + (radius * 0.88f * sin(a)).toFloat()),
            Offset(center.x + (radius * 0.94f * cos(a)).toFloat(), center.y + (radius * 0.94f * sin(a)).toFloat()),
            2f, StrokeCap.Round)
    }

    // Sayılar
    val ring  = radius * 0.62f
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign   = android.graphics.Paint.Align.CENTER
        this.color  = accentColor.toArgb()
        textSize    = radius * 0.22f
        typeface    = android.graphics.Typeface.DEFAULT_BOLD
    }
    drawContext.canvas.nativeCanvas.apply {
        for (h in 1..12) {
            val a  = Math.toRadians((h * 30 - 90).toDouble())
            val tx = center.x + (ring * cos(a)).toFloat()
            val ty = center.y + (ring * sin(a)).toFloat() - (paint.ascent() + paint.descent()) / 2f
            drawText("$h", tx, ty, paint)
        }
    }

    // ── Akrep (kısa, kalın) ──────────────────────────────────────────────
    val hourAngle = Math.toRadians(((hour % 12) * 30f + minute * 0.5f - 90).toDouble())
    drawLine(accentColor.copy(alpha = 0.25f), center,
        Offset(center.x + (radius * 0.48f * cos(hourAngle)).toFloat(), center.y + (radius * 0.48f * sin(hourAngle)).toFloat()),
        22f, StrokeCap.Round)
    drawLine(Color(0xFF1A1A2E), center,
        Offset(center.x + (radius * 0.48f * cos(hourAngle)).toFloat(), center.y + (radius * 0.48f * sin(hourAngle)).toFloat()),
        10f, StrokeCap.Round)

    // ── Yelkovan (uzun, ince) ─────────────────────────────────────────────
    val minAngle = Math.toRadians((minute * 6f - 90).toDouble())
    drawLine(accentColor.copy(alpha = 0.25f), center,
        Offset(center.x + (radius * 0.70f * cos(minAngle)).toFloat(), center.y + (radius * 0.70f * sin(minAngle)).toFloat()),
        14f, StrokeCap.Round)
    drawLine(Color(0xFF1A1A2E), center,
        Offset(center.x + (radius * 0.70f * cos(minAngle)).toFloat(), center.y + (radius * 0.70f * sin(minAngle)).toFloat()),
        5f, StrokeCap.Round)

    // Merkez noktası
    drawCircle(Color(0xFF1A1A2E), 12f, center)
    drawCircle(Color.White, 6f, center)
}
