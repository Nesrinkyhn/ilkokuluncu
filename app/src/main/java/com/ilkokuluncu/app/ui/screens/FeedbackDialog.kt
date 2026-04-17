package com.ilkokuluncu.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ── Uygulama bilgileri (değiştir) ─────────────────────────────────────────────
private const val APP_PACKAGE      = "com.ilkokuluncu.app"
private const val FEEDBACK_EMAIL   = "ilkokuluncu@gmail.com"
private const val FEEDBACK_SUBJECT = "İlkokuluncu - Geri Bildirim"

/**
 * Akıllı geri bildirim diyaloğu.
 *
 * • 4-5 yıldız → Play Store değerlendirme sayfasını açar
 * • 1-3 yıldız → Metin kutusu gösterir, e-mail ile gönderir
 */
@Composable
fun FeedbackDialog(onDismiss: () -> Unit) {
    val context  = LocalContext.current
    var stars    by remember { mutableStateOf(0) }
    var comment  by remember { mutableStateOf("") }
    var sent     by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape     = RoundedCornerShape(24.dp),
            color     = Color.White,
            shadowElevation = 16.dp,
            modifier  = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (sent) {
                    // ── Gönderildi ekranı ────────────────────────────────
                    SentScreen(onDismiss = onDismiss)
                } else {
                    // ── Yıldız seçimi ────────────────────────────────────
                    Text(
                        "Uygulamayı beğendin mi? 😊",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Değerlendirmen gelişmemize çok yardımcı olur!",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    // Yıldızlar
                    StarRatingRow(
                        stars    = stars,
                        onSelect = { stars = it }
                    )

                    Spacer(Modifier.height(20.dp))

                    when {
                        stars == 0 -> {
                            // Henüz seçim yok
                            Text(
                                "Yukarıdaki yıldızlardan birine dokun",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }

                        stars >= 4 -> {
                            // Yüksek puan → Play Store
                            HighRatingContent(
                                stars    = stars,
                                context  = context,
                                onDismiss = onDismiss
                            )
                        }

                        else -> {
                            // Düşük puan → yorum kutusu
                            LowRatingContent(
                                comment  = comment,
                                onCommentChange = { comment = it },
                                context  = context,
                                onSent   = { sent = true },
                                onDismiss = onDismiss
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    TextButton(onClick = onDismiss) {
                        Text("Daha sonra", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ─── 5 yıldız satırı ─────────────────────────────────────────────────────────
@Composable
private fun StarRatingRow(stars: Int, onSelect: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val selected = i <= stars
            val scale by animateFloatAsState(
                targetValue    = if (selected) 1.25f else 1f,
                animationSpec  = spring(Spring.DampingRatioLowBouncy),
                label          = "star$i"
            )
            Text(
                text     = if (selected) "⭐" else "☆",
                fontSize = 36.sp,
                modifier = Modifier
                    .scale(scale)
                    .clickable { onSelect(i) }
            )
        }
    }

    Spacer(Modifier.height(6.dp))

    val label = when (stars) {
        1 -> "Hiç beğenmedim 😔"
        2 -> "Pek beğenmedim"
        3 -> "İdare eder 😐"
        4 -> "Çok beğendim 😊"
        5 -> "Harika! 🎉"
        else -> ""
    }
    if (label.isNotEmpty()) {
        Text(label, fontSize = 15.sp, color = Color(0xFF667eea), fontWeight = FontWeight.Medium)
    }
}

// ─── Yüksek puan içeriği (4-5 yıldız) ───────────────────────────────────────
@Composable
private fun HighRatingContent(stars: Int, context: Context, onDismiss: () -> Unit) {
    Text(
        text = if (stars == 5) "Harika, teşekkürler! 🥰" else "Çok teşekkürler! 😊",
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF333333),
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Play Store'da değerlendirmen daha fazla çocuğa ulaşmamızı sağlar!",
        fontSize = 13.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            openPlayStore(context)
            onDismiss()
        },
        shape  = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text("⭐ Play Store'da Değerlendir", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// ─── Düşük puan içeriği (1-3 yıldız) ────────────────────────────────────────
@Composable
private fun LowRatingContent(
    comment: String,
    onCommentChange: (String) -> Unit,
    context: Context,
    onSent: () -> Unit,
    onDismiss: () -> Unit
) {
    Text(
        "Görüşünü duymak isteriz. Ne geliştirebiliriz?",
        fontSize = 14.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value         = comment,
        onValueChange = onCommentChange,
        placeholder   = { Text("Düşüncelerini yaz...") },
        modifier      = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(12.dp),
        maxLines = 4
    )
    Spacer(Modifier.height(14.dp))
    Button(
        onClick = {
            sendFeedbackEmail(context, comment)
            onSent()
        },
        enabled  = comment.isNotBlank(),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea)),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text("📧 Gönder", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// ─── Gönderildi ekranı ────────────────────────────────────────────────────────
@Composable
private fun SentScreen(onDismiss: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("💌", fontSize = 56.sp)
        Text(
            "Teşekkürler!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        Text(
            "Geri bildiriminiz bize ulaştı.\nGeliştirmek için elimizden geleni yapacağız.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick  = onDismiss,
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea)),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Tamam", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Yardımcılar ─────────────────────────────────────────────────────────────
private fun openPlayStore(context: Context) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$APP_PACKAGE"))
        )
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$APP_PACKAGE"))
        )
    }
}

private fun sendFeedbackEmail(context: Context, body: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL,   arrayOf(FEEDBACK_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, FEEDBACK_SUBJECT)
        putExtra(Intent.EXTRA_TEXT,    body)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "E-posta uygulaması seç"))
    } catch (_: ActivityNotFoundException) { }
}
