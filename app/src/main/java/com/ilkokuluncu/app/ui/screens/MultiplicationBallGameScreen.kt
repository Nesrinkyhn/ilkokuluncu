package com.ilkokuluncu.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.data.*

// ── Renk paleti ───────────────────────────────────────────────────────────────
private val BALL_COLORS = listOf(
    Color(0xFFFF1744), Color(0xFFD500F9), Color(0xFF2979FF), Color(0xFF00E5FF),
    Color(0xFF00E676), Color(0xFFFF9100), Color(0xFFFFEA00), Color(0xFFFF4081),
    Color(0xFF69F0AE), Color(0xFF40C4FF)
)

private fun ballColor(idx: Int) = BALL_COLORS[idx % BALL_COLORS.size]
private fun textOnBall(idx: Int): Color {
    val light = setOf(3, 4, 6, 8, 9) // açık renkler → siyah yazı
    return if (idx % BALL_COLORS.size in light) Color.Black else Color.White
}

// ── Ana ekran ─────────────────────────────────────────────────────────────────
@Composable
fun MultiplicationBallGameScreen(
    state: MultiplicationGameState,
    onEvent: (MultiplicationGameEvent) -> Unit,
    onBackPress: () -> Unit
) {
    val bg = Brush.verticalGradient(listOf(Color(0xFF0D1B4B), Color(0xFF1A3575), Color(0xFF0D1B4B)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {

        // ── Oyun oynanırken ───────────────────────────────────────────────────
        if (state.phase != MultiplicationPhase.GAME_OVER && state.phase != MultiplicationPhase.VICTORY) {

            Column(modifier = Modifier.fillMaxSize()) {

                // Üst bar
                GameHeaderBar(state = state, onBackPress = onBackPress)

                // Soru kartı
                QuestionCard(state = state)

                Spacer(Modifier.height(12.dp))

                // Balon alanı
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val areaW = constraints.maxWidth.toFloat()
                    val areaH = constraints.maxHeight.toFloat()

                    // Uçan balonlar (Faz 1 ve 2) — sayı GİZLİ (tıklayınca görünür + yukarı çıkar)
                    state.floatingBalls.forEach { ball ->
                        key(ball.id) {
                            FloatingBallItem(
                                ball       = ball,
                                areaW      = areaW,
                                areaH      = areaH,
                                ballSizePx = with(LocalDensity.current) { 76.dp.toPx() },
                                isWrong    = ball.id == state.wrongBallId,
                                isSelected = ball.id == state.selectedBallId,
                                showValue  = false,   // ← sayı gizli (isSelected ile override olur)
                                onTap      = { onEvent(MultiplicationGameEvent.FloatingBallTapped(ball.id)) }
                            )
                        }
                    }

                    // Cevap balonları (Faz 3) — sayı GÖRÜNÜR
                    state.answerBalls.forEach { ball ->
                        key(ball.id) {
                            FloatingBallItem(
                                ball           = ball,
                                areaW          = areaW,
                                areaH          = areaH,
                                ballSizePx     = with(LocalDensity.current) { 88.dp.toPx() },
                                isWrong        = ball.id == state.wrongBallId,
                                isCorrectFlash = ball.id == state.correctFlashBallId,
                                showValue      = true,    // ← sayı görünür
                                onTap          = { onEvent(MultiplicationGameEvent.AnswerBallTapped(ball.id)) }
                            )
                        }
                    }

                    // Faz ipucu metni
                    val hint = when (state.phase) {
                        MultiplicationPhase.PICK_FIRST  -> "Birinci sayıyı seç 👇"
                        MultiplicationPhase.PICK_SECOND -> "İkinci sayıyı seç 👇"
                        MultiplicationPhase.ANSWER      -> "Doğru sonucu bul!"
                        else -> ""
                    }
                    if (hint.isNotEmpty()) {
                        Text(
                            text     = hint,
                            color    = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }

        // ── Oyun bitti ────────────────────────────────────────────────────────
        if (state.phase == MultiplicationPhase.GAME_OVER) {
            GameOverOverlay(
                score    = state.score,
                onRetry  = { onEvent(MultiplicationGameEvent.RestartGame) },
                onBack   = onBackPress
            )
        }

        // ── Zafer ekranı ──────────────────────────────────────────────────────
        if (state.phase == MultiplicationPhase.VICTORY) {
            VictoryOverlay(
                score      = state.score,
                bestScore  = state.bestScore,
                isNewBest  = state.isNewBest,
                onRetry    = { onEvent(MultiplicationGameEvent.RestartGame) },
                onBack     = onBackPress
            )
        }
    }
}

// ── Üst bar: lolipoplar + soru no + puan + timer ──────────────────────────────
@Composable
fun GameHeaderBar(state: MultiplicationGameState, onBackPress: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Geri
        IconButton(
            onClick  = onBackPress,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
        }

        Spacer(Modifier.width(8.dp))

        // Lolipoplar (canlar): dolu 🍭 / boş = soluk + ❌
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { i ->
                if (i < state.lives) {
                    Text("🍭", fontSize = 18.sp)
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🍭", fontSize = 18.sp,
                            modifier = Modifier.graphicsLayer { alpha = 0.22f })
                        Text("❌", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Soru sayacı
        Text(
            text     = "${state.questionCount + 1} / 50",
            color    = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.width(12.dp))

        // Puan
        Text(
            text     = "⭐ ${state.score}",
            color    = Color(0xFFFFEA00),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.width(12.dp))

        // Timer (sadece ANSWER fazında)
        if (state.phase == MultiplicationPhase.ANSWER) {
            val timerColor by animateColorAsState(
                targetValue = when {
                    state.timeLeft > 6f -> Color(0xFF00E676)
                    state.timeLeft > 3f -> Color(0xFFFF9100)
                    else                -> Color(0xFFFF1744)
                },
                label = "timerColor"
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(timerColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "${state.timeLeft.toInt()}",
                    color      = timerColor,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ── Soru kartı ────────────────────────────────────────────────────────────────
@Composable
fun QuestionCard(state: MultiplicationGameState) {
    val first  = state.firstNumber?.toString()  ?: "?"
    val second = state.secondNumber?.toString() ?: "_"
    val result = if (state.phase == MultiplicationPhase.ANSWER) "?" else "_"

    val cardBg = Brush.horizontalGradient(
        listOf(Color(0xFF283593), Color(0xFF3949AB))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            NumberBox(first,  highlight = state.firstNumber  != null)
            Spacer(Modifier.width(10.dp))
            Text("✖", fontSize = 30.sp, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.width(10.dp))
            NumberBox(second, highlight = state.secondNumber != null)
            Spacer(Modifier.width(10.dp))
            Text("=",  fontSize = 30.sp, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.width(10.dp))
            NumberBox(result, highlight = false, isResult = true)
        }
    }
}

@Composable
private fun NumberBox(text: String, highlight: Boolean, isResult: Boolean = false) {
    val bg = when {
        isResult  -> Color(0xFFFF9100).copy(alpha = 0.3f)
        highlight -> Color(0xFF00E676).copy(alpha = 0.3f)
        else      -> Color.White.copy(alpha = 0.1f)
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = text,
            fontSize   = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Color.White
        )
    }
}

// ── Uçan / cevap balonları ────────────────────────────────────────────────────
@Composable
fun FloatingBallItem(
    ball: MultBall,
    areaW: Float,
    areaH: Float,
    ballSizePx: Float,
    isWrong: Boolean,
    isCorrectFlash: Boolean = false,
    isSelected: Boolean = false,   // tıklandı → sayıyı göster + yukarı çık
    showValue: Boolean = true,
    onTap: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ball_${ball.id}")

    // Tam ekranı kat et: xStart → xEnd
    val x by infiniteTransition.animateFloat(
        initialValue = ball.xStart * (areaW - ballSizePx),
        targetValue  = ball.xEnd   * (areaW - ballSizePx),
        animationSpec = infiniteRepeatable(
            animation  = tween(ball.xSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x_${ball.id}"
    )

    val y by infiniteTransition.animateFloat(
        initialValue = ball.yStart * (areaH - ballSizePx),
        targetValue  = ball.yEnd   * (areaH - ballSizePx),
        animationSpec = infiniteRepeatable(
            animation  = tween(ball.ySpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y_${ball.id}"
    )

    // Seçilince yukarı doğru yüksel
    val riseOffset by animateFloatAsState(
        targetValue   = if (isSelected) -260f else 0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label         = "rise_${ball.id}"
    )

    val baseColor = ballColor(ball.colorIndex)
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected     -> Color(0xFF00E676)
            isWrong        -> Color(0xFFFF1744)
            isCorrectFlash -> Color(0xFF00E676)
            else           -> baseColor
        },
        animationSpec = tween(200),
        label = "ballColor_${ball.id}"
    )

    val sizeDp = with(LocalDensity.current) { ballSizePx.toDp() }
    val displayValue = showValue || isSelected   // seçilince her zaman sayıyı göster

    Box(
        modifier = Modifier
            .offset { IntOffset(x.toInt(), (y + riseOffset).toInt()) }
            .size(sizeDp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(enabled = !isSelected, onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        if (displayValue) {
            Text(
                text       = "${ball.value}",
                fontSize   = if (sizeDp > 80.dp) 30.sp else 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = textOnBall(ball.colorIndex)
            )
        }
    }
}

// ── Oyun bitti overlay ────────────────────────────────────────────────────────
@Composable
fun GameOverOverlay(score: Int, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth(0.85f)
                .shadow(16.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1A0A2E), Color(0xFF2D1B5E))))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("😢", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("Canlar Bitti!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("Puan: $score", fontSize = 20.sp, color = Color(0xFFFFEA00), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(28.dp))
            OverlayButton("🔄 Yeniden Oyna", Color(0xFF7B1FA2), onRetry)
            Spacer(Modifier.height(12.dp))
            OverlayButton("◀ Menüye Dön", Color(0xFF37474F), onBack)
        }
    }
}

// ── Zafer overlay ─────────────────────────────────────────────────────────────
@Composable
fun VictoryOverlay(score: Int, bestScore: Int, isNewBest: Boolean, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth(0.85f)
                .shadow(16.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFF1565C0))))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (isNewBest) "🏆" else "🎉", fontSize = 72.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text       = if (isNewBest) "Yeni Rekor!" else "Tebrikler!",
                fontSize   = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            // Puan kutusu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("⭐ $score", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFEA00))
                    if (!isNewBest) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text     = "En iyi: $bestScore",
                            fontSize = 15.sp,
                            color    = Color.White.copy(alpha = 0.65f)
                        )
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text("Önceki rekor kırıldı! 🎯", fontSize = 14.sp, color = Color(0xFF00E676))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            OverlayButton("🔄 Tekrar Oyna",  Color(0xFF00897B), onRetry)
            Spacer(Modifier.height(12.dp))
            OverlayButton("◀ Menüye Dön", Color(0xFF37474F), onBack)
        }
    }
}

// ── Yardımcı buton ────────────────────────────────────────────────────────────
@Composable
private fun OverlayButton(text: String, color: Color, onClick: () -> Unit) {
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
