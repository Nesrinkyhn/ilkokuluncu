package com.ilkokuluncu.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.data.*
import kotlin.math.roundToInt

private val CHAR_COLORS = listOf(
    Color(0xFFEF9A9A), Color(0xFF80DEEA), Color(0xFFA5D6A7),
    Color(0xFFFFF176), Color(0xFFCE93D8)
)

@Composable
fun PatternPuzzleScreen(
    state: PatternPuzzleState,
    onEvent: (PatternPuzzleEvent) -> Unit,
    onBackPress: () -> Unit
) {
    // Soru değişince sürükleme sıfırlansın
    var draggedId by remember(state.questionIndex) { mutableStateOf<Int?>(null) }
    var dragPos   by remember(state.questionIndex) { mutableStateOf(Offset.Zero) }

    val bg = Brush.verticalGradient(
        listOf(Color(0xFF4A148C), Color(0xFF6A1B9A), Color(0xFF283593))
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(bg)) {
        val W       = constraints.maxWidth.toFloat()
        val H       = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // Büyütülmüş top boyutu
        val charSizePx  = (W * 0.170f).coerceAtMost(118f)
        val charSizeDp  = with(density) { charSizePx.toDp() }
        val charRowY    = H * 0.76f
        val slotCount   = state.slots.size.coerceAtLeast(1)
        // Bir slot genişliği — bırakma toleransı buradan hesaplanır
        val slotWidthPx = W / slotCount

        // Hareketli topların GÜNCEL konumunu pointerInput içinden okuyabilmek için
        val currentChars    by rememberUpdatedState(state.chars)
        val currentW        by rememberUpdatedState(W)
        val currentCharRowY by rememberUpdatedState(charRowY)
        val currentSizePx   by rememberUpdatedState(charSizePx)
        val currentSlotW    by rememberUpdatedState(slotWidthPx)
        val currentSlots    by rememberUpdatedState(state.slots)

        // Sürükleme algılama — tüm ekran kapsıyor
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Güncel konumları kullan (stale closure sorunu önlendi)
                            val chars = currentChars
                            val rowY  = currentCharRowY
                            val szPx  = currentSizePx
                            val w     = currentW
                            val nearest = chars.minByOrNull { c ->
                                (Offset(c.x * w, rowY) - offset).getDistance()
                            }
                            if (nearest != null &&
                                (Offset(nearest.x * w, rowY) - offset).getDistance() < szPx * 1.5f
                            ) {
                                draggedId = nearest.id
                                dragPos   = offset
                            }
                        },
                        onDrag       = { _, delta -> dragPos += delta },
                        onDragEnd    = {
                            val cid = draggedId
                            if (cid != null) {
                                // ── Bırakma tespiti ────────────────────────────
                                val rowY  = currentCharRowY
                                val szPx  = currentSizePx
                                val slotW = currentSlotW
                                val slots = currentSlots
                                val inSlotZone = dragPos.y < rowY - szPx * 0.4f
                                if (inSlotZone) {
                                    val slot = slots
                                        .filter { it.isBlank && !it.filled }
                                        .minByOrNull { s ->
                                            val cx = (s.index + 0.5f) * slotW
                                            kotlin.math.abs(cx - dragPos.x)
                                        }
                                    if (slot != null) {
                                        val cx    = (slot.index + 0.5f) * slotW
                                        val xDist = kotlin.math.abs(cx - dragPos.x)
                                        // Slot genişliğinin %80'i içindeyse kabul et
                                        if (xDist < slotW * 0.80f) {
                                            onEvent(PatternPuzzleEvent.Dropped(cid, slot.index))
                                        }
                                    }
                                }
                            }
                            draggedId = null
                        },
                        onDragCancel = { draggedId = null }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── HUD ───────────────────────────────────────────────────────
                PuzzleHUD(state = state, onBackPress = onBackPress)

                Spacer(Modifier.height(6.dp))

                // ── Soru bilgisi ──────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(state.theme.emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Soru ${state.questionIndex + 1} / 30",
                        color = Color.White, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── Örüntü kutu satırı ────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.slots.forEach { slot ->
                        val isGreen = slot.index == state.flashCorrectSlot
                        val isRed   = slot.index == state.flashWrongSlot

                        val boxBg by animateColorAsState(
                            targetValue = when {
                                isGreen    -> Color(0xFF00C853)
                                isRed      -> Color(0xFFD50000)
                                slot.filled -> Color(0xFF2E7D32).copy(alpha = 0.90f)
                                slot.isBlank -> Color(0x88546E7A)
                                else        -> Color(0xFF1A237E).copy(alpha = 0.80f)
                            },
                            animationSpec = tween(200),
                            label = "sc${slot.index}"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.80f)
                                .padding(horizontal = 2.dp, vertical = 4.dp)
                                .shadow(4.dp, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .background(boxBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (slot.isBlank && !slot.filled) {
                                Text(
                                    "?",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(state.theme.emoji, fontSize = 18.sp)
                                    Text(
                                        "${slot.value}",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Yönlendirme metni ─────────────────────────────────────────
                Text(
                    text = "Doğru ${state.theme.label}ı boşluğa sürükle!",
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // ── Yürüyen karakterler ───────────────────────────────────────────
            state.chars.forEach { char ->
                if (char.id == draggedId) return@forEach   // sürüklenenı atla
                val cx = char.x * W
                CharCircle(
                    char     = char,
                    theme    = state.theme,
                    sizeDp   = charSizeDp,
                    sizePx   = charSizePx,
                    cx       = cx,
                    cy       = charRowY,
                    isLifted = false
                )
            }

            // ── Sürüklenen karakter (parmak konumunda) ────────────────────────
            draggedId?.let { cid ->
                state.chars.find { it.id == cid }?.let { char ->
                    CharCircle(
                        char     = char,
                        theme    = state.theme,
                        sizeDp   = charSizeDp * 1.15f,
                        sizePx   = charSizePx * 1.15f,
                        cx       = dragPos.x,
                        cy       = dragPos.y,
                        isLifted = true
                    )
                }
            }

            // ── GAME_OVER / VICTORY ───────────────────────────────────────────
            if (state.phase == PuzzlePh.GAME_OVER || state.phase == PuzzlePh.VICTORY) {
                PuzzleEndOverlay(
                    emoji     = when {
                        state.phase == PuzzlePh.VICTORY && state.isNewBest -> "🏆"
                        state.phase == PuzzlePh.VICTORY -> "🎉"
                        else -> "😢"
                    },
                    title     = when {
                        state.phase == PuzzlePh.VICTORY && state.isNewBest -> "Yeni Rekor!"
                        state.phase == PuzzlePh.VICTORY -> "Tebrikler!"
                        else -> "Bitti!"
                    },
                    score     = state.score,
                    bestScore = state.bestScore,
                    isNewBest = state.isNewBest,
                    onRetry   = { onEvent(PatternPuzzleEvent.Restart) },
                    onBack    = onBackPress
                )
            }
        }
    }
}

// ── Karakter dairesi ──────────────────────────────────────────────────────────
@Composable
private fun CharCircle(
    char: PuzzleChar,
    theme: PuzzleTheme,
    sizeDp: androidx.compose.ui.unit.Dp,
    sizePx: Float,
    cx: Float,
    cy: Float,
    isLifted: Boolean
) {
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (cx - sizePx / 2).roundToInt(),
                    (cy - sizePx / 2).roundToInt()
                )
            }
            .size(sizeDp)
            .graphicsLayer {
                shadowElevation = if (isLifted) 24f else 6f
                scaleX = if (isLifted) 1.12f else 1f
                scaleY = if (isLifted) 1.12f else 1f
            }
            .shadow(if (isLifted) 14.dp else 5.dp, CircleShape)
            .clip(CircleShape)
            .background(CHAR_COLORS[char.colorIndex % CHAR_COLORS.size]),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(theme.emoji, fontSize = (sizeDp.value * 0.34f).sp)
            Text(
                "${char.value}",
                color      = Color.Black,
                fontSize   = (sizeDp.value * 0.27f).sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign  = TextAlign.Center
            )
        }
    }
}

// ── HUD ───────────────────────────────────────────────────────────────────────
@Composable
private fun PuzzleHUD(state: PatternPuzzleState, onBackPress: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick  = onBackPress,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.25f))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
        }

        Spacer(Modifier.width(8.dp))

        // Canlar
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { i ->
                if (i < state.lives) {
                    Text("❤️", fontSize = 15.sp)
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text("❤️", fontSize = 15.sp,
                            modifier = Modifier.graphicsLayer { alpha = 0.22f })
                        Text("❌", fontSize = 9.sp)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Timer
        val timerColor = when {
            state.timeLeft > 120f -> Color(0xFF00E676)
            state.timeLeft > 60f  -> Color(0xFFFF9100)
            else                  -> Color(0xFFFF1744)
        }
        Text(
            text = "⏱ ${state.timeLeft.toInt()}s",
            color = timerColor, fontSize = 14.sp, fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = "⭐ ${state.score}",
            color = Color(0xFFFFEA00), fontSize = 15.sp, fontWeight = FontWeight.Bold
        )
    }
}

// ── Bitiş overlay ─────────────────────────────────────────────────────────────
@Composable
private fun PuzzleEndOverlay(
    emoji: String, title: String,
    score: Int, bestScore: Int, isNewBest: Boolean,
    onRetry: () -> Unit, onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(16.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF1A0A2E), Color(0xFF2D1B5E)))
                )
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 72.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(14.dp))
            Text("⭐ $score", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFEA00))
            if (isNewBest) {
                Text("Yeni rekor! 🎯", fontSize = 14.sp, color = Color(0xFF00E676))
            } else if (bestScore > 0) {
                Text("En iyi: $bestScore", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF00897B))
                    .clickable(onClick = onRetry)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🔄 Tekrar Oyna", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF37474F))
                    .clickable(onClick = onBack)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("◀ Menüye Dön", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
