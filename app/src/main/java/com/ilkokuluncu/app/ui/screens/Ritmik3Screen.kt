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
import androidx.compose.ui.geometry.Offset
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
import com.ilkokuluncu.app.viewmodel.Ritmik3Sound
import com.ilkokuluncu.app.viewmodel.Ritmik3ViewModel
import com.ilkokuluncu.app.ui.effects.GameBackgroundMusic
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.*

// ── Renk paleti ──────────────────────────────────────────────────────────────
private val COMET_CORE = listOf(
    Color(0xFFFF6D00),   // 0=alt: amber/turuncu
    Color(0xFF00B0FF),   // 1=orta: cyan/mavi
    Color(0xFFE040FB)    // 2=üst: mor/pembe
)
private val COMET_GLOW = listOf(
    Color(0xFFFFAB40),
    Color(0xFF80D8FF),
    Color(0xFFEA80FC)
)
private val GOLD = Color(0xFFFFD700)

// ── Track Y konumları ─────────────────────────────────────────────────────────
private fun r3TrackYs(sh: Float) = listOf(sh * 0.76f, sh * 0.50f, sh * 0.24f)

@Composable
fun Ritmik3Screen(
    state: Ritmik3State,
    viewModel: Ritmik3ViewModel,
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
                Ritmik3Sound.Tap      -> pool.play(sTap,     0.60f, 0.60f, 1, 0, 1.0f)
                Ritmik3Sound.Correct  -> pool.play(sCorrect, 0.90f, 0.90f, 1, 0, 1.1f)
                Ritmik3Sound.Wrong    -> pool.play(sWrong,   0.90f, 0.90f, 1, 0, 0.9f)
                Ritmik3Sound.CycleWin -> pool.play(sCorrect, 1.00f, 1.00f, 1, 0, 1.5f)
            }
        }
    }

    val tm = rememberTextMeasurer()
    val starsState = rememberUpdatedState(state.stars)
    val phaseState = rememberUpdatedState(state.phase)

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF08081A))) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (phaseState.value != Ritmik3Phase.PLAYING) return@detectTapGestures
                        val sh     = size.height.toFloat()
                        val trackYs = r3TrackYs(sh)
                        val stars  = starsState.value
                        for (star in stars) {
                            if (star.hitCorrect || star.hitWrong) continue
                            val ty   = trackYs[star.track]
                            val dist = sqrt(
                                (offset.x - star.x).pow(2) + (offset.y - ty).pow(2)
                            )
                            if (dist <= R3_STAR_R * 1.6f) {
                                viewModel.onStarTapped(star.id)
                                break
                            }
                        }
                    }
                }
        ) {
            val sw = size.width
            val sh = size.height

            if (state.screenW != sw) viewModel.setScreenWidth(sw)

            val trackYs = r3TrackYs(sh)

            // ── Uzay arka planı ──────────────────────────────────────────────
            drawRect(Brush.verticalGradient(listOf(
                Color(0xFF08081A), Color(0xFF0D0D2B), Color(0xFF08081A)
            )))

            // Nebula lekeler
            drawCircle(Color(0xFF1A0033).copy(alpha = 0.35f), sw * 0.35f, Offset(sw * 0.25f, sh * 0.3f))
            drawCircle(Color(0xFF001A33).copy(alpha = 0.28f), sw * 0.30f, Offset(sw * 0.75f, sh * 0.65f))

            // Yıldızlar
            val rng = java.util.Random(77L)
            repeat(100) {
                val sx  = rng.nextFloat() * sw
                val sy  = rng.nextFloat() * sh
                val sr  = rng.nextFloat() * 2.5f + 0.4f
                val sa  = 0.35f + rng.nextFloat() * 0.65f
                val col = when (rng.nextInt(4)) {
                    0    -> Color(0xFFADD8E6)   // açık mavi
                    1    -> Color(0xFFFFFFAA)   // sarımsı
                    2    -> Color(0xFFFFCCFF)   // pembe
                    else -> Color.White
                }
                drawCircle(col.copy(alpha = sa), sr, Offset(sx, sy))
            }

            // ── Track glow çizgileri ─────────────────────────────────────────
            trackYs.forEachIndexed { i, ty ->
                drawLine(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            COMET_GLOW[i].copy(alpha = 0.18f),
                            COMET_GLOW[i].copy(alpha = 0.22f),
                            COMET_GLOW[i].copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    Offset(0f, ty), Offset(sw, ty),
                    strokeWidth = 2.5f
                )
            }

            // Hedef çizgisi
            val targetX = sw * 0.22f
            drawLine(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.White.copy(0.20f), Color.Transparent)
                ),
                Offset(targetX, 0f), Offset(targetX, sh),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 9f))
            )

            // ── Kuyruklu yıldızlar ───────────────────────────────────────────
            for (star in state.stars) {
                val ty = trackYs[star.track]
                r3DrawComet(this, star, ty, tm, density)
            }

            // Fail kırmızı flaş
            if (state.phase == Ritmik3Phase.FAIL_ANIM) {
                val a = (state.failAnim / 2.2f).coerceIn(0f, 0.5f)
                drawRect(Color(0xFFD50000).copy(alpha = a))
            }
        }

        // ── HUD ─────────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Geri
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xC8D32F2F), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) { detectTapGestures { onBackPress() } },
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }

                // İlerleme: 3 6 9 … 30
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    RITMIK3_SEQUENCE.forEach { num ->
                        val done    = num in state.correctHits
                        val current = num == state.currentTarget && state.phase == Ritmik3Phase.PLAYING
                        Box(
                            modifier = Modifier
                                .background(
                                    when {
                                        done    -> Color(0xFF4CAF50)
                                        current -> GOLD
                                        else    -> Color.White.copy(0.12f)
                                    },
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp),
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
                        color = GOLD.copy(alpha = 0.65f), fontSize = 11.sp)
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hedef: ${state.currentTarget}", color = GOLD,
                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                if (state.cycleCount > 0) {
                    Text("Tur ${state.cycleCount + 1}", color = Color(0xFF80DEEA),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("Hız ${state.speed.toInt()}", color = Color.White.copy(0.4f), fontSize = 11.sp)
            }
        }

        // ── Geri sayım ──────────────────────────────────────────────────────
        if (state.phase == Ritmik3Phase.COUNTDOWN) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val cdText = if (state.cycleCount == 0) {
                    val n = ceil(state.countdown).toInt().coerceIn(1, 3)
                    if (state.countdown > 0.5f) n.toString() else "Başla!"
                } else "Devam! 🚀"
                Text(
                    text       = cdText,
                    fontSize   = if (state.cycleCount == 0) 96.sp else 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (state.cycleCount == 0) Color.White else GOLD,
                    modifier   = Modifier
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 40.dp, vertical = 20.dp)
                )
            }
        }

        // ── Fail ekranı ─────────────────────────────────────────────────────
        if (state.phase == Ritmik3Phase.FAIL_ANIM && state.failAnim > 1.5f) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.60f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💥", fontSize = 64.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Tuzağa düştün! Baştan başlıyoruz",
                        fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF5252))
                    Spacer(Modifier.height(8.dp))
                    Text("Sıra: ${RITMIK3_SEQUENCE.joinToString(" → ")}",
                        fontSize = 13.sp, color = Color.White.copy(0.75f))
                }
            }
        }
    }
}

// ── Kuyruklu yıldız çizimi ───────────────────────────────────────────────────
private fun r3DrawComet(
    scope: DrawScope,
    star: Ritmik3Star,
    centerY: Float,
    tm: TextMeasurer,
    density: Float
) {
    with(scope) {
        val cx = star.x
        val cy = centerY

        val scale = when {
            star.hitCorrect -> 1f + (star.anim / 0.7f) * 0.35f
            star.hitWrong   -> (star.anim / 0.6f).coerceIn(0.15f, 1f)
            else            -> 1f
        }
        val alpha = when {
            star.hitWrong && !star.hitCorrect -> (star.anim / 0.6f).coerceIn(0f, 1f)
            else -> 1f
        }

        val core = COMET_CORE[star.track]
        val glow = COMET_GLOW[star.track]

        withTransform({ scale(scale, scale, Offset(cx, cy)) }) {

            // ── Kuyruk (sağa uzanır — yıldız sola gittiği için iz sağda kalır)
            val steps = 24
            for (i in 1..steps) {
                val t  = i.toFloat() / steps
                val tx = cx + t * R3_TRAIL_LEN
                // manuel üstel azalma: t=0 → 1.0, t=1 → ~0
                val ta  = (1f - t) * (1f - t) * alpha
                val tr  = (R3_STAR_R * (1f - t * 0.88f)).coerceAtLeast(2f)
                drawCircle(glow.copy(alpha = ta * 0.40f), tr * 1.4f, Offset(tx, cy))
                drawCircle(core.copy(alpha = ta * 0.80f), tr,         Offset(tx, cy))
            }

            // ── Dış parıltı (katmanlı solid renkler)
            drawCircle(glow.copy(alpha = 0.12f * alpha), R3_STAR_R * 2.8f, Offset(cx, cy))
            drawCircle(glow.copy(alpha = 0.20f * alpha), R3_STAR_R * 1.9f, Offset(cx, cy))

            // ── Yıldız gövdesi
            val bodyColor = when {
                star.hitCorrect -> Color(0xFF4CAF50)
                star.hitWrong   -> Color(0xFFD50000)
                else            -> core
            }
            // İç parlak alan (vurgu noktası)
            drawCircle(Color.White.copy(alpha = 0.55f * alpha), R3_STAR_R * 0.45f,
                Offset(cx - R3_STAR_R * 0.22f, cy - R3_STAR_R * 0.22f))
            // Ana gövde
            drawCircle(bodyColor.copy(alpha = alpha), R3_STAR_R, Offset(cx, cy))
            // Üst katman parlaklık
            drawCircle(glow.copy(alpha = 0.35f * alpha), R3_STAR_R * 0.70f,
                Offset(cx - R3_STAR_R * 0.15f, cy - R3_STAR_R * 0.15f))

            // ── Hedef yıldız: altın halka + 6 kıvılcım
            val isTarget = star.isCorrect && !star.hitCorrect && !star.hitWrong
            if (isTarget) {
                drawCircle(GOLD.copy(alpha = 0.85f), R3_STAR_R + 5f, Offset(cx, cy),
                    style = Stroke(3.5f))
                // 6 kıvılcım noktası
                val sparks = 6
                val sparkR = R3_STAR_R + 18f
                for (i in 0 until sparks) {
                    val angle = (i.toFloat() / sparks) * (2f * PI.toFloat())
                    val sx = cx + cos(angle.toDouble()).toFloat() * sparkR
                    val sy = cy + sin(angle.toDouble()).toFloat() * sparkR
                    drawCircle(GOLD.copy(alpha = 0.85f), 5.5f, Offset(sx, sy))
                }
            }

            // ── Sayı / ikon
            val emoji = when {
                star.hitCorrect -> "✓"
                star.hitWrong   -> "✗"
                else            -> star.number.toString()
            }
            val fsPx = when {
                star.number >= 20 -> 41f
                star.number >= 10 -> 49f
                else              -> 56f
            }
            val style = TextStyle(
                fontSize   = (fsPx / density).sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White.copy(alpha = alpha)
            )
            val m = tm.measure(emoji, style)
            drawText(m, topLeft = Offset(cx - m.size.width / 2f, cy - m.size.height / 2f))
        }
    }
}
