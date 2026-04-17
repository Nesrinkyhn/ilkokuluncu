package com.ilkokuluncu.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.data.BallGameEvent
import com.ilkokuluncu.app.data.BallGameState
import com.ilkokuluncu.app.data.BallItem
import com.ilkokuluncu.app.data.BallPhase
import kotlin.math.cos
import kotlin.math.sin

// ─── Renkler ─────────────────────────────────────────────────────────────────
private val bgColorSets = listOf(
    listOf(Color(0xFF1A237E), Color(0xFF4A148C)),
    listOf(Color(0xFF1B5E20), Color(0xFF006064)),
    listOf(Color(0xFF880E4F), Color(0xFF311B92)),
    listOf(Color(0xFFBF360C), Color(0xFF6D4C41)),
    listOf(Color(0xFF006064), Color(0xFF1A237E)),
    listOf(Color(0xFF4A148C), Color(0xFF880E4F)),
    listOf(Color(0xFF37474F), Color(0xFF1B5E20)),
    listOf(Color(0xFF4E342E), Color(0xFF37474F))
)

private val ballColors = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
    Color(0xFFFF8F00), Color(0xFF8E24AA), Color(0xFF00838F),
    Color(0xFFE91E63), Color(0xFF00BCD4)
)

private val clockColors = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
    Color(0xFFFF8F00), Color(0xFF8E24AA), Color(0xFF00ACC1),
    Color(0xFFE91E63), Color(0xFF7B1FA2)
)

// ─── Ana ekran ────────────────────────────────────────────────────────────────
@Composable
fun BallGameScreen(
    state: BallGameState,
    onEvent: (BallGameEvent) -> Unit,
    onBackPress: () -> Unit,
    viewModel: com.ilkokuluncu.app.viewmodel.BallGameViewModel? = null,
    modifier: Modifier = Modifier
) {
    val bgColors   = bgColorSets[state.bgIndex % bgColorSets.size]
    val clockColor = clockColors[state.bgIndex % clockColors.size]

    // Sonuç ekranı
    if (state.showTestResult) {
        BallTestResultScreen(state = state, onEvent = onEvent, onBackPress = onBackPress)
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgColors))
    ) {
        if (state.isTestMode) {
            // ── Test modu ────────────────────────────────────────────────────
            BallTestPlayScreen(
                state = state, onEvent = onEvent, onBackPress = onBackPress,
                clockColor = clockColor, bgColors = bgColors, viewModel = viewModel
            )
        } else {
            // ── Normal mod ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
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
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.9f), shadowElevation = 4.dp) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐", fontSize = 22.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("${state.score}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF667eea))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Saat kadranı
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawBallClock(hour = state.currentHour, minute = state.currentMinute, color = clockColor)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Faz talimatı
                AnimatedContent(
                    targetState = state.phase,
                    transitionSpec = {
                        (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.75f)) togetherWith
                        (fadeOut(tween(180)) + scaleOut(tween(180)))
                    },
                    label = "phaseLabel"
                ) { phase ->
                    val (icon, text) = when (phase) {
                        BallPhase.HOUR   -> "🕐" to "Akrebi bul!"
                        BallPhase.MINUTE -> "🕑" to "Yelkovanı bul!"
                        BallPhase.AFERIN -> "🎉" to "Aferin!"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(icon, fontSize = 28.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = text,
                            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Zıplayan toplar alanı
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val areaW = maxWidth
                    val areaH = maxHeight
                    state.balls.forEach { ball ->
                        BouncingBall(
                            ball       = ball,
                            isWrong    = state.wrongBallId == ball.id,
                            areaWidth  = areaW,
                            areaHeight = areaH,
                            onClick    = { onEvent(BallGameEvent.BallTapped(ball.id)) }
                        )
                    }
                }
            }

            // Aferin overlay
            AnimatedVisibility(
                visible  = state.showAferin,
                enter    = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                exit     = fadeOut(tween(400)),
                modifier = Modifier.align(Alignment.Center)
            ) { AferinOverlay() }
        }

        // Test hazır dialog
        if (state.showTestReadyDialog) {
            BallTestReadyDialog(
                onAccept  = { onEvent(BallGameEvent.AcceptTestChallenge) },
                onDismiss = { onEvent(BallGameEvent.DismissTestDialog) }
            )
        }
    }
}

// ─── Test hazır dialog ────────────────────────────────────────────────────────
@Composable
private fun BallTestReadyDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 16.dp) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎯", fontSize = 56.sp)
                Spacer(Modifier.height(12.dp))
                Text("Test Zamanı!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A237E))
                Spacer(Modifier.height(8.dp))
                Text(
                    "300 puana ulaştın!\nÖnce akrebi, sonra yelkovanı bul!\n10 soruda 7 doğru → Başarılı!",
                    fontSize = 15.sp, color = Color.Gray, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
                ) { Text("Teste Başla! 🚀", fontSize = 18.sp) }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onDismiss) {
                    Text("Daha fazla çalış", color = Color.Gray)
                }
            }
        }
    }
}

// ─── Test oyun ekranı (iki aşamalı: akrep → yelkovan) ───────────────────────
@Composable
private fun BallTestPlayScreen(
    state: BallGameState,
    onEvent: (BallGameEvent) -> Unit,
    onBackPress: () -> Unit,
    clockColor: Color,
    bgColors: List<Color>,
    viewModel: com.ilkokuluncu.app.viewmodel.BallGameViewModel?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: test rozeti + soru sayacı
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.9f)) {
                Text(
                    "🎯 TEST",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E)
                )
            }
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.9f)) {
                Text(
                    "Soru ${state.testQuestion + 1}/10  ✅${state.testCorrect}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Saat kadranı (normal modla aynı)
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawBallClock(hour = state.currentHour, minute = state.currentMinute, color = clockColor)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Faz talimatı (normal modla aynı)
        AnimatedContent(
            targetState = state.phase,
            transitionSpec = {
                (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.75f)) togetherWith
                (fadeOut(tween(180)) + scaleOut(tween(180)))
            },
            label = "testPhaseLabel"
        ) { phase ->
            val (icon, text) = when (phase) {
                BallPhase.HOUR   -> "🕐" to "Akrebi bul!"
                BallPhase.MINUTE -> "🕑" to "Yelkovanı bul!"
                BallPhase.AFERIN -> "🎉" to "Aferin!"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(icon, fontSize = 28.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = text,
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Zıplayan sayı topları
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val areaW = maxWidth
            val areaH = maxHeight
            state.balls.forEach { ball ->
                BouncingBall(
                    ball       = ball,
                    isWrong    = state.wrongBallId == ball.id,
                    isCorrect  = state.testShownCorrect == ball.id,
                    areaWidth  = areaW,
                    areaHeight = areaH,
                    onClick    = {
                        if (state.testShownCorrect == null)
                            onEvent(BallGameEvent.BallTapped(ball.id))
                    }
                )
            }
        }
    }
}

// ─── Test sonuç ekranı ────────────────────────────────────────────────────────
@Composable
private fun BallTestResultScreen(
    state: BallGameState,
    onEvent: (BallGameEvent) -> Unit,
    onBackPress: () -> Unit
) {
    val passed = state.testPassed
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(
                if (passed) listOf(Color(0xFF1B5E20), Color(0xFF004D40))
                else listOf(Color(0xFF4A148C), Color(0xFF880E4F))
            )),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (passed) "🏆" else "😢", fontSize = 72.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                if (passed) "Harika! Geçtin!" else "Tekrar Dene",
                fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text("${state.testCorrect}/10 doğru", fontSize = 22.sp, color = Color.White.copy(alpha = 0.9f))
            if (passed) {
                Spacer(Modifier.height(8.dp))
                Text("🎯 Dakika Uzmanısın!", fontSize = 18.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp))
            if (passed) {
                Button(
                    onClick = onBackPress,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) { Text("Menüye Dön 🎉", fontSize = 18.sp) }
            } else {
                Button(
                    onClick = { onEvent(BallGameEvent.RetryTest) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea))
                ) { Text("Tekrar Dene 🔄", fontSize = 18.sp) }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onBackPress) {
                    Text("Menüye Dön", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// ─── Zıplayan top ─────────────────────────────────────────────────────────────
@Composable
private fun BouncingBall(
    ball: BallItem,
    isWrong: Boolean,
    isCorrect: Boolean = false,
    areaWidth: Dp,
    areaHeight: Dp,
    onClick: () -> Unit
) {
    val ballSize = 104.dp
    val maxXdp   = (areaWidth - ballSize).value.coerceAtLeast(0f)
    val maxYdp   = (areaHeight - ballSize).value.coerceAtLeast(0f)

    val inf = rememberInfiniteTransition(label = "ball${ball.id}")

    val xAnim by inf.animateFloat(
        initialValue  = ball.xStart * maxXdp,
        targetValue   = (1f - ball.xStart) * maxXdp,
        animationSpec = infiniteRepeatable(
            animation  = tween(ball.xSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bx${ball.id}"
    )
    val yAnim by inf.animateFloat(
        initialValue  = ball.yStart * maxYdp,
        targetValue   = (1f - ball.yStart) * maxYdp,
        animationSpec = infiniteRepeatable(
            animation  = tween(ball.ySpeed, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "by${ball.id}"
    )

    // Yanlış tıklamada sallama
    val shakeAnim by animateFloatAsState(
        targetValue   = if (isWrong) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.15f, stiffness = 900f),
        label         = "shake${ball.id}"
    )
    val shakeOffset = shakeAnim * 12f

    // Ölçek animasyonu (yanlış = küçül)
    val scaleAnim by animateFloatAsState(
        targetValue   = if (isWrong) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "sc${ball.id}"
    )

    val bgColor = when {
        isCorrect -> Color(0xFF00C853)
        else      -> ballColors[ball.colorIndex % ballColors.size]
    }

    Box(
        modifier = Modifier
            .offset(x = (xAnim + shakeOffset).dp, y = yAnim.dp)
            .size(ballSize)
            .scale(scaleAnim)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(bgColor.copy(alpha = 0.7f), bgColor),
                    radius = 120f
                ),
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Parlama efekti
        Box(
            modifier = Modifier
                .size(28.dp)
                .offset(x = (-14).dp, y = (-14).dp)
                .background(Color.White.copy(alpha = 0.25f), CircleShape)
        )
        Text(
            text       = ball.label,
            fontSize   = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Color.White,
            textAlign  = TextAlign.Center,
            lineHeight = 26.sp
        )
        // Doğru/yanlış overlay (test modunda)
        if (isCorrect || isWrong) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isCorrect) Color(0xFF00C853).copy(alpha = 0.55f)
                        else           Color(0xFFD50000).copy(alpha = 0.55f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isCorrect) "✅" else "❌", fontSize = 28.sp)
            }
        }
    }
}

// ─── Aferin overlay ───────────────────────────────────────────────────────────
@Composable
private fun AferinOverlay() {
    val inf = rememberInfiniteTransition(label = "aferin")
    val pulse by inf.animateFloat(
        initialValue  = 0.92f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "pulse"
    )
    val rotate by inf.animateFloat(
        initialValue  = -8f,
        targetValue   = 8f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "rot"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(pulse)
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
            .padding(horizontal = 40.dp, vertical = 28.dp)
    ) {
        Text("🎉", fontSize = 64.sp, modifier = Modifier.rotate(rotate))
        Spacer(Modifier.height(8.dp))
        Text(
            "Aferin!",
            fontSize   = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Color(0xFFFFD700)
        )
        Text(
            "Harika iş çıkardın! 🌟",
            fontSize = 16.sp,
            color    = Color.White.copy(alpha = 0.9f)
        )
    }
}

// ─── Saat kadranı (Canvas çizimi) ────────────────────────────────────────────
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBallClock(
    hour: Int,
    minute: Int,
    color: Color
) {
    val radius = minOf(size.width, size.height) / 2f * 0.88f
    val center = Offset(size.width / 2f, size.height / 2f)

    // Arka plan
    drawCircle(Color.White, radius, center)
    drawCircle(color.copy(alpha = 0.12f), radius, center)
    drawCircle(color, radius, center, style = Stroke(6f))

    // Saat tik işaretleri
    for (h in 0 until 12) {
        val angle = Math.toRadians((h * 30 - 90).toDouble())
        val outer = radius * 0.94f
        val inner = radius * 0.78f
        drawLine(
            color       = color,
            start       = Offset(center.x + (inner * cos(angle)).toFloat(), center.y + (inner * sin(angle)).toFloat()),
            end         = Offset(center.x + (outer * cos(angle)).toFloat(), center.y + (outer * sin(angle)).toFloat()),
            strokeWidth = 5f,
            cap         = StrokeCap.Round
        )
    }

    // Dakika tik işaretleri
    for (m in 0 until 60) {
        if (m % 5 == 0) continue
        val angle = Math.toRadians((m * 6 - 90).toDouble())
        val outer = radius * 0.94f
        val inner = radius * 0.88f
        drawLine(
            color       = color.copy(alpha = 0.3f),
            start       = Offset(center.x + (inner * cos(angle)).toFloat(), center.y + (inner * sin(angle)).toFloat()),
            end         = Offset(center.x + (outer * cos(angle)).toFloat(), center.y + (outer * sin(angle)).toFloat()),
            strokeWidth = 2f,
            cap         = StrokeCap.Round
        )
    }

    // Sayılar (1–12)
    val numRing = radius * 0.62f
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign   = android.graphics.Paint.Align.CENTER
        this.color  = color.toArgb()
        textSize    = radius * 0.22f
        typeface    = android.graphics.Typeface.DEFAULT_BOLD
    }
    drawContext.canvas.nativeCanvas.apply {
        for (h in 1..12) {
            val angle = Math.toRadians((h * 30 - 90).toDouble())
            val tx = center.x + (numRing * cos(angle)).toFloat()
            val ty = center.y + (numRing * sin(angle)).toFloat() - (paint.ascent() + paint.descent()) / 2f
            drawText("$h", tx, ty, paint)
        }
    }

    // Akrep (saat kolu) – kalın, kısa
    val hourAngle = Math.toRadians(((hour % 12) * 30f + minute * 0.5f - 90f).toDouble())
    drawLine(
        color       = color,
        start       = center,
        end         = Offset(
            center.x + (radius * 0.48f * cos(hourAngle)).toFloat(),
            center.y + (radius * 0.48f * sin(hourAngle)).toFloat()
        ),
        strokeWidth = 11f,
        cap         = StrokeCap.Round
    )

    // Yelkovan (dakika kolu) – ince, uzun
    val minuteAngle = Math.toRadians((minute * 6f - 90f).toDouble())
    drawLine(
        color       = Color(0xFF444444),
        start       = center,
        end         = Offset(
            center.x + (radius * 0.70f * cos(minuteAngle)).toFloat(),
            center.y + (radius * 0.70f * sin(minuteAngle)).toFloat()
        ),
        strokeWidth = 6f,
        cap         = StrokeCap.Round
    )

    // Merkez noktası
    drawCircle(color, 12f, center)
    drawCircle(Color.White, 6f, center)
}
