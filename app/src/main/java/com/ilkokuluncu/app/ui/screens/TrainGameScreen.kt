package com.ilkokuluncu.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.data.TrainCharacter
import com.ilkokuluncu.app.data.TrainGameEvent
import com.ilkokuluncu.app.data.TrainGameState
import com.ilkokuluncu.app.data.TrainLevelMode
import com.ilkokuluncu.app.ui.components.CelebrationEffect
import com.ilkokuluncu.app.ui.components.MAX_WAGONS
import com.ilkokuluncu.app.ui.effects.rememberSoundEffectPlayer
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// ─── Level 3 sözlü zaman formatı yardımcıları ────────────────────────────────
private fun hourAccusative(hour: Int): String = when (hour) {
    1 -> "biri"; 2 -> "ikiyi"; 3 -> "üçü"; 4 -> "dördü"; 5 -> "beşi"
    6 -> "altıyı"; 7 -> "yediyi"; 8 -> "sekizi"; 9 -> "dokuzu"; 10 -> "onu"
    11 -> "on biri"; 12 -> "on ikiyi"; else -> "$hour'i"
}
private fun hourDative(hour: Int): String = when (hour) {
    1 -> "bire"; 2 -> "ikiye"; 3 -> "üçe"; 4 -> "dörde"; 5 -> "beşe"
    6 -> "altıya"; 7 -> "yediye"; 8 -> "sekize"; 9 -> "dokuza"; 10 -> "ona"
    11 -> "on bire"; 12 -> "on ikiye"; else -> "$hour'e"
}
/** "H:MM" → sözlü Türkçe (15→"çeyrek geçiyor", 45→"çeyrek var") */
private fun timeToVerbal(timeStr: String): String {
    val (h, m) = timeStr.split(":").map { it.toInt() }
    return when (m) {
        15 -> "${hourAccusative(h)} çeyrek geçiyor"
        45 -> {
            val next = if (h == 12) 1 else h + 1
            "${hourDative(next)} çeyrek var"
        }
        else -> timeStr
    }
}

// ─── AnimatedContent için soru durumu (overlay baked-in) ─────────────────────
private data class QuestionAnimState(
    val hour: Int,
    val minute: Int,
    val wagonCount: Int,
    val trainName: String,
    val showCorrect: Boolean,
    val showWrong: Boolean
)

// ─── Level 2: Vagon kimlik listeleri ──────────────────────────────────────────
private val wagonColors = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
    Color(0xFFFDD835), Color(0xFF8E24AA), Color(0xFFFB8C00),
)
private val wagonEmojis = listOf("🦁", "🐘", "🐸", "🦊", "🐼", "🦋")

// ─── Level 3: Gemi kimlik listeleri ───────────────────────────────────────────
private val shipColors = listOf(
    Color(0xFFE53935), // 0 Kırmızı
    Color(0xFF1E88E5), // 1 Mavi
    Color(0xFF43A047), // 2 Yeşil
    Color(0xFFFF8F00), // 3 Amber
    Color(0xFF8E24AA), // 4 Mor
    Color(0xFF00838F), // 5 Teal
)
// Tüm gemilerde can simidi var – rengi gemi rengiyle eşleşir
private val shipLifeRing = "🛟"
private val shipEmojis = listOf("🚢", "⛴️", "🛥️", "⛵", "🚤", "🛳️")

// ─── Ana ekran ─────────────────────────────────────────────────────────────────
@Composable
fun TrainGameScreen(
    gameState: TrainGameState,
    onEvent: (TrainGameEvent) -> Unit,
    onBackPress: () -> Unit,
    onTestPassed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bgColors = if (gameState.levelMode == TrainLevelMode.QUARTER_HOURS) {
        listOf(Color(0xFF0D47A1), Color(0xFF006064)) // okyanus: lacivert → koyu teal
    } else {
        listOf(Color(gameState.currentTrain.bgColor1), Color(gameState.currentTrain.bgColor2))
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = bgColors))
    ) {
        if (!gameState.showResult) {
            TrainGamePlayScreen(gameState = gameState, onEvent = onEvent, onBackPress = onBackPress)
        }
        if (gameState.showResult) {
            TrainTestResultScreen(gameState = gameState, onEvent = onEvent, onBackPress = onBackPress, onTestPassed = onTestPassed)
        }
        if (gameState.showTestReadyDialog) {
            TrainTestReadyDialog(
                onAccept = { onEvent(TrainGameEvent.AcceptTestChallenge) },
                onDismiss = { onEvent(TrainGameEvent.DismissTestReadyDialog) }
            )
        }
        if (gameState.showCelebration) {
            CelebrationEffect()
        }
    }
}

// ─── Oyun ekranı ───────────────────────────────────────────────────────────────
@Composable
fun TrainGamePlayScreen(
    gameState: TrainGameState,
    onEvent: (TrainGameEvent) -> Unit,
    onBackPress: () -> Unit
) {
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showWrongMark  by remember { mutableStateOf(false) }
    var showCorrectMark by remember { mutableStateOf(false) }
    var locked         by remember { mutableStateOf(false) }

    val ms            = gameState.currentMinute.toString().padStart(2, '0')
    val correctAnswer = "${gameState.currentHour}:$ms"
    val sounds = rememberSoundEffectPlayer()

    // Hangi slot dolacak? → Level 2: vagon rengi/emojisi, Level 3: gemi rengi
    val targetSlotIndex    = gameState.wagonCount % MAX_WAGONS
    val isShipMode         = gameState.levelMode == TrainLevelMode.QUARTER_HOURS
    val questionSlotColor  = if (isShipMode) shipColors[targetSlotIndex] else wagonColors[targetSlotIndex]
    val questionWagonColor = questionSlotColor   // uyumluluk için alias
    val questionWagonEmoji = if (isShipMode) shipLifeRing else wagonEmojis[targetSlotIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackPress,
                modifier = Modifier.background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.9f), shadowElevation = 4.dp) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", fontSize = 22.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${gameState.score}",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF667eea)
                        )
                        if (gameState.isTestMode) {
                            Spacer(Modifier.width(14.dp))
                            Text("Test: ${gameState.testQuestion + 1}/10", fontSize = 15.sp, color = Color.Gray)
                        }
                    }
                }
                if (gameState.isTestMode) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            gameState.testTimeRemaining <= 2 -> Color(0xFFFF5252)
                            gameState.testTimeRemaining <= 3 -> Color(0xFFFFAB40)
                            else -> Color(0xFF00E676)
                        }.copy(alpha = 0.9f),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏱️", fontSize = 22.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("${gameState.testTimeRemaining}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (gameState.isTestMode) {
            // ── Test modu: büyük saat yazısı ──────────────────────────────
            Text(
                text = if (isShipMode) "Hangi gemi doğru saati gösteriyor?"
                       else            "Hangi vagon doğru saati gösteriyor?",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.92f), textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            val minuteStr = gameState.currentMinute.toString().padStart(2, '0')
            val testTimeText = if (isShipMode && gameState.questionStyle == 1)
                timeToVerbal("${gameState.currentHour}:$minuteStr")
            else
                "${gameState.currentHour}:$minuteStr"
            Text(
                text = testTimeText,
                fontSize = if (isShipMode && gameState.questionStyle == 1) 32.sp else 62.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isShipMode) {
                    TestShipRow(
                        gameState       = gameState,
                        onShipSelected  = { hour ->
                            sounds.playTap()
                            onEvent(TrainGameEvent.WagonSelected(hour))
                        },
                        onPlayCorrect   = { sounds.playCorrect() },
                        onPlayWrong     = { sounds.playWrongWithVibration() }
                    )
                } else {
                    TestWagonRow(
                        gameState       = gameState,
                        onWagonSelected = { hour ->
                            sounds.playTap()
                            onEvent(TrainGameEvent.WagonSelected(hour))
                        },
                        onPlayCorrect   = { sounds.playCorrect() },
                        onPlayWrong     = { sounds.playWrongWithVibration() }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        } else {
            // ── Normal mod: soru metni + banner + büyük soru aracı + butonlar ──
            val questionText = if (isShipMode) {
                if (gameState.questionStyle == 1)
                    "Saat kaçı gösteriyor? ⚓"
                else
                    "Geminin saatini oku! ⚓"
            } else gameState.currentTrain.question
            Text(
                text = questionText,
                fontSize = 19.sp, fontWeight = FontWeight.Bold,
                color = Color.White, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            if (isShipMode) AnimatedShipBanner() else AnimatedTrainBanner(trainEmoji = gameState.currentTrain.emoji)
            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = QuestionAnimState(
                        hour        = gameState.currentHour,
                        minute      = gameState.currentMinute,
                        wagonCount  = gameState.wagonCount,
                        trainName   = gameState.currentTrain.name,
                        showCorrect = showCorrectMark,
                        showWrong   = showWrongMark
                    ),
                    transitionSpec = {
                        val questionChanged = initialState.hour != targetState.hour ||
                                             initialState.minute != targetState.minute ||
                                             initialState.wagonCount != targetState.wagonCount
                        val comingFromCelebration = initialState.showCorrect
                        if (questionChanged && !comingFromCelebration) {
                            (slideInVertically { -it } + fadeIn(tween(280))) togetherWith
                            (slideOutVertically { (it * 1.2f).toInt() } + scaleOut(targetScale = 0.35f) + fadeOut(tween(350)))
                        } else {
                            EnterTransition.None togetherWith ExitTransition.None
                        }
                    },
                    label = "questionWagon"
                ) { state ->
                    if (isShipMode) {
                        QuestionShip(
                            hour            = state.hour,
                            minute          = state.minute,
                            shipColor       = questionSlotColor,
                            shipEmoji       = shipEmojis[targetSlotIndex % shipEmojis.size],
                            showCorrectMark = state.showCorrect,
                            showWrongMark   = state.showWrong
                        )
                    } else {
                        QuestionWagon(
                            hour            = state.hour,
                            minute          = state.minute,
                            wagonColor      = questionWagonColor,
                            wagonEmoji      = questionWagonEmoji,
                            showCorrectMark = state.showCorrect,
                            showWrongMark   = state.showWrong
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            val labels = listOf("A)", "B)", "C)")
            if (isShipMode) {
                // ── Ship modu: dikey A/B/C ────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gameState.options.forEachIndexed { idx, option ->
                        val isSelected    = selectedAnswer == option
                        val isCorrect     = option == correctAnswer
                        val showAsCorrect = gameState.showCorrectAnswerAfterWrong && option == gameState.correctAnswerToShow
                        val visualState = when {
                            selectedAnswer == null && !gameState.showCorrectAnswerAfterWrong -> AnswerVisualState.Normal
                            showAsCorrect            -> AnswerVisualState.Correct
                            isCorrect                -> AnswerVisualState.Correct
                            isSelected && !isCorrect -> AnswerVisualState.Wrong
                            else                     -> AnswerVisualState.Normal
                        }
                        val displayText = if (gameState.questionStyle == 1) timeToVerbal(option) else option
                        AnswerButton(
                            text    = "${labels[idx]} $displayText",
                            state   = visualState,
                            enabled = !locked && !gameState.showCorrectAnswerAfterWrong,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (locked || gameState.showCorrectAnswerAfterWrong) return@AnswerButton
                            locked = true
                            selectedAnswer = option
                            sounds.playTap()
                            if (option == correctAnswer) { showCorrectMark = true; sounds.playCorrect() }
                            else { showWrongMark = true; sounds.playWrongWithVibration() }
                        }
                    }
                }
            } else {
                // ── Tren modu: yatay ─────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gameState.options.forEach { option ->
                        val isSelected    = selectedAnswer == option
                        val isCorrect     = option == correctAnswer
                        val showAsCorrect = gameState.showCorrectAnswerAfterWrong && option == gameState.correctAnswerToShow
                        val visualState = when {
                            selectedAnswer == null && !gameState.showCorrectAnswerAfterWrong -> AnswerVisualState.Normal
                            showAsCorrect            -> AnswerVisualState.Correct
                            isCorrect                -> AnswerVisualState.Correct
                            isSelected && !isCorrect -> AnswerVisualState.Wrong
                            else                     -> AnswerVisualState.Normal
                        }
                        AnswerButton(
                            text = option, state = visualState,
                            enabled = !locked && !gameState.showCorrectAnswerAfterWrong,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (locked || gameState.showCorrectAnswerAfterWrong) return@AnswerButton
                            locked = true
                            selectedAnswer = option
                            sounds.playTap()
                            if (option == correctAnswer) { showCorrectMark = true; sounds.playCorrect() }
                            else { showWrongMark = true; sounds.playWrongWithVibration() }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── Alt ilerleme satırı ────────────────────────────────────────────
        if (isShipMode) {
            ShipProgressRow(
                wagonCount      = gameState.wagonCount,
                targetSlotIndex = targetSlotIndex
            )
        } else {
            TrainProgressRow(
                trainEmoji      = gameState.currentTrain.emoji,
                wagonCount      = gameState.wagonCount,
                targetSlotIndex = targetSlotIndex
            )
        }

        Spacer(Modifier.height(6.dp))
    }

    // Yeni soru geldiğinde overlay'i kaldır — böylece eski saat görünmez
    LaunchedEffect(gameState.currentHour, gameState.currentMinute) {
        showCorrectMark = false
        showWrongMark   = false
        selectedAnswer  = null
        locked          = false
    }

    // Feedback süresi dolunca event'i gönder, sıfırlamayı yukarıdaki effect yapar
    LaunchedEffect(showWrongMark, showCorrectMark) {
        if (showWrongMark || showCorrectMark) {
            delay(900)
            selectedAnswer?.let { onEvent(TrainGameEvent.AnswerSelected(it)) }
            selectedAnswer = null   // çift gönderimi önle; locked ve mark'lar yeni soru bekler
        }
    }
}

// ─── Animasyonlu gemi banner (Level 3) ───────────────────────────────────────
@Composable
fun AnimatedShipBanner() {
    val inf = rememberInfiniteTransition(label = "shipBanner")
    // Hafif sallanma (deniz dalgası gibi)
    val rockDeg by inf.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rock"
    )
    val bobY by inf.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    // Dalga emojileri
    val wave1 by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val wave2 by inf.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "w2"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Text("🌊", fontSize = 20.sp, modifier = Modifier.alpha(0.4f + wave1 * 0.6f))
        Spacer(Modifier.width(4.dp))
        Text(
            text = "🚢",
            fontSize = 44.sp,
            modifier = Modifier
                .offset(y = bobY.dp)
                .rotate(rockDeg)
        )
        Spacer(Modifier.width(4.dp))
        Text("🌊", fontSize = 20.sp, modifier = Modifier.alpha(0.4f + wave2 * 0.6f))
    }
}

// ─── Büyük soru gemisi (Level 3 normal mod) ───────────────────────────────────
@Composable
fun QuestionShip(
    hour: Int,
    minute: Int,
    shipColor: Color,
    shipEmoji: String = "🚢",
    showCorrectMark: Boolean = false,
    showWrongMark: Boolean   = false
) {
    val inf = rememberInfiniteTransition(label = "shipQ")
    val rockDeg by inf.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "qRock"
    )
    val bobY by inf.animateFloat(
        initialValue = 0f, targetValue = -7f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "qBob"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Gemi Emoji + can simidi rozeti
        Box(
            modifier = Modifier
                .offset(y = bobY.dp)
                .rotate(rockDeg),
            contentAlignment = Alignment.TopEnd
        ) {
            Text(shipEmoji, fontSize = 80.sp)
            Text(
                text = shipLifeRing,
                fontSize = 26.sp,
                modifier = Modifier.offset(x = 4.dp, y = (-4).dp)
            )
        }

        // Saat kadranı dairesi
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(4.dp, shipColor, CircleShape)
        ) {
            SmallClockFace(hour = hour, minute = minute, trainColor = shipColor, modifier = Modifier.fillMaxSize())
            if (showCorrectMark || showWrongMark) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (showCorrectMark) Color(0xFF00C853).copy(alpha = 0.82f)
                            else Color(0xFFD50000).copy(alpha = 0.82f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) { Text(if (showCorrectMark) "✅" else "❌", fontSize = 50.sp) }
            }
        }
    }
}

// ─── Alt gemi ilerleme satırı (Level 3) ──────────────────────────────────────
@Composable
fun ShipProgressRow(wagonCount: Int, targetSlotIndex: Int) {
    val filledCount = wagonCount % (MAX_WAGONS + 1)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (filledCount == 0 && wagonCount > 0) "🎉 Filo tamam! Devam et!"
                   else "🚢 $filledCount / $MAX_WAGONS gemi",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("⚓", fontSize = 30.sp)
                for (index in 0 until MAX_WAGONS) {
                    MiniShipSlot(
                        isFilled        = index < filledCount,
                        isTarget        = !( index < filledCount) && index == targetSlotIndex,
                        color           = shipColors[index],
                        emoji           = shipEmojis[index % shipEmojis.size],
                        modifier        = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ─── Mini gemi slotu ─────────────────────────────────────────────────────────
@Composable
fun MiniShipSlot(isFilled: Boolean, isTarget: Boolean, color: Color, emoji: String = "🚢", modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue   = if (isFilled) 1f else if (isTarget) 1f else 0.75f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label         = "ss"
    )
    val alpha by animateFloatAsState(
        targetValue   = if (isFilled) 1f else if (isTarget) 0.6f else 0.25f,
        animationSpec = tween(200), label = "sa"
    )
    val inf = rememberInfiniteTransition(label = "sp")
    val pulse by inf.animateFloat(
        initialValue  = 1f,
        targetValue   = if (isTarget) 1.15f else 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label         = "pulse"
    )

    Box(
        modifier = modifier
            .scale(scale * pulse)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                isFilled || isTarget -> emoji
                else                 -> "⭐"
            },
            fontSize = 22.sp
        )
    }
}

// ─── Test modu: 3 gemi seçeneği (Level 3) ────────────────────────────────────
@Composable
fun TestShipRow(
    gameState: TrainGameState,
    onShipSelected: (Int) -> Unit,
    onPlayCorrect: () -> Unit,
    onPlayWrong: () -> Unit
) {
    var selectedHour by remember { mutableStateOf<Int?>(null) }
    var locked       by remember { mutableStateOf(false) }

    LaunchedEffect(gameState.testQuestion, gameState.testCorrectHour) {
        selectedHour = null
        locked       = false
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        gameState.testWagonOptions.forEachIndexed { idx, hour ->
            val isSelected    = selectedHour == hour
            val isCorrect     = hour == gameState.testCorrectHour
            val showAsCorrect = gameState.testCorrectWagonShown == hour ||
                                (isSelected && isCorrect && locked)
            val showAsWrong   = isSelected && !isCorrect && locked

            SelectableTestShip(
                hour        = hour,
                minute      = gameState.currentMinute,
                shipColor   = shipColors[idx % shipColors.size],
                shipEmoji   = shipEmojis[idx % shipEmojis.size],
                showCorrect = showAsCorrect,
                showWrong   = showAsWrong,
                enabled     = !locked && !gameState.showCorrectAnswerAfterWrong,
                modifier    = Modifier.weight(1f)
            ) {
                if (locked || gameState.showCorrectAnswerAfterWrong) return@SelectableTestShip
                locked = true
                selectedHour = hour
                if (isCorrect) onPlayCorrect() else onPlayWrong()
                onShipSelected(hour)
            }
        }
    }
}

// ─── Tek seçilebilir test gemisi ─────────────────────────────────────────────
@Composable
fun SelectableTestShip(
    hour: Int,
    minute: Int,
    shipColor: Color,
    shipEmoji: String = "🚢",
    showCorrect: Boolean,
    showWrong: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "ts$hour")
    val rock by inf.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tsr"
    )
    val bob by inf.animateFloat(
        initialValue = 0f, targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tsb"
    )
    val borderColor = when {
        showCorrect -> Color(0xFF00C853)
        showWrong   -> Color(0xFFD50000)
        else        -> Color.White.copy(alpha = 0.35f)
    }
    val borderWidth = if (showCorrect || showWrong) 3.5.dp else 2.dp

    Column(
        modifier = modifier.clickable(enabled = enabled) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Gemi emoji (rocking + bobbing)
        Text(
            text = shipEmoji,
            fontSize = 36.sp,
            modifier = Modifier
                .offset(y = bob.dp)
                .rotate(rock)
        )

        // Saat kadranı dairesi
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(borderWidth, borderColor, CircleShape)
        ) {
            SmallClockFace(hour = hour, minute = minute, trainColor = shipColor, modifier = Modifier.fillMaxSize())
            if (showCorrect || showWrong) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (showCorrect) Color(0xFF00C853).copy(alpha = 0.82f)
                            else Color(0xFFD50000).copy(alpha = 0.82f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) { Text(if (showCorrect) "✅" else "❌", fontSize = 28.sp) }
            }
        }

        // Can simidi
        Text(shipLifeRing, fontSize = 16.sp)
    }
}

// ─── Animasyonlu tren banner ──────────────────────────────────────────────────
@Composable
fun AnimatedTrainBanner(trainEmoji: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "trainBanner")

    // Tren yatay ileri-geri
    val trainX by infiniteTransition.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "trainX"
    )
    // Hafif dikey sıçrama
    val trainY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "trainY"
    )

    // Buhar 1 – büyük
    val steam1Alpha by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "s1a"
    )
    val steam1X by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart),
        label = "s1x"
    )
    // Buhar 2 – orta (faz kaydırmalı)
    val steam2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "s2a"
    )
    val steam2X by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "s2x"
    )
    // Buhar 3 – küçük
    val steam3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "s3a"
    )
    val steam3X by infiniteTransition.animateFloat(
        initialValue = 18f, targetValue = 40f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Restart),
        label = "s3x"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        // Tren emojisi – zıplıyor ve yatay sallanıyor
        Text(
            text = trainEmoji,
            fontSize = 42.sp,
            modifier = Modifier.offset(x = trainX.dp, y = trainY.dp)
        )

        Spacer(Modifier.width(2.dp))

        // Buhar bulutları – üst üste offset ile
        Box(modifier = Modifier.width(60.dp).height(40.dp)) {
            Text(
                text = "💨",
                fontSize = 22.sp,
                modifier = Modifier
                    .offset(x = steam1X.dp, y = (-4).dp)
                    .alpha(steam1Alpha)
            )
            Text(
                text = "💨",
                fontSize = 18.sp,
                modifier = Modifier
                    .offset(x = steam2X.dp, y = 6.dp)
                    .alpha(steam2Alpha)
            )
            Text(
                text = "💨",
                fontSize = 14.sp,
                modifier = Modifier
                    .offset(x = steam3X.dp, y = 0.dp)
                    .alpha(steam3Alpha)
            )
        }
    }
}

// ─── Büyük soru vagonu ─────────────────────────────────────────────────────────
@Composable
fun QuestionWagon(
    hour: Int,
    minute: Int = 30,
    wagonColor: Color,
    wagonEmoji: String,
    showWrongMark: Boolean   = false,
    showCorrectMark: Boolean = false
) {
    // Tren sallama animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "trainShake")
    val shake by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue  = 3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(x = shake.dp)   // tüm vagon sallanır
    ) {

        // ── Hayvan rozeti ──────────────────────────────────────────────────
        Box(contentAlignment = Alignment.Center) {
            // Dış renkli halka
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(wagonColor, CircleShape)
                    .border(3.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // İç beyaz daire
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = wagonEmoji, fontSize = 34.sp)
                }
            }
            // Durum etiketi (💨 veya 😢) sağ alt köşede
            Text(
                text = if (showWrongMark) "😢" else "💨",
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        // Vagon gövdesi
        Box(
            modifier = Modifier
                .width(270.dp)
                .height(185.dp)
                .background(wagonColor, RoundedCornerShape(22.dp))
        ) {
            // Üst dekoratif şerit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(
                        Color.White.copy(alpha = 0.22f),
                        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                    )
                    .align(Alignment.TopCenter)
            )

            // Saat penceresi – merkez
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                SmallClockFace(
                    hour = hour, minute = minute,
                    trainColor = wagonColor,
                    modifier = Modifier.fillMaxSize()
                )
                // Doğru / yanlış overlay
                if (showCorrectMark || showWrongMark) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (showCorrectMark) Color(0xFF00C853).copy(alpha = 0.78f)
                                else Color(0xFFD50000).copy(alpha = 0.78f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (showCorrectMark) "✅" else "❌", fontSize = 42.sp)
                    }
                }
            }
        }

        // Alt kiriş
        Box(
            modifier = Modifier
                .width(252.dp)
                .height(8.dp)
                .background(wagonColor.copy(alpha = 0.55f))
        )

        // Tekerlekler
        Row(
            modifier = Modifier.width(242.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WheelPair(wagonColor)
            WheelPair(wagonColor)
        }
    }
}

// ─── Tekerlek çifti ────────────────────────────────────────────────────────────
@Composable
fun WheelPair(hubColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(Color(0xFF1A1A1A), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.Center)
                        .background(hubColor, CircleShape)
                )
            }
        }
    }
}

// ─── Saat kadranı (vagon penceresi için) ──────────────────────────────────────
@Composable
fun SmallClockFace(
    hour: Int,
    minute: Int,
    trainColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Kadran biraz iç boşluk bıraksın
        val radius = minOf(size.width, size.height) / 2.3f
        val center = Offset(size.width / 2, size.height / 2)

        // Saat yüzü arka plan
        drawCircle(color = Color.White, radius = radius, center = center)
        drawCircle(color = trainColor.copy(alpha = 0.3f), radius = radius, center = center, style = Stroke(5f))

        // Tik işaretleri (dakika + saat)
        for (m in 0 until 60) {
            val angle  = Math.toRadians((m * 6 - 90).toDouble())
            val isHour = m % 5 == 0
            // Sayılar için yer bırak: tik çizgileri dışta kalsın
            val outer = radius * 0.97f
            val inner = if (isHour) radius * 0.86f else radius * 0.92f
            drawLine(
                color = if (isHour) trainColor.copy(alpha = 0.45f) else Color(0xFFCCCCCC),
                start = Offset(center.x + (inner * cos(angle)).toFloat(), center.y + (inner * sin(angle)).toFloat()),
                end   = Offset(center.x + (outer * cos(angle)).toFloat(), center.y + (outer * sin(angle)).toFloat()),
                strokeWidth = if (isHour) 3.5f else 1.5f,
                cap = StrokeCap.Round
            )
        }

        // Renkli sayılar (1–12) — sayılar tik çizgilerinin içinde
        // numRing: sayıların yerleştirileceği yarıçap oranı (0.72f → tiklerin iç tarafı)
        val numRing = radius * 0.72f
        val numPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign   = android.graphics.Paint.Align.CENTER
            color       = trainColor.toArgb()
            textSize    = radius * 0.28f          // küçük sayılar (2,4,5,7,8,10,11)
            typeface    = android.graphics.Typeface.DEFAULT_BOLD
        }
        val quarterPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign   = android.graphics.Paint.Align.CENTER
            color       = trainColor.toArgb()
            textSize    = radius * 0.33f          // büyük sayılar (3,6,9,12)
            typeface    = android.graphics.Typeface.DEFAULT_BOLD
        }
        drawContext.canvas.nativeCanvas.apply {
            for (h in 1..12) {
                val angle = Math.toRadians((h * 30 - 90).toDouble())
                val paint = if (h % 3 == 0) quarterPaint else numPaint
                val tx = center.x + (numRing * cos(angle)).toFloat()
                val ty = center.y + (numRing * sin(angle)).toFloat() - (paint.ascent() + paint.descent()) / 2f
                drawText(h.toString(), tx, ty, paint)
            }
        }

        // Akrep (saat kolu) – yarım saatte araya girer
        val hourDeg = (hour * 30f + minute * 0.5f) - 90f
        rotate(degrees = hourDeg, pivot = center) {
            drawLine(Color(0xFF1A1A1A), center, Offset(center.x + radius * 0.44f, center.y), 9f, StrokeCap.Round)
        }
        // Yelkovan (dakika kolu)
        rotate(degrees = minute * 6f - 90f, pivot = center) {
            drawLine(Color(0xFF333333), center, Offset(center.x + radius * 0.62f, center.y), 6f, StrokeCap.Round)
        }

        // Merkez pin
        drawCircle(trainColor, radius = 9f, center = center)
        drawCircle(Color.White, radius = 4f, center = center)
    }
}

// ─── Alt tren satırı ──────────────────────────────────────────────────────────
@Composable
fun TrainProgressRow(
    trainEmoji: String,
    wagonCount: Int,
    targetSlotIndex: Int
) {
    val filledCount = wagonCount % (MAX_WAGONS + 1)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (filledCount == 0 && wagonCount > 0) "🎉 Tren tamam! Devam et!"
                   else "🚃 $filledCount / $MAX_WAGONS vagon",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = trainEmoji, fontSize = 30.sp)

                for (index in 0 until MAX_WAGONS) {
                    val isFilled   = index < filledCount
                    val isTarget   = !isFilled && index == targetSlotIndex
                    MiniWagonSlot(
                        isFilled   = isFilled,
                        isTarget   = isTarget,
                        color      = wagonColors[index],
                        emoji      = wagonEmojis[index],
                        modifier   = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ─── Mini vagon slotu ─────────────────────────────────────────────────────────
@Composable
fun MiniWagonSlot(
    isFilled: Boolean,
    isTarget: Boolean,
    color: Color,
    emoji: String,
    modifier: Modifier = Modifier
) {
    // Dolu vagonlar için giriş animasyonu
    val scale by animateFloatAsState(
        targetValue  = if (isFilled) 1f else if (isTarget) 1f else 0.82f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "slotScale"
    )
    val alpha by animateFloatAsState(
        targetValue  = if (isFilled) 1f else if (isTarget) 0.55f else 0.2f,
        animationSpec = tween(200),
        label = "slotAlpha"
    )

    // Hedef slot pulsing (sıradaki dolacak slot)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isTarget) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(480), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .scale(scale * pulseScale)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vagon gövdesi – dolu ve hedef için renk, boşlar için gri
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    if (isFilled || isTarget) color
                    else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Hayvan emoji penceresi (dolu veya hedef slot)
            if (isFilled || isTarget) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 12.sp)
                }
            } else {
                Text(text = "⭐", fontSize = 13.sp)
            }
        }

        // Alt kiriş
        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(4.dp)
                .background(
                    if (isFilled || isTarget) color.copy(alpha = 0.55f)
                    else Color.White.copy(alpha = 0.06f)
                )
        )

        // Tekerlekler
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (isFilled) Color(0xFF2A2A2A)
                            else if (isTarget) color.copy(alpha = 0.6f)
                            else Color.White.copy(alpha = 0.12f),
                            CircleShape
                        )
                )
            }
        }
    }
}

// ─── Test modu: 3 seçenekli vagon satırı ─────────────────────────────────────
@Composable
fun TestWagonRow(
    gameState: TrainGameState,
    onWagonSelected: (Int) -> Unit,
    onPlayCorrect: () -> Unit,
    onPlayWrong: () -> Unit
) {
    var selectedHour by remember { mutableStateOf<Int?>(null) }
    var locked       by remember { mutableStateOf(false) }

    // Yeni soru gelince sıfırla
    LaunchedEffect(gameState.testQuestion, gameState.testCorrectHour) {
        selectedHour = null
        locked       = false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        gameState.testWagonOptions.forEachIndexed { idx, hour ->
            val isSelected     = selectedHour == hour
            val isCorrect      = hour == gameState.testCorrectHour
            val showAsCorrect  = gameState.testCorrectWagonShown == hour ||
                                 (isSelected && isCorrect && locked)
            val showAsWrong    = isSelected && !isCorrect && locked

            SelectableTestWagon(
                hour          = hour,
                minute        = gameState.currentMinute,
                wagonColor    = wagonColors[idx % wagonColors.size],
                wagonEmoji    = wagonEmojis[idx % wagonEmojis.size],
                showCorrect   = showAsCorrect,
                showWrong     = showAsWrong,
                enabled       = !locked && !gameState.showCorrectAnswerAfterWrong,
                modifier      = Modifier.weight(1f)
            ) {
                if (locked || gameState.showCorrectAnswerAfterWrong) return@SelectableTestWagon
                locked = true
                selectedHour = hour
                if (isCorrect) onPlayCorrect() else onPlayWrong()
                onWagonSelected(hour)
            }
        }
    }
}

// ─── Tek seçilebilir test vagonu ──────────────────────────────────────────────
@Composable
fun SelectableTestWagon(
    hour: Int,
    minute: Int,
    wagonColor: Color,
    wagonEmoji: String,
    showCorrect: Boolean,
    showWrong: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tw$hour")
    val shake by infiniteTransition.animateFloat(
        initialValue = -2f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
        label = "tws"
    )

    val borderColor = when {
        showCorrect -> Color(0xFF00C853)
        showWrong   -> Color(0xFFD50000)
        else        -> Color.Transparent
    }

    Column(
        modifier = modifier
            .offset(x = shake.dp)
            .clickable(enabled = enabled) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hayvan rozeti
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(wagonColor, CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = wagonEmoji, fontSize = 20.sp)
            }
        }

        Spacer(Modifier.height(3.dp))

        // Vagon gövdesi + saat kadranı
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.88f)
                .border(3.5.dp, borderColor, RoundedCornerShape(14.dp))
                .background(wagonColor, RoundedCornerShape(14.dp))
        ) {
            // Üst şerit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                    )
                    .align(Alignment.TopCenter)
            )
            // Saat
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .aspectRatio(1f)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                SmallClockFace(
                    hour = hour, minute = minute,
                    trainColor = wagonColor,
                    modifier = Modifier.fillMaxSize()
                )
                // Doğru / yanlış overlay
                if (showCorrect || showWrong) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (showCorrect) Color(0xFF00C853).copy(alpha = 0.80f)
                                else Color(0xFFD50000).copy(alpha = 0.80f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (showCorrect) "✅" else "❌", fontSize = 26.sp)
                    }
                }
            }
        }

        // Alt kiriş
        Box(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .height(5.dp)
                .background(wagonColor.copy(alpha = 0.55f))
        )

        // Tekerlekler
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF1A1A1A), CircleShape)
                )
            }
        }
    }
}

// ─── Test dialogları ──────────────────────────────────────────────────────────
@Composable
fun TrainTestReadyDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🎉 Harika!", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("300 puana ulaştın! 🚂✨", fontSize = 20.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text("Teste hazır mısın?", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, color = Color(0xFF667eea))
                Spacer(Modifier.height(8.dp))
                Text("10 soruda 7+ doğru yapman gerekiyor",
                    fontSize = 14.sp, textAlign = TextAlign.Center, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00b894))) {
                Text("EVET, BAŞLA! 🚀", fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Daha Sonra", fontSize = 16.sp) }
        }
    )
}

@Composable
fun TrainTestResultScreen(
    gameState: TrainGameState,
    onEvent: (TrainGameEvent) -> Unit,
    onBackPress: () -> Unit,
    onTestPassed: () -> Unit = {}
) {
    val passed    = gameState.testPassed
    val isShip    = gameState.levelMode == TrainLevelMode.QUARTER_HOURS

    val titleText = if (passed) {
        if (isShip) "🌊 MUHTEŞEM! 🌊" else "🎉 HARIKA! 🎉"
    } else {
        if (isShip) "⚓ Tekrar Dene" else "😢 Tekrar Dene"
    }

    val unlockText = when {
        passed && !isShip -> "🔓 Level 3 kilidi açıldı!"
        passed &&  isShip -> "⭐ Çeyrek saatleri tamamladın!"
        else              -> null
    }

    val continueText = if (isShip) "Tebrikler! Menüye Dön 🚢" else "Tebrikler! Seviye Listesine Dön 🎯"

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            titleText,
            fontSize = 36.sp, fontWeight = FontWeight.Bold,
            color = Color.White, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text("${gameState.testCorrect}/10 doğru", fontSize = 24.sp, color = Color.White.copy(alpha = 0.9f))

        if (unlockText != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                unlockText,
                fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700), textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(48.dp))

        if (passed) {
            Button(
                onClick = onTestPassed,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isShip) Color(0xFF0D47A1) else Color(0xFF00b894)
                )
            ) {
                Text(continueText, fontSize = 18.sp)
            }
        } else {
            Button(
                onClick = { onEvent(TrainGameEvent.RetryTest) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea))
            ) {
                Text("Tekrar Dene 🔄", fontSize = 20.sp)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBackPress) {
                Text("Menüye Dön", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
            }
        }
    }
}
