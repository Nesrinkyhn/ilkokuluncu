package com.ilkokuluncu.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.ilkokuluncu.app.ui.components.RedBackButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.data.TimeCalcEvent
import com.ilkokuluncu.app.data.TimeCalcGameState
import com.ilkokuluncu.app.data.TimeCalcPhase
import com.ilkokuluncu.app.ui.components.ClockLevel1FramesPlayer
import com.ilkokuluncu.app.ui.effects.rememberSoundEffectPlayer
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val GOAL_SCORE = 500

// ─── Ana ekran yönlendirici ───────────────────────────────────────────────────
@Composable
fun TimeCalcGameScreen(
    state: TimeCalcGameState,
    onEvent: (TimeCalcEvent) -> Unit,
    onBackPress: () -> Unit
) {
    val sounds = rememberSoundEffectPlayer()

    // Ses tetikleyicileri
    val prevCorrect = remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(state.lastCorrect, state.phase) {
        if (state.lastCorrect != prevCorrect.value && state.lastCorrect != null) {
            if (state.lastCorrect == true) sounds.playCorrect()
            else sounds.playWrongWithVibration()
            prevCorrect.value = state.lastCorrect
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1B2A4A))))
    ) {
        when (state.phase) {
            TimeCalcPhase.QUESTION, TimeCalcPhase.FEEDBACK -> {
                TimeCalcPlayScreen(state = state, onEvent = onEvent, onBackPress = onBackPress)
            }
            TimeCalcPhase.GAME_OVER -> {
                TimeCalcGameOverScreen(state = state, onEvent = onEvent, onBackPress = onBackPress)
            }
            TimeCalcPhase.VICTORY -> {
                TimeCalcVictoryScreen(
                    state = state,
                    onContinue = { onEvent(TimeCalcEvent.RestartGame) },
                    onBack = onBackPress,
                    sounds = sounds
                )
            }
        }
    }
}

// ─── Oyun alanı ───────────────────────────────────────────────────────────────
@Composable
private fun TimeCalcPlayScreen(
    state: TimeCalcGameState,
    onEvent: (TimeCalcEvent) -> Unit,
    onBackPress: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availH   = maxHeight
        val clockSz  = (availH * 0.30f).coerceIn(130.dp, 200.dp)
        val btnH     = (availH * 0.08f).coerceIn(48.dp, 62.dp)
        val spSm     = (availH * 0.02f).coerceIn(4.dp, 14.dp)
        val spMd     = (availH * 0.03f).coerceIn(6.dp, 20.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RedBackButton(
                    onClick = onBackPress,
                    modifier = Modifier,
                    shape = RoundedCornerShape(12.dp),
                    size = 48
                )

                // Hedef çubuğu
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "⭐ ${state.score} / $GOAL_SCORE",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(8.dp)
                            .background(Color.White.copy(0.2f), RoundedCornerShape(4.dp))
                    ) {
                        val progress = (state.score.toFloat() / GOAL_SCORE).coerceIn(0f, 1f)
                        val animProg by animateFloatAsState(progress, tween(300), label = "prog")
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animProg)
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFF00E676), Color(0xFFFFD600))),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }

                // Canlar
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) { i -> Text(if (i < state.lives) "❤️" else "🖤", fontSize = 20.sp) }
                }
            }

            Spacer(Modifier.height(spSm))

            // ── Zamanlayıcı ──────────────────────────────────────────────────
            TimerBar(timeLeft = state.timeLeft, total = 10f)

            Spacer(Modifier.height(spMd))

            // ── Soru metni ───────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.offset.label + " saat kaç olur?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            Spacer(Modifier.height(spMd))

            // ── Saat kadranı ─────────────────────────────────────────────────
            TimeCalcClockView(
                hour   = state.clockHour,
                minute = state.clockMinute,
                size   = clockSz
            )

            // Dijital zaman
            Text(
                text = "${state.clockHour}:${state.clockMinute.toString().padStart(2, '0')}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(0.85f)
            )

            Spacer(Modifier.height(spMd))

            // ── Şıklar ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isLocked = state.phase == TimeCalcPhase.FEEDBACK

                state.options.forEachIndexed { idx, (h, m) ->
                    val label = listOf("A)", "B)", "C)")[idx]
                    val isSelected = state.selectedAnswer == (h to m)
                    val isCorrect  = (h to m) == state.correctAnswer

                    val btnColor = when {
                        !isLocked                 -> Color.White.copy(0.92f)
                        isSelected && isCorrect   -> Color(0xFF00C853)
                        isSelected && !isCorrect  -> Color(0xFFD50000)
                        isCorrect && isLocked     -> Color(0xFF00C853)
                        else                      -> Color.White.copy(0.55f)
                    }
                    val textColor = when {
                        !isLocked -> Color(0xFF0D1B2A)
                        isSelected || (isCorrect && isLocked) -> Color.White
                        else -> Color(0xFF0D1B2A).copy(0.55f)
                    }

                    TimeCalcAnswerBtn(
                        label     = "$label ${h}:${m.toString().padStart(2, '0')}",
                        bgColor   = btnColor,
                        textColor = textColor,
                        enabled   = !isLocked,
                        height    = btnH
                    ) {
                        onEvent(TimeCalcEvent.Answer(h, m))
                    }
                }
            }

            Spacer(Modifier.height(spSm))

            // ── Delta göstergesi ─────────────────────────────────────────────
            if (state.phase == TimeCalcPhase.FEEDBACK && state.pointsDelta != 0) {
                val isPlus = state.pointsDelta > 0
                Text(
                    text = if (isPlus) "+${state.pointsDelta} 🎉" else "${state.pointsDelta} 😕",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isPlus) Color(0xFF00E676) else Color(0xFFFF5252)
                )
            }
        }
    }
}

// ─── Zamanlayıcı çubuğu ───────────────────────────────────────────────────────
@Composable
private fun TimerBar(timeLeft: Float, total: Float) {
    val frac    = (timeLeft / total).coerceIn(0f, 1f)
    val animFrac by animateFloatAsState(frac, tween(100), label = "timer")
    val barColor = when {
        frac > 0.5f -> Color(0xFF00E676)
        frac > 0.25f -> Color(0xFFFFAB40)
        else -> Color(0xFFFF5252)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Color.White.copy(0.15f), RoundedCornerShape(5.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animFrac)
                .background(barColor, RoundedCornerShape(5.dp))
        )
    }
}

// ─── Analog saat kadranı (büyük) ─────────────────────────────────────────────
@Composable
private fun TimeCalcClockView(
    hour: Int,
    minute: Int,
    size: Dp = 180.dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r  = this.size.width * 0.44f

        // Dış daire gölge
        drawCircle(Color(0xFF667eea).copy(0.3f), r + 8f, Offset(cx, cy))
        // Kadran
        drawCircle(Color.White, r, Offset(cx, cy))
        drawCircle(Color(0xFF1A237E), r, Offset(cx, cy), style = Stroke(4f))

        // Saat çizgileri
        for (i in 0..11) {
            val ang = (i * 30.0 - 90.0) * PI / 180.0
            val len  = if (i % 3 == 0) r * 0.18f else r * 0.10f
            val sw   = if (i % 3 == 0) 3f else 1.5f
            drawLine(
                Color(0xFF1A237E),
                Offset(cx + (r - len) * cos(ang).toFloat(), cy + (r - len) * sin(ang).toFloat()),
                Offset(cx + r * cos(ang).toFloat(),         cy + r * sin(ang).toFloat()),
                sw, StrokeCap.Round
            )
        }

        // Sayılar
        for (i in 1..12) {
            val ang  = ((i - 3) * 30.0) * PI / 180.0
            val nr   = r * 0.72f
            val nx   = cx + nr * cos(ang).toFloat()
            val ny   = cy + nr * sin(ang).toFloat()
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    textSize     = r * 0.22f
                    textAlign    = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    color        = android.graphics.Color.rgb(26, 35, 126)
                }
                drawText(i.toString(), nx, ny + r * 0.08f, paint)
            }
        }

        // Akrep
        val hFrac  = (hour % 12) + minute / 60f
        val hAngle = (hFrac * 30f - 90f) * PI.toFloat() / 180f
        drawLine(Color(0xFF1A237E), Offset(cx, cy),
            Offset(cx + r * 0.50f * cos(hAngle), cy + r * 0.50f * sin(hAngle)), 7f, StrokeCap.Round)

        // Yelkovan
        val mAngle = (minute * 6f - 90f) * PI.toFloat() / 180f
        drawLine(Color(0xFFE53935), Offset(cx, cy),
            Offset(cx + r * 0.72f * cos(mAngle), cy + r * 0.72f * sin(mAngle)), 4f, StrokeCap.Round)

        // Merkez
        drawCircle(Color(0xFF1A237E), 6f, Offset(cx, cy))
        drawCircle(Color(0xFFE53935), 4f, Offset(cx, cy))
    }
}

// ─── Şık butonu ───────────────────────────────────────────────────────────────
@Composable
private fun TimeCalcAnswerBtn(
    label: String,
    bgColor: Color,
    textColor: Color,
    enabled: Boolean,
    height: Dp,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (pressed) 0.93f else 1f,
        spring(Spring.DampingRatioMediumBouncy), label = "sc")

    Button(
        onClick = { pressed = true; onClick() },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(height).scale(sc),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor, contentColor = textColor,
            disabledContainerColor = bgColor, disabledContentColor = textColor
        )
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }

    LaunchedEffect(pressed) { if (pressed) { delay(100); pressed = false } }
}

// ─── Oyun bitti ───────────────────────────────────────────────────────────────
@Composable
private fun TimeCalcGameOverScreen(
    state: TimeCalcGameState,
    onEvent: (TimeCalcEvent) -> Unit,
    onBackPress: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💔 Canlar Bitti!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text("Puan: ${state.score}", fontSize = 24.sp, color = Color.White.copy(0.9f))
        Text("Hedef: $GOAL_SCORE", fontSize = 18.sp, color = Color(0xFFFFD600))
        Text("En İyi: ${state.bestScore}", fontSize = 18.sp, color = Color(0xFFFFD600))
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = { onEvent(TimeCalcEvent.RestartGame) },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea)),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Tekrar Dene 🔄", fontSize = 20.sp) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBackPress) {
            Text("Menüye Dön", color = Color.White.copy(0.65f), fontSize = 16.sp)
        }
    }
}

// ─── ZAFER EKRANI (500 puan) ──────────────────────────────────────────────────
@Composable
private fun TimeCalcVictoryScreen(
    state: TimeCalcGameState,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    sounds: com.ilkokuluncu.app.ui.effects.SoundEffectPlayer
) {
    var showFrames   by remember { mutableStateOf(true) }
    var showOverlay  by remember { mutableStateOf(false) }

    // Alkış sesi
    LaunchedEffect(Unit) {
        sounds.playCorrect()
        delay(600)
        sounds.playCorrect()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Arka plan rengi
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1B2A)))

        // Frame animasyonu
        if (showFrames) {
            ClockLevel1FramesPlayer(
                frameDurationMs = 120L,
                onFinish = {
                    showFrames  = false
                    showOverlay = true
                }
            )
        }

        // Havai fişek + tebrik overlay
        if (showOverlay) {
            FireworksCanvas(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val emojiScale by rememberInfiniteTransition(label = "es")
                    .animateFloat(1f, 1.25f,
                        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "emoji"
                    )

                Text("🏆", fontSize = (96 * emojiScale).sp)
                Spacer(Modifier.height(8.dp))

                Text(
                    if (state.newRecord) "🎉 YENİ REKOR! 🎉" else "🎉 TAMAMLADIN! 🎉",
                    fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    "$GOAL_SCORE puana ulaştın!",
                    fontSize = 20.sp, color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold
                )
                if (state.newRecord) {
                    Text("En iyi: ${state.bestScore}", fontSize = 16.sp, color = Color(0xFFFFD600))
                }

                Spacer(Modifier.height(36.dp))

                Button(
                    onClick = onContinue,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                    modifier = Modifier.fillMaxWidth().height(58.dp)
                ) { Text("Tekrar Oyna ▶", fontSize = 20.sp, fontWeight = FontWeight.Bold) }

                Spacer(Modifier.height(12.dp))

                TextButton(onClick = onBack) {
                    Text("Menüye Dön", color = Color.White.copy(0.65f), fontSize = 16.sp)
                }
            }
        }
    }
}

// ─── Havai fişek canvas ───────────────────────────────────────────────────────
private data class FwParticle(
    val x: Float, val y: Float,
    val vx: Float, val vy: Float,
    val color: Color, val life: Float,
    val maxLife: Float
)

@Composable
private fun FireworksCanvas(modifier: Modifier = Modifier) {
    val fwColors = listOf(
        Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFFFFD700),
        Color(0xFF00E676), Color(0xFF2979FF), Color(0xFFFF4081),
        Color(0xFFE040FB), Color(0xFFFFA726)
    )

    var particles by remember { mutableStateOf<List<FwParticle>>(emptyList()) }

    LaunchedEffect(Unit) {
        val startMs = System.currentTimeMillis()
        while (System.currentTimeMillis() - startMs < 5000L) {
            delay(16)
            // Yeni patlama her 400ms
            val elapsed = System.currentTimeMillis() - startMs
            if (elapsed % 400 < 20) {
                val bx = Random.nextFloat()
                val by = Random.nextFloat() * 0.6f + 0.1f
                val color = fwColors.random()
                val burst = List(28) {
                    val ang = Random.nextFloat() * 2f * PI.toFloat()
                    val spd = Random.nextFloat() * 0.012f + 0.004f
                    FwParticle(bx, by, cos(ang) * spd, sin(ang) * spd, color,
                        life = 1f, maxLife = 1f)
                }
                particles = (particles + burst).takeLast(300)
            }
            particles = particles
                .map { it.copy(x = it.x + it.vx, y = it.y + it.vy + 0.001f, life = it.life - 0.018f) }
                .filter { it.life > 0f }
        }
    }

    Canvas(modifier = modifier) {
        particles.forEach { p ->
            drawCircle(
                p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                radius = 6f,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}
