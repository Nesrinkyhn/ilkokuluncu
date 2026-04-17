package com.ilkokuluncu.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.data.AnimalCharacter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hayvan şeklinde analog saat
 * Her hayvan için özel shape
 */
@Composable
fun ClockView(
    hour: Int,
    animalEmoji: String = "🐱",
    animal: AnimalCharacter = AnimalCharacter.CAT,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val radius = size.width * 0.42f

                when (animal) {
                    AnimalCharacter.CAT -> drawCatClock(centerX, centerY, radius, animal, hour)
                    AnimalCharacter.DOG -> drawDogClock(centerX, centerY, radius, animal, hour)
                    AnimalCharacter.RABBIT -> drawRabbitClock(centerX, centerY, radius, animal, hour)
                    AnimalCharacter.BEAR -> drawBearClock(centerX, centerY, radius, animal, hour)
                    AnimalCharacter.FOX -> drawFoxClock(centerX, centerY, radius, animal, hour)
                    AnimalCharacter.PANDA -> drawPandaClock(centerX, centerY, radius, animal, hour)
                    AnimalCharacter.LION -> drawLionClock(centerX, centerY, radius, animal, hour)
                    AnimalCharacter.TIGER -> drawTigerClock(centerX, centerY, radius, animal, hour)
                    AnimalCharacter.COW -> drawCowClock(centerX, centerY, radius, animal, hour)
                    AnimalCharacter.PIG -> drawPigClock(centerX, centerY, radius, animal, hour)
                    AnimalCharacter.MONKEY -> drawMonkeyClock(centerX, centerY, radius, animal, hour)
                    AnimalCharacter.ELEPHANT -> drawElephantClock(centerX, centerY, radius, animal, hour)
                }
            }

            Text(
                text = animalEmoji,
                fontSize = 48.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-24).dp)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCatClock(
    centerX: Float, centerY: Float, radius: Float, animal: AnimalCharacter, hour: Int
) {
    val earSize = radius * 0.3f
    val leftEarPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(centerX - radius * 0.6f, centerY - radius * 0.8f)
        lineTo(centerX - radius * 0.3f, centerY - radius * 1.1f)
        lineTo(centerX - radius * 0.2f, centerY - radius * 0.7f)
        close()
    }
    val rightEarPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(centerX + radius * 0.6f, centerY - radius * 0.8f)
        lineTo(centerX + radius * 0.3f, centerY - radius * 1.1f)
        lineTo(centerX + radius * 0.2f, centerY - radius * 0.7f)
        close()
    }
    drawPath(leftEarPath, brush = Brush.linearGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor))))
    drawPath(rightEarPath, brush = Brush.linearGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor))))
    drawCircle(Brush.radialGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor)), Offset(centerX, centerY), radius), radius, Offset(centerX, centerY))
    drawCircle(Color.White, radius, Offset(centerX, centerY), style = Stroke(8f))
    drawClockElements(centerX, centerY, radius, hour)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDogClock(
    centerX: Float, centerY: Float, radius: Float, animal: AnimalCharacter, hour: Int
) {
    val earWidth = radius * 0.4f
    val earHeight = radius * 0.7f
    drawOval(Brush.linearGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor))), Offset(centerX - radius - earWidth * 0.5f, centerY - radius * 0.2f), Size(earWidth, earHeight))
    drawOval(Brush.linearGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor))), Offset(centerX + radius - earWidth * 0.5f, centerY - radius * 0.2f), Size(earWidth, earHeight))
    drawCircle(Brush.radialGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor)), Offset(centerX, centerY), radius), radius, Offset(centerX, centerY))
    drawCircle(Color.White, radius, Offset(centerX, centerY), style = Stroke(8f))
    drawClockElements(centerX, centerY, radius, hour)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRabbitClock(
    centerX: Float, centerY: Float, radius: Float, animal: AnimalCharacter, hour: Int
) {
    val earWidth = radius * 0.25f
    val earHeight = radius * 1.2f
    drawOval(Brush.linearGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor))), Offset(centerX - radius * 0.5f - earWidth * 0.5f, centerY - radius - earHeight * 0.7f), Size(earWidth, earHeight))
    drawOval(Brush.linearGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor))), Offset(centerX + radius * 0.5f - earWidth * 0.5f, centerY - radius - earHeight * 0.7f), Size(earWidth, earHeight))
    drawCircle(Brush.radialGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor)), Offset(centerX, centerY), radius), radius, Offset(centerX, centerY))
    drawCircle(Color.White, radius, Offset(centerX, centerY), style = Stroke(8f))
    drawClockElements(centerX, centerY, radius, hour)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawElephantClock(
    centerX: Float, centerY: Float, radius: Float, animal: AnimalCharacter, hour: Int
) {
    val trunkPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(centerX, centerY + radius * 0.8f)
        cubicTo(centerX - radius * 0.2f, centerY + radius * 1.2f, centerX - radius * 0.4f, centerY + radius * 1.5f, centerX - radius * 0.3f, centerY + radius * 1.8f)
    }
    drawPath(trunkPath, Color(animal.primaryColor), style = Stroke(radius * 0.2f, cap = StrokeCap.Round))
    val earRadius = radius * 0.6f
    drawCircle(Brush.radialGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor))), earRadius, Offset(centerX - radius * 0.8f, centerY))
    drawCircle(Brush.radialGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor))), earRadius, Offset(centerX + radius * 0.8f, centerY))
    drawCircle(Brush.radialGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor)), Offset(centerX, centerY), radius), radius, Offset(centerX, centerY))
    drawCircle(Color.White, radius, Offset(centerX, centerY), style = Stroke(8f))
    drawClockElements(centerX, centerY, radius, hour)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPigClock(
    centerX: Float, centerY: Float, radius: Float, animal: AnimalCharacter, hour: Int
) {
    val earPath1 = androidx.compose.ui.graphics.Path().apply {
        moveTo(centerX - radius * 0.6f, centerY - radius * 0.7f)
        lineTo(centerX - radius * 0.3f, centerY - radius * 0.9f)
        lineTo(centerX - radius * 0.4f, centerY - radius * 0.5f)
        close()
    }
    val earPath2 = androidx.compose.ui.graphics.Path().apply {
        moveTo(centerX + radius * 0.6f, centerY - radius * 0.7f)
        lineTo(centerX + radius * 0.3f, centerY - radius * 0.9f)
        lineTo(centerX + radius * 0.4f, centerY - radius * 0.5f)
        close()
    }
    drawPath(earPath1, Color(animal.primaryColor))
    drawPath(earPath2, Color(animal.primaryColor))
    drawCircle(Brush.radialGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor)), Offset(centerX, centerY), radius), radius, Offset(centerX, centerY))
    drawOval(Color(animal.secondaryColor), Offset(centerX - radius * 0.25f, centerY + radius * 0.4f), Size(radius * 0.5f, radius * 0.3f))
    drawCircle(Color.White, radius, Offset(centerX, centerY), style = Stroke(8f))
    drawClockElements(centerX, centerY, radius, hour)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBearClock(
    centerX: Float, centerY: Float, radius: Float, animal: AnimalCharacter, hour: Int
) {
    drawCircle(Color(animal.primaryColor), radius * 0.35f, Offset(centerX - radius * 0.7f, centerY - radius * 0.7f))
    drawCircle(Color(animal.primaryColor), radius * 0.35f, Offset(centerX + radius * 0.7f, centerY - radius * 0.7f))
    drawCircle(Brush.radialGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor)), Offset(centerX, centerY), radius), radius, Offset(centerX, centerY))
    drawCircle(Color.White, radius, Offset(centerX, centerY), style = Stroke(8f))
    drawClockElements(centerX, centerY, radius, hour)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFoxClock(
    centerX: Float, centerY: Float, radius: Float, animal: AnimalCharacter, hour: Int
) = drawCatClock(centerX, centerY, radius, animal, hour)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPandaClock(
    centerX: Float, centerY: Float, radius: Float, animal: AnimalCharacter, hour: Int
) = drawBearClock(centerX, centerY, radius, animal, hour)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLionClock(
    centerX: Float, centerY: Float, radius: Float, animal: AnimalCharacter, hour: Int
) {
    for (i in 0..11) {
        val angle = i * 30.0
        val startR = radius * 1.1f
        val endR = radius * 1.4f
        val x1 = centerX + startR * cos(angle * PI / 180).toFloat()
        val y1 = centerY + startR * sin(angle * PI / 180).toFloat()
        val x2 = centerX + endR * cos(angle * PI / 180).toFloat()
        val y2 = centerY + endR * sin(angle * PI / 180).toFloat()
        drawLine(Brush.linearGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor))), Offset(x1, y1), Offset(x2, y2), radius * 0.15f, StrokeCap.Round)
    }
    drawCircle(Brush.radialGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor)), Offset(centerX, centerY), radius), radius, Offset(centerX, centerY))
    drawCircle(Color.White, radius, Offset(centerX, centerY), style = Stroke(8f))
    drawClockElements(centerX, centerY, radius, hour)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTigerClock(
    centerX: Float, centerY: Float, radius: Float, animal: AnimalCharacter, hour: Int
) {
    for (i in 0..5) {
        val angle = i * 60.0
        val startR = radius * 0.7f
        val endR = radius * 1.0f
        val x1 = centerX + startR * cos(angle * PI / 180).toFloat()
        val y1 = centerY + startR * sin(angle * PI / 180).toFloat()
        val x2 = centerX + endR * cos(angle * PI / 180).toFloat()
        val y2 = centerY + endR * sin(angle * PI / 180).toFloat()
        drawLine(Color.Black.copy(0.3f), Offset(x1, y1), Offset(x2, y2), 4f)
    }
    drawCircle(Brush.radialGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor)), Offset(centerX, centerY), radius), radius, Offset(centerX, centerY))
    drawCircle(Color.White, radius, Offset(centerX, centerY), style = Stroke(8f))
    drawClockElements(centerX, centerY, radius, hour)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCowClock(
    centerX: Float, centerY: Float, radius: Float, animal: AnimalCharacter, hour: Int
) {
    drawCircle(Color.Black.copy(0.3f), radius * 0.2f, Offset(centerX - radius * 0.4f, centerY - radius * 0.3f))
    drawCircle(Color.Black.copy(0.3f), radius * 0.25f, Offset(centerX + radius * 0.3f, centerY + radius * 0.2f))
    drawCircle(Brush.radialGradient(listOf(Color(animal.primaryColor), Color(animal.secondaryColor)), Offset(centerX, centerY), radius), radius, Offset(centerX, centerY))
    drawCircle(Color.White, radius, Offset(centerX, centerY), style = Stroke(8f))
    drawClockElements(centerX, centerY, radius, hour)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMonkeyClock(
    centerX: Float, centerY: Float, radius: Float, animal: AnimalCharacter, hour: Int
) = drawBearClock(centerX, centerY, radius, animal, hour)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClockElements(
    centerX: Float, centerY: Float, radius: Float, hour: Int
) {
    // RENKLI RAKAMLAR + SİYAH KONTUR (Hem renkli hem görünür!)
    val numberColors = listOf(
        Color(0xFFFF1744), // Parlak Kırmızı
        Color(0xFF00E676), // Parlak Yeşil
        Color(0xFF2979FF), // Parlak Mavi
        Color(0xFFFFEA00), // Parlak Sarı
        Color(0xFFE040FB), // Parlak Mor
        Color(0xFF00E5FF), // Parlak Cyan
        Color(0xFFFF6E40), // Parlak Turuncu
        Color(0xFF76FF03), // Parlak Lime
        Color(0xFFFF4081), // Parlak Pembe
        Color(0xFF651FFF), // Parlak Koyu Mor
        Color(0xFF00BFA5), // Parlak Turkuaz
        Color(0xFFFFD600)  // Parlak Altın
    )

    for (i in 1..12) {
        val angle = (i - 3) * 30.0
        val numberRadius = radius * 0.7f
        val x = centerX + numberRadius * cos(angle * PI / 180).toFloat()
        val y = centerY + numberRadius * sin(angle * PI / 180).toFloat()

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                textSize = 46f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }

            // 1. SİYAH KONTUR (her arka planda görünsün!)
            paint.color = android.graphics.Color.BLACK
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 7f
            drawText(i.toString(), x, y + 14f, paint)

            // 2. RENKLİ RAKAM (ön plan - renkli!)
            paint.color = numberColors[i - 1].toArgb()
            paint.style = android.graphics.Paint.Style.FILL
            paint.strokeWidth = 0f
            drawText(i.toString(), x, y + 14f, paint)
        }
    }

    // AKREP - Koyu + Açık renkli
    val hourAngle = (hour % 12) * 30f - 90f
    val hourHandLength = radius * 0.45f
    rotate(hourAngle, Offset(centerX, centerY)) {

        drawLine(Color.Black, Offset(centerX, centerY), Offset(centerX + hourHandLength, centerY), 14f, StrokeCap.Round)

        drawLine(Color(0xFFFF1744), Offset(centerX, centerY), Offset(centerX + hourHandLength, centerY), 10f, StrokeCap.Round)
    }

    // YELKOVAN - Koyu + Açık renkli
    rotate(-90f, Offset(centerX, centerY)) {
        // Siyah gölge
        drawLine(Color.Black, Offset(centerX, centerY), Offset(centerX + radius * 0.65f, centerY), 10f, StrokeCap.Round)
        // Mavi ibre (daha görünür!)
        drawLine(Color(0xFF2979FF), Offset(centerX, centerY), Offset(centerX + radius * 0.65f, centerY), 6f, StrokeCap.Round)
    }

    // MERKEZ - Sarı kenarlı siyah (daha eğlenceli!)
    drawCircle(Color.Black, 14f, Offset(centerX, centerY))
    drawCircle(Color(0xFFFFEA00), 10f, Offset(centerX, centerY))
}