package com.ilkokuluncu.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.data.*

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MarioGameScreen(
    state: MarioState,
    onEvent: (MarioEvent) -> Unit,
    onBackPress: () -> Unit
) {
    // Ekranı yan moda kilitle
    val activity = LocalContext.current as? Activity
    DisposableEffect(Unit) {
        val prev = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { activity?.requestedOrientation = prev }
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF42A5F5), Color(0xFFBBDEFB)))
        )
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val scale   = screenH / MARIO_WORLD_H

        // Kamera: oyuncu konumunu yumuşat
        val maxCam  = (MARIO_WORLD_W * scale - screenW).coerceAtLeast(0f)
        val camX    = (state.cameraX * scale).coerceIn(0f, maxCam)

        // ── Canvas ────────────────────────────────────────────────────
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {

            // Arka plan tepecikleri (parallax %20)
            drawHills(camX * 0.2f, screenW, screenH)

            // Bulutlar (parallax %12)
            drawClouds(camX * 0.12f, screenW, screenH)

            // Platformlar
            state.platforms.forEach { drawPlatform(it, camX, scale, screenH) }

            // Bitiş bayrağı
            drawGoal(state.goalX, camX, scale, screenH)

            // Coinler
            state.coins.forEach { drawCoin(it, camX, scale) }

            // Düşmanlar
            state.enemies.forEach { drawEnemy(it, camX, scale) }

            // Oyuncu
            drawMarioPlayer(state.player, camX, scale)
        }

        // ── HUD ───────────────────────────────────────────────────────
        MarioHUD(state = state, onBackPress = onBackPress)

        // ── Kontroller (◀ ▶) ─────────────────────────────────────────
        MarioControls(onEvent = onEvent)

        // ── Swipe-up zıplama — en üste, düğmeler kendi eventini tüketir
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startY = down.position.y
                            var jumped = false
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.find { it.id == down.id } ?: break
                                if (!ch.pressed) break
                                if (!jumped && startY - ch.position.y > 38f) {
                                    onEvent(MarioEvent.JumpDown)
                                    onEvent(MarioEvent.JumpUp)
                                    jumped = true
                                }
                            }
                        }
                    }
                }
        )

        // ── Bitiş ekranı ──────────────────────────────────────────────
        when (state.phase) {
            MarioPhase.VICTORY  -> MarioEndOverlay("🏆 Tebrikler!", state.player.score,
                { onEvent(MarioEvent.Restart) }, onBackPress)
            MarioPhase.GAME_OVER -> MarioEndOverlay("😢 Oyun Bitti", state.player.score,
                { onEvent(MarioEvent.Restart) }, onBackPress)
            else -> {}
        }
    }
}

// ── Drawing ──────────────────────────────────────────────────────────────────

private fun DrawScope.drawHills(camOff: Float, sw: Float, sh: Float) {
    val colors = listOf(Color(0xFF81C784), Color(0xFF66BB6A), Color(0xFF4CAF50))
    val hillData = listOf(
        Triple(300f, sh * 0.78f, 240f),
        Triple(750f, sh * 0.82f, 190f),
        Triple(1200f, sh * 0.76f, 280f),
        Triple(1700f, sh * 0.80f, 210f),
        Triple(2200f, sh * 0.77f, 260f),
        Triple(2700f, sh * 0.81f, 200f),
        Triple(3200f, sh * 0.79f, 230f),
    )
    hillData.forEach { (hx, hy, r) ->
        val dx = hx - (camOff % (sw + 600f))
        if (dx + r > 0 && dx - r < sw) {
            drawCircle(colors[((hx / 300).toInt()) % colors.size], r, Offset(dx, hy))
        }
    }
}

private fun DrawScope.drawClouds(camOff: Float, sw: Float, sh: Float) {
    val cloudSpots = listOf(120f, 380f, 650f, 900f, 1150f, 1400f)
    val cloudY = sh * 0.14f
    cloudSpots.forEach { cx ->
        val dx = cx - (camOff % 1500f)
        if (dx > -220f && dx < sw + 220f) drawCloud(Offset(dx, cloudY), sh * 0.055f)
        val dx2 = dx + 1500f
        if (dx2 > -220f && dx2 < sw + 220f) drawCloud(Offset(dx2, cloudY + sh * 0.06f), sh * 0.045f)
    }
}

private fun DrawScope.drawCloud(pos: Offset, r: Float) {
    val c = Color.White.copy(alpha = 0.92f)
    drawCircle(c, r,        pos)
    drawCircle(c, r * 0.7f, Offset(pos.x + r * 1.1f, pos.y + r * 0.25f))
    drawCircle(c, r * 0.7f, Offset(pos.x - r * 1.1f, pos.y + r * 0.25f))
    drawCircle(c, r * 0.55f,Offset(pos.x + r * 1.85f, pos.y + r * 0.65f))
    drawCircle(c, r * 0.55f,Offset(pos.x - r * 1.85f, pos.y + r * 0.65f))
}

private fun DrawScope.drawPlatform(plat: MPlatform, camX: Float, scale: Float, sh: Float) {
    val sx = plat.x * scale - camX
    val sy = plat.y * scale
    val sw = plat.w * scale
    val sh2 = plat.h * scale
    if (sx + sw < 0 || sx > size.width) return

    if (plat.isGrass) {
        // Çim zemin
        drawRect(Color(0xFF5D4037), Offset(sx, sy + scale * 10f), Size(sw, sh2 - scale * 10f))
        drawRect(Color(0xFF388E3C), Offset(sx, sy), Size(sw, scale * 12f))
        drawRect(Color(0xFF4CAF50), Offset(sx, sy + scale * 2f), Size(sw, scale * 5f))
        // Küçük taşlar
        val tileW = scale * 50f
        var tx = sx
        while (tx < sx + sw) {
            drawLine(Color(0xFF4E342E).copy(alpha = 0.25f), Offset(tx, sy + scale * 12f), Offset(tx, sy + sh2), scale * 1f)
            tx += tileW
        }
    } else {
        // Tuğla platform
        drawRect(Color(0xFFA0522D), Offset(sx, sy), Size(sw, sh2))
        drawRect(Color(0xFFBF6F3C), Offset(sx, sy), Size(sw, sh2 * 0.35f))
        val bW = scale * 18f; val bH = scale * 13f
        var bx = sx; while (bx < sx + sw) {
            drawLine(Color(0xFF6D3B1F), Offset(bx, sy), Offset(bx, sy + sh2), 1.5f); bx += bW
        }
        var by = sy; while (by < sy + sh2) {
            drawLine(Color(0xFF6D3B1F), Offset(sx, by), Offset(sx + sw, by), 1.5f); by += bH
        }
    }
}

private fun DrawScope.drawGoal(goalX: Float, camX: Float, scale: Float, sh: Float) {
    val gx = goalX * scale - camX
    if (gx < -80f || gx > size.width + 80f) return
    val poleX = gx + scale * 10f
    val poleTop = sh * 0.12f
    // Direk
    drawLine(Color(0xFF9E9E9E), Offset(poleX, poleTop), Offset(poleX, sh), scale * 4f)
    // Bayrak
    val flagPath = Path().apply {
        moveTo(poleX, poleTop)
        lineTo(poleX + scale * 38f, poleTop + scale * 18f)
        lineTo(poleX, poleTop + scale * 36f)
        close()
    }
    drawPath(flagPath, Color(0xFFE53935))
    drawCircle(Color(0xFFFFD700), scale * 7f, Offset(poleX, poleTop - scale * 6f))
    // "GOAL" kutusu
    drawRect(Color(0xFF388E3C), Offset(gx - scale * 5f, sh * 0.35f), Size(scale * 60f, scale * 30f))
}

private fun DrawScope.drawCoin(coin: MCoin, camX: Float, scale: Float) {
    if (coin.collected && coin.collectAnim <= 0f) return
    val cx = coin.x * scale - camX + scale * 10f
    val animRise = if (coin.collected) (1f - coin.collectAnim) * scale * 24f else 0f
    val cy = coin.y * scale - animRise + scale * 10f
    val r  = scale * 10f
    if (cx + r < 0 || cx - r > size.width) return
    val alpha = if (coin.collected) (coin.collectAnim * 2f).coerceAtMost(1f) else 1f
    drawCircle(Color(0xFFFFD700).copy(alpha = alpha), r, Offset(cx, cy))
    drawCircle(Color(0xFFFFF9C4).copy(alpha = alpha), r * 0.5f, Offset(cx - r * 0.2f, cy - r * 0.2f))
}

private fun DrawScope.drawEnemy(enemy: MEnemy, camX: Float, scale: Float) {
    if (!enemy.alive && enemy.squishTimer <= 0f) return

    val EW     = MARIO_TILE * 1.10f * scale          // biraz büyüttük
    val origEH = MARIO_TILE * 1.10f * scale
    val EH     = if (enemy.squished) origEH * 0.26f else origEH
    val ex     = enemy.x * scale - camX
    val ey     = if (enemy.squished) enemy.y * scale + (origEH - EH) * 0.9f
                 else enemy.y * scale
    if (ex + EW < 0 || ex > size.width) return

    val alpha = if (enemy.squished) (enemy.squishTimer / 0.4f).coerceIn(0f, 1f) else 1f
    val ecx   = ex + EW * 0.5f

    // ── Şapka (kubbe: bezier) ─────────────────────────────────────
    val capBottom = ey + EH * 0.58f
    val capPath = Path().apply {
        moveTo(ex - EW * 0.06f, capBottom)
        cubicTo(
            ex - EW * 0.06f, ey - EH * 0.30f,
            ex + EW * 1.06f, ey - EH * 0.30f,
            ex + EW * 1.06f, capBottom
        )
        close()
    }
    drawPath(capPath, Color(0xFFD32F2F).copy(alpha = alpha))      // kırmızı şapka

    // Şapka kenar çizgisi (hacim)
    drawPath(Path().apply {
        moveTo(ex + EW * 0.15f, ey + EH * 0.10f)
        cubicTo(ecx - EW*0.1f, ey - EH*0.18f, ecx + EW*0.1f, ey - EH*0.18f, ex + EW*0.85f, ey + EH*0.10f)
    }.also {}, Color(0xFFB71C1C).copy(alpha = alpha * 0.6f), style = Stroke(scale * 3f))

    // Büyük beyaz benekler
    drawCircle(Color.White.copy(alpha * 0.95f), EW * 0.12f, Offset(ecx - EW * 0.26f, ey + EH * 0.13f))
    drawCircle(Color.White.copy(alpha * 0.95f), EW * 0.10f, Offset(ecx + EW * 0.22f, ey + EH * 0.09f))
    drawCircle(Color.White.copy(alpha * 0.90f), EW * 0.08f, Offset(ecx + EW * 0.01f, ey + EH * 0.22f))

    // ── Vücut (aşağı oval) ────────────────────────────────────────
    val bodyTop = ey + EH * 0.46f
    val bodyH   = EH * 0.60f
    drawOval(Color(0xFFD7A57A).copy(alpha = alpha),
        Offset(ex + EW * 0.06f, bodyTop), Size(EW * 0.88f, bodyH))

    if (!enemy.squished) {
        val eyeY = bodyTop + bodyH * 0.28f

        // Gözler
        drawCircle(Color.White,           EW * 0.13f, Offset(ecx - EW * 0.22f, eyeY))
        drawCircle(Color.White,           EW * 0.13f, Offset(ecx + EW * 0.22f, eyeY))
        drawCircle(Color(0xFF1B0000),     EW * 0.075f, Offset(ecx - EW * 0.21f, eyeY + EW * 0.015f))
        drawCircle(Color(0xFF1B0000),     EW * 0.075f, Offset(ecx + EW * 0.21f, eyeY + EW * 0.015f))

        // Kızgın kaşlar
        drawLine(Color(0xFF3E2723),
            Offset(ecx - EW * 0.40f, eyeY - EW * 0.16f),
            Offset(ecx - EW * 0.07f, eyeY - EW * 0.08f), scale * 3f)
        drawLine(Color(0xFF3E2723),
            Offset(ecx + EW * 0.07f, eyeY - EW * 0.08f),
            Offset(ecx + EW * 0.40f, eyeY - EW * 0.16f), scale * 3f)

        // Ayaklar
        val footY  = ey + EH - EH * 0.06f
        val frame  = enemy.animFrame.toInt()
        val fOff   = if (frame == 0) EH * 0.07f else -EH * 0.07f
        drawOval(Color(0xFF4E342E).copy(alpha),
            Offset(ex + EW * 0.02f, footY + fOff), Size(EW * 0.40f, EH * 0.20f))
        drawOval(Color(0xFF4E342E).copy(alpha),
            Offset(ex + EW * 0.58f, footY - fOff), Size(EW * 0.40f, EH * 0.20f))
    }
}

private fun DrawScope.drawMarioPlayer(player: MarioPlayer, camX: Float, scale: Float) {
    val px = player.x * scale - camX
    val py = player.y * scale
    val PW = MARIO_PLAYER_W * scale
    val PH = MARIO_PLAYER_H * scale
    if (px + PW < -20f || px > size.width + 20f) return
    if (player.invTimer > 0f && (player.invTimer * 10).toInt() % 2 == 0) return

    val cx = px + PW * 0.5f
    val cy = py + PH * 0.5f
    val r  = PW * 0.5f

    // Zıplama/düşme ezme-germe
    val jumping = !player.onGround && player.vy < -60f
    val falling = !player.onGround && player.vy > 120f
    val sX = when { jumping -> 0.82f; falling -> 1.18f; else -> 1f }
    val sY = when { jumping -> 1.20f; falling -> 0.84f; else -> 1f }

    withTransform({ scale(sX, sY, Offset(cx, cy)) }) {

        // ── Gövde (turuncu top) ──────────────────────────────────────
        drawCircle(Color(0xFFFF7043), r, Offset(cx, cy))
        // Parlama (sol üst)
        drawCircle(Color(0xFFFFCC80).copy(alpha = 0.55f), r * 0.52f,
            Offset(cx - r * 0.28f, cy - r * 0.30f))
        // Alt gölge
        drawCircle(Color(0xFFBF360C).copy(alpha = 0.35f), r * 0.65f,
            Offset(cx + r * 0.15f, cy + r * 0.20f))

        // ── Gözler ───────────────────────────────────────────────────
        val eyeShift = r * 0.10f * (if (player.facingRight) 1f else -1f)
        val eyeR     = r * 0.30f
        val lEye     = Offset(cx - r * 0.30f + eyeShift, cy - r * 0.10f)
        val rEye     = Offset(cx + r * 0.30f + eyeShift, cy - r * 0.10f)

        drawCircle(Color.White, eyeR, lEye)
        drawCircle(Color.White, eyeR, rEye)

        val pupilShift = Offset(r * 0.09f * (if (player.facingRight) 1f else -1f), r * 0.05f)
        drawCircle(Color(0xFF1A237E), eyeR * 0.60f, lEye + pupilShift)
        drawCircle(Color(0xFF1A237E), eyeR * 0.60f, rEye + pupilShift)
        // Göz parlaması
        drawCircle(Color.White, eyeR * 0.24f, lEye + Offset(-eyeR * 0.22f, -eyeR * 0.22f))
        drawCircle(Color.White, eyeR * 0.24f, rEye + Offset(-eyeR * 0.22f, -eyeR * 0.22f))

        // ── Yanaklar (pembe) ─────────────────────────────────────────
        drawCircle(Color(0xFFE91E63).copy(alpha = 0.32f), r * 0.24f,
            Offset(cx - r * 0.66f, cy + r * 0.12f))
        drawCircle(Color(0xFFE91E63).copy(alpha = 0.32f), r * 0.24f,
            Offset(cx + r * 0.66f, cy + r * 0.12f))

        // ── Ağız ─────────────────────────────────────────────────────
        val mouthPath = Path().apply {
            moveTo(cx - r * 0.22f, cy + r * 0.30f)
            cubicTo(cx - r * 0.10f, cy + r * 0.48f, cx + r * 0.10f, cy + r * 0.48f, cx + r * 0.22f, cy + r * 0.30f)
        }
        drawPath(mouthPath, Color(0xFF7B1FA2), style = Stroke(scale * 2.5f))

        // ── Ayaklar (küçük toplar) ────────────────────────────────────
        val footY = cy + r * sY * 0.9f
        val frame = player.animFrame.toInt()
        val fOff  = if (player.vx != 0f && player.onGround)
            (if (frame == 0) r * 0.10f else -r * 0.10f) else 0f
        drawCircle(Color(0xFFD84315), r * 0.22f, Offset(cx - r * 0.32f, footY + fOff))
        drawCircle(Color(0xFFD84315), r * 0.22f, Offset(cx + r * 0.32f, footY - fOff))
    }
}

// ── HUD ──────────────────────────────────────────────────────────────────────
@Composable
private fun MarioHUD(state: MarioState, onBackPress: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(Color.Black.copy(0.28f))
                .clickable(onClick = onBackPress),
            contentAlignment = Alignment.Center
        ) { Text("◀", fontSize = 14.sp, color = Color.White) }

        Spacer(Modifier.width(8.dp))

        repeat(state.player.lives.coerceIn(0, 5)) { Text("❤️", fontSize = 15.sp) }

        Spacer(Modifier.weight(1f))

        Text(
            "⭐ ${state.player.score}",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// ── Kontroller (sadece ◀ ▶ — zıplama swipe ile) ──────────────────────────────
@Composable
private fun MarioControls(onEvent: (MarioEvent) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HoldButton("◀", 60.dp, onDown = { onEvent(MarioEvent.LeftDown)  }, onUp = { onEvent(MarioEvent.LeftUp) })
            HoldButton("▶", 60.dp, onDown = { onEvent(MarioEvent.RightDown) }, onUp = { onEvent(MarioEvent.RightUp) })
        }
        // Zıplama ipucu
        Text(
            "↑ Yukarı çek = zıpla",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun HoldButton(
    label: String,
    size: Dp,
    bgColor: Color = Color.White.copy(alpha = 0.28f),
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    Box(
        Modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(bgColor)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        onDown()
                        do {
                            val ev = awaitPointerEvent()
                        } while (ev.changes.any { it.pressed })
                        onUp()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = (size.value * 0.40f).sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
    }
}

// ── Bitiş overlay ─────────────────────────────────────────────────────────────
@Composable
private fun MarioEndOverlay(title: String, score: Int, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xBB000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.48f)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1A0A2E), Color(0xFF2D1B5E))))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title,  fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("⭐ $score", fontSize = 30.sp, color = Color(0xFFFFEA00), fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
                    .background(Color(0xFF00897B)).clickable(onClick = onRetry).padding(12.dp),
                contentAlignment = Alignment.Center
            ) { Text("🔄 Tekrar Oyna", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
                    .background(Color(0xFF37474F)).clickable(onClick = onBack).padding(12.dp),
                contentAlignment = Alignment.Center
            ) { Text("◀ Menü", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        }
    }
}
