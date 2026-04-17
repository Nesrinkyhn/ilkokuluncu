package com.ilkokuluncu.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilkokuluncu.app.data.GameModule

@Composable
fun GameCard(
    module: GameModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    val cardColors = if (module.isInstalled) {
        listOf(Color(0xFF667eea), Color(0xFF764ba2))
    } else {
        listOf(Color(0xFFcccccc), Color(0xFFaaaaaa))
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(enabled = !module.isDownloading) {
                if (module.isInstalled || !module.isDownloading) {
                    isPressed = true
                    onClick()
                }
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(colors = cardColors.map { it.copy(alpha = 0.1f) })
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // İkon
                Text(
                    text = module.icon,
                    fontSize = 64.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Başlık
                Text(
                    text = module.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (module.isInstalled) Color(0xFF667eea) else Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Açıklama
                Text(
                    text = module.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                // İndirme durumu
                if (module.isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = module.downloadProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF667eea)
                    )
                    Text(
                        text = "İndiriliyor... %${(module.downloadProgress * 100).toInt()}",
                        fontSize = 12.sp,
                        color = Color(0xFF667eea),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else if (!module.isInstalled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "📥 ${module.sizeInMB} MB",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // Kilit ikonu
            if (!module.isInstalled && !module.isDownloading) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Kilitli",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(32.dp),
                    tint = Color.Gray
                )
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
