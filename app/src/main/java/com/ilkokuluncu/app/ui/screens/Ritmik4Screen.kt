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
import androidx.activity.compose.BackHandler
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
    BackHandler { onBackPress() }

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

            // ── Uzak dağ / kum tepeleri silüeti (çok katmanlı) ──────────────────
            val duneRng = java.util.Random(99L)

            // İlk katman (en uzak, hafif)
            var dx = 0f
            while (dx < sw) {
                val dw = 100f + duneRng.nextFloat() * 180f
                val dh = 12f + duneRng.nextFloat() * 28f
                val path = Path().apply {
                    moveTo(dx, horizon)
                    quadraticBezierTo(dx + dw / 2f, horizon - dh, dx + dw, horizon)
                    close()
                }
                drawPath(path, Color(0xFFD4A856).copy(alpha = 0.35f))
                dx += dw * 0.65f
            }

            // İkinci katman (ortada)
            dx = -40f
            while (dx < sw) {
                val dw = 85f + duneRng.nextFloat() * 130f
                val dh = 16f + duneRng.nextFloat() * 36f
                val path = Path().apply {
                    moveTo(dx, horizon)
                    quadraticBezierTo(dx + dw / 2f, horizon - dh, dx + dw, horizon)
                    close()
                }
                drawPath(path, Color(0xFFD6B956).copy(alpha = 0.50f))
                dx += dw * 0.70f
            }

            // Üçüncü katman (en yakın, koyu)
            dx = -20f
            while (dx < sw) {
                val dw = 90f + duneRng.nextFloat() * 140f
                val dh = 18f + duneRng.nextFloat() * 42f
                val path = Path().apply {
                    moveTo(dx, horizon)
                    quadraticBezierTo(dx + dw / 2f, horizon - dh, dx + dw, horizon)
                    close()
                }
                drawPath(path, Color(0xFFCB9D4A).copy(alpha = 0.65f))
                dx += dw * 0.72f
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

            // ── Süs: taşlar, çalılar ve kaktüsler ────────────────────────────────
            val decRng = java.util.Random(55L)
            repeat(32) {
                val rx = decRng.nextFloat() * sw
                val ry = horizon + decRng.nextFloat() * (sh - horizon)
                // Yol bantlarına denk geliyorsa çizme
                val onRoad = trackYs.any { ty ->
                    ry in (ty - R4_CAR_H / 2f - 12f)..(ty + R4_CAR_H / 2f + 12f)
                }
                if (!onRoad) {
                    when (decRng.nextInt(4)) {
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
                        2 -> { // Kaya kümesi
                            drawCircle(Color(0xFF8B7355).copy(alpha = 0.70f), 9f,  Offset(rx, ry))
                            drawCircle(Color(0xFF8B7355).copy(alpha = 0.55f), 6f,  Offset(rx + 10f, ry + 4f))
                        }
                        else -> { // Kaktüs
                            val cactusHeight = 30f + decRng.nextFloat() * 35f
                            // Ana gövde
                            drawRect(
                                Color(0xFF6B8E23).copy(alpha = 0.85f),
                                topLeft = Offset(rx - 5f, ry - cactusHeight),
                                size = Size(10f, cactusHeight)
                            )
                            // Koyu ton gölge
                            drawRect(
                                Color(0xFF4A6319).copy(alpha = 0.50f),
                                topLeft = Offset(rx - 2f, ry - cactusHeight),
                                size = Size(4f, cactusHeight)
                            )
                            // Sol kol
                            drawRect(
                                Color(0xFF6B8E23).copy(alpha = 0.80f),
                                topLeft = Offset(rx - 14f, ry - cactusHeight * 0.65f),
                                size = Size(9f, 12f)
                            )
                            // Sağ kol
                            drawRect(
                                Color(0xFF6B8E23).copy(alpha = 0.80f),
                                topLeft = Offset(rx + 5f, ry - cactusHeight * 0.50f),
                                size = Size(9f, 11f)
                            )
                            // Üst kol (mini)
                            drawRect(
                                Color(0xFF6B8E23).copy(alpha = 0.75f),
                                topLeft = Offset(rx - 8f, ry - cactusHeight * 0.25f),
                                size = Size(7f, 10f)
                            )
                            // Çiçek (kırmızı, tepede)
                            drawCircle(Color(0xFFFF6B6B).copy(alpha = 0.85f), 4.5f, Offset(rx, ry - cactusHeight - 3f))
                            // Dikenler (küçük beyaz noktalar)
                            listOf(
                                Offset(rx - 4f, ry - cactusHeight * 0.75f),
                                Offset(rx + 3f, ry - cactusHeight * 0.60f),
                                Offset(rx - 6f, ry - cactusHeight * 0.40f),
                                Offset(rx + 4f, ry - cactusHeight * 0.20f),
                                Offset(rx - 3f, ry - cactusHeight * 0.10f)
                            ).forEach { spine ->
                                drawCircle(Color(0xFFFFF9C4).copy(alpha = 0.70f), 1.5f, spine)
                            }
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
                        .size(54.dp)
                        .background(Color.Red, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) { detectTapGestures { onBackPress() } },
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }

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

// ── Kamyonet (pickup truck 🛻) çizimi ────────────────────────────────────────
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
        val cx   = left + W / 2f

        val alpha = when {
            car.hitWrong && !car.hitCorrect -> (car.anim / 0.6f).coerceIn(0f, 1f)
            else -> 1f
        }
        val scale = when {
            car.hitCorrect -> 1f + (car.anim / 0.7f) * 0.15f
            car.hitWrong   -> (car.anim / 0.6f).coerceIn(0.2f, 1f)
            else           -> 1f
        }.coerceIn(0.2f, 1.5f)

        withTransform({ scale(scale, scale, Offset(cx, centerY)) }) {

            val bodyColor = CAR_BODY[car.track]
            val darkColor = CAR_DARK[car.track]

            // ── Ölçüler ─────────────────────────────────────────
            val wheelR   = H * 0.20f
            val baseY    = centerY + H * 0.30f      // şasi altı
            val shassisH = H * 0.18f                // şasi yüksekliği
            val shassisT = baseY - shassisH

            // Kabin (sol = ön): W'nin %45'i
            val cabLeft  = left + W * 0.02f
            val cabW     = W * 0.45f
            val cabTop   = centerY - H * 0.42f
            val cabH     = shassisT - cabTop

            // Yük kasası (sağ = arka): W'nin %48'i
            val bedLeft  = cabLeft + cabW
            val bedW     = W * 0.48f
            val bedTop   = centerY - H * 0.18f      // kabin daha kısa
            val bedH     = shassisT - bedTop

            // Tekerlek X
            val fwX = cabLeft + cabW * 0.30f        // ön tekerlek (sol)
            val rwX = bedLeft + bedW * 0.70f        // arka tekerlek (sağ)

            // ── Tekerlek gölgesi
            drawCircle(Color.Black.copy(0.25f * alpha), wheelR + 4f, Offset(fwX, baseY + 2f))
            drawCircle(Color.Black.copy(0.25f * alpha), wheelR + 4f, Offset(rwX, baseY + 2f))

            // ── Şasi / gövde tabanı
            drawRoundRect(
                darkColor.copy(alpha = alpha),
                topLeft      = Offset(left + 2f, shassisT),
                size         = Size(W - 4f, shassisH),
                cornerRadius = CornerRadius(6f)
            )

            // ── Yük kasası arka duvarlar (açık üst)
            // Zemin
            drawRoundRect(
                bodyColor.copy(alpha = alpha),
                topLeft      = Offset(bedLeft, bedTop),
                size         = Size(bedW, bedH),
                cornerRadius = CornerRadius(4f)
            )
            // Sol duvar (kabin tarafı)
            drawRect(
                darkColor.copy(alpha = alpha),
                topLeft = Offset(bedLeft, bedTop),
                size    = Size(5f, bedH)
            )
            // Sağ duvar (arka)
            drawRect(
                darkColor.copy(alpha = alpha),
                topLeft = Offset(bedLeft + bedW - 5f, bedTop),
                size    = Size(5f, bedH)
            )
            // Üst kenar çizgisi
            drawLine(
                darkColor.copy(alpha = alpha),
                Offset(bedLeft, bedTop), Offset(bedLeft + bedW, bedTop),
                strokeWidth = 3.5f
            )

            // ── Kabin gövdesi
            drawRoundRect(
                darkColor.copy(alpha = alpha),
                topLeft      = Offset(cabLeft - 1f, cabTop - 1f),
                size         = Size(cabW + 2f, cabH + 2f),
                cornerRadius = CornerRadius(12f)
            )
            drawRoundRect(
                bodyColor.copy(alpha = alpha),
                topLeft      = Offset(cabLeft, cabTop),
                size         = Size(cabW, cabH),
                cornerRadius = CornerRadius(11f)
            )

            // ── Ön cam (sol = ön taraf)
            drawRoundRect(
                Color(0xFF87CEEB).copy(alpha = 0.85f * alpha),
                topLeft      = Offset(cabLeft + cabW * 0.06f, cabTop + cabH * 0.10f),
                size         = Size(cabW * 0.42f, cabH * 0.75f),
                cornerRadius = CornerRadius(7f)
            )
            drawRoundRect(
                Color.Black.copy(alpha = 0.25f * alpha),
                topLeft      = Offset(cabLeft + cabW * 0.06f, cabTop + cabH * 0.10f),
                size         = Size(cabW * 0.42f, cabH * 0.75f),
                cornerRadius = CornerRadius(7f),
                style        = Stroke(1.5f)
            )

            // ── Arka cam (sağ taraf)
            drawRoundRect(
                Color(0xFF87CEEB).copy(alpha = 0.65f * alpha),
                topLeft      = Offset(cabLeft + cabW * 0.57f, cabTop + cabH * 0.12f),
                size         = Size(cabW * 0.35f, cabH * 0.70f),
                cornerRadius = CornerRadius(6f)
            )

            // ── Ön far
            drawRoundRect(
                Color(0xFFFFF176).copy(alpha = 0.90f * alpha),
                topLeft      = Offset(cabLeft + 2f, cabTop + cabH * 0.22f),
                size         = Size(7f, cabH * 0.22f),
                cornerRadius = CornerRadius(3f)
            )
            // Far ışık huzmesi
            drawLine(
                Color(0xFFFFF9C4).copy(alpha = 0.45f * alpha),
                Offset(cabLeft + 2f, cabTop + cabH * 0.30f),
                Offset(cabLeft - 18f, cabTop + cabH * 0.30f),
                strokeWidth = 6f
            )

            // ── Ön ayna
            drawRoundRect(
                darkColor.copy(alpha = 0.80f * alpha),
                topLeft      = Offset(cabLeft - 10f, cabTop + cabH * 0.30f),
                size         = Size(10f, 6f),
                cornerRadius = CornerRadius(2f)
            )

            // ── Tekerlekler
            listOf(fwX, rwX).forEach { wx ->
                // Lastik
                drawCircle(Color(0xFF1A1A1A).copy(alpha = alpha), wheelR, Offset(wx, baseY))
                drawCircle(Color.White.copy(alpha = 0.20f * alpha), wheelR, Offset(wx, baseY),
                    style = Stroke(2f))
                // Jant
                drawCircle(Color(0xFFB0B0B0).copy(alpha = alpha), wheelR * 0.58f, Offset(wx, baseY))
                // Jant kolları (+ şeklinde)
                val jr = wheelR * 0.52f
                drawLine(Color(0xFF777777).copy(alpha = alpha),
                    Offset(wx - jr, baseY), Offset(wx + jr, baseY), strokeWidth = 2.5f)
                drawLine(Color(0xFF777777).copy(alpha = alpha),
                    Offset(wx, baseY - jr), Offset(wx, baseY + jr), strokeWidth = 2.5f)
                // Jant göbeği
                drawCircle(Color(0xFF555555).copy(alpha = alpha), wheelR * 0.20f, Offset(wx, baseY))
            }

            // ── Tampon (ön)
            drawRoundRect(
                darkColor.copy(alpha = 0.90f * alpha),
                topLeft      = Offset(cabLeft - 1f, shassisT - 4f),
                size         = Size(8f, shassisH + 4f),
                cornerRadius = CornerRadius(3f)
            )

            // ── Hit overlay
            if (car.hitCorrect || car.hitWrong) {
                val hitColor = if (car.hitCorrect) Color(0xFF4CAF50) else Color(0xFFD50000)
                drawRoundRect(
                    hitColor.copy(alpha = 0.55f * alpha),
                    topLeft      = Offset(cabLeft, cabTop),
                    size         = Size(cabW, cabH),
                    cornerRadius = CornerRadius(11f)
                )
                drawRoundRect(
                    hitColor.copy(alpha = 0.45f * alpha),
                    topLeft      = Offset(bedLeft, bedTop),
                    size         = Size(bedW, bedH),
                    cornerRadius = CornerRadius(4f)
                )
            }

            // ── Sayı (kasada göster)
            val label = when {
                car.hitCorrect -> "✓"
                car.hitWrong   -> "✗"
                else           -> car.number.toString()
            }
            val fsPx = when {
                car.number >= 40 -> 28f
                car.number >= 10 -> 32f
                else             -> 36f
            }
            val style = TextStyle(
                fontSize   = (fsPx / density).sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White.copy(alpha = alpha),
                shadow     = Shadow(Color.Black.copy(0.7f), Offset(1.5f, 1.5f), 3f)
            )
            val m  = tm.measure(label, style)
            val tx = bedLeft + bedW / 2f - m.size.width / 2f
            val ty = bedTop + bedH / 2f - m.size.height / 2f
            drawText(m, topLeft = Offset(tx, ty))

            // ── Hedef kamyonet: altın çerçeve
            val isTarget = car.isCorrect && !car.hitCorrect && !car.hitWrong
            if (isTarget) {
                drawRoundRect(
                    GOLD.copy(alpha = 0.92f),
                    topLeft      = Offset(left - 4f, cabTop - 4f),
                    size         = Size(W + 8f, baseY - cabTop + wheelR + 8f),
                    cornerRadius = CornerRadius(14f),
                    style        = Stroke(3.5f)
                )
                listOf(
                    Offset(left - 4f, cabTop - 4f),
                    Offset(left + W + 4f, cabTop - 4f),
                    Offset(left - 4f, baseY + wheelR + 4f),
                    Offset(left + W + 4f, baseY + wheelR + 4f)
                ).forEach { drawCircle(GOLD.copy(alpha = 0.85f), 5f, it) }
            }
        }
    }
}
