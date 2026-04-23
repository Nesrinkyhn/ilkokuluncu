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
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.*

// ── Dondurma renkleri (top + parlak) ──────────────────────────────────────────
private val ICE_COLORS = listOf(
    Pair(Color(0xFFFF8FAB), Color(0xFFFFCCDA)),  // 0 çilek/pembe
    Pair(Color(0xFF5CE19A), Color(0xFFAAF0CB)),  // 1 nane/yeşil
    Pair(Color(0xFF7AB3F0), Color(0xFFBDD6F8)),  // 2 yaban mersini/mavi
    Pair(Color(0xFFFFF176), Color(0xFFFFFABE)),  // 3 limon/sarı
    Pair(Color(0xFFFFAB76), Color(0xFFFFD4B3)),  // 4 şeftali/turuncu
    Pair(Color(0xFFD98EE8), Color(0xFFEFC5F5))   // 5 üzüm/mor
)
private val CONE_COLOR  = Color(0xFFD4894A)
private val CONE_LIGHT  = Color(0xFFE8A86A)
private val CONE_DARK   = Color(0xFFAA6830)
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

    // TODO: GameBackgroundMusic temporarily disabled for debugging
    // GameBackgroundMusic(volume = 0.28f)

    val pool = remember {
        SoundPool.Builder().setMaxStreams(6)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            ).build()
    }
    val sTap     = remember { try { pool.load(context, R.raw.clock_tap,     1) } catch(e: Exception) { 0 } }
    val sCorrect = remember { try { pool.load(context, R.raw.clock_correct, 1) } catch(e: Exception) { 0 } }
    val sWrong   = remember { try { pool.load(context, R.raw.clock_wrong,   1) } catch(e: Exception) { 0 } }
    DisposableEffect(pool) { onDispose { pool.release() } }

    LaunchedEffect(viewModel) {
        viewModel.sounds.collect { snd ->
            try {
                when (snd) {
                    Ritmik5Sound.Tap      -> if (sTap > 0) pool.play(sTap,     0.60f, 0.60f, 1, 0, 1.0f)
                    Ritmik5Sound.Correct  -> if (sCorrect > 0) pool.play(sCorrect, 0.90f, 0.90f, 1, 0, 1.1f)
                    Ritmik5Sound.Wrong    -> if (sWrong > 0) pool.play(sWrong,   0.90f, 0.90f, 1, 0, 0.9f)
                    Ritmik5Sound.CycleWin -> if (sCorrect > 0) pool.play(sCorrect, 1.00f, 1.00f, 1, 0, 1.5f)
                }
            } catch (e: Exception) { /* Ses çalma hatası ignore */ }
        }
    }

    val tm         = rememberTextMeasurer()
    val conesState = rememberUpdatedState(state.cones)
    val phaseState = rememberUpdatedState(state.phase)

    Box(modifier = Modifier.fillMaxSize()) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (phaseState.value != Ritmik5Phase.PLAYING) return@detectTapGestures
                        val sh      = size.height.toFloat()
                        val trackYs = r5TrackYs(sh)
                        val cones   = conesState.value
                        for (cone in cones) {
                            if (cone.hitCorrect || cone.hitWrong) continue
                            val ty   = trackYs[cone.track]
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

            val trackYs = r5TrackYs(sh)
            val seaTop  = sh * 0.30f
            val sandTop = sh * 0.65f

            // ── Gökyüzü ─────────────────────────────────────────────────────
            drawRect(
                Brush.verticalGradient(listOf(SKY_TOP, SKY_MID, Color(0xFFB2EBF2)),
                    startY = 0f, endY = seaTop),
                size = Size(sw, seaTop)
            )

            // ── Güneş ────────────────────────────────────────────────────────
            val sunX = sw * 0.86f
            val sunY = sh * 0.11f
            drawCircle(Color(0xFFFFF9C4).copy(alpha = 0.35f), 72f, Offset(sunX, sunY))
            drawCircle(Color(0xFFFFF176).copy(alpha = 0.75f), 48f, Offset(sunX, sunY))
            drawCircle(Color(0xFFFFEE58), 30f, Offset(sunX, sunY))

            // Güneş ışınları
            repeat(8) { i ->
                val angle = i * (PI / 4f).toFloat()
                val r1 = 38f; val r2 = 62f
                drawLine(Color(0xFFFFF176).copy(alpha = 0.50f),
                    Offset(sunX + cos(angle) * r1, sunY + sin(angle) * r1),
                    Offset(sunX + cos(angle) * r2, sunY + sin(angle) * r2),
                    strokeWidth = 3.5f)
            }

            // ── Martılar ─────────────────────────────────────────────────────
            val gullRng = java.util.Random(17L)
            repeat(5) {
                val gx = gullRng.nextFloat() * sw * 0.75f + sw * 0.05f
                val gy = sh * 0.05f + gullRng.nextFloat() * sh * 0.18f
                val gs = 6f + gullRng.nextFloat() * 8f
                // Basit V şekli (martı silueti)
                val path = Path().apply {
                    moveTo(gx - gs, gy)
                    quadraticBezierTo(gx - gs * 0.4f, gy - gs * 0.6f, gx, gy + gs * 0.1f)
                    quadraticBezierTo(gx + gs * 0.4f, gy - gs * 0.6f, gx + gs, gy)
                }
                drawPath(path, Color.White.copy(alpha = 0.75f), style = Stroke(2.5f))
            }

            // ── Deniz ────────────────────────────────────────────────────────
            drawRect(
                Brush.verticalGradient(listOf(SEA_LIGHT, SEA_DARK),
                    startY = seaTop, endY = sandTop),
                topLeft = Offset(0f, seaTop),
                size    = Size(sw, sandTop - seaTop)
            )

            // Deniz balıkları / dalgalar (yatay çizgiler)
            val waveRng = java.util.Random(33L)
            repeat(6) {
                val wy2 = seaTop + waveRng.nextFloat() * (sandTop - seaTop - 10f)
                val wx1 = waveRng.nextFloat() * sw * 0.5f
                val ww  = 60f + waveRng.nextFloat() * 120f
                drawLine(WAVE_WHITE.copy(alpha = 0.30f),
                    Offset(wx1, wy2), Offset(wx1 + ww, wy2), strokeWidth = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }

            // ── Ufuk çizgisi (deniz + gökyüzü sınırı) ────────────────────────
            drawLine(WAVE_WHITE.copy(alpha = 0.55f),
                Offset(0f, seaTop), Offset(sw, seaTop), strokeWidth = 2f)

            // ── Yelkenli kayıklar ─────────────────────────────────────────────
            r5DrawSailboat(this, sw * 0.18f, sh * 0.39f, sh * 0.095f)
            r5DrawSailboat(this, sw * 0.62f, sh * 0.44f, sh * 0.075f)

            // ── Kum ──────────────────────────────────────────────────────────
            // Hafif dalga efekti — kum/deniz sınırı
            val wavePath = Path().apply {
                moveTo(0f, sandTop)
                var wsx = 0f
                while (wsx <= sw) {
                    val wEnd = (wsx + 55f).coerceAtMost(sw)
                    val midX = wsx + (wEnd - wsx) / 2f
                    val midY = sandTop + if ((wsx / 55).toInt() % 2 == 0) -6f else 6f
                    quadraticBezierTo(midX, midY, wEnd, sandTop)
                    wsx = wEnd
                }
                lineTo(sw, sh); lineTo(0f, sh); close()
            }
            drawPath(wavePath, Brush.verticalGradient(listOf(SAND_LIGHT, SAND_DARK),
                startY = sandTop, endY = sh))

            // Kum üst köpüğü (beyaz dalga)
            val foamPath = Path().apply {
                moveTo(0f, sandTop)
                var wsx = 0f
                while (wsx <= sw) {
                    val wEnd = (wsx + 55f).coerceAtMost(sw)
                    val midX = wsx + (wEnd - wsx) / 2f
                    val midY = sandTop + if ((wsx / 55).toInt() % 2 == 0) -6f else 6f
                    quadraticBezierTo(midX, midY, wEnd, sandTop)
                    wsx = wEnd
                }
            }
            drawPath(foamPath, WAVE_WHITE.copy(alpha = 0.70f), style = Stroke(4f))

            // Kum üzerinde küçük taş & deniz kabuğu izleri
            val sandRng = java.util.Random(77L)
            repeat(20) {
                val sx2 = sandRng.nextFloat() * sw
                val sy2 = sandTop + 10f + sandRng.nextFloat() * (sh - sandTop - 10f)
                val sr2 = 2f + sandRng.nextFloat() * 5f
                drawCircle(SAND_DARK.copy(alpha = 0.50f), sr2, Offset(sx2, sy2))
            }

            // ── Track işaret çizgileri ────────────────────────────────────────
            trackYs.forEachIndexed { _, ty ->
                drawLine(Color.White.copy(alpha = 0.18f),
                    Offset(0f, ty + R5_ICE_R + R5_CONE_H * 0.6f),
                    Offset(sw, ty + R5_ICE_R + R5_CONE_H * 0.6f),
                    strokeWidth = 1.5f,
                    pathEffect  = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)))
            }

            // ── Hedef çizgisi (solda) ─────────────────────────────────────────
            val targetX = sw * 0.22f
            drawLine(Color.White.copy(alpha = 0.28f),
                Offset(targetX, 0f), Offset(targetX, sh),
                strokeWidth = 2f,
                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(14f, 9f)))

            // ── Dondurma külahları ────────────────────────────────────────────
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

        // ── HUD ──────────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color.Black.copy(0.18f), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) { detectTapGestures { onBackPress() } },
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }

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
                                        done    -> Color(0xFF4CAF50)
                                        current -> GOLD
                                        else    -> Color.Black.copy(0.22f)
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
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
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
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.55f)),
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

// ── Yelkenli kayık ────────────────────────────────────────────────────────────
private fun r5DrawSailboat(scope: DrawScope, cx: Float, baseY: Float, size: Float) {
    with(scope) {
        val hw = size * 1.4f   // tekne yarı genişliği
        val hh = size * 0.40f  // tekne yüksekliği

        // Gövde (yay şekli)
        val hullPath = Path().apply {
            moveTo(cx - hw, baseY)
            quadraticBezierTo(cx, baseY + hh, cx + hw, baseY)
            close()
        }
        drawPath(hullPath, Color(0xFF8D6E48))
        drawPath(hullPath, Color(0xFFA1887F).copy(alpha = 0.60f), style = Stroke(2f))

        // Direk
        drawLine(Color(0xFF5D4037),
            Offset(cx, baseY), Offset(cx, baseY - size * 2.2f),
            strokeWidth = size * 0.08f)

        // Yelken (üçgen)
        val sailPath = Path().apply {
            moveTo(cx, baseY - size * 2.1f)
            lineTo(cx + hw * 1.1f, baseY - size * 0.5f)
            lineTo(cx, baseY - size * 0.1f)
            close()
        }
        drawPath(sailPath, Color.White.copy(alpha = 0.90f))
        drawPath(sailPath, Color(0xFFBBDEFB).copy(alpha = 0.45f), style = Stroke(1.5f))

        // İkinci küçük yelken
        val sail2Path = Path().apply {
            moveTo(cx, baseY - size * 2.0f)
            lineTo(cx - hw * 0.7f, baseY - size * 0.7f)
            lineTo(cx, baseY - size * 0.15f)
            close()
        }
        drawPath(sail2Path, Color(0xFFFFCDD2).copy(alpha = 0.80f))

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
        // Top merkezi trackY'nin biraz üzerinde, külah aşağıda
        val scoopCY = centerY - R5_CONE_H * 0.25f
        val coneTopY = scoopCY + R5_ICE_R * 0.75f
        val coneTipY = coneTopY + R5_CONE_H
        val coneHW   = R5_ICE_R * 1.10f  // külah ağzı yarı genişliği

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

            val (scoopColor, lightColor) = ICE_COLORS[cone.colorIdx % ICE_COLORS.size]

            val bodyColor = when {
                cone.hitCorrect -> Color(0xFF4CAF50)
                cone.hitWrong   -> Color(0xFFD50000)
                else            -> scoopColor
            }

            // ── Külah gölgesi
            val shadowPath = Path().apply {
                moveTo(cx - coneHW + 3f, coneTopY + 3f)
                lineTo(cx + 3f, coneTipY + 3f)
                lineTo(cx + coneHW + 3f, coneTopY + 3f)
                close()
            }
            drawPath(shadowPath, Color.Black.copy(alpha = 0.18f * alpha))

            // ── Külah (waffle üçgen)
            val conePath = Path().apply {
                moveTo(cx - coneHW, coneTopY)
                lineTo(cx, coneTipY)
                lineTo(cx + coneHW, coneTopY)
                close()
            }
            drawPath(conePath, CONE_COLOR.copy(alpha = alpha))

            // Waffle çizgileri (yatay)
            val waffleCount = 4
            for (i in 1 until waffleCount) {
                val t  = i.toFloat() / waffleCount
                val wy = coneTopY + (coneTipY - coneTopY) * t
                val wx = coneHW * (1f - t)
                drawLine(CONE_DARK.copy(alpha = 0.50f * alpha),
                    Offset(cx - wx, wy), Offset(cx + wx, wy), strokeWidth = 1.5f)
            }
            // Waffle çizgileri (çapraz sol)
            for (i in 0..3) {
                val t  = i.toFloat() / 4f
                val y1 = coneTopY + (coneTipY - coneTopY) * t
                val x1 = cx - coneHW * (1f - t)
                val t2 = (i + 1).toFloat() / 4f
                val y2 = coneTopY + (coneTipY - coneTopY) * t2
                val x2 = cx + coneHW * (1f - t2)
                drawLine(CONE_DARK.copy(alpha = 0.35f * alpha), Offset(x1, y1), Offset(x2, y2), strokeWidth = 1f)
            }
            // Waffle çizgileri (çapraz sağ)
            for (i in 0..3) {
                val t  = i.toFloat() / 4f
                val y1 = coneTopY + (coneTipY - coneTopY) * t
                val x1 = cx + coneHW * (1f - t)
                val t2 = (i + 1).toFloat() / 4f
                val y2 = coneTopY + (coneTipY - coneTopY) * t2
                val x2 = cx - coneHW * (1f - t2)
                drawLine(CONE_DARK.copy(alpha = 0.35f * alpha), Offset(x1, y1), Offset(x2, y2), strokeWidth = 1f)
            }
            // Külah parlak kenar
            drawLine(CONE_LIGHT.copy(alpha = 0.60f * alpha),
                Offset(cx - coneHW, coneTopY), Offset(cx, coneTipY), strokeWidth = 2f)

            // ── Dondurma topu — dış parlaklık
            drawCircle(bodyColor.copy(alpha = 0.30f * alpha), R5_ICE_R * 1.55f, Offset(cx, scoopCY))
            drawCircle(bodyColor.copy(alpha = 0.55f * alpha), R5_ICE_R * 1.20f, Offset(cx, scoopCY))

            // ── Ana top
            drawCircle(bodyColor.copy(alpha = alpha), R5_ICE_R, Offset(cx, scoopCY))

            // Parlak nokta (sol-üst)
            drawCircle(lightColor.copy(alpha = 0.80f * alpha), R5_ICE_R * 0.38f,
                Offset(cx - R5_ICE_R * 0.28f, scoopCY - R5_ICE_R * 0.28f))

            // ── Krema kıvrımı (üstte)
            val creamPath = Path().apply {
                moveTo(cx - R5_ICE_R * 0.75f, scoopCY - R5_ICE_R * 0.55f)
                quadraticBezierTo(cx - R5_ICE_R * 0.25f, scoopCY - R5_ICE_R * 1.10f,
                    cx, scoopCY - R5_ICE_R * 0.65f)
                quadraticBezierTo(cx + R5_ICE_R * 0.25f, scoopCY - R5_ICE_R * 1.10f,
                    cx + R5_ICE_R * 0.75f, scoopCY - R5_ICE_R * 0.55f)
            }
            drawPath(creamPath, lightColor.copy(alpha = 0.55f * alpha), style = Stroke(3.5f, cap = StrokeCap.Round))

            // ── Kiraz (en üstte)
            if (!cone.hitCorrect && !cone.hitWrong) {
                val cherryY = scoopCY - R5_ICE_R * 1.10f
                drawLine(Color(0xFF795548).copy(alpha = alpha),
                    Offset(cx, cherryY), Offset(cx + R5_ICE_R * 0.3f, cherryY - R5_ICE_R * 0.4f),
                    strokeWidth = 2.5f)
                drawCircle(Color(0xFFE53935).copy(alpha = alpha), R5_ICE_R * 0.18f,
                    Offset(cx + R5_ICE_R * 0.3f, cherryY - R5_ICE_R * 0.4f))
                drawCircle(Color(0xFFFF8A80).copy(alpha = 0.70f * alpha), R5_ICE_R * 0.08f,
                    Offset(cx + R5_ICE_R * 0.25f, cherryY - R5_ICE_R * 0.48f))
            }

            // ── Hedef külah: altın çerçeve
            val isTarget = cone.isCorrect && !cone.hitCorrect && !cone.hitWrong
            if (isTarget) {
                drawCircle(GOLD.copy(alpha = 0.90f), R5_ICE_R + 5f, Offset(cx, scoopCY),
                    style = Stroke(3.5f))
                // Kıvılcım noktaları
                repeat(6) { i ->
                    val angle = (i.toFloat() / 6f) * 2f * PI.toFloat()
                    val sr = R5_ICE_R + 16f
                    drawCircle(GOLD.copy(alpha = 0.88f), 5f,
                        Offset(cx + cos(angle) * sr, scoopCY + sin(angle) * sr))
                }
            }

            // ── Sayı
            val label = when {
                cone.hitCorrect -> "✓"
                cone.hitWrong   -> "✗"
                else            -> cone.number.toString()
            }
            val fsPx = when {
                cone.number >= 50 -> 30f
                cone.number >= 10 -> 34f
                else              -> 38f
            }
            val style = TextStyle(
                fontSize   = (fsPx / density).sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White.copy(alpha = alpha),
                shadow     = Shadow(Color.Black.copy(0.55f), Offset(1f, 1f), 2f)
            )
            val m = tm.measure(label, style)
            drawText(m, topLeft = Offset(cx - m.size.width / 2f, scoopCY - m.size.height / 2f))
        }
    }
}
