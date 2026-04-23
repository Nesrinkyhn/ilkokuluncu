package com.ilkokuluncu.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.ilkokuluncu.app.ui.components.RedBackButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.data.MatchGameEvent
import com.ilkokuluncu.app.data.MatchGameState
import com.ilkokuluncu.app.data.MatchItem
import com.ilkokuluncu.app.ui.components.CelebrationEffect
import com.ilkokuluncu.app.ui.effects.rememberSoundEffectPlayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Sabitler ─────────────────────────────────────────────────────────────────
private const val GOAL_CORRECT  = 50
private const val MAX_WRONG     = 5
private const val TOTAL_TIME    = 750f

// 10 parlak çocuk rengi
private val ITEM_COLORS = listOf(
    Color(0xFFFF1744), // parlak kırmızı
    Color(0xFFD500F9), // neon mor
    Color(0xFF2979FF), // parlak mavi
    Color(0xFF00E5FF), // neon camgöbeği
    Color(0xFF00E676), // neon yeşil
    Color(0xFFFF9100), // parlak turuncu
    Color(0xFFFFEA00), // parlak sarı
    Color(0xFFFF4081), // sıcak pembe
    Color(0xFF69F0AE), // açık yeşil
    Color(0xFF40C4FF), // açık mavi
)

// ── Ana ekran ─────────────────────────────────────────────────────────────────
@Composable
fun MatchGameScreen(
    state: MatchGameState,
    onEvent: (MatchGameEvent) -> Unit,
    onBackPress: () -> Unit
) {
    val sounds = rememberSoundEffectPlayer()

    val prevCorrect = remember { mutableStateOf(0) }
    LaunchedEffect(state.totalCorrect) {
        if (state.totalCorrect > prevCorrect.value) sounds.playCorrect()
        prevCorrect.value = state.totalCorrect
    }
    LaunchedEffect(state.wrongFlashIds) {
        if (state.wrongFlashIds != null) sounds.playWrongWithVibration()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isVictory  -> {
                MatchVictoryScreen(state = state, onBack = onBackPress)
                CelebrationEffect()
            }
            state.isGameOver -> GameOverScreen(
                state     = state,
                onRestart = { onEvent(MatchGameEvent.RestartGame) },
                onBack    = onBackPress
            )
            state.labelItems.isEmpty() -> Box(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0D1B6E), Color(0xFF1A237E))))
            )
            else -> MatchPlayArea(state = state, onEvent = onEvent, onBack = onBackPress)
        }
    }
}

// ── Oyun alanı ────────────────────────────────────────────────────────────────
@Composable
private fun MatchPlayArea(
    state: MatchGameState,
    onEvent: (MatchGameEvent) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1B6E), Color(0xFF1A237E))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RedBackButton(
                    onClick = onBack,
                    modifier = Modifier,
                    shape = RoundedCornerShape(10.dp),
                    size = 60
                )

                // Puan
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.15f)) {
                    Text(
                        "⭐ ${state.score}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }

                // Lolipop canlar
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(MAX_WRONG) { i ->
                        Text(
                            text     = if (i < state.wrongCount) "💀" else "🍭",
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(5.dp))

            // ── İlerleme + En iyi skor ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("✓ ${state.totalCorrect}/$GOAL_CORRECT", fontSize = 12.sp, color = Color.White.copy(0.85f))
                Text("🏆 ${state.bestScore}", fontSize = 12.sp, color = Color(0xFFFFD600))
            }
            Spacer(Modifier.height(2.dp))
            // Doğru cevap ilerleme çubuğu
            Box(
                modifier = Modifier.fillMaxWidth().height(5.dp)
                    .clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.totalCorrect / GOAL_CORRECT.toFloat())
                        .fillMaxHeight()
                        .background(Color(0xFF00E676))
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Toplam süre çubuğu ──────────────────────────────────────────
            val timeRatio = state.totalTimeLeft / TOTAL_TIME
            val timerColor = when {
                timeRatio > 0.5f -> Color(0xFF00E676)
                timeRatio > 0.25f -> Color(0xFFFFB300)
                else              -> Color(0xFFFF5252)
            }
            val mins = (state.totalTimeLeft / 60).toInt()
            val secs = (state.totalTimeLeft % 60).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏱", fontSize = 12.sp)
                Text(
                    "%02d:%02d".format(mins, secs),
                    fontSize = 12.sp, color = timerColor, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(5.dp)
                    .clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(timeRatio.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(timerColor)
                )
            }

            Spacer(Modifier.height(6.dp))

            // ── Kartlar ─────────────────────────────────────────────────────
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cardH = ((maxHeight - 20.dp) / 5).coerceIn(52.dp, 96.dp)

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sol: etiket kartları
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.labelItems.forEach { item ->
                            LabelCard(
                                item           = item,
                                isSelected     = item.id == state.selectedLabelId,
                                isWrongFlash   = state.wrongFlashIds?.first == item.id,
                                isCorrectFlash = state.correctFlashIds?.first == item.id,
                                cardHeight     = cardH,
                                onClick        = { onEvent(MatchGameEvent.SelectLabel(item.id)) }
                            )
                        }
                    }

                    // Sağ: kadran kartları
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.clockItems.forEach { item ->
                            ClockCard(
                                item           = item,
                                isWrongFlash   = state.wrongFlashIds?.second == item.id,
                                isCorrectFlash = state.correctFlashIds?.second == item.id,
                                cardHeight     = cardH,
                                onClick        = { onEvent(MatchGameEvent.SelectClock(item.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Etiket kartı – canlı arka plan rengi ──────────────────────────────────────
@Composable
private fun LabelCard(
    item: MatchItem,
    isSelected: Boolean,
    isWrongFlash: Boolean,
    isCorrectFlash: Boolean,
    cardHeight: Dp,
    onClick: () -> Unit
) {
    val baseColor = ITEM_COLORS[item.colorIdx % ITEM_COLORS.size]
    val bgBrush = when {
        isCorrectFlash -> Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF43A047)))
        isWrongFlash   -> Brush.horizontalGradient(listOf(Color(0xFFB71C1C), Color(0xFFE53935)))
        isSelected     -> Brush.horizontalGradient(listOf(baseColor, baseColor.copy(alpha = 0.85f)))
        else           -> Brush.horizontalGradient(listOf(baseColor.copy(alpha = 1f), baseColor.copy(alpha = 0.75f)))
    }
    val borderColor = when {
        isCorrectFlash -> Color(0xFF00E676)
        isSelected     -> Color.White
        else           -> Color.White.copy(0.3f)
    }
    val borderWidth = if (isSelected || isCorrectFlash) 2.5.dp else 1.dp

    val sc by animateFloatAsState(
        if (isSelected) 1.04f else 1f,
        spring(Spring.DampingRatioMediumBouncy),
        label = "sc"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .scale(sc)
            .clip(RoundedCornerShape(14.dp))
            .background(bgBrush)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Açık renklerde (sarı, açık yeşil) siyah metin daha okunabilir
        val textColor = if (item.colorIdx in listOf(6, 8, 9)) Color.Black else Color.White
        Text(
            text       = item.label,
            fontSize   = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = textColor,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.padding(horizontal = 8.dp)
        )
    }
}

// ── Kadran kartı – canlı arka plan rengi ─────────────────────────────────────
@Composable
private fun ClockCard(
    item: MatchItem,
    isWrongFlash: Boolean,
    isCorrectFlash: Boolean,
    cardHeight: Dp,
    onClick: () -> Unit
) {
    val baseColor = ITEM_COLORS[item.colorIdx % ITEM_COLORS.size]

    val bgBrush = when {
        isCorrectFlash -> Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF43A047)))
        isWrongFlash   -> Brush.horizontalGradient(listOf(Color(0xFFB71C1C), Color(0xFFE53935)))
        else           -> Brush.horizontalGradient(listOf(baseColor.copy(alpha = 1f), baseColor.copy(alpha = 0.70f)))
    }
    val borderColor = when {
        isCorrectFlash -> Color(0xFF00E676)
        isWrongFlash   -> Color.White
        else           -> Color.White.copy(0.3f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val clockSize = (cardHeight.value * 0.76f).dp
        SmallAnalogClock(hour = item.hour, minute = item.minute, size = clockSize)
    }
}

// ── Küçük analog saat ─────────────────────────────────────────────────────────
@Composable
private fun SmallAnalogClock(hour: Int, minute: Int, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r  = this.size.width * 0.44f

        // Beyaz kadran yüzü
        drawCircle(Color.White, r, Offset(cx, cy))
        drawCircle(Color(0xFF1A237E), r, Offset(cx, cy), style = Stroke(3f))

        // Saat çizgileri
        for (i in 0..11) {
            val ang = (i * 30.0 - 90.0) * PI / 180.0
            val len = if (i % 3 == 0) r * 0.18f else r * 0.10f
            drawLine(
                Color(0xFF1A237E),
                Offset((cx + (r - len) * cos(ang)).toFloat(), (cy + (r - len) * sin(ang)).toFloat()),
                Offset((cx + r * cos(ang)).toFloat(), (cy + r * sin(ang)).toFloat()),
                if (i % 3 == 0) 2.5f else 1.2f,
                StrokeCap.Round
            )
        }

        // Akrep
        val hAngle = ((hour % 12 + minute / 60f) * 30f - 90f) * PI.toFloat() / 180f
        drawLine(Color(0xFF1A237E), Offset(cx, cy),
            Offset(cx + r * 0.50f * cos(hAngle), cy + r * 0.50f * sin(hAngle)), 5f, StrokeCap.Round)

        // Yelkovan
        val mAngle = (minute * 6f - 90f) * PI.toFloat() / 180f
        drawLine(Color(0xFFE53935), Offset(cx, cy),
            Offset(cx + r * 0.72f * cos(mAngle), cy + r * 0.72f * sin(mAngle)), 3f, StrokeCap.Round)

        // Merkez
        drawCircle(Color(0xFF1A237E), 4f, Offset(cx, cy))
    }
}

// ── Zafer ekranı ─────────────────────────────────────────────────────────────
@Composable
private fun MatchVictoryScreen(state: MatchGameState, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1B6E), Color(0xFF1A237E)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🎉", fontSize = 80.sp)
            Text("Tebrikler!", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("$GOAL_CORRECT soruyu tamamladın!", fontSize = 16.sp, color = Color.White.copy(0.85f))
            Spacer(Modifier.height(4.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(0.15f)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⭐ Puan: ${state.score}", fontSize = 28.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    Text("🏆 En İyi: ${state.bestScore}", fontSize = 18.sp, color = Color(0xFFFFD600))
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = onBack,
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Ana Menüye Dön 🏠", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Oyun bitti ekranı ─────────────────────────────────────────────────────────
@Composable
private fun GameOverScreen(state: MatchGameState, onRestart: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1B6E), Color(0xFF1A237E)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (state.totalTimeLeft <= 0f) "⏰" else "💀", fontSize = 64.sp)
            Text(
                if (state.totalTimeLeft <= 0f) "Süre Doldu!" else "5 Yanlış!",
                fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
            Text(
                "Doğru: ${state.totalCorrect}  •  Puan: ${state.score}",
                fontSize = 16.sp, color = Color.White.copy(0.8f)
            )
            Text("🏆 En İyi: ${state.bestScore}", fontSize = 16.sp, color = Color(0xFFFFD600))
            Spacer(Modifier.height(20.dp))
            Button(
                onClick  = onRestart,
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea)),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Yeniden Başla 🔄", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBack) {
                Text("Ana Menüye Dön", color = Color.White.copy(0.7f), fontSize = 15.sp)
            }
        }
    }
}
