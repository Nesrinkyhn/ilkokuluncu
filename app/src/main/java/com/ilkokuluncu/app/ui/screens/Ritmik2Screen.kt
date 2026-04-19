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
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.*
import com.ilkokuluncu.app.R
import com.ilkokuluncu.app.data.*
import com.ilkokuluncu.app.viewmodel.Ritmik2Sound
import com.ilkokuluncu.app.viewmodel.Ritmik2ViewModel
import com.ilkokuluncu.app.ui.effects.GameBackgroundMusic
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.*

// ── Ray renkleri ─────────────────────────────────────────────────────────────
private val TRACK_COLORS = listOf(
    listOf(Color(0xFFE65100), Color(0xFFFF8F00)),  // 0=alt: turuncu
    listOf(Color(0xFF4A148C), Color(0xFF7B1FA2)),  // 1=orta: mor
    listOf(Color(0xFF004D40), Color(0xFF00897B))   // 2=üst: deniz yeşili
)
private val TRACK_LABELS  = listOf("0", "1", "2")
private val CORRECT_GLOW  = Color(0xFFFFD700)

@Composable
fun Ritmik2Screen(
    state: Ritmik2State,
    viewModel: Ritmik2ViewModel,
    onBackPress: () -> Unit
) {
    val context  = LocalContext.current
    val activity = context as? Activity

    // Yatay kilitle
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Arka plan müziği
    GameBackgroundMusic(volume = 0.30f)

    // SoundPool — efekt sesleri
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
                Ritmik2Sound.Tap      -> pool.play(sTap,     0.65f, 0.65f, 1, 0, 1.0f)
                Ritmik2Sound.Correct  -> pool.play(sCorrect, 0.90f, 0.90f, 1, 0, 1.1f)
                Ritmik2Sound.Wrong    -> pool.play(sWrong,   0.90f, 0.90f, 1, 0, 0.9f)
                Ritmik2Sound.CycleWin -> pool.play(sCorrect, 1.00f, 1.00f, 1, 0, 1.4f)
            }
        }
    }

    val tm = rememberTextMeasurer()

    // Dalga karo pozisyonları için güncel state kopyası (pointerInput closure stale olmasın)
    val tilesState = rememberUpdatedState(state.tiles)
    val phaseState = rememberUpdatedState(state.phase)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B1F))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (phaseState.value != Ritmik2Phase.PLAYING) return@detectTapGestures
                        val sh = size.height.toFloat()
                        val trackYs = trackYPositions(sh)
                        val tiles = tilesState.value
                        for (tile in tiles) {
                            if (tile.hitCorrect || tile.hitWrong) continue
                            val ty = trackYs[tile.track]
                            if (offset.x in tile.x..(tile.x + R2_TILE_W) &&
                                offset.y in (ty - R2_TILE_H / 2)..(ty + R2_TILE_H / 2)
                            ) {
                                viewModel.onTileTapped(tile.id)
                                break
                            }
                        }
                    }
                }
        ) {
            val sw = size.width
            val sh = size.height

            // Ekran genişliğini ViewModel'e bildir
            if (state.screenW != sw) viewModel.setScreenWidth(sw)

            val trackYs = trackYPositions(sh)

            // ── Arka plan degradesi ──────────────────────────────────────────
            drawRect(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0B1F), Color(0xFF12123A), Color(0xFF0B0B1F))
                )
            )

            // Yıldızlar (sabit seed → stabil)
            val starRng = java.util.Random(42L)
            repeat(80) {
                val sx = starRng.nextFloat() * sw
                val sy = starRng.nextFloat() * sh
                val sr = starRng.nextFloat() * 2.2f + 0.5f
                val sa = 0.4f + starRng.nextFloat() * 0.6f
                drawCircle(Color.White.copy(alpha = sa), sr, Offset(sx, sy))
            }

            // ── Raylar ──────────────────────────────────────────────────────
            trackYs.forEachIndexed { i, ty ->
                // Ray bandı
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(0.05f), Color.White.copy(0.02f)),
                        startY = ty - R2_TILE_H / 2 - 6f,
                        endY   = ty + R2_TILE_H / 2 + 6f
                    ),
                    topLeft = Offset(0f, ty - R2_TILE_H / 2 - 6f),
                    size    = Size(sw, R2_TILE_H + 12f)
                )
                // Üst çizgi
                drawLine(
                    TRACK_COLORS[i][0].copy(alpha = 0.4f),
                    Offset(0f, ty - R2_TILE_H / 2 - 2f), Offset(sw, ty - R2_TILE_H / 2 - 2f),
                    strokeWidth = 1.5f
                )
                // Alt çizgi
                drawLine(
                    TRACK_COLORS[i][1].copy(alpha = 0.4f),
                    Offset(0f, ty + R2_TILE_H / 2 + 2f), Offset(sw, ty + R2_TILE_H / 2 + 2f),
                    strokeWidth = 1.5f
                )
            }

            // "Hedef çizgisi" — sol taraf, karoların buraya gelmesi gerekiyor
            val targetX = sw * 0.22f
            drawLine(
                Color.White.copy(alpha = 0.25f),
                Offset(targetX, 0f), Offset(targetX, sh),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            )

            // ── Karolar ─────────────────────────────────────────────────────
            for (tile in state.tiles) {
                val ty = trackYs[tile.track]
                r2DrawTile(this, tile, ty, tm)
            }

            // ── Fail kırmızı flaşı ──────────────────────────────────────────
            if (state.phase == Ritmik2Phase.FAIL_ANIM) {
                val flashAlpha = (state.failAnim / 2.2f).coerceIn(0f, 0.55f)
                drawRect(Color(0xFFD50000).copy(alpha = flashAlpha))
            }

            // ── Tur kazanma altın flaşı ─────────────────────────────────────
            if (state.phase == Ritmik2Phase.CYCLE_WIN) {
                val flashAlpha = ((state.cycleWinAnim - 1.8f) / 1.0f).coerceIn(0f, 0.45f)
                drawRect(Color(0xFFFFD700).copy(alpha = flashAlpha))
            }
        }

        // ── HUD ─────────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {

            // Üst satır: geri | ilerleme noktaları | skor
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Geri butonu
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) { detectTapGestures { onBackPress() } },
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = Color.White, fontSize = 15.sp) }

                // Sayı dizisi: 2 4 6 … 20
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    RITMIK2_SEQUENCE.forEach { num ->
                        val done    = num in state.correctHits
                        val current = num == state.currentTarget && state.phase == Ritmik2Phase.PLAYING
                        Box(
                            modifier = Modifier
                                .background(
                                    when {
                                        done    -> Color(0xFF4CAF50)
                                        current -> Color(0xFFFFD700)
                                        else    -> Color.White.copy(alpha = 0.13f)
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
                    Text(
                        text       = "⭐ ${state.totalScore}",
                        color      = Color(0xFFFFD700),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 20.sp
                    )
                    Text(
                        text     = "+${state.pointsPerHit} puan",
                        color    = Color(0xFFFFD700).copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                }
            }

            // Alt satır: hedef | hız | tur
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = "Hedef: ${state.currentTarget}",
                    color      = Color(0xFFFFD700),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 16.sp
                )
                if (state.cycleCount > 0) {
                    Text(
                        text      = "Tur ${state.cycleCount + 1}",
                        color     = Color(0xFF80DEEA),
                        fontSize  = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text    = "Hız ${state.speed.toInt()}",
                    color   = Color.White.copy(0.45f),
                    fontSize = 11.sp
                )
            }
        }

        // ── Geri sayım (sadece oyun ilk açılışında) ─────────────────────────
        if (state.phase == Ritmik2Phase.COUNTDOWN) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val cdNum = ceil(state.countdown).toInt().coerceIn(1, 3)
                Text(
                    text       = if (state.countdown > 0.5f) cdNum.toString() else "Başla!",
                    fontSize   = 96.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White,
                    modifier   = Modifier.background(
                        Color.Black.copy(alpha = 0.45f), RoundedCornerShape(24.dp)
                    ).padding(horizontal = 40.dp, vertical = 20.dp)
                )
            }
        }

        // ── Fail ekranı ─────────────────────────────────────────────────────
        if (state.phase == Ritmik2Phase.FAIL_ANIM && state.failAnim > 1.5f) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("❌", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Yanlış! Baştan Başlıyoruz",
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color(0xFFFF5252)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Doğru sıra: ${RITMIK2_SEQUENCE.joinToString(" → ")}",
                        fontSize = 14.sp,
                        color    = Color.White.copy(0.75f)
                    )
                }
            }
        }

        // CYCLE_WIN: sadece kısa altın flaş, mesaj yok
    }
}

// ── Karo çizimi ──────────────────────────────────────────────────────────────
private fun r2DrawTile(scope: DrawScope, tile: RitmikTile, centerY: Float, tm: TextMeasurer) {
    with(scope) {
        val topLeft = Offset(tile.x, centerY - R2_TILE_H / 2)
        val size    = Size(R2_TILE_W, R2_TILE_H)

        // Animasyon ölçeği
        val scale = when {
            tile.hitCorrect -> 1f + (tile.anim / 0.7f) * 0.18f
            tile.hitWrong   -> 1f - (tile.anim / 0.6f) * 0.12f
            else            -> 1f
        }.coerceIn(0.5f, 1.5f)

        // Alfa
        val alpha = when {
            tile.hitWrong && !tile.hitCorrect -> (tile.anim / 0.4f).coerceIn(0f, 0.75f)
            else -> 1f
        }

        val colors = TRACK_COLORS[tile.track]
        val cx = tile.x + R2_TILE_W / 2
        val cy = centerY

        withTransform({
            scale(scale, scale, Offset(cx, cy))
        }) {
            // Altın parıltı (doğru karo)
            if (tile.isCorrect && !tile.hitCorrect && !tile.hitWrong) {
                drawRoundRect(
                    CORRECT_GLOW.copy(alpha = 0.35f * alpha),
                    topLeft = Offset(tile.x - 6f, centerY - R2_TILE_H / 2 - 6f),
                    size    = Size(R2_TILE_W + 12f, R2_TILE_H + 12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f)
                )
            }

            // Karo gövdesi
            val bgColor = when {
                tile.hitCorrect -> Color(0xFF4CAF50)
                tile.hitWrong   -> Color(0xFFD50000)
                else            -> null
            }
            if (bgColor != null) {
                drawRoundRect(
                    bgColor.copy(alpha = alpha),
                    topLeft = topLeft, size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f)
                )
            } else {
                drawRoundRect(
                    Brush.linearGradient(
                        listOf(colors[0].copy(alpha = alpha), colors[1].copy(alpha = alpha)),
                        start = topLeft, end = Offset(tile.x + R2_TILE_W, centerY + R2_TILE_H / 2)
                    ),
                    topLeft = topLeft, size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f)
                )
            }

            // Kenarlık
            val borderColor = if (tile.isCorrect && !tile.hitCorrect) CORRECT_GLOW else Color.White.copy(0.3f)
            drawRoundRect(
                borderColor.copy(alpha = alpha),
                topLeft = topLeft, size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f),
                style = Stroke(if (tile.isCorrect && !tile.hitCorrect) 3.5f else 1.5f)
            )

            // Sayı yazısı
            val emoji = when {
                tile.hitCorrect -> "✓"
                tile.hitWrong   -> "✗"
                else            -> tile.number.toString()
            }
            val fontSize = if (tile.number >= 10) 36f else 42f
            val style = TextStyle(
                fontSize   = (fontSize / density).sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White.copy(alpha = alpha)
            )
            val m = tm.measure(emoji, style)
            drawText(m, topLeft = Offset(cx - m.size.width / 2f, cy - m.size.height / 2f))
        }
    }
}

// ── Yardımcılar ──────────────────────────────────────────────────────────────
private fun trackYPositions(sh: Float) = listOf(sh * 0.76f, sh * 0.50f, sh * 0.24f)
