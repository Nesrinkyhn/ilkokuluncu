package com.ilkokuluncu.app.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
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
import com.ilkokuluncu.app.viewmodel.Ritmik4Sound
import com.ilkokuluncu.app.viewmodel.Ritmik4ViewModel
import com.ilkokuluncu.app.ui.effects.GameBackgroundMusic
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.ceil

// ── Çöl renk paleti ──────────────────────────────────────────────────────────
private val SKY_TOP      = Color(0xFF5BA3D6)
private val SKY_BOTTOM   = Color(0xFFF7C57E)
private val SAND_TOP     = Color(0xFFE8C87A)
private val SAND_BOTTOM  = Color(0xFFBF8A3C)
private val ROAD_COLOR   = Color(0xFFD4A853)
private val ROAD_DARK    = Color(0xFFC09840)
private val GOLD         = Color(0xFFFFD700)

// Araba renkleri (track başına)
private val CAR_BODY = listOf(
    Color(0xFFCD853F),  // 0=alt: kumsal tonu
    Color(0xFF6B8E23),  // 1=orta: ordu yeşili
    Color(0xFFB22222)   // 2=üst: çöl kırmızısı
)
private val CAR_DARK = listOf(
    Color(0xFF8B5E2D),
    Color(0xFF4A6319),
    Color(0xFF7A1010)
)

// Track Y konumları
private fun r4TrackYs(sh: Float) = listOf(sh * 0.76f, sh * 0.50f, sh * 0.24f)

@Composable
fun Ritmik4Screen(
    state: Ritmik4State,
    viewModel: Ritmik4ViewModel,
    onBackPress: () -> Unit
) {
    val context  = LocalContext.current
    val activity = context as? Activity
    val density  = LocalDensity.current.density

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    GameBackgroundMusic(volume = 0.28f)

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
                Ritmik4Sound.Tap      -> pool.play(sTap,     0.60f, 0.60f, 1, 0, 1.0f)
                Ritmik4Sound.Correct  -> pool.play(sCorrect, 0.90f, 0.90f, 1, 0, 1.1f)
                Ritmik4Sound.Wrong    -> pool.play(sWrong,   0.90f, 0.90f, 1, 0, 0.9f)
                Ritmik4Sound.CycleWin -> pool.play(sCorrect, 1.00f, 1.00f, 1, 0, 1.5f)
            }
        }
    }

    val tm        = rememberTextMeasurer()
    val carsState = rememberUpdatedState(state.cars)
    val phaseState = rememberUpdatedState(state.phase)

    Box(modifier = Modifier.fillMaxSize()) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (phaseState.value != Ritmik4Phase.PLAYING) return@detectTapGestures
                        val sh      = size.height.toFloat()
                        val trackYs = r4TrackYs(sh)
                        val cars    = carsState.value
                        for (car in cars) {
                            if (car.hitCorrect || car.hitWrong) continue
                            val ty = trackYs[car.track]
                            if (offset.x in car.x..(car.x + R4_CAR_W) &&
                                offset.y in (ty - R4_CAR_H / 2f)..(ty + R4_CAR_H / 2f)
                            ) {
                                viewModel.onCarTapped(car.id)
                                break
                            }
                        }
                    }
                }
        ) {
            val sw = size.width
            val sh = size.height

            if (state.screenW != sw) viewModel.setScreenWidth(sw)

            val trackYs = r4TrackYs(sh)
            val horizon = sh * 0.42f

            // ── Gökyüzü ─────────────────────────────────────────────────────
            drawRect(
                Brush.verticalGradient(
                    listOf(SKY_TOP, SKY_BOTTOM),
                    startY = 0f, endY = horizon
                ),
                size = Size(sw, horizon)
            )

            // ── Güneş ────────────────────────────────────────────────────────
            val sunX = sw * 0.88f
            val sunY = sh * 0.12f
            drawCircle(Color(0xFFFFF9C4).copy(alpha = 0.40f), 62f, Offset(sunX, sunY))
            drawCircle(Color(0xFFFFF176).copy(alpha = 0.70f), 42f, Offset(sunX, sunY))
            drawCircle(Color(0xFFFFEE58), 28f, Offset(sunX, sunY))

            // ── Uzak dağ / kum tepeleri silüeti ──────────────────────────────
            val duneRng = java.util.Random(99L)
            var dx = 0f
            while (dx < sw) {
                val dw = 80f + duneRng.nextFloat() * 140f
                val dh = 18f + duneRng.nextFloat() * 40f
                val path = Path().apply {
                    moveTo(dx, horizon)
                    quadraticBezierTo(dx + dw / 2f, horizon - dh, dx + dw, horizon)
                    close()
                }
                drawPath(path, Color(0xFFE0A84B).copy(alpha = 0.60f))
                dx += dw * 0.70f
            }

            // ── Çöl zemini ───────────────────────────────────────────────────
            drawRect(
                Brush.verticalGradient(
                    listOf(SAND_TOP, SAND_BOTTOM),
                    startY = horizon, endY = sh
                ),
                topLeft = Offset(0f, horizon),
                size    = Size(sw, sh - horizon)
            )

            // ── Yol bantları (her track için) ─────────────────────────────────
            trackYs.forEach { ty ->
                val roadTop = ty - R4_CAR_H / 2f - 10f
                val roadH   = R4_CAR_H + 20f
                // Yol yüzeyi
                drawRect(
                    ROAD_COLOR.copy(alpha = 0.70f),
                    topLeft = Offset(0f, roadTop),
                    size    = Size(sw, roadH)
                )
                // Yol kenar şeritleri
                drawLine(ROAD_DARK.copy(alpha = 0.80f),
                    Offset(0f, roadTop),          Offset(sw, roadTop),          strokeWidth = 2.5f)
                drawLine(ROAD_DARK.copy(alpha = 0.80f),
                    Offset(0f, roadTop + roadH),  Offset(sw, roadTop + roadH),  strokeWidth = 2.5f)
                // Yol merkez kesik çizgisi
                drawLine(
                    Color.White.copy(alpha = 0.50f),
                    Offset(0f, ty), Offset(sw, ty),
                    strokeWidth = 2f,
                    pathEffect  = PathEffect.dashPathEffect(floatArrayOf(20f, 14f))
                )
            }

            // ── Süs: taşlar ve çalılar ────────────────────────────────────────
            val decRng = java.util.Random(55L)
            repeat(30) {
                val rx = decRng.nextFloat() * sw
                val ry = horizon + decRng.nextFloat() * (sh - horizon)
                // Yol bantlarına denk geliyorsa çizme
                val onRoad = trackYs.any { ty ->
                    ry in (ty - R4_CAR_H / 2f - 12f)..(ty + R4_CAR_H / 2f + 12f)
                }
                if (!onRoad) {
                    when (decRng.nextInt(3)) {
                        0 -> { // Taş
                            val rr = 6f + decRng.nextFloat() * 12f
                            drawCircle(Color(0xFF9E9070).copy(alpha = 0.80f), rr,     Offset(rx, ry))
                            drawCircle(Color(0xFFBBAA88).copy(alpha = 0.50f), rr * 0.55f,
                                Offset(rx - rr * 0.2f, ry - rr * 0.2f))
                        }
                        1 -> { // Çalı
                            val br = 8f + decRng.nextFloat() * 10f
                            drawCircle(Color(0xFF7A6025).copy(alpha = 0.75f), br,      Offset(rx, ry))
                            drawCircle(Color(0xFF9A7830).copy(alpha = 0.60f), br * 0.7f,
                                Offset(rx - br * 0.25f, ry - br * 0.3f))
                        }
                        else -> { // Kaya kümesi
                            drawCircle(Color(0xFF8B7355).copy(alpha = 0.70f), 9f,  Offset(rx, ry))
                            drawCircle(Color(0xFF8B7355).copy(alpha = 0.55f), 6f,  Offset(rx + 10f, ry + 4f))
                        }
                    }
                }
            }

            // ── Hedef çizgisi (solda) ─────────────────────────────────────────
            val targetX = sw * 0.22f
            drawLine(
                Color(0xFFFFFFFF).copy(alpha = 0.30f),
                Offset(targetX, 0f), Offset(targetX, sh),
                strokeWidth = 2.5f,
                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(14f, 9f))
            )

            // ── Arabalar ──────────────────────────────────────────────────────
            for (car in state.cars) {
                val ty = trackYs[car.track]
                r4DrawCar(this, car, ty, tm, density)
            }

            // ── Fail kırmızı flaşı ────────────────────────────────────────────
            if (state.phase == Ritmik4Phase.FAIL_ANIM) {
                val flashAlpha = (state.failAnim / 2.2f).coerceIn(0f, 0.55f)
                drawRect(Color(0xFFD50000).copy(alpha = flashAlpha))
            }
        }

        // ── HUD ──────────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Geri butonu
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) { detectTapGestures { onBackPress() } },
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }

                // Sayı dizisi: 4 8 12 … 40
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    RITMIK4_SEQUENCE.forEach { num ->
                        val done    = num in state.correctHits
                        val current = num == state.currentTarget && state.phase == Ritmik4Phase.PLAYING
                        Box(
                            modifier = Modifier
                                .background(
                                    when {
                                        done    -> Color(0xFF4CAF50)
                                        current -> GOLD
                                        else    -> Color.Black.copy(alpha = 0.22f)
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
                    Text(
                        text       = "⭐ ${state.totalScore}",
                        color      = GOLD,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 20.sp
                    )
                    Text(
                        text     = "+${state.pointsPerHit} puan",
                        color    = GOLD.copy(alpha = 0.70f),
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = "Hedef: ${state.currentTarget}",
                    color      = Color(0xFF4E2800),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 16.sp
                )
                if (state.cycleCount > 0) {
                    Text(
                        text       = "Tur ${state.cycleCount + 1}",
                        color      = Color(0xFF4E2800),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text     = "Hız ${state.speed.toInt()}",
                    color    = Color(0xFF4E2800).copy(alpha = 0.60f),
                    fontSize = 11.sp
                )
            }
        }

        // ── Geri sayım ────────────────────────────────────────────────────────
        if (state.phase == Ritmik4Phase.COUNTDOWN) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val cdText = if (state.cycleCount == 0) {
                    val n = ceil(state.countdown).toInt().coerceIn(1, 3)
                    if (state.countdown > 0.5f) n.toString() else "Başla! 🚗"
                } else "Devam! 🏜️"
                Text(
                    text       = cdText,
                    fontSize   = if (state.cycleCount == 0) 96.sp else 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (state.cycleCount == 0) Color.White else GOLD,
                    modifier   = Modifier
                        .background(Color.Black.copy(0.45f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 40.dp, vertical = 20.dp)
                )
            }
        }

        // ── Fail ekranı ───────────────────────────────────────────────────────
        if (state.phase == Ritmik4Phase.FAIL_ANIM && state.failAnim > 1.5f) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.58f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🚨", fontSize = 64.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Yanlış araba! Baştan başlıyoruz",
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color(0xFFFF5252)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sıra: ${RITMIK4_SEQUENCE.joinToString(" → ")}",
                        fontSize = 13.sp,
                        color    = Color.White.copy(0.75f)
                    )
                }
            }
        }
    }
}

// ── Safari araba çizimi ───────────────────────────────────────────────────────
private fun r4DrawCar(
    scope: DrawScope,
    car: Ritmik4Car,
    centerY: Float,
    tm: TextMeasurer,
    density: Float
) {
    with(scope) {
        val left = car.x
        val W    = R4_CAR_W
        val H    = R4_CAR_H

        val alpha = when {
            car.hitWrong && !car.hitCorrect -> (car.anim / 0.6f).coerceIn(0f, 1f)
            else -> 1f
        }
        val scale = when {
            car.hitCorrect -> 1f + (car.anim / 0.7f) * 0.15f
            car.hitWrong   -> (car.anim / 0.6f).coerceIn(0.2f, 1f)
            else           -> 1f
        }.coerceIn(0.2f, 1.5f)

        val cx = left + W / 2f

        withTransform({ scale(scale, scale, Offset(cx, centerY)) }) {

            val bodyColor = CAR_BODY[car.track]
            val darkColor = CAR_DARK[car.track]

            // Oranlar
            val wheelR  = H * 0.18f
            val bodyTop = centerY - H * 0.28f   // gövde başlangıcı
            val bodyH   = H * 0.55f
            val cabTop  = centerY - H * 0.50f   // kabin başlangıcı
            val cabH    = H * 0.26f
            val cabLeft = left + W * 0.14f
            val cabW    = W * 0.72f

            // ── Tekerlek gölgeleri
            val fwX = left + W * 0.20f
            val rwX = left + W * 0.78f
            val wyY = bodyTop + bodyH - wheelR * 0.35f
            drawCircle(Color.Black.copy(alpha = 0.22f * alpha), wheelR + 5f, Offset(fwX, wyY + 3f))
            drawCircle(Color.Black.copy(alpha = 0.22f * alpha), wheelR + 5f, Offset(rwX, wyY + 3f))

            // ── Tekerlekler (lastik)
            drawCircle(Color(0xFF1C1C1C).copy(alpha = alpha), wheelR, Offset(fwX, wyY))
            drawCircle(Color(0xFF1C1C1C).copy(alpha = alpha), wheelR, Offset(rwX, wyY))
            // Jant
            drawCircle(Color(0xFFD4AF37).copy(alpha = alpha), wheelR * 0.55f, Offset(fwX, wyY))
            drawCircle(Color(0xFFD4AF37).copy(alpha = alpha), wheelR * 0.55f, Offset(rwX, wyY))
            // Göbek
            drawCircle(Color(0xFF888888).copy(alpha = alpha), wheelR * 0.22f, Offset(fwX, wyY))
            drawCircle(Color(0xFF888888).copy(alpha = alpha), wheelR * 0.22f, Offset(rwX, wyY))

            // ── Gövde gölgesi
            drawRoundRect(
                Color.Black.copy(alpha = 0.20f * alpha),
                topLeft      = Offset(left + 4f, bodyTop + 4f),
                size         = Size(W, bodyH),
                cornerRadius = CornerRadius(10f)
            )

            // ── Ana gövde
            drawRoundRect(
                darkColor.copy(alpha = alpha),
                topLeft      = Offset(left, bodyTop),
                size         = Size(W, bodyH),
                cornerRadius = CornerRadius(10f)
            )
            drawRoundRect(
                bodyColor.copy(alpha = alpha),
                topLeft      = Offset(left + 2f, bodyTop + 2f),
                size         = Size(W - 4f, bodyH * 0.65f),
                cornerRadius = CornerRadius(8f)
            )

            // ── Kabin
            drawRoundRect(
                darkColor.copy(alpha = alpha),
                topLeft      = Offset(cabLeft - 2f, cabTop - 2f),
                size         = Size(cabW + 4f, cabH + 4f),
                cornerRadius = CornerRadius(14f)
            )
            drawRoundRect(
                bodyColor.copy(alpha = alpha),
                topLeft      = Offset(cabLeft, cabTop),
                size         = Size(cabW, cabH),
                cornerRadius = CornerRadius(12f)
            )

            // ── Ön cam (araba sola gittiği için sol = ön)
            drawRoundRect(
                Color(0xFF87CEEB).copy(alpha = 0.82f * alpha),
                topLeft      = Offset(cabLeft + cabW * 0.04f, cabTop + cabH * 0.12f),
                size         = Size(cabW * 0.38f, cabH * 0.78f),
                cornerRadius = CornerRadius(7f)
            )
            // ── Arka cam
            drawRoundRect(
                Color(0xFF87CEEB).copy(alpha = 0.65f * alpha),
                topLeft      = Offset(cabLeft + cabW * 0.55f, cabTop + cabH * 0.14f),
                size         = Size(cabW * 0.37f, cabH * 0.72f),
                cornerRadius = CornerRadius(6f)
            )

            // ── Tavan rafı
            drawLine(
                darkColor.copy(alpha = 0.9f * alpha),
                Offset(cabLeft + cabW * 0.08f, cabTop + 4f),
                Offset(cabLeft + cabW * 0.92f, cabTop + 4f),
                strokeWidth = 4f
            )
            drawLine(
                darkColor.copy(alpha = 0.55f * alpha),
                Offset(cabLeft + cabW * 0.15f, cabTop + 1.5f),
                Offset(cabLeft + cabW * 0.85f, cabTop + 1.5f),
                strokeWidth = 2f
            )

            // ── Ön tampon / far
            drawLine(
                darkColor.copy(alpha = alpha),
                Offset(left + 5f, bodyTop + bodyH * 0.25f),
                Offset(left + 5f, bodyTop + bodyH * 0.75f),
                strokeWidth = 4f
            )
            drawCircle(Color(0xFFFFF176).copy(alpha = 0.80f * alpha), 4f,
                Offset(left + 6f, bodyTop + bodyH * 0.35f))

            // ── Hit overlay
            if (car.hitCorrect || car.hitWrong) {
                val hitColor = if (car.hitCorrect) Color(0xFF4CAF50) else Color(0xFFD50000)
                drawRoundRect(
                    hitColor.copy(alpha = 0.65f * alpha),
                    topLeft      = Offset(left, bodyTop),
                    size         = Size(W, bodyH),
                    cornerRadius = CornerRadius(10f)
                )
            }

            // ── Sayı (araba gövdesinin ortasında)
            val label = when {
                car.hitCorrect -> "✓"
                car.hitWrong   -> "✗"
                else           -> car.number.toString()
            }
            val fsPx = when {
                car.number >= 40 -> 30f
                car.number >= 10 -> 34f
                else             -> 38f
            }
            val style = TextStyle(
                fontSize   = (fsPx / density).sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White.copy(alpha = alpha),
                shadow     = Shadow(Color.Black.copy(0.6f), Offset(1f, 1f), 2f)
            )
            val m    = tm.measure(label, style)
            val tx   = left + W / 2f - m.size.width / 2f
            val ty   = bodyTop + bodyH / 2f - m.size.height / 2f
            drawText(m, topLeft = Offset(tx, ty))

            // ── Hedef araba: altın çerçeve
            val isTarget = car.isCorrect && !car.hitCorrect && !car.hitWrong
            if (isTarget) {
                drawRoundRect(
                    GOLD.copy(alpha = 0.92f),
                    topLeft      = Offset(left - 3f, bodyTop - 3f),
                    size         = Size(W + 6f, bodyH + 6f),
                    cornerRadius = CornerRadius(13f),
                    style        = Stroke(3.5f)
                )
                // Parlayan köşe noktaları
                val corners = listOf(
                    Offset(left - 3f, bodyTop - 3f),
                    Offset(left + W + 3f, bodyTop - 3f),
                    Offset(left - 3f, bodyTop + bodyH + 3f),
                    Offset(left + W + 3f, bodyTop + bodyH + 3f)
                )
                corners.forEach { c ->
                    drawCircle(GOLD.copy(alpha = 0.85f), 5f, c)
                }
            }
        }
    }
}
