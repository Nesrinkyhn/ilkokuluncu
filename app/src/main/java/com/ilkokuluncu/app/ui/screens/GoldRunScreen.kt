package com.ilkokuluncu.app.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.*
import androidx.compose.ui.draw.clip
import com.ilkokuluncu.app.data.*
import com.ilkokuluncu.app.ui.effects.GoldRunSoundPlayer
import com.ilkokuluncu.app.viewmodel.GoldRunViewModel
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.*

@Composable
fun GoldRunScreen(
    state: GoldRunState,
    onEvent: (GoldRunEvent) -> Unit,
    onBackPress: () -> Unit,
    viewModel: GoldRunViewModel
) {
    val activity = LocalContext.current as? Activity
    val context  = LocalContext.current

    // Lock landscape
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Sound player with lifecycle
    val soundPlayer = remember { GoldRunSoundPlayer(context) }
    DisposableEffect(soundPlayer) {
        soundPlayer.startMusic()
        onDispose { soundPlayer.release() }
    }

    // Consume sound events from VM
    LaunchedEffect(viewModel) {
        viewModel.sounds.collect { sound ->
            soundPlayer.play(sound)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1565C0))
    ) {
        // ── Game Canvas ───────────────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw  = size.width
            val sh  = size.height
            // Side-scroller: yüksekliği ekrana sığdır, yatay kamera kaydırır
            val scale = sh / GOLD_WORLD_H
            val cam   = state.cameraX

            fun wx(worldX: Float) = (worldX - cam) * scale
            fun wy(worldY: Float) = worldY * scale
            fun ws(s2: Float)     = s2 * scale

            // ── Gökyüzü degradesi (tam ekran) ────────────────────────────────
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF1565C0), Color(0xFF42A5F5), Color(0xFF90CAF9)),
                    startY = 0f, endY = sh
                )
            )

            // ── Güneş ────────────────────────────────────────────────────────
            val sunCX = sw * 0.88f
            val sunCY = sh * 0.14f
            val sunR  = ws(28f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF59D), Color(0xFFFFD54F), Color(0x00FFD54F)),
                    center = Offset(sunCX, sunCY), radius = sunR * 2.2f
                ),
                radius = sunR * 2.2f, center = Offset(sunCX, sunCY)
            )
            drawCircle(Color(0xFFFFF176), sunR, Offset(sunCX, sunCY))

            // ── Deniz (ufuk hattının altı) ────────────────────────────────────
            val horizonY = sh * 0.58f
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF0288D1), Color(0xFF006064)),
                    startY = horizonY, endY = sh
                ),
                topLeft = Offset(0f, horizonY),
                size    = Size(sw, sh - horizonY)
            )
            // Ufuk çizgisi
            drawLine(Color(0xFF80DEEA).copy(alpha = 0.6f),
                Offset(0f, horizonY), Offset(sw, horizonY), strokeWidth = ws(2f))
            // Dalga çizgileri
            val waveY1 = horizonY + (sh - horizonY) * 0.25f
            val waveY2 = horizonY + (sh - horizonY) * 0.50f
            val waveY3 = horizonY + (sh - horizonY) * 0.72f
            val waveOffset = (cam * 0.05f) % (sw * 0.5f)
            for (wy2 in listOf(waveY1, waveY2, waveY3)) {
                val wavePath = Path()
                var wx2 = -waveOffset
                wavePath.moveTo(wx2, wy2)
                while (wx2 < sw + 80f) {
                    wavePath.cubicTo(
                        wx2 + 20f, wy2 - ws(4f),
                        wx2 + 40f, wy2 + ws(4f),
                        wx2 + 60f, wy2
                    )
                    wx2 += 60f
                }
                drawPath(wavePath, Color(0xFF80DEEA).copy(alpha = 0.35f),
                    style = Stroke(ws(2.5f)))
            }

            // ── Deniz araçları (ufuk yakını, çok yavaş parallax) ─────────────
            val boatScroll = cam * 0.07f
            val boatBaseY  = horizonY - ws(2f)
            // (ekrandaki x başlangıcı, büyük mü?, boyut çarpanı)
            val boatDefs = listOf(
                Triple(sw * 0.18f, false, 1.00f),   // yelkenli
                Triple(sw * 0.52f, true,  1.00f),   // gemi
                Triple(sw * 0.82f, false, 0.90f)    // yelkenli
            )
            boatDefs.forEach { (baseX, big, sz) ->
                val sx = baseX - boatScroll
                // ekran dışına çıkınca karşı taraftan çıksın (modulo wrap)
                val wrapped = ((sx % sw) + sw) % sw
                if (big) grDrawShip(wrapped, boatBaseY, ws(28f) * sz)
                else grDrawSailboat(wrapped, boatBaseY, ws(14f) * sz)
            }

            // ── Bulutlar (kamerayla yavaş hareket) ───────────────────────────
            val cloudParallax = cam * 0.15f
            val clouds = listOf(
                Triple(400f - cloudParallax % GOLD_WORLD_W, sh * 0.10f, ws(38f)),
                Triple(900f - cloudParallax % GOLD_WORLD_W, sh * 0.06f, ws(30f)),
                Triple(1500f - cloudParallax % GOLD_WORLD_W, sh * 0.12f, ws(34f)),
                Triple(2200f - cloudParallax % GOLD_WORLD_W, sh * 0.08f, ws(28f)),
                Triple(2900f - cloudParallax % GOLD_WORLD_W, sh * 0.11f, ws(36f)),
                Triple(3600f - cloudParallax % GOLD_WORLD_W, sh * 0.07f, ws(32f))
            )
            clouds.forEach { (cx, cy, cr) ->
                val screenCX = (cx % (sw + cr * 3)) - cr
                val cc = Color.White.copy(alpha = 0.90f)
                drawCircle(cc, cr * 0.65f, Offset(screenCX - cr * 0.6f, cy + cr * 0.1f))
                drawCircle(cc, cr * 0.80f, Offset(screenCX,             cy))
                drawCircle(cc, cr * 0.65f, Offset(screenCX + cr * 0.7f, cy + cr * 0.05f))
                drawCircle(cc, cr * 0.50f, Offset(screenCX - cr * 0.2f, cy - cr * 0.35f))
            }

            // ── Platformlar (ada/zemin) ───────────────────────────────────────
            for (p in state.platforms) {
                val px = wx(p.x); val py = wy(p.y)
                val pw = ws(p.w); val ph = ws(p.h)
                if (px + pw < 0 || px > sw) continue
                // Kum üst katmanı
                drawRect(Color(0xFFF9D66B), Offset(px, py), Size(pw, ws(12f)))
                drawRect(Color(0xFFEFC050), Offset(px, py), Size(pw, ws(5f)))
                // Toprak/kaya gövde
                drawRect(Color(0xFF8D6E48), Offset(px, py + ws(12f)), Size(pw, ph - ws(12f)))
                drawRect(Color(0xFF795548).copy(alpha = 0.6f),
                    Offset(px, py + ws(22f)), Size(pw, ws(3f)))
                drawRect(Color(0xFF795548).copy(alpha = 0.4f),
                    Offset(px, py + ws(34f)), Size(pw, ws(3f)))
            }

            // Thorns
            for (th in state.thorns) {
                val tx = wx(th.x); val ty = wy(th.y)
                val tw = ws(GOLD_THORN_W); val thh = ws(GOLD_THORN_H)
                if (tx + tw < 0 || tx > sw) continue
                grDrawThorn(tx, ty, tw, thh, th.id)
            }

            // Gold coins
            for (c in state.coins) {
                if (c.collected && c.anim <= 0f) continue
                if (c.wrong && c.anim <= 0f) continue
                val rawCR = if (c.isNormal) GOLD_NORMAL_COIN_R else GOLD_COIN_R
                val cx = wx(c.x); val cy = wy(c.y)
                val cr = ws(rawCR)
                if (cx + cr * 2 < 0 || cx > sw) continue

                val animDur    = if (c.isNormal) 0.35f else 0.6f
                val collectOff = if (c.collected) -ws(30f) * (1f - c.anim / animDur) else 0f
                val wrongShake = if (c.wrong) sin(c.anim * 40f) * ws(4f) else 0f
                val alpha      = if (c.collected || c.wrong) (c.anim / animDur).coerceIn(0f, 1f) else 1f
                val drawCX = cx + cr + wrongShake
                val drawCY = cy + cr + collectOff

                if (c.isNormal) {
                    // Normal coin: parlak altın, küçük, yıldız parıltısı
                    drawCircle(Color(0xFFFFD700).copy(alpha = alpha), cr, Offset(drawCX, drawCY))
                    drawCircle(Color(0xFFFFF59D).copy(alpha = alpha * 0.7f), cr * 0.55f,
                        Offset(drawCX - cr * 0.2f, drawCY - cr * 0.2f))
                    drawCircle(Color(0xFFF9A825).copy(alpha = alpha), cr,
                        Offset(drawCX, drawCY), style = Stroke(ws(1.5f)))
                } else {
                    // Cevap coini: daha büyük, doğru=parlak, yanlış=mat kırmızı
                    val coinColor = when {
                        c.wrong     -> Color(0xFFE53935)
                        c.isCorrect -> Color(0xFFFFD700)
                        else        -> Color(0xFFCCAA00)
                    }
                    drawCircle(coinColor.copy(alpha = alpha), cr, Offset(drawCX, drawCY))
                    drawCircle(Color(0xFFFFEA00).copy(alpha = alpha * 0.6f), cr * 0.7f,
                        Offset(drawCX - cr * 0.15f, drawCY - cr * 0.15f))
                    drawCircle(Color(0xFFB8860B).copy(alpha = alpha), cr,
                        Offset(drawCX, drawCY), style = Stroke(ws(2f)))
                }
            }

            // Ball (soccer ball)
            val b   = state.ball
            val bx  = wx(b.x + GOLD_BALL_R)
            val by_ = wy(b.y + GOLD_BALL_R)
            val br  = ws(GOLD_BALL_R)
            val blinkOk = b.invTimer <= 0f || ((b.invTimer * 6).toInt() % 2 == 0)
            if (blinkOk && b.deadTimer <= 0f) {
                grDrawSoccerBall(bx, by_, br, b.angle)
            }
            if (b.deadTimer > 0f) {
                val da = (b.deadTimer / 1.2f).coerceIn(0f, 1f)
                grDrawSoccerBall(bx, by_, br * (0.6f + 0.4f * da), b.angle, alpha = da)
            }

            // Question cloud (shape only; text drawn in CoinNumbers layer)
            if (state.question != null && state.phase == GoldRunPhase.PLAYING) {
                grDrawQuestionCloud(bx, by_ - br - ws(72f), ws(110f), ws(42f))
            }
        }

        // Text overlay (coins + question cloud text)
        GrTextLayer(state = state)

        // ── HUD ───────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(state.lives) { Text("⚽", fontSize = 18.sp) }
                repeat((5 - state.lives).coerceAtLeast(0)) { Text("🖤", fontSize = 18.sp) }
            }
            Text(
                text  = "Puan: ${state.score}",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier
                    .background(Color(0x88000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Text(
                text  = "${state.questionsAnswered}/${state.totalQuestions}",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier
                    .background(Color(0x88000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        // ── Controls ──────────────────────────────────────────────────────────
        GrControlsOverlay(onEvent = onEvent, onBackPress = onBackPress)

        // ── End overlays ──────────────────────────────────────────────────────
        if (state.phase == GoldRunPhase.VICTORY) {
            GrVictoryOverlay(score = state.score,
                onRestart = { onEvent(GoldRunEvent.Restart) }, onBack = onBackPress)
        }
        if (state.phase == GoldRunPhase.GAME_OVER) {
            GrGameOverOverlay(score = state.score,
                onRestart = { onEvent(GoldRunEvent.Restart) }, onBack = onBackPress)
        }
    }
}

// ── Canvas helpers (prefixed gr to avoid clashes) ─────────────────────────────

private fun DrawScope.grDrawSoccerBall(cx: Float, cy: Float, r: Float, angle: Float, alpha: Float = 1f) {
    val center = Offset(cx, cy)
    drawCircle(Color.White.copy(alpha), r, center)
    val patchColor = Color.Black.copy(alpha)
    val a = Math.toRadians(angle.toDouble()).toFloat()
    drawCircle(patchColor, r * 0.28f, center)
    for (i in 0 until 5) {
        val ang = a + i * (2f * PI.toFloat() / 5f)
        val px = cx + cos(ang) * r * 0.58f
        val py = cy + sin(ang) * r * 0.58f
        drawCircle(patchColor, r * 0.22f, Offset(px, py))
    }
    drawCircle(Color(0xFF333333).copy(alpha), r, center, style = Stroke(r * 0.06f))

    // ── Yüz (döndürme yok — her zaman öne bakıyor) ───────────────────────────
    val eyeOffX = r * 0.30f
    val eyeOffY = r * 0.18f
    val eyeR    = r * 0.16f
    val pupilR  = r * 0.09f
    // Gözler
    drawCircle(Color.White.copy(alpha), eyeR, Offset(cx - eyeOffX, cy - eyeOffY))
    drawCircle(Color.White.copy(alpha), eyeR, Offset(cx + eyeOffX, cy - eyeOffY))
    // Gözbebekleri
    drawCircle(Color(0xFF1A1A1A).copy(alpha), pupilR, Offset(cx - eyeOffX + pupilR * 0.3f, cy - eyeOffY + pupilR * 0.3f))
    drawCircle(Color(0xFF1A1A1A).copy(alpha), pupilR, Offset(cx + eyeOffX + pupilR * 0.3f, cy - eyeOffY + pupilR * 0.3f))
    // Kaşlar
    drawLine(Color(0xFF1A1A1A).copy(alpha),
        Offset(cx - eyeOffX - eyeR * 0.8f, cy - eyeOffY - eyeR * 1.2f),
        Offset(cx - eyeOffX + eyeR * 0.8f, cy - eyeOffY - eyeR * 0.9f),
        strokeWidth = r * 0.09f)
    drawLine(Color(0xFF1A1A1A).copy(alpha),
        Offset(cx + eyeOffX - eyeR * 0.8f, cy - eyeOffY - eyeR * 0.9f),
        Offset(cx + eyeOffX + eyeR * 0.8f, cy - eyeOffY - eyeR * 1.2f),
        strokeWidth = r * 0.09f)
    // Gülümseme
    val smilePath = Path().apply {
        moveTo(cx - r * 0.30f, cy + r * 0.22f)
        quadraticBezierTo(cx, cy + r * 0.55f, cx + r * 0.30f, cy + r * 0.22f)
    }
    drawPath(smilePath, Color(0xFF1A1A1A).copy(alpha), style = Stroke(r * 0.09f, cap = StrokeCap.Round))
}

private fun DrawScope.grDrawSailboat(cx: Float, baseY: Float, size: Float) {
    // Gövde (trapez)
    val hw = size * 2.4f
    val hh = size * 0.55f
    val hull = Path().apply {
        moveTo(cx - hw * 0.50f, baseY)
        lineTo(cx + hw * 0.50f, baseY)
        lineTo(cx + hw * 0.38f, baseY + hh)
        lineTo(cx - hw * 0.38f, baseY + hh)
        close()
    }
    drawPath(hull, Color(0xFF6D4C41))
    drawPath(hull, Color(0xFF4E342E), style = Stroke(size * 0.07f))
    // Su çizgisi
    drawLine(Color(0xFFFFFFFF).copy(alpha = 0.4f),
        Offset(cx - hw * 0.48f, baseY + hh * 0.35f),
        Offset(cx + hw * 0.48f, baseY + hh * 0.35f), strokeWidth = size * 0.08f)
    // Direk
    val mastX = cx - size * 0.05f
    drawLine(Color(0xFF5D4037),
        Offset(mastX, baseY), Offset(mastX, baseY - size * 2.0f),
        strokeWidth = size * 0.09f)
    // Ana yelken (sağa)
    val sail1 = Path().apply {
        moveTo(mastX, baseY - size * 2.0f)
        lineTo(mastX, baseY - size * 0.08f)
        lineTo(mastX + size * 1.3f, baseY - size * 0.35f)
        close()
    }
    drawPath(sail1, Color(0xFFF5F5F5).copy(alpha = 0.92f))
    drawPath(sail1, Color(0xFFB0BEC5).copy(alpha = 0.4f), style = Stroke(size * 0.05f))
    // Ön yelken (sola)
    val sail2 = Path().apply {
        moveTo(mastX, baseY - size * 2.0f)
        lineTo(mastX, baseY - size * 0.08f)
        lineTo(mastX - size * 0.85f, baseY - size * 0.5f)
        close()
    }
    drawPath(sail2, Color(0xFFFFECB3).copy(alpha = 0.88f))
    drawPath(sail2, Color(0xFFFFCC80).copy(alpha = 0.4f), style = Stroke(size * 0.05f))
}

private fun DrawScope.grDrawShip(cx: Float, baseY: Float, size: Float) {
    val hw = size * 3.6f
    val hh = size * 0.75f
    // Ana gövde
    val hull = Path().apply {
        moveTo(cx - hw * 0.50f, baseY)
        lineTo(cx + hw * 0.50f, baseY)
        lineTo(cx + hw * 0.46f, baseY + hh)
        lineTo(cx - hw * 0.46f, baseY + hh)
        close()
    }
    drawPath(hull, Color(0xFF455A64))
    drawPath(hull, Color(0xFF263238), style = Stroke(size * 0.07f))
    // Beyaz şerit
    drawLine(Color(0xFFFFFFFF).copy(alpha = 0.55f),
        Offset(cx - hw * 0.46f, baseY + hh * 0.28f),
        Offset(cx + hw * 0.46f, baseY + hh * 0.28f),
        strokeWidth = size * 0.12f)
    // Üst yapı (köprü)
    val supW = hw * 0.40f
    val supH = size * 0.90f
    val supL = cx - supW * 0.5f + size * 0.3f
    drawRect(Color(0xFFECEFF1),
        topLeft = Offset(supL, baseY - supH), size = Size(supW, supH))
    drawRect(Color(0xFFB0BEC5),
        topLeft = Offset(supL, baseY - supH), size = Size(supW, supH),
        style = Stroke(size * 0.06f))
    // Camlar
    for (i in 0 until 3) {
        drawRect(Color(0xFF80D8FF).copy(alpha = 0.85f),
            topLeft = Offset(supL + size * 0.15f + i * supW * 0.30f, baseY - supH * 0.62f),
            size = Size(size * 0.22f, size * 0.28f))
    }
    // Baca
    val fnX = supL + supW * 0.6f
    drawRect(Color(0xFF37474F),
        topLeft = Offset(fnX, baseY - supH - size * 0.55f),
        size = Size(size * 0.28f, size * 0.55f))
    drawRect(Color(0xFFB71C1C),
        topLeft = Offset(fnX - size * 0.04f, baseY - supH - size * 0.58f),
        size = Size(size * 0.36f, size * 0.14f))
    // Bayrak
    val flagPath = Path().apply {
        val fx = supL + supW * 0.1f
        val fy = baseY - supH
        moveTo(fx, fy)
        lineTo(fx + size * 0.45f, fy + size * 0.22f)
        lineTo(fx, fy + size * 0.44f)
        close()
    }
    drawPath(flagPath, Color(0xFFEF5350))
}

// Çalı canavar renk paleti: parlak pembe, mori yeşil, sarı (id'ye göre döner)
private val BUSH_MONSTER_COLORS = listOf(
    Triple(Color(0xFFFF4DD9), Color(0xFFFF80EC), Color(0xFFCC00AA)),  // pembe
    Triple(Color(0xFF39FF14), Color(0xFF80FF60), Color(0xFF1A8000)),  // neon yeşil
    Triple(Color(0xFFFFE033), Color(0xFFFFEF80), Color(0xFFCC9900))   // sarı
)

private fun DrawScope.grDrawThorn(tx: Float, ty: Float, tw: Float, th: Float, id: Int = 0) {
    val (bodyColor, lightColor, darkColor) = BUSH_MONSTER_COLORS[id % 3]

    // ── Gövde (yuvarlak çalı gövdesi) ─────────────────────────────────────
    drawOval(darkColor, topLeft = Offset(tx, ty + th * 0.35f), size = Size(tw, th * 0.65f))
    drawOval(bodyColor, topLeft = Offset(tx + tw * 0.04f, ty + th * 0.30f),
        size = Size(tw * 0.92f, th * 0.65f))

    // ── Sivri uçlar (dikenlerin rengini body'e uyarla) ────────────────────
    for (i in 0 until 5) {
        val sx    = tx + tw * (0.1f + i * 0.2f)
        val baseY = ty + th * 0.4f
        val path  = Path().apply {
            moveTo(sx, baseY)
            lineTo(sx - tw * 0.07f, ty + th * 0.08f + (i % 2) * th * 0.06f)
            lineTo(sx + tw * 0.07f, baseY)
            close()
        }
        drawPath(path, darkColor)
        // Parlak kenar
        val path2 = Path().apply {
            moveTo(sx, baseY)
            lineTo(sx - tw * 0.03f, ty + th * 0.14f + (i % 2) * th * 0.06f)
            lineTo(sx + tw * 0.02f, baseY)
            close()
        }
        drawPath(path2, lightColor.copy(alpha = 0.55f))
    }

    // ── Üst parlaklık (canavar tüylü görünüm) ─────────────────────────────
    drawOval(lightColor.copy(alpha = 0.40f),
        topLeft = Offset(tx + tw * 0.12f, ty + th * 0.28f),
        size    = Size(tw * 0.55f, th * 0.28f))

    // ── Gözler ────────────────────────────────────────────────────────────
    val eyeR   = tw * 0.14f
    val pupilR = eyeR * 0.55f
    val eyeLX  = tx + tw * 0.30f
    val eyeRX  = tx + tw * 0.68f
    val eyeY   = ty + th * 0.52f

    // Göz akları
    drawCircle(Color.White, eyeR, Offset(eyeLX, eyeY))
    drawCircle(Color.White, eyeR, Offset(eyeRX, eyeY))
    // Gözbebekleri (sağ-alt'a bakan kötü bakış)
    drawCircle(Color(0xFF1A1A1A), pupilR, Offset(eyeLX + pupilR * 0.4f, eyeY + pupilR * 0.4f))
    drawCircle(Color(0xFF1A1A1A), pupilR, Offset(eyeRX + pupilR * 0.4f, eyeY + pupilR * 0.4f))
    // Parlak nokta
    drawCircle(Color.White.copy(alpha = 0.80f), pupilR * 0.30f,
        Offset(eyeLX + pupilR * 0.1f, eyeY - pupilR * 0.3f))
    drawCircle(Color.White.copy(alpha = 0.80f), pupilR * 0.30f,
        Offset(eyeRX + pupilR * 0.1f, eyeY - pupilR * 0.3f))
    // Kaşlar (kızgın V şeklinde)
    drawLine(darkColor,
        Offset(eyeLX - eyeR * 0.9f, eyeY - eyeR * 1.4f),
        Offset(eyeLX + eyeR * 0.6f, eyeY - eyeR * 0.9f),
        strokeWidth = tw * 0.06f)
    drawLine(darkColor,
        Offset(eyeRX - eyeR * 0.6f, eyeY - eyeR * 0.9f),
        Offset(eyeRX + eyeR * 0.9f, eyeY - eyeR * 1.4f),
        strokeWidth = tw * 0.06f)

    // ── Ağız (dişli sırıtma) ──────────────────────────────────────────────
    val mouthY = eyeY + eyeR * 1.6f
    val mouthL = tx + tw * 0.22f
    val mouthR = tx + tw * 0.78f
    val mouthPath = Path().apply {
        moveTo(mouthL, mouthY)
        quadraticBezierTo(tx + tw * 0.50f, mouthY + th * 0.10f, mouthR, mouthY)
    }
    drawPath(mouthPath, darkColor, style = Stroke(tw * 0.06f, cap = StrokeCap.Round))
    // Küçük dişler
    val toothW = (mouthR - mouthL) / 5f
    for (t in 0 until 4) {
        val tl = mouthL + t * toothW + toothW * 0.15f
        val tr = tl + toothW * 0.70f
        val toothPath = Path().apply {
            moveTo(tl, mouthY + th * 0.01f)
            lineTo((tl + tr) / 2f, mouthY + th * 0.07f)
            lineTo(tr, mouthY + th * 0.01f)
        }
        drawPath(toothPath, Color.White)
    }
}

private fun DrawScope.grDrawQuestionCloud(cx: Float, cy: Float, w: Float, h: Float) {
    val cloudColor = Color.White.copy(alpha = 0.92f)
    drawOval(cloudColor, Offset(cx - w * 0.5f, cy - h * 0.4f), Size(w, h * 0.9f))
    drawCircle(cloudColor, h * 0.45f, Offset(cx - w * 0.25f, cy - h * 0.3f))
    drawCircle(cloudColor, h * 0.38f, Offset(cx + w * 0.2f,  cy - h * 0.25f))
    val tailPath = Path().apply {
        moveTo(cx - w * 0.08f, cy + h * 0.3f)
        lineTo(cx - w * 0.02f, cy + h * 0.55f)
        lineTo(cx + w * 0.08f, cy + h * 0.3f)
        close()
    }
    drawPath(tailPath, cloudColor)
}

// ── Text layer (needs Composable scope for TextMeasurer) ──────────────────────
@Composable
private fun GrTextLayer(state: GoldRunState) {
    val tm = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxSize()) {
        val sw = size.width; val sh = size.height
        val scale = sh / GOLD_WORLD_H           // yüksekliğe göre ölçek
        val cam   = state.cameraX
        fun wx(x: Float) = (x - cam) * scale
        fun wy(y: Float) = y * scale
        fun ws(s2: Float) = s2 * scale

        // Coin value texts (sadece cevap coinleri — normal coinler numarasız)
        // fontSize sp → piksel: sp * density. İstediğimiz piksel boyutu cr*0.8 olsun → sp = piksel/density
        for (c in state.coins) {
            if (c.isNormal) continue   // normal coinlerde numara yok
            if (c.collected && c.anim <= 0f) continue
            if (c.wrong && c.anim <= 0f) continue
            val cx = wx(c.x); val cy = wy(c.y)
            val cr = ws(GOLD_COIN_R)
            if (cx + cr * 2 < 0 || cx > sw) continue

            val collectOff = if (c.collected) -ws(30f) * (1f - c.anim / 0.6f) else 0f
            val wrongShake = if (c.wrong) sin(c.anim * 40f) * ws(4f) else 0f
            val alpha      = if (c.collected || c.wrong) (c.anim / 0.6f).coerceIn(0f, 1f) else 1f
            val drawCX = cx + cr + wrongShake
            val drawCY = cy + cr + collectOff

            // cr piksel cinsinden; density ile bölünce sp'ye çevrilir
            // (Cevap coini burada, isNormal=false olanlar zaten filtreden geçti)
            val fontSizeSp = (cr * 0.75f / density)
            val style = TextStyle(
                fontSize   = fontSizeSp.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF5D3A00).copy(alpha = alpha)
            )
            val m = tm.measure(c.value.toString(), style)
            drawText(m, topLeft = Offset(drawCX - m.size.width / 2f, drawCY - m.size.height / 2f))
        }

        // Question cloud text (büyük bulut)
        if (state.question != null && state.phase == GoldRunPhase.PLAYING) {
            val b   = state.ball
            val bx  = (b.x + GOLD_BALL_R - cam) * scale
            val by_ = (b.y + GOLD_BALL_R) * scale
            val br  = GOLD_BALL_R * scale
            val cloudH = 42f * scale   // bulut yüksekliği piksel
            val qcy = by_ - br - 72f * scale

            // bulut içine sığacak font: piksel/density → sp
            val fontSizeSp = (cloudH * 0.38f / density)
            val qStyle = TextStyle(
                fontSize   = fontSizeSp.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF1A237E)
            )
            val qm = tm.measure(state.question.display, qStyle)
            drawText(qm, topLeft = Offset(bx - qm.size.width / 2f, qcy - qm.size.height / 2f))
        }
    }
}

// ── Controls overlay ──────────────────────────────────────────────────────────
@Composable
private fun GrControlsOverlay(onEvent: (GoldRunEvent) -> Unit, onBackPress: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Ekranın tamamı: dokun = zıpla ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitFirstDown(requireUnconsumed = false)
                            onEvent(GoldRunEvent.JumpDown)
                            do {
                                val ev = awaitPointerEvent()
                            } while (ev.changes.any { it.pressed })
                            onEvent(GoldRunEvent.JumpUp)
                        }
                    }
                }
        )

        // ── Zıplama ipucu (ortada, soluk) ────────────────────────────────────
        Text(
            text     = "☝ dokun = zıpla",
            color    = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        )

        // ── Geri butonu (✕) ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(44.dp)
                .background(Color(0xFFE53935), CircleShape)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitFirstDown(requireUnconsumed = false)
                            onBackPress()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) { Text("✕", color = Color.White, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
    }
}

// ── End-game overlays ─────────────────────────────────────────────────────────
@Composable
private fun GrVictoryOverlay(score: Int, onRestart: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🏆 Tebrikler!", fontSize = 36.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Puan: $score", fontSize = 24.sp, color = Color.White)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GrOverlayBtn("Tekrar", Color(0xFF4CAF50), onRestart)
                GrOverlayBtn("Çıkış",  Color(0xFFE53935), onBack)
            }
        }
    }
}

@Composable
private fun GrGameOverOverlay(score: Int, onRestart: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("💔 Oyun Bitti", fontSize = 32.sp, color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Puan: $score", fontSize = 24.sp, color = Color.White)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GrOverlayBtn("Tekrar", Color(0xFF4CAF50), onRestart)
                GrOverlayBtn("Çıkış",  Color(0xFFE53935), onBack)
            }
        }
    }
}

@Composable
private fun GrOverlayBtn(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) { awaitFirstDown(requireUnconsumed = false); onClick() }
                }
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}
