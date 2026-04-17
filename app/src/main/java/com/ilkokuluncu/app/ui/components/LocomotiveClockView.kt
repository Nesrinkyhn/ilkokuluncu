package com.ilkokuluncu.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Saat kadranı ön yüzünde olan sevimli lokomotif canvas bileşeni.
 *
 * @param hour       Gösterilecek saat (1-12)
 * @param minute     Dakika (Level 2'de hep 30)
 * @param bodyColor  Lokomotif ana rengi (vagon slotuyla eşleşir)
 * @param isHappy    true → gözler neşeli, false → üzgün
 * @param showCorrectMark  Doğru cevap overlay
 * @param showWrongMark    Yanlış cevap overlay
 */
@Composable
fun LocomotiveClockView(
    hour: Int,
    minute: Int = 30,
    bodyColor: Color = Color(0xFF1565C0),
    isHappy: Boolean = true,
    showCorrectMark: Boolean = false,
    showWrongMark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loco")

    // Bacadan çıkan buhar animasyonu
    val steamPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "steam"
    )

    // Lokomotif sallama (ray titreşimi)
    val shakeX by infiniteTransition.animateFloat(
        initialValue = -2.5f, targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
        label = "shakeX"
    )
    val shakeY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(240, easing = LinearEasing), RepeatMode.Reverse),
        label = "shakeY"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.65f)
            .offset(x = shakeX.dp, y = shakeY.dp)
    ) {
        val W = size.width
        val H = size.height

        // Koordinat yardımcıları — 300x182 referans boyutuna göre ölçeklenir
        fun px(v: Float) = v / 300f * W
        fun py(v: Float) = v / 182f * H

        // ── RAYLAR ───────────────────────────────────────────────────────────
        drawRails(::px, ::py, W)

        // ── TEKERLEKLER ──────────────────────────────────────────────────────
        drawWheel(px(50f), py(158f), px(15f), bodyColor)      // ön pilot
        drawWheel(px(120f), py(162f), px(24f), bodyColor)     // ana sürüş 1
        drawWheel(px(195f), py(162f), px(24f), bodyColor)     // ana sürüş 2
        drawWheel(px(260f), py(158f), px(15f), bodyColor)     // arka

        // Bağlantı çubuğu
        drawLine(
            Color(0xFFB71C1C),
            Offset(px(120f), py(162f)), Offset(px(195f), py(162f)),
            strokeWidth = px(5f), cap = StrokeCap.Round
        )
        // Küçük piston kolu
        drawLine(
            Color(0xFF8B0000),
            Offset(px(50f), py(158f)), Offset(px(120f), py(155f)),
            strokeWidth = px(3f), cap = StrokeCap.Round
        )

        // ── KAZAN GÖVDESİ ────────────────────────────────────────────────────
        // Ana gövde
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(px(24f), py(90f)),
            size = Size(px(208f), py(50f)),
            cornerRadius = CornerRadius(px(14f))
        )
        // Alt kırmızı şerit
        drawRoundRect(
            color = Color(0xFFB71C1C),
            topLeft = Offset(px(24f), py(130f)),
            size = Size(px(208f), py(13f)),
            cornerRadius = CornerRadius(px(6f))
        )
        // Altın şerit
        drawRect(
            color = Color(0xFFFFB300),
            topLeft = Offset(px(24f), py(128f)),
            size = Size(px(208f), py(4f))
        )
        // Gövde outline
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.4f).compositeOver(Color.Black.copy(alpha = 0.6f)),
            topLeft = Offset(px(24f), py(90f)),
            size = Size(px(208f), py(50f)),
            cornerRadius = CornerRadius(px(14f)),
            style = Stroke(px(2.5f))
        )

        // ── KABİN ────────────────────────────────────────────────────────────
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(px(210f), py(65f)),
            size = Size(px(86f), py(78f)),
            cornerRadius = CornerRadius(px(8f))
        )
        // Kabin çatısı (kırmızı, kavisli)
        val roofPath = Path().apply {
            moveTo(px(205f), py(67f))
            quadraticBezierTo(px(253f), py(44f), px(300f), py(67f))
            lineTo(px(300f), py(70f))
            lineTo(px(205f), py(70f))
            close()
        }
        drawPath(roofPath, Color(0xFFB71C1C))
        drawPath(roofPath, Color(0xFF8B0000), style = Stroke(px(2f)))
        // Çatı altın kenarı
        drawLine(
            Color(0xFFFFB300),
            Offset(px(205f), py(68f)), Offset(px(300f), py(68f)),
            px(3f)
        )
        // Kabin outline
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.4f).compositeOver(Color.Black.copy(alpha = 0.6f)),
            topLeft = Offset(px(210f), py(65f)),
            size = Size(px(86f), py(78f)),
            cornerRadius = CornerRadius(px(8f)),
            style = Stroke(px(2.5f))
        )
        // Kabin yan penceresi
        drawRoundRect(
            color = Color(0xFFB3E5FC).copy(alpha = 0.7f),
            topLeft = Offset(px(240f), py(80f)),
            size = Size(px(50f), py(34f)),
            cornerRadius = CornerRadius(px(5f))
        )
        drawRoundRect(
            color = Color(0xFF1A1A1A),
            topLeft = Offset(px(240f), py(80f)),
            size = Size(px(50f), py(34f)),
            cornerRadius = CornerRadius(px(5f)),
            style = Stroke(px(2f))
        )

        // ── GÖZLER (kabin ön yüzünde) ─────────────────────────────────────────
        drawEyes(px(216f), py(95f), py(122f), px(12.5f), isHappy)

        // ── BACA ─────────────────────────────────────────────────────────────
        val chiX = px(110f)
        drawRoundRect(
            Color(0xFF212121),
            Offset(chiX - px(8f), py(52f)),
            Size(px(16f), py(40f)),
            CornerRadius(px(4f))
        )
        // Baca üst flare
        drawRoundRect(
            Color(0xFF212121),
            Offset(chiX - px(13f), py(44f)),
            Size(px(26f), py(12f)),
            CornerRadius(px(5f))
        )
        // Baca altın halkası
        drawRect(
            Color(0xFFFFB300),
            Offset(chiX - px(8f), py(84f)),
            Size(px(16f), py(5f))
        )

        // ── BUHAR PUFLARI ─────────────────────────────────────────────────────
        for (i in 0..2) {
            val phase = (steamPhase + i * 0.33f) % 1f
            val pY = py(40f) - phase * py(40f)
            val alpha = (1f - phase).pow(1.3f) * 0.78f
            val pR = px(6f) + phase * px(14f)
            drawCircle(Color.White.copy(alpha = alpha), pR, Offset(chiX + (i - 1) * px(10f), pY))
            drawCircle(Color(0xFFDDDDDD).copy(alpha = alpha * 0.35f), pR,
                Offset(chiX + (i - 1) * px(10f), pY), style = Stroke(px(1.5f)))
        }

        // ── DOME (kazan üstü) ─────────────────────────────────────────────────
        drawOval(Color(0xFFFFB300), Offset(px(162f) - px(17f), py(76f)), Size(px(34f), py(20f)))
        drawOval(Color(0xFF1A1A1A), Offset(px(162f) - px(17f), py(76f)), Size(px(34f), py(20f)), style = Stroke(px(2f)))

        // ── ÖN TAMPON / COWCATCHER ────────────────────────────────────────────
        val cowPath = Path().apply {
            moveTo(px(24f), py(130f))
            lineTo(px(8f), py(160f))
            lineTo(px(24f), py(160f))
            lineTo(px(34f), py(130f))
            close()
        }
        drawPath(cowPath, Color(0xFFB71C1C))
        repeat(3) { bar ->
            val barY = py(137f + bar * 8f)
            drawLine(Color(0xFF8B0000), Offset(px(10f + bar * 2f), barY), Offset(px(31f), barY), px(2f))
        }
        // Ön far
        drawOval(Color(0xFFFFE082), Offset(px(18f), py(90f)), Size(px(13f), py(9f)))
        drawOval(Color(0xFF1A1A1A), Offset(px(18f), py(90f)), Size(px(13f), py(9f)), style = Stroke(px(1.5f)))

        // ── SAAT KADRANI (lokomotif ön yüzünde) ───────────────────────────────
        val clkCX = px(76f)
        val clkCY = py(115f)
        val clkR  = px(33f)

        // Gölge
        drawCircle(Color(0x33000000), clkR + px(3f), Offset(clkCX + px(2f), clkCY + px(2f)))
        // Altın dış halka
        drawCircle(Color(0xFFFFB300), clkR + px(2.5f), Offset(clkCX, clkCY))
        // Beyaz kadran
        drawCircle(Color.White, clkR, Offset(clkCX, clkCY))

        // Saat çizgileri
        for (h in 1..12) {
            val ang = Math.toRadians((h * 30.0 - 90.0))
            val d = if (h % 3 == 0) clkR * 0.76f else clkR * 0.82f
            drawCircle(
                Color(0xFF333333),
                if (h % 3 == 0) px(3f) else px(1.8f),
                Offset((clkCX + d * cos(ang)).toFloat(), (clkCY + d * sin(ang)).toFloat())
            )
        }

        // Akrep (yarım saat pozisyonu dahil)
        val hourAng = Math.toRadians(((hour * 30.0 + minute * 0.5) - 90.0))
        drawLine(
            Color(0xFF1A1A1A), Offset(clkCX, clkCY),
            Offset((clkCX + clkR * 0.52f * cos(hourAng)).toFloat(), (clkCY + clkR * 0.52f * sin(hourAng)).toFloat()),
            px(4.5f), StrokeCap.Round
        )

        // Yelkovan (30 dk = saat 6 konumu)
        val minAng = Math.toRadians((minute * 6.0 - 90.0))
        drawLine(
            Color(0xFF555555), Offset(clkCX, clkCY),
            Offset((clkCX + clkR * 0.68f * cos(minAng)).toFloat(), (clkCY + clkR * 0.68f * sin(minAng)).toFloat()),
            px(3f), StrokeCap.Round
        )

        // Merkez pim
        drawCircle(Color(0xFFB71C1C), px(4.5f), Offset(clkCX, clkCY))
        drawCircle(Color.White, px(2f), Offset(clkCX, clkCY))

        // Kadran outline
        drawCircle(Color(0xFF1A1A1A), clkR, Offset(clkCX, clkCY), style = Stroke(px(2.5f)))

        // Doğru/Yanlış cevap renk overlay
        if (showCorrectMark || showWrongMark) {
            drawCircle(
                color = if (showCorrectMark) Color(0xFF00C853).copy(alpha = 0.72f)
                        else Color(0xFFD50000).copy(alpha = 0.72f),
                radius = clkR,
                center = Offset(clkCX, clkCY)
            )
        }
    }
}

// ─── Yardımcı çizim fonksiyonları ────────────────────────────────────────────

private fun DrawScope.drawRails(
    px: (Float) -> Float,
    py: (Float) -> Float,
    W: Float
) {
    val r1 = py(174f)
    val r2 = py(183f)
    var tx = 0f
    while (tx < W) {
        drawRect(Color(0xFF795548), Offset(tx, r1 - py(2f)), Size(px(13f), py(11f)))
        tx += px(20f)
    }
    drawRect(Color(0xFF9E9E9E), Offset(0f, r1 - py(2f)), Size(W, py(4f)))
    drawRect(Color(0xFF9E9E9E), Offset(0f, r2 - py(2f)), Size(W, py(4f)))
}

private fun DrawScope.drawWheel(
    cx: Float, cy: Float, outerR: Float,
    accentColor: Color
) {
    // Dış halka (kırmızı)
    drawCircle(Color(0xFFB71C1C), outerR, Offset(cx, cy))
    // Karanlık iç
    drawCircle(Color(0xFF1A1A1A), outerR * 0.8f, Offset(cx, cy), style = Stroke(outerR * 0.22f))
    // Parmak maşalar (6 adet)
    for (i in 0 until 6) {
        val ang = PI * i / 3.0
        drawLine(
            Color(0xFF8B0000),
            Offset((cx + outerR * 0.12f * cos(ang)).toFloat(), (cy + outerR * 0.12f * sin(ang)).toFloat()),
            Offset((cx + outerR * 0.75f * cos(ang)).toFloat(), (cy + outerR * 0.75f * sin(ang)).toFloat()),
            strokeWidth = outerR * 0.13f, cap = StrokeCap.Round
        )
    }
    // Altın göbek
    drawCircle(Color(0xFFFFB300), outerR * 0.22f, Offset(cx, cy))
    drawCircle(Color(0xFF1A1A1A), outerR * 0.10f, Offset(cx, cy))
    // Dış outline
    drawCircle(Color(0xFF1A1A1A), outerR, Offset(cx, cy), style = Stroke(outerR * 0.09f))
}

private fun DrawScope.drawEyes(
    eyeX: Float, eye1Y: Float, eye2Y: Float,
    eyeR: Float, isHappy: Boolean
) {
    // Bakış yönü: mutluysa sola-aşağı (samimi), üzgünse aşağı
    val irisOff = if (isHappy) Offset(-eyeR * 0.18f, eyeR * 0.1f)
                  else         Offset(0f, eyeR * 0.28f)

    listOf(eye1Y, eye2Y).forEach { ey ->
        // Göz akı
        drawCircle(Color.White, eyeR, Offset(eyeX, ey))
        // İris (mavi)
        drawCircle(Color(0xFF1E88E5), eyeR * 0.56f, Offset(eyeX + irisOff.x, ey + irisOff.y))
        // Göz bebeği
        drawCircle(Color(0xFF0D0D0D), eyeR * 0.30f, Offset(eyeX + irisOff.x, ey + irisOff.y))
        // Pırıltı
        drawCircle(Color.White, eyeR * 0.13f,
            Offset(eyeX + irisOff.x - eyeR * 0.18f, ey + irisOff.y - eyeR * 0.18f))
        // Outline
        drawCircle(Color(0xFF1A1A1A), eyeR, Offset(eyeX, ey), style = Stroke(eyeR * 0.14f))
    }

    // Kaşlar
    val browY = -eyeR * 1.45f
    val tilt  = if (isHappy) -eyeR * 0.18f else eyeR * 0.22f
    listOf(eye1Y, eye2Y).forEach { ey ->
        drawLine(
            Color(0xFF1A1A1A),
            Offset(eyeX - eyeR * 0.55f + tilt, ey + browY + if (!isHappy) eyeR * 0.15f else 0f),
            Offset(eyeX + eyeR * 0.55f - tilt, ey + browY),
            strokeWidth = eyeR * 0.22f, cap = StrokeCap.Round
        )
    }
}

// Color.compositeOver extension (eğer API'de yoksa)
private fun Color.compositeOver(background: Color): Color {
    val a = this.alpha
    return Color(
        red   = this.red   * a + background.red   * (1 - a),
        green = this.green * a + background.green * (1 - a),
        blue  = this.blue  * a + background.blue  * (1 - a),
        alpha = 1f
    )
}
