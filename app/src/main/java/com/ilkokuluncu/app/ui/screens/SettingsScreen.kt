package com.ilkokuluncu.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Gizlilik politikası URL'si — kendi sayfanla değiştir
private const val PRIVACY_POLICY_URL = "https://sites.google.com/view/ilkokuluncugizlilik/ana-sayfa"

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showFeedback by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.ArrowBack, "Geri", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text("⚙️ Ayarlar", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(24.dp))

            // ── Geri Bildirim kartı ──────────────────────────────────────────
            SettingsActionCard(
                icon        = "⭐",
                title       = "Uygulamamızı Değerlendir",
                description = "Görüşlerin bizim için çok değerli!",
                gradient    = listOf(Color(0xFFFF9800), Color(0xFFFFD600)),
                onClick     = { showFeedback = true }
            )

            Spacer(Modifier.height(12.dp))

            // ── Geri bildirim gönder ─────────────────────────────────────────
            SettingsActionCard(
                icon        = "📧",
                title       = "Geri Bildirim Gönder",
                description = "Bir sorun mu var? Bildirmekten çekinme.",
                gradient    = listOf(Color(0xFF667eea), Color(0xFF764ba2)),
                onClick     = { showFeedback = true }
            )

            Spacer(Modifier.height(12.dp))

            // ── Gizlilik Politikası ──────────────────────────────────────────
            SettingsActionCard(
                icon        = "🔒",
                title       = "Gizlilik Politikası",
                description = "Verilerinin nasıl korunduğunu öğren.",
                gradient    = listOf(Color(0xFF00897B), Color(0xFF26C6DA)),
                onClick     = {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
                        )
                    } catch (_: ActivityNotFoundException) {}
                }
            )

            Spacer(Modifier.height(24.dp))

            // ── Bilgi kartı ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsInfoItem(title = "Versiyon",    value = "1.0.0")
                    Spacer(Modifier.height(8.dp))
                    SettingsInfoItem(title = "Geliştirici", value = "İlkokuluncu Ekibi")
                    Spacer(Modifier.height(8.dp))
                    SettingsInfoItem(title = "Platform",    value = "Android 9+")
                }
            }
        }
    }

    if (showFeedback) {
        FeedbackDialog(onDismiss = { showFeedback = false })
    }
}

// ─── Eylem kartı (gradient arka plan, tıklanabilir) ──────────────────────────
@Composable
private fun SettingsActionCard(
    icon: String,
    title: String,
    description: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { pressed = true; onClick() },
        shape     = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradient))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(icon, fontSize = 26.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,       fontSize = 16.sp, fontWeight = FontWeight.Bold,    color = Color.White)
                Text(description, fontSize = 13.sp, color = Color.White.copy(0.82f))
            }
            Text("›", fontSize = 24.sp, color = Color.White.copy(0.8f))
        }
    }

    LaunchedEffect(pressed) {
        if (pressed) { kotlinx.coroutines.delay(100); pressed = false }
    }
}
@Composable
fun SettingsItem(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 32.sp,
            modifier = Modifier.padding(end = 16.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        
        Text(
            text = "›",
            fontSize = 24.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun SettingsInfoItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333)
        )
    }
}
