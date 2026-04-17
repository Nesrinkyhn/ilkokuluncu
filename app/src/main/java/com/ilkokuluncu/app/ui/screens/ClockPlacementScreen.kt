package com.ilkokuluncu.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.data.ClockPlacementEvent
import com.ilkokuluncu.app.data.ClockPlacementState
import com.ilkokuluncu.app.data.PlacementPhase
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// ─── Renkler ─────────────────────────────────────────────────────────────────
private val placementBgColors = listOf(
    listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D).copy(alpha = 0.85f)),
    listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
    listOf(Color(0xFFFC5C7D), Color(0xFF6A82FB)),
    listOf(Color(0xFFFF6B35), Color(0xFFF7C59F).copy(alpha = 0.85f)),
    listOf(Color(0xFF134E5E), Color(0xFF71B280)),
    listOf(Color(0xFF4776E6), Color(0xFF8E54E9)),
    listOf(Color(0xFFDA4453), Color(0xFF89216B))
)

private val clockFaceColors = listOf(
    Color(0xFF6A11CB), Color(0xFF11998E), Color(0xFF8E2DE2), Color(0xFFFC5C7D),
    Color(0xFFE65100), Color(0xFF134E5E), Color(0xFF4776E6), Color(0xFFDA4453)
)

// ─── Ana ekran ────────────────────────────────────────────────────────────────
@Composable
fun ClockPlacementScreen(
    state: ClockPlacementState,
    onEvent: (ClockPlacementEvent) -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ── Oyun bitti ekranı ────────────────────────────────────────────────────
    if (state.showGameOver) {
        ClockPlacementGameOverScreen(
            state       = state,
            onRetry     = { onEvent(ClockPlacementEvent.StartFresh) },
            onBackPress = onBackPress
        )
        return
    }

    val bgColors   = placementBgColors[state.bgIndex % placementBgColors.size]
    val clockColor = clockFaceColors[state.bgIndex % clockFaceColors.size]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgColors))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackPress,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                }
                Text(
                    "⏰ Kadranı Yerleştir",
                    fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                )
                // En iyi skor
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.9f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏆", fontSize = 16.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (state.bestScore > 0) "${state.bestScore}" else "—",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF667eea)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Şeker canları ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    val alive = i < state.lives
                    val scale by animateFloatAsState(
                        targetValue   = if (alive) 1f else 0.75f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label         = "candy$i"
                    )
                    Text(
                        text     = if (alive) "🍬" else "🩶",
                        fontSize = 32.sp,
                        modifier = Modifier
                            .scale(scale)
                            .alpha(if (alive) 1f else 0.4f)
                            .padding(horizontal = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Skor + soru sayısı ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.9f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", fontSize = 20.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${state.score}",
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF667eea)
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.18f)) {
                    Text(
                        "Soru ${state.questionCount}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        fontSize = 15.sp, color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Hayvanlı soru kartı ──────────────────────────────────────────
            val questionAnimals = listOf(
                "🐱","🐶","🦊","🐸","🐼","🐨","🦁","🐯","🐻","🐮",
                "🐷","🦝","🐹","🐰","🐺","🦄","🐧","🦋","🐬","🦜",
                "🐙","🦑","🦈","🐊","🦒","🦓","🦘","🐘","🦩","🐓"
            )
            val animal = questionAnimals[state.questionCount % questionAnimals.size]
            val digitalTime = "${state.targetHour}:${state.targetMinute.toString().padStart(2, '0')}"

            // Hayvan zıplama animasyonu
            val animalInf = rememberInfiniteTransition(label = "animalBounce")
            val animalY by animalInf.animateFloat(
                initialValue  = 0f,
                targetValue   = -10f,
                animationSpec = infiniteRepeatable(
                    tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse
                ),
                label = "ay"
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.20f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Hayvan — soru değişince animasyonla giriş
                    AnimatedContent(
                        targetState = animal,
                        transitionSpec = {
                            (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.4f)) togetherWith
                            (fadeOut(tween(200)) + scaleOut(tween(200)))
                        },
                        label = "animalChange"
                    ) { a ->
                        Text(
                            text     = a,
                            fontSize = 52.sp,
                            modifier = Modifier.offset(y = animalY.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    // Dijital saat
                    Text(
                        text       = digitalTime,
                        fontSize   = 46.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White,
                        textAlign  = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Talimat ──────────────────────────────────────────────────────
            AnimatedContent(
                targetState = state.isHourPhase,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f)) togetherWith
                    (fadeOut(tween(150)))
                },
                label = "instruction"
            ) { isHour ->
                Text(
                    if (isHour) "🕐 Akrebi yerleştir!" else "🕑 Yelkovanı yerleştir!",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── İnteraktif saat kadranı ──────────────────────────────────────
            // rememberUpdatedState → pointerInput(Unit) blokları yeniden başlatılmadan
            // her zaman güncel açıyı okur
            val currentActiveAngle by rememberUpdatedState(state.activeHandAngle)
            val currentIsHourPhase by rememberUpdatedState(state.isHourPhase)
            var isDraggingHand     by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    // Tek dokunuş → aktif eli o açıya snap'le
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val angleDeg = (atan2(offset.y - cy, offset.x - cx) * 180f / Math.PI.toFloat() + 90f + 360f) % 360f
                            onEvent(ClockPlacementEvent.AngleDragged(angleDeg))
                        }
                    }
                    // Sürükleme → yalnızca elin çizgisi üzerinden başlarsa kabul et
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val cx      = size.width  / 2f
                                val cy      = size.height / 2f
                                val radius  = minOf(size.width, size.height) / 2f * 0.88f
                                val handLen = if (currentIsHourPhase) radius * 0.48f else radius * 0.70f
                                val handRad = Math.toRadians((currentActiveAngle - 90.0))
                                val tipX    = cx + (handLen * cos(handRad)).toFloat()
                                val tipY    = cy + (handLen * sin(handRad)).toFloat()

                                // Merkez→uç çizgisine parmak mesafesi
                                val dx    = tipX - cx;  val dy = tipY - cy
                                val lenSq = dx * dx + dy * dy
                                val t     = (((offset.x - cx) * dx + (offset.y - cy) * dy) / lenSq).coerceIn(0f, 1f)
                                val nearX = cx + t * dx;  val nearY = cy + t * dy
                                val distToLine = hypot(offset.x - nearX, offset.y - nearY)

                                // Yarıçapın %22'si kadar tolerans (~24 dp)
                                isDraggingHand = distToLine < radius * 0.22f
                            },
                            onDrag = { change, _ ->
                                if (isDraggingHand) {
                                    change.consume()
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val angleDeg = (atan2(change.position.y - cy, change.position.x - cx) * 180f / Math.PI.toFloat() + 90f + 360f) % 360f
                                    onEvent(ClockPlacementEvent.AngleDragged(angleDeg))
                                }
                            },
                            onDragEnd    = { isDraggingHand = false },
                            onDragCancel = { isDraggingHand = false }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Saat kadranı çizimi
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawPlacementClock(state = state, activeColor = clockColor)
                }

                // Doğru overlay
                val correctAlpha by animateFloatAsState(
                    targetValue   = if (state.phase == PlacementPhase.CORRECT) 1f else 0f,
                    animationSpec = tween(180),
                    label         = "correctAlpha"
                )
                if (correctAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF00C853).copy(alpha = 0.72f * correctAlpha), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✅", fontSize = 64.sp)
                            if (state.lastPoints > 0) {
                                Text(
                                    "+${state.lastPoints}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Süre doldu overlay
                val timeoutAlpha by animateFloatAsState(
                    targetValue   = if (state.phase == PlacementPhase.TIMEOUT) 1f else 0f,
                    animationSpec = tween(150),
                    label         = "timeoutAlpha"
                )
                if (timeoutAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFD50000).copy(alpha = 0.72f * timeoutAlpha), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("⏰", fontSize = 64.sp) }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Zamanlayıcı çubuğu ───────────────────────────────────────────
            val timerFrac   = (state.timeLeft / 10f).coerceIn(0f, 1f)
            val timerColor  = when {
                timerFrac > 0.6f -> Color(0xFF00E676)
                timerFrac > 0.3f -> Color(0xFFFFD600)
                else             -> Color(0xFFFF5252)
            }
            val timerAnim by animateFloatAsState(
                targetValue   = timerFrac,
                animationSpec = tween(90),
                label         = "timerBar"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "%.1f s".format(state.timeLeft.coerceAtLeast(0f)),
                    fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(7.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(timerAnim)
                            .background(timerColor, RoundedCornerShape(7.dp))
                    )
                }
            }
        }

        // ── Yeni Rekor banner ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = state.newRecord,
            enter    = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it },
            exit     = fadeOut(tween(400)) + slideOutVertically(tween(400)) { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
        ) {
            val inf   = rememberInfiniteTransition(label = "rec")
            val scale by inf.animateFloat(
                initialValue  = 0.96f,
                targetValue   = 1.04f,
                animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
                label         = "rs"
            )
            Surface(
                shape         = RoundedCornerShape(20.dp),
                color         = Color(0xFFFFD600),
                shadowElevation = 12.dp,
                modifier      = Modifier.scale(scale)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎉", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Yeni Rekor!  ${state.bestScore}",
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A237E)
                    )
                }
            }
        }
    }
}

// ─── Saat kadranı çizim fonksiyonu ───────────────────────────────────────────
private fun DrawScope.drawPlacementClock(
    state: ClockPlacementState,
    activeColor: Color
) {
    val radius = minOf(size.width, size.height) / 2f * 0.88f
    val center = Offset(size.width / 2f, size.height / 2f)

    // Arka plan
    drawCircle(Color.White, radius, center)
    drawCircle(activeColor.copy(alpha = 0.10f), radius, center)
    drawCircle(activeColor, radius, center, style = androidx.compose.ui.graphics.drawscope.Stroke(6f))

    // Büyük tik işaretleri (saat)
    for (h in 0 until 12) {
        val a   = Math.toRadians((h * 30 - 90).toDouble())
        val out = radius * 0.94f; val inn = radius * 0.78f
        drawLine(
            activeColor,
            Offset(center.x + (inn * cos(a)).toFloat(), center.y + (inn * sin(a)).toFloat()),
            Offset(center.x + (out * cos(a)).toFloat(), center.y + (out * sin(a)).toFloat()),
            6f, StrokeCap.Round
        )
    }

    // Küçük tik işaretleri (dakika)
    for (m in 0 until 60) {
        if (m % 5 == 0) continue
        val a   = Math.toRadians((m * 6 - 90).toDouble())
        val out = radius * 0.94f; val inn = radius * 0.88f
        drawLine(
            activeColor.copy(alpha = 0.25f),
            Offset(center.x + (inn * cos(a)).toFloat(), center.y + (inn * sin(a)).toFloat()),
            Offset(center.x + (out * cos(a)).toFloat(), center.y + (out * sin(a)).toFloat()),
            2f, StrokeCap.Round
        )
    }

    // Sayılar 1–12
    val ring  = radius * 0.62f
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign   = android.graphics.Paint.Align.CENTER
        this.color  = activeColor.toArgb()
        textSize    = radius * 0.22f
        typeface    = android.graphics.Typeface.DEFAULT_BOLD
    }
    drawContext.canvas.nativeCanvas.apply {
        for (h in 1..12) {
            val a  = Math.toRadians((h * 30 - 90).toDouble())
            val tx = center.x + (ring * cos(a)).toFloat()
            val ty = center.y + (ring * sin(a)).toFloat() - (paint.ascent() + paint.descent()) / 2f
            drawText("$h", tx, ty, paint)
        }
    }

    // Diğer el — isHourPhase=true ise yelkovan (soluk), false ise akrep (yeşil ✓)
    val otherRad  = Math.toRadians((state.otherHandAngle - 90.0))
    val doneColor = Color(0xFF00C853)    // doğru yerleştirilmiş el rengi
    if (state.isHourPhase) {
        // Diğer = yelkovan — bekliyor, soluk gri
        drawLine(
            Color.Gray.copy(alpha = 0.28f), center,
            Offset(center.x + (radius * 0.66f * cos(otherRad)).toFloat(), center.y + (radius * 0.66f * sin(otherRad)).toFloat()),
            5f, StrokeCap.Round
        )
    } else {
        // Diğer = akrep — doğru yerleştirildi, yeşil parlıyor
        drawLine(doneColor.copy(alpha = 0.30f), center,
            Offset(center.x + (radius * 0.48f * cos(otherRad)).toFloat(), center.y + (radius * 0.48f * sin(otherRad)).toFloat()),
            22f, StrokeCap.Round)
        drawLine(doneColor, center,
            Offset(center.x + (radius * 0.48f * cos(otherRad)).toFloat(), center.y + (radius * 0.48f * sin(otherRad)).toFloat()),
            12f, StrokeCap.Round)
    }

    // Aktif el (parlak, renkli) – kullanıcı bunu hareket ettirir
    val actRad = Math.toRadians((state.activeHandAngle - 90.0))
    if (state.isHourPhase) {
        // Aktif = akrep (kısa, kalın) – parlama halkası
        drawLine(activeColor.copy(alpha = 0.28f), center,
            Offset(center.x + (radius * 0.48f * cos(actRad)).toFloat(), center.y + (radius * 0.48f * sin(actRad)).toFloat()),
            26f, StrokeCap.Round)
        drawLine(activeColor, center,
            Offset(center.x + (radius * 0.48f * cos(actRad)).toFloat(), center.y + (radius * 0.48f * sin(actRad)).toFloat()),
            13f, StrokeCap.Round)
    } else {
        // Aktif = yelkovan (uzun, ince) – parlama halkası
        drawLine(activeColor.copy(alpha = 0.28f), center,
            Offset(center.x + (radius * 0.70f * cos(actRad)).toFloat(), center.y + (radius * 0.70f * sin(actRad)).toFloat()),
            16f, StrokeCap.Round)
        drawLine(activeColor, center,
            Offset(center.x + (radius * 0.70f * cos(actRad)).toFloat(), center.y + (radius * 0.70f * sin(actRad)).toFloat()),
            7f, StrokeCap.Round)
    }

    // Merkez noktası
    drawCircle(activeColor, 14f, center)
    drawCircle(Color.White, 7f, center)
}

// ─── Oyun bitti ekranı ────────────────────────────────────────────────────────
@Composable
private fun ClockPlacementGameOverScreen(
    state: ClockPlacementState,
    onRetry: () -> Unit,
    onBackPress: () -> Unit
) {
    val isRecord = state.score > 0 && state.score >= state.bestScore

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF4A148C)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Boş şekerler
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(3) {
                    Text("🩶", fontSize = 36.sp, modifier = Modifier.padding(horizontal = 6.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("😢", fontSize = 72.sp)

            Spacer(Modifier.height(12.dp))

            Text(
                "Başaramadın!",
                fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Skor: ${state.score}",
                fontSize = 22.sp, color = Color.White.copy(alpha = 0.9f)
            )

            if (isRecord && state.score > 0) {
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFD600)) {
                    Text(
                        "🏆 Yeni Rekor!",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A237E),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick   = onRetry,
                modifier  = Modifier.fillMaxWidth().height(56.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea))
            ) {
                Text("Yeniden Başla 🔄", fontSize = 18.sp)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onBackPress) {
                Text("Menüye Dön", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
            }
        }
    }
}
