package com.ilkokuluncu.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.ilkokuluncu.app.ui.components.RedBackButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.data.ClockGameEvent
import com.ilkokuluncu.app.data.ClockGameState
import com.ilkokuluncu.app.ui.components.CelebrationEffect
import com.ilkokuluncu.app.ui.components.ClockLevel1FramesPlayer
import com.ilkokuluncu.app.ui.components.ClockView
import com.ilkokuluncu.app.ui.components.SparkleBurst
import com.ilkokuluncu.app.ui.effects.rememberSoundEffectPlayer
import kotlinx.coroutines.delay

@Composable
fun ClockGameScreen(
    gameState: ClockGameState,
    onEvent: (ClockGameEvent) -> Unit,
    onBackPress: () -> Unit,
    onNavigateToLevel2: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColors = listOf(
        Color(gameState.currentAnimal.bgColor1),
        Color(gameState.currentAnimal.bgColor2)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = bgColors))
    ) {
        if (gameState.showVictoryAnimation) {
            ClockLevel1FramesPlayer(
                onFinish = {
                    onNavigateToLevel2()
                }
            )
        }

        if (!gameState.showResult && !gameState.showVictoryAnimation) {
            GamePlayScreen(
                gameState = gameState,
                onEvent = onEvent,
                onBackPress = onBackPress
            )
        }

        if (gameState.showResult && !gameState.showVictoryAnimation) {
            TestResultScreen(gameState, onEvent, onNavigateToLevel2)
        }

        if (gameState.showTestReadyDialog) {
            TestReadyDialog(
                onAccept = { onEvent(ClockGameEvent.AcceptTestChallenge) },
                onDismiss = { onEvent(ClockGameEvent.DismissTestReadyDialog) }
            )
        }

        if (gameState.showCelebration && !gameState.showVictoryAnimation) {
            CelebrationEffect()
        }
    }
}

enum class AnswerVisualState {
    Normal,
    Correct,
    Wrong
}

@Composable
fun GamePlayScreen(
    gameState: ClockGameState,
    onEvent: (ClockGameEvent) -> Unit,
    onBackPress: () -> Unit
) {
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var showWrongMark by remember { mutableStateOf(false) }
    var showCorrectMark by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var triggerSparkle by remember { mutableStateOf(0) }

    val correctAnswer = gameState.currentHour
    val sounds = rememberSoundEffectPlayer()

    val markScale = remember { Animatable(0.4f) }
    val markAlpha = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        val availH = maxHeight
        // Ekran yüksekliğine göre orantılı boyutlar
        val clockH   = (availH * 0.36f).coerceIn(160.dp, 280.dp)
        val btnH     = (availH * 0.08f).coerceIn(46.dp,  62.dp)
        val spTop    = (availH * 0.05f).coerceIn(6.dp,   48.dp)
        val spMid    = (availH * 0.04f).coerceIn(6.dp,   40.dp)
        val spBot    = (availH * 0.03f).coerceIn(4.dp,   32.dp)
        val qFont    = if (availH < 580.dp) 17.sp else 22.sp
        val btnFont  = if (availH < 580.dp) 15.sp else 18.sp

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RedBackButton(
                        onClick = onBackPress,
                        modifier = Modifier,
                        shape = RoundedCornerShape(12.dp),
                        size = 48
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⭐", fontSize = 24.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${gameState.score}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF667eea)
                                )
                                if (gameState.isTestMode) {
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = "Test: ${gameState.testQuestion + 1}/10",
                                        fontSize = 16.sp,
                                        color = Color.Gray
                                    )
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
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⏱️", fontSize = 24.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${gameState.testTimeRemaining}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                    }
                }

                Spacer(Modifier.height(spTop))

                Text(
                    text = gameState.currentAnimal.question,
                    fontSize = qFont,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(spMid))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(clockH),
                    contentAlignment = Alignment.Center
                ) {
                    ClockView(
                        hour = gameState.currentHour,
                        animalEmoji = gameState.currentAnimal.emoji,
                        animal = gameState.currentAnimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SparkleBurst(
                        trigger = triggerSparkle,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Spacer(Modifier.height(spMid))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    gameState.options.forEach { option ->
                        val isSelected = selectedAnswer == option
                        val isCorrect = option == correctAnswer
                        val showAsCorrect = gameState.showCorrectAnswerAfterWrong &&
                                option == gameState.correctAnswerToShow
                        val visualState = when {
                            selectedAnswer == null && !gameState.showCorrectAnswerAfterWrong -> AnswerVisualState.Normal
                            showAsCorrect -> AnswerVisualState.Correct
                            isCorrect -> AnswerVisualState.Correct
                            isSelected && !isCorrect -> AnswerVisualState.Wrong
                            else -> AnswerVisualState.Normal
                        }
                        AnswerButton(
                            text = "$option:00",
                            state = visualState,
                            enabled = !locked && !gameState.showCorrectAnswerAfterWrong,
                            buttonHeight = btnH,
                            fontSize = btnFont,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        ) {
                            if (locked || gameState.showCorrectAnswerAfterWrong) return@AnswerButton
                            locked = true
                            selectedAnswer = option
                            sounds.playTap()
                            if (option == correctAnswer) {
                                showCorrectMark = true
                                triggerSparkle++
                                sounds.playCorrect()
                            } else {
                                showWrongMark = true
                                sounds.playWrongWithVibration()
                            }
                        }
                    }
                }

                Spacer(Modifier.height(spBot))
            }

            val feedbackText = when {
                showWrongMark -> "❌"
                showCorrectMark -> "✅"
                else -> null
            }

            if (feedbackText != null) {
                Text(
                    text = feedbackText,
                    fontSize = 88.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            scaleX = markScale.value
                            scaleY = markScale.value
                            alpha = markAlpha.value
                        }
                )
            }

            if (showCorrectMark) {
                CorrectGlow(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    LaunchedEffect(showWrongMark, showCorrectMark) {
        if (showWrongMark || showCorrectMark) {
            markScale.snapTo(0.4f)
            markAlpha.snapTo(0f)
            markScale.animateTo(
                targetValue = 1.15f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
            )
            markAlpha.animateTo(1f)
            delay(420)
            markAlpha.animateTo(0f)
        }
    }

    LaunchedEffect(showWrongMark, showCorrectMark) {
        if (showWrongMark || showCorrectMark) {
            delay(900)

            selectedAnswer?.let {
                onEvent(ClockGameEvent.AnswerSelected(it))
            }

            selectedAnswer = null
            showWrongMark = false
            showCorrectMark = false
            locked = false
        }
    }
}

@Composable
fun CorrectGlow(modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = 1.08f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "glowScale"
    )

    Box(
        modifier = modifier
            .size(220.dp)
            .scale(scale)
            .alpha(0.28f)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF59D),
                        Color(0xFFFFD54F),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
    )
}

@Composable
fun AnswerButton(
    text: String,
    state: AnswerVisualState = AnswerVisualState.Normal,
    enabled: Boolean = true,
    buttonHeight: androidx.compose.ui.unit.Dp = 60.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val containerColor = when (state) {
        AnswerVisualState.Normal -> Color.White.copy(alpha = 0.92f)
        AnswerVisualState.Correct -> Color(0xFF00C853)
        AnswerVisualState.Wrong -> Color(0xFFD50000)
    }

    val contentColor = when (state) {
        AnswerVisualState.Normal -> Color(0xFF667eea)
        AnswerVisualState.Correct -> Color.White
        AnswerVisualState.Wrong -> Color.White
    }

    Button(
        onClick = {
            isPressed = true
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .height(buttonHeight)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor
        )
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

@Composable
fun TestReadyDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "🎉 Harika!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("300 puana ulaştın!", fontSize = 20.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Teste hazır mısın?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF667eea)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "10 soruda 7+ doğru yapman gerekiyor",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00b894))
            ) {
                Text("EVET, BAŞLA! 🚀", fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Daha Sonra", fontSize = 16.sp)
            }
        }
    )
}

@Composable
fun TestResultScreen(
    gameState: ClockGameState,
    onEvent: (ClockGameEvent) -> Unit,
    onNavigateToLevel2: () -> Unit
) {
    val passed = gameState.testPassed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (passed) "🎉 HARIKA! 🎉" else "😢 Tekrar Dene",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "${gameState.testCorrect}/10 doğru",
            fontSize = 24.sp,
            color = Color.White.copy(alpha = 0.9f)
        )

        Spacer(Modifier.height(48.dp))

        if (passed) {
            Button(
                onClick = { onNavigateToLevel2() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00b894))
            ) {
                Text("Level 2'ye Geç! 🎯", fontSize = 20.sp)
            }
        } else {
            Button(
                onClick = { onEvent(ClockGameEvent.RetryTest) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea))
            ) {
                Text("Tekrar Dene 🔄", fontSize = 20.sp)
            }
        }
    }
}