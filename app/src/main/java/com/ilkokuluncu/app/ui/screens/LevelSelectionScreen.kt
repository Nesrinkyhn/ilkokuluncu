package com.ilkokuluncu.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.ads.AdConfig
import com.ilkokuluncu.app.ads.AdManager
import com.ilkokuluncu.app.data.GameLevel
import com.ilkokuluncu.app.data.GameModule
import com.ilkokuluncu.app.data.ScoreEntry
import com.ilkokuluncu.app.ui.components.BannerAdView
import com.ilkokuluncu.app.viewmodel.ScoreSummary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LevelSelectionScreen(
    gameModule: GameModule,
    scoreSummary: ScoreSummary? = null,
    onBackClick: () -> Unit,
    onLevelClick: (GameLevel) -> Unit,
    onTrainingClick: ((String) -> Unit)? = null,
    onTestClick: ((Int) -> Unit)? = null,
    adManager: AdManager? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "title")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF093FB),
                        Color(0xFFF5576C)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = gameModule.icon,
                        fontSize = 32.sp
                    )
                    Text(
                        text = gameModule.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Seviye Başlığı
            Text(
                text = "Seviyeler",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.offset(y = offsetY.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Level Listesi + Geçmiş
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(gameModule.levels) { level ->
                    LevelCard(
                        level = level,
                        onClick = { if (level.isUnlocked && !level.isComingSoon) onLevelClick(level) }
                    )
                }
                if (gameModule.id == "clock_reading" && onTrainingClick != null) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        AntrenmanlarSection(
                            placementBest   = scoreSummary?.clockPlacementBest ?: 0,
                            quarterBest     = scoreSummary?.quarterBest ?: 0,
                            matchBest       = scoreSummary?.matchBest ?: 0,
                            timeCalcBest    = scoreSummary?.timeCalcBest ?: 0,
                            onTrainingClick = onTrainingClick
                        )
                    }
                }
                if (gameModule.id == "clock_reading" && onTestClick != null) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        TestlerSection(onTestClick = onTestClick)
                    }
                }
                if (scoreSummary != null) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        LevelHistorySection(scoreSummary = scoreSummary)
                    }
                }
            }
        }

        // ── Banner reklam (alt) ───────────────────────────────────────────
        if (adManager != null) {
            BannerAdView(adUnitId = AdConfig.BANNER_LEVEL_SELECT)
        }
    }
}

@Composable
fun LevelCard(
    level: GameLevel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    val isPlayable = level.isUnlocked && !level.isComingSoon

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(enabled = isPlayable) {
                if (isPlayable) {
                    isPressed = true
                    onClick()
                }
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlayable) 8.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayable) Color.White else Color.White.copy(alpha = 0.7f)
        )
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = when {
                            !level.isUnlocked -> Brush.horizontalGradient(
                                listOf(Color.Gray.copy(alpha = 0.1f), Color.Gray.copy(alpha = 0.1f))
                            )
                            level.isComingSoon -> Brush.horizontalGradient(
                                listOf(Color(0xFFFF8F00).copy(alpha = 0.08f), Color(0xFFFFA000).copy(alpha = 0.08f))
                            )
                            else -> Brush.horizontalGradient(
                                listOf(Color(0xFF667eea).copy(alpha = 0.1f), Color(0xFF764ba2).copy(alpha = 0.1f))
                            )
                        }
                    )
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // İkon
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        !level.isUnlocked  -> Color.Gray
                        level.isComingSoon -> Color(0xFFFF8F00)
                        else               -> Color(0xFF667eea)
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = level.icon, fontSize = 32.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Bilgiler
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Level ${level.levelNumber}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                !level.isUnlocked  -> Color.Gray
                                level.isComingSoon -> Color(0xFFFF8F00)
                                else               -> Color(0xFF667eea)
                            }
                        )
                        if (!level.isUnlocked) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Lock, contentDescription = "Kilitli",
                                tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }

                    Text(
                        text = level.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPlayable) Color(0xFF333333) else Color.Gray
                    )

                    Text(
                        text = level.description,
                        fontSize = 14.sp,
                        color = if (isPlayable) Color.Gray else Color.Gray.copy(alpha = 0.6f)
                    )

                    Spacer(Modifier.height(4.dp))

                    when {
                        level.isComingSoon -> {
                            // Yakında rozeti
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF8F00).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "🚧 Yakında geliyor...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        !level.isUnlocked && level.requiredScore > 0 -> {
                            Text(
                                text = "🏆 ${level.requiredScore} puan gerekiyor",
                                fontSize = 12.sp,
                                color = Color(0xFFFF6B6B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

// ─── Saat okuma geçmiş bölümü ────────────────────────────────────────────────
@Composable
fun LevelHistorySection(scoreSummary: ScoreSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "⏰ Geçmiş",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(14.dp))

        // En iyi skorlar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LevelBestCard("L1\nTam",     scoreSummary.level1Best, Color(0xFF667eea), Modifier.weight(1f))
            LevelBestCard("L2\nYarım",   scoreSummary.level2Best, Color(0xFF43A047), Modifier.weight(1f))
            LevelBestCard("L3\nÇeyrek",  scoreSummary.level3Best, Color(0xFFFB8C00), Modifier.weight(1f))
            LevelBestCard("L4\nDakika",  scoreSummary.level4Best, Color(0xFFE53935), Modifier.weight(1f))
        }

        if (scoreSummary.history.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "🕐 Son Oyunlar",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(8.dp))
            scoreSummary.history.take(10).forEach { entry ->
                LevelHistoryRow(entry = entry)
                Spacer(Modifier.height(6.dp))
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Henüz oyun oynanmadı. Hadi başlayalım! 🚀",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LevelBestCard(label: String, bestScore: Int, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center, lineHeight = 16.sp)
        Spacer(Modifier.height(6.dp))
        if (bestScore > 0) {
            Text("🏆 $bestScore", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        } else {
            Text("—", fontSize = 22.sp, color = Color.White.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun LevelHistoryRow(entry: ScoreEntry) {
    val levelColor = when (entry.level) { 1 -> Color(0xFF667eea); 2 -> Color(0xFF43A047); 3 -> Color(0xFFFB8C00); else -> Color(0xFFE53935) }
    val levelLabel = when (entry.level) { 1 -> "L1"; 2 -> "L2"; 3 -> "L3"; else -> "L4" }
    val diffDays   = ((System.currentTimeMillis() - entry.timestamp) / (1000 * 60 * 60 * 24)).toInt()
    val dateStr    = when {
        diffDays == 0 -> "Bugün"
        diffDays == 1 -> "Dün"
        diffDays < 7  -> "$diffDays gün önce"
        else -> SimpleDateFormat("dd MMM", Locale("tr")).format(Date(entry.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(levelColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(levelLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text("⭐ ${entry.score}", fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = Color.White, modifier = Modifier.weight(1f))
        Text(
            text = if (entry.passed) "✅ Geçti" else "🎯 Devam",
            fontSize = 13.sp,
            color = if (entry.passed) Color(0xFF69F0AE) else Color.White.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(10.dp))
        Text(dateStr, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
    }
}

// ─── Antrenmanlar bölümü ─────────────────────────────────────────────────────
@Composable
fun AntrenmanlarSection(
    placementBest: Int,
    quarterBest: Int = 0,
    matchBest: Int = 0,
    timeCalcBest: Int = 0,
    onTrainingClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            "🏋️ Antrenmanlar",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White
        )
        Text(
            "Levellerden bağımsız, istediğin kadar çalış!",
            fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f)
        )

        Spacer(Modifier.height(14.dp))

        TrainingCard(
            icon        = "⏰",
            title       = "Kadranı Yerleştir",
            description = "Akrebi ya da yelkovanı doğru yere sürükle!",
            gradient    = listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),
            bestScore   = placementBest,
            onClick     = { onTrainingClick("clock_hand_placement") }
        )

        Spacer(Modifier.height(10.dp))

        TrainingCard(
            icon        = "🕐",
            title       = "Çeyrek Geçe / Kala",
            description = "Saat doğru mu yanlış mı? Hızlıca karar ver!",
            gradient    = listOf(Color(0xFF7C3AED), Color(0xFF0EA5E9)),
            bestScore   = quarterBest,
            onClick     = { onTrainingClick("quarter_game") }
        )

        Spacer(Modifier.height(10.dp))

        TrainingCard(
            icon        = "🔗",
            title       = "Eşleştir",
            description = "Saati sürükleyerek doğru kadranla eşleştir!",
            gradient    = listOf(Color(0xFF00897B), Color(0xFF26C6DA)),
            bestScore   = matchBest,
            onClick     = { onTrainingClick("match_game") }
        )

        Spacer(Modifier.height(10.dp))

        TrainingCard(
            icon        = "⏩",
            title       = "Saat Hesapla",
            description = "15 dk, 1 saat sonra/önce kaç olur? 500 puana ulaş!",
            gradient    = listOf(Color(0xFFFF6B35), Color(0xFFF7C59F)),
            bestScore   = timeCalcBest,
            onClick     = { onTrainingClick("time_calc_game") }
        )
    }
}

@Composable
private fun TrainingCard(
    icon: String,
    title: String,
    description: String,
    gradient: List<Color>,
    bestScore: Int,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "tscale"
    )

    Card(
        modifier  = Modifier.fillMaxWidth().scale(scale).clickable { pressed = true; onClick() },
        shape     = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradient))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Büyük ikon kutusu
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(icon, fontSize = 30.sp)
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(description, fontSize = 13.sp, color = Color.White.copy(alpha = 0.82f))
                if (bestScore > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏆", fontSize = 13.sp)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "En iyi: $bestScore",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD600)
                        )
                    }
                }
            }

            Text("▶", fontSize = 22.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }

    LaunchedEffect(pressed) {
        if (pressed) { kotlinx.coroutines.delay(120); pressed = false }
    }
}

// ─── Testler bölümü ──────────────────────────────────────────────────────────
@Composable
fun TestlerSection(onTestClick: (Int) -> Unit) {

    val tests = listOf(
        Triple(1, "📝 Test Level 1", "Tam saatler testine gir!"),
        Triple(2, "📝 Test Level 2", "Yarım saatler testine gir!"),
        Triple(3, "📝 Test Level 3", "Çeyrek saatler testine gir!")
    )

    val gradients = listOf(
        listOf(Color(0xFF1565C0), Color(0xFF42A5F5)),
        listOf(Color(0xFF2E7D32), Color(0xFF66BB6A)),
        listOf(Color(0xFFE65100), Color(0xFFFFB74D))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            "📝 Testler",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White
        )
        Text(
            "Hazır mısın? Öğrendiklerini test et!",
            fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f)
        )

        Spacer(Modifier.height(14.dp))

        tests.forEachIndexed { index, (levelNum, title, desc) ->
            TrainingCard(
                icon        = "🎯",
                title       = title,
                description = desc,
                gradient    = gradients[index],
                bestScore   = 0,           // Testlerin skoru yok
                onClick     = { onTestClick(levelNum) }
            )
            if (index < tests.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}
