package com.ilkokuluncu.app.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.*
import com.ilkokuluncu.app.R
import com.ilkokuluncu.app.data.*
import com.ilkokuluncu.app.viewmodel.Ritmik5Sound
import com.ilkokuluncu.app.viewmodel.Ritmik5ViewModel
import com.ilkokuluncu.app.ui.effects.GameBackgroundMusic
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import kotlin.math.*

// ── Dondurma renkleri (ana + açık ton) ───────────────────────────────────────
private val ICE_COLORS = listOf(
    Pair(Color(0xFFFF8FAB), Color(0xFFFFCCDA)),  // 0 çilek/pembe
    Pair(Color(0xFF4ADCD6), Color(0xFF9AEAE8)),  // 1 teal
    Pair(Color(0xFF9C6FE4), Color(0xFFCBA8F0)),  // 2 lavanta/mor
    Pair(Color(0xFF5CE19A), Color(0xFFAAF0CB)),  // 3 nane/yeşil
    Pair(Color(0xFFFFAB76), Color(0xFFFFD4B3)),  // 4 şeftali/turuncu
    Pair(Color(0xFF7AB3F0), Color(0xFFBDD6F8)),  // 5 mavi
    Pair(Color(0xFFFFF176), Color(0xFFFFFABE)),  // 6 limon/sarı
    Pair(Color(0xFFFF7043), Color(0xFFFFAB91)),  // 7 mercan/kırmızı
    Pair(Color(0xFFD98EE8), Color(0xFFEFC5F5)),  // 8 leylak/mor
    Pair(Color(0xFF26C6DA), Color(0xFF80DEEA)),  // 9 cyan
)
private val CONE_FILL   = Color(0xFFF5A623)
private val CONE_DARK   = Color(0xFF3D2000)
private val CONE_SHADE  = Color(0xFFD4843A)
private val GOLD        = Color(0xFFFFD700)

// ── Kumsal renkleri ───────────────────────────────────────────────────────────
private val SKY_TOP     = Color(0xFF4FC3F7)
private val SKY_MID     = Color(0xFF81D4FA)
private val SEA_LIGHT   = Color(0xFF26C6DA)
private val SEA_DARK    = Color(0xFF0097A7)
private val SAND_LIGHT  = Color(0xFFF5DFA0)
private val SAND_DARK   = Color(0xFFD4A84B)
private val WAVE_WHITE  = Color(0xFFE0F7FA)

// Track Y konumları
private fun r5TrackYs(sh: Float) = listOf(sh * 0.78f, sh * 0.53f, sh * 0.28f)

@Composable
fun Ritmik5Screen(
    state: Ritmik5State,
    viewModel: Ritmik5ViewModel,
    onBackPress: () -> Unit
) {
    val context   = LocalContext.current
    val activity  = context as? Activity
    val density   = LocalDensity.current.density

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    GameBackgroundMusic(volume = 0.28f)

    // Donanım/sistem geri tuşu → önceki ekrana dön
    BackHandler { onBackPress() }

    val pool = remember {
        SoundPool.Builder().setMaxStreams(6)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            ).build()
    }
    val sTap     = remember { pool.load(context, R.raw.clock_tap,     1) }
    val sCorrect = remember { pool.load(context, R.raw.clock_correct, 1) }
    val sWrong   = remember { pool.load(context, R.raw.clock_wrong,   1) }
    DisposableEffect(pool) { onDispose { pool.release() } }

    LaunchedEffect(viewModel) {
        viewModel.sounds.collect { snd ->
            when (snd) {
                Ritmik5Sound.Tap      -> pool.play(sTap,     0.60f, 0.60f, 1, 0, 1.0f)
                Ritmik5Sound.Correct  -> pool.play(sCorrect, 0.90f, 0.90f, 1, 0, 1.1f)
                Ritmik5Sound.Wrong    -> pool.play(sWrong,   0.90f, 0.90f, 1, 0, 0.9f)
                Ritmik5Sound.CycleWin -> pool.play(sCorrect, 1.00f, 1.00f, 1, 0, 1.5f)
            }
        }
    }

    val tm         = rememberTextMeasurer()
    val conesState = rememberUpdatedState(state.cones)
    val phaseState = rememberUpdatedState(state.phase)

    // Sabit seed'li rastgele değerleri bir kez üret (stabil ve GC'siz)
    val gullData = remember {
        val r = java.util.Random(17L)
        List(5) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) }
    }
    val waveData = remember {
        val r = java.util.Random(33L)
        List(6) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) }
    }
    val sandData = remember {
        val r = java.util.Random(77L)
        List(20) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) }
    }

    Box(modifier = Modifier.fillMaxSize()) {


        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (phaseState.value != Ritmik5Phase.PLAYING) return@detectTapGestures
                        val sh = size.height.toFloat()
                        val trackYs = r5TrackYs(sh)
                        val cones = conesState.value
                        for (cone in cones) {
                            if (cone.hitCorrect || cone.hitWrong) continue
                            val ty = trackYs[cone.track]
                            val dist = sqrt(
                                (offset.x - cone.x).pow(2) + (offset.y - ty).pow(2)
                            )
                            if (dist <= R5_ICE_R * 1.9f) {
                                viewModel.onConeTapped(cone.id)
                                break
                            }
                        }
                    }
                }
        ) {
            val sw = size.width
            val sh = size.height

            if (state.screenW != sw) viewModel.setScreenWidth(sw)

            // ── Gökyüzü (gradient) ────────────────────────────────────────────
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF87CEEB), Color(0xFFB0E0E6)),
                    startY = 0f, endY = sh
                ),
                size = Size(sw, sh)
            )

            // ── Bulutlar ──────────────────────────────────────────────────────
            drawCloud(this, sw * 0.12f, sh * 0.15f, 35f)
            drawCloud(this, sw * 0.35f, sh * 0.08f, 28f)
            drawCloud(this, sw * 0.60f, sh * 0.18f, 40f)
            drawCloud(this, sw * 0.82f, sh * 0.10f, 30f)

            val trackYs = r5TrackYs(sh)

            // ── Track işaret çizgileri ────────────────────────────────────────
            trackYs.forEachIndexed { _, ty ->
                drawLine(Color.White.copy(alpha = 0.18f),
                    Offset(0f, ty + R5_ICE_R + R5_CONE_H * 0.6f),
                    Offset(sw, ty + R5_ICE_R + R5_CONE_H * 0.6f),
                    strokeWidth = 1.5f)
            }

            // ── Hedef çizgisi (solda) ─────────────────────────────────────────
            val targetX = sw * 0.22f
            drawLine(Color.White.copy(alpha = 0.28f),
                Offset(targetX, 0f), Offset(targetX, sh),
                strokeWidth = 2f)

            // ── Dondurma topları ──────────────────────────────────────────────
            for (cone in state.cones) {
                val ty = trackYs[cone.track]
                r5DrawIceCream(this, cone, ty, tm, density)
            }

            // ── Fail kırmızı flaşı ────────────────────────────────────────────
            if (state.phase == Ritmik5Phase.FAIL_ANIM) {
                val a = (state.failAnim / 2.2f).coerceIn(0f, 0.52f)
                drawRect(Color(0xFFD50000).copy(alpha = a))
            }
        }

        // ── Toplanan dondurma külahları (her 3 doğru = 1 🍦) ─────────────────
        val collectedCones = state.totalCorrectCatches / 3
        if (collectedCones > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                repeat(collectedCones) {
                    Text("🍦", fontSize = 28.sp)
                }
            }
        }

        // ── HUD ──────────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFE53935), CircleShape)
                        .pointerInput(Unit) { detectTapGestures { onBackPress() } },
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }

                // 5 10 15 … 50
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    RITMIK5_SEQUENCE.forEach { num ->
                        val done    = num in state.correctHits
                        val current = num == state.currentTarget && state.phase == Ritmik5Phase.PLAYING
                        Box(
                            modifier = Modifier
                                .background(
                                    when {
                                        done -> Color(0xFF4CAF50)
                                        current -> GOLD
                                        else -> Color.Black.copy(0.22f)
                                    },
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = num.toString(),
                                color      = if (current) Color.Black else Color.White,
                                fontSize   = if (current) 13.sp else 11.sp,
                                fontWeight = if (current || done) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Skor
                Column(horizontalAlignment = Alignment.End) {
                    Text("⭐ ${state.totalScore}", color = GOLD,
                        fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text("+${state.pointsPerHit} puan",
                        color = GOLD.copy(0.70f), fontSize = 11.sp)
                }
            }

            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hedef: ${state.currentTarget}",
                    color = Color(0xFF004D40), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                if (state.cycleCount > 0) {
                    Text("Tur ${state.cycleCount + 1}",
                        color = Color(0xFF00695C), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("Hız ${state.speed.toInt()}",
                    color = Color(0xFF00695C).copy(0.60f), fontSize = 11.sp)
            }
        }

        // ── Geri sayım ────────────────────────────────────────────────────────
        if (state.phase == Ritmik5Phase.COUNTDOWN) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val cdText = if (state.cycleCount == 0) {
                    val n = ceil(state.countdown).toInt().coerceIn(1, 3)
                    if (state.countdown > 0.5f) n.toString() else "Başla! 🍦"
                } else "Devam! 🌊"
                Text(
                    text       = cdText,
                    fontSize   = if (state.cycleCount == 0) 96.sp else 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (state.cycleCount == 0) Color.White else GOLD,
                    modifier   = Modifier
                        .background(Color.Black.copy(0.40f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 40.dp, vertical = 20.dp)
                )
            }
        }

        // ── Fail ekranı ───────────────────────────────────────────────────────
        if (state.phase == Ritmik5Phase.FAIL_ANIM && state.failAnim > 1.5f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍦", fontSize = 64.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Yanlış külah! Baştan başlıyoruz",
                        fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF5252))
                    Spacer(Modifier.height(8.dp))
                    Text("Sıra: ${RITMIK5_SEQUENCE.joinToString(" → ")}",
                        fontSize = 13.sp, color = Color.White.copy(0.75f))
                }
            }
        }
    }
}

// ── Sevimli Bulut ───────────────────────────────────────────────────────────
private fun drawCloud(scope: DrawScope, cx: Float, cy: Float, size: Float) {
    with(scope) {
        // 3 daire ile bulut şekli
        drawCircle(Color.White.copy(alpha = 0.85f), size, Offset(cx - size * 0.7f, cy))
        drawCircle(Color.White.copy(alpha = 0.90f), size * 1.2f, Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.85f), size, Offset(cx + size * 0.7f, cy))
    }
}

// ── Sevimli Balık ───────────────────────────────────────────────────────────
private fun drawSimpleFish(scope: DrawScope, x: Float, y: Float, color: Color) {
    with(scope) {
        // Vücut (elips)
        drawOval(color, topLeft = Offset(x - 15f, y - 8f), size = Size(30f, 16f))
        // Göz (beyaz + siyah)
        drawCircle(Color.White, 4f, Offset(x + 8f, y - 3f))
        drawCircle(Color.Black, 2f, Offset(x + 9f, y - 3f))
        // Kuyruk (üçgen)
        drawLine(color, Offset(x - 15f, y - 5f), Offset(x - 28f, y - 10f), strokeWidth = 3f)
        drawLine(color, Offset(x - 15f, y + 5f), Offset(x - 28f, y + 10f), strokeWidth = 3f)
    }
}

// ── Basit yelkenli (Path allocation yok) ─────────────────────────────────────
private fun r5DrawSimpleSailboat(scope: DrawScope, cx: Float, baseY: Float, size: Float) {
    with(scope) {
        val hw = size * 1.4f
        // Gövde — oval
        drawOval(
            Color(0xFF8D6E48),
            topLeft = Offset(cx - hw, baseY - size * 0.15f),
            size    = Size(hw * 2f, size * 0.5f)
        )
        // Direk
        drawLine(Color(0xFF5D4037),
            Offset(cx, baseY), Offset(cx, baseY - size * 2.2f),
            strokeWidth = size * 0.08f)
        // Yelken — 3 drawLine ile üçgen
        drawLine(Color.White.copy(alpha = 0.90f),
            Offset(cx, baseY - size * 2.1f), Offset(cx + hw, baseY - size * 0.3f), strokeWidth = 3f)
        drawLine(Color.White.copy(alpha = 0.90f),
            Offset(cx + hw, baseY - size * 0.3f), Offset(cx, baseY - size * 0.1f), strokeWidth = 3f)
        drawLine(Color.White.copy(alpha = 0.90f),
            Offset(cx, baseY - size * 2.1f), Offset(cx, baseY - size * 0.1f), strokeWidth = 3f)
        // Bayrak
        drawCircle(Color(0xFFFF5252), size * 0.08f, Offset(cx, baseY - size * 2.2f))
    }
}

// ── Dondurma külahı çizimi ────────────────────────────────────────────────────
private fun r5DrawIceCream(
    scope: DrawScope,
    cone: Ritmik5Cone,
    centerY: Float,
    tm: TextMeasurer,
    density: Float
) {
    with(scope) {
        val cx = cone.x
        val scoopCY = centerY  // Alt topun merkezi (doğrudan center)

        val alpha = when {
            cone.hitWrong && !cone.hitCorrect -> (cone.anim / 0.6f).coerceIn(0f, 1f)
            else -> 1f
        }
        val scale = when {
            cone.hitCorrect -> 1f + (cone.anim / 0.7f) * 0.20f
            cone.hitWrong   -> (cone.anim / 0.6f).coerceIn(0.2f, 1f)
            else            -> 1f
        }.coerceIn(0.2f, 1.5f)

        withTransform({ scale(scale, scale, Offset(cx, centerY)) }) {

            val (bottomColor, bottomLight) = ICE_COLORS[cone.colorIdx % ICE_COLORS.size]
            val (topColor,    topLight)    = ICE_COLORS[(cone.colorIdx + 3) % ICE_COLORS.size]

            val bottomC = when {
                cone.hitCorrect -> Color(0xFF4CAF50)
                cone.hitWrong   -> Color(0xFFD50000)
                else            -> bottomColor
            }
            val topC = when {
                cone.hitCorrect -> Color(0xFF66BB6A)
                cone.hitWrong   -> Color(0xFFEF5350)
                else            -> topColor
            }

            // Külah yok — sadece iki dondurma topu

            // ─── 1) Alt top (büyük) ──────────────────────────────────────────
            // Outline
            drawCircle(CONE_DARK.copy(alpha = alpha), R5_ICE_R + 3f, Offset(cx, scoopCY))
            // Gölge (hafif)
            drawCircle(bottomC.copy(alpha = 0.35f * alpha), R5_ICE_R + 10f, Offset(cx + 3f, scoopCY + 3f))
            // Ana renk
            drawCircle(bottomC.copy(alpha = alpha), R5_ICE_R, Offset(cx, scoopCY))
            // Parlak nokta (büyük)
            drawCircle(Color.White.copy(alpha = 0.80f * alpha), R5_ICE_R * 0.26f,
                Offset(cx - R5_ICE_R * 0.33f, scoopCY - R5_ICE_R * 0.33f))
            // Parlak nokta (küçük)
            drawCircle(Color.White.copy(alpha = 0.55f * alpha), R5_ICE_R * 0.13f,
                Offset(cx - R5_ICE_R * 0.18f, scoopCY - R5_ICE_R * 0.55f))

            // ─── 2) Üst top (küçük) ───────────────────────────────────────────
            val topR  = R5_ICE_R * 0.80f
            val topCX = cx + R5_ICE_R * 0.08f
            val topCY = scoopCY - R5_ICE_R * 0.90f
            // Outline
            drawCircle(CONE_DARK.copy(alpha = alpha), topR + 3f, Offset(topCX, topCY))
            // Ana renk
            drawCircle(topC.copy(alpha = alpha), topR, Offset(topCX, topCY))
            // Parlak
            drawCircle(Color.White.copy(alpha = 0.80f * alpha), topR * 0.26f,
                Offset(topCX - topR * 0.32f, topCY - topR * 0.32f))
            drawCircle(Color.White.copy(alpha = 0.50f * alpha), topR * 0.13f,
                Offset(topCX - topR * 0.15f, topCY - topR * 0.54f))
            // Hafif renk lekesi (derinlik için)
            drawCircle(topLight.copy(alpha = 0.40f * alpha), topR * 0.45f,
                Offset(topCX + topR * 0.18f, topCY + topR * 0.20f))

            // ─── 3) Hedef: altın çerçeve ─────────────────────────────────────
            val isTarget = cone.isCorrect && !cone.hitCorrect && !cone.hitWrong
            if (isTarget) {
                drawCircle(GOLD.copy(alpha = 0.90f), R5_ICE_R + 6f, Offset(cx, scoopCY),
                    style = Stroke(3.5f))
                repeat(6) { i ->
                    val angle = (i.toFloat() / 6f) * 2f * PI.toFloat()
                    drawCircle(GOLD.copy(alpha = 0.88f), 5f,
                        Offset(cx + cos(angle) * (R5_ICE_R + 18f),
                               scoopCY + sin(angle) * (R5_ICE_R + 18f)))
                }
            }

            // ─── 4) Sayı (alt topun ortasında) ───────────────────────────────
            val label = when {
                cone.hitCorrect -> "✓"
                cone.hitWrong   -> "✗"
                else            -> cone.number.toString()
            }
            val fsPx = if (cone.number >= 10) 48f else 54f
            val style = TextStyle(
                fontSize   = (fsPx / density).sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White.copy(alpha = alpha),
                shadow     = Shadow(Color.Black.copy(0.60f), Offset(1.5f, 1.5f), 3f)
            )
            val m = tm.measure(label, style)
            drawText(m, topLeft = Offset(cx - m.size.width / 2f, scoopCY - m.size.height / 2f))
        }
    }
}
