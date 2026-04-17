package com.ilkokuluncu.app.data

import androidx.compose.ui.graphics.Color

/**
 * Tren karakteri - Level 2 için
 */
data class TrainCharacter(
    val name: String,
    val emoji: String,
    val question: String,
    val bgColor1: Long,
    val bgColor2: Long,
    val trainColor: Long
) {
    companion object {
        val EXPRESS = TrainCharacter(
            name = "Ekspres",
            emoji = "🚂",
            question = "Ekspres treni saat kaç?",
            bgColor1 = 0xFF667eea,
            bgColor2 = 0xFF764ba2,
            trainColor = 0xFFE53935
        )
        
        val BULLET = TrainCharacter(
            name = "Hızlı",
            emoji = "🚄",
            question = "Hızlı tren saat kaç?",
            bgColor1 = 0xFF38ef7d,
            bgColor2 = 0xFF11998e,
            trainColor = 0xFF1E88E5
        )
        
        val METRO = TrainCharacter(
            name = "Metro",
            emoji = "🚇",
            question = "Metro saat kaç?",
            bgColor1 = 0xFFfa709a,
            bgColor2 = 0xFFfee140,
            trainColor = 0xFFFB8C00
        )
        
        val STEAM = TrainCharacter(
            name = "Buharlı",
            emoji = "🚂",
            question = "Buharlı tren saat kaç?",
            bgColor1 = 0xFF30cfd0,
            bgColor2 = 0xFF330867,
            trainColor = 0xFF6A1B9A
        )
        
        fun random(): TrainCharacter {
            return listOf(EXPRESS, BULLET, METRO, STEAM).random()
        }
    }
}

/**
 * Vagon renkleri
 */
object WagonColors {
    val colors = listOf(
        0xFFE53935L, // Kırmızı
        0xFF1E88E5L, // Mavi
        0xFF43A047L, // Yeşil
        0xFFFDD835L, // Sarı
        0xFF8E24AAL, // Mor
        0xFFFB8C00L  // Turuncu
    )
    
    fun getColor(index: Int): Long {
        return colors[index % colors.size]
    }
}
