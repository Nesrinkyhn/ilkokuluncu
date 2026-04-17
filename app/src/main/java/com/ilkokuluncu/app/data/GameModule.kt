package com.ilkokuluncu.app.data

/**
 * Oyun modülü veri sınıfı
 * Her oyun kartı için gerekli bilgileri tutar
 */
data class GameModule(
    val id: String,
    val title: String,
    val description: String,
    val icon: String, // Emoji olarak
    val isInstalled: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val version: String = "1.0",
    val sizeInMB: Float = 0f,
    val levels: List<GameLevel> = emptyList()
)

/**
 * Oyun seviyeleri
 */
data class GameLevel(
    val id: String,
    val levelNumber: Int,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false,
    val requiredScore: Int = 0,
    val isComingSoon: Boolean = false   // true → içerik henüz yok, yakında etiketi gösterilir
)

/**
 * Hayvan karakterleri - Her hayvanın kendine özel renkleri var
 */
enum class AnimalCharacter(
    val emoji: String,
    val displayName: String,
    val question: String,
    val primaryColor: Long, // Ana renk (saat yüzü)
    val secondaryColor: Long, // İkinci renk (gradyan)
    val bgColor1: Long, // Arka plan renk 1
    val bgColor2: Long // Arka plan renk 2
) {
    CAT("🐱", "Kedicik", "Kedicik saatin kaç olduğunu merak ediyor",
        0xFFFF6B6B, 0xFFFF8E53, 0xFFFFA07A, 0xFFFFD89B), // Turuncu tonları

    DOG("🐶", "Köpek", "Köpek kaç olduğunu öğrenmek istiyor",
        0xFF4ECDC4, 0xFF44A08D, 0xFF96E6A1, 0xFFD4FC79), // Yeşil-turkuaz

    RABBIT("🐰", "Tavşan", "Tavşan saate bakmak istiyor",
        0xFFBB8FCE, 0xFF9B59B6, 0xFFEE9CA7, 0xFFFFDDE1), // Pembe-mor

    BEAR("🐻", "Ayıcık", "Ayıcık saat kaç diye soruyor",
        0xFFE67E22, 0xFFD35400, 0xFFFFCCBC, 0xFFFFE0B2), // Kahverengi

    FOX("🦊", "Tilki", "Tilki saati okumaya çalışıyor",
        0xFFE74C3C, 0xFFC0392B, 0xFFFFAB91, 0xFFFFCCBC), // Kırmızı-turuncu

    PANDA("🐼", "Panda", "Panda saat kaç merak ediyor",
        0xFF34495E, 0xFF2C3E50, 0xFFE0E0E0, 0xFFF5F5F5), // Siyah-beyaz

    LION("🦁", "Aslan", "Aslan saati öğrenmek istiyor",
        0xFFF39C12, 0xFFE67E22, 0xFFFFE082, 0xFFFFECB3), // Altın sarısı

    TIGER("🐯", "Kaplan", "Kaplan saat kaç diye soruyor",
        0xFFE67E22, 0xFFD35400, 0xFFFFCC80, 0xFFFFE082), // Turuncu-sarı

    COW("🐮", "İnek", "İnek saate bakmak istiyor",
        0xFF95A5A6, 0xFF7F8C8D, 0xFFE8EAF6, 0xFFC5CAE9), // Gri-mavi

    PIG("🐷", "Domuz", "Domuz saat kaç merak ediyor",
        0xFFFF8A80, 0xFFFF5252, 0xFFFFCDD2, 0xFFF8BBD0), // Pembe

    MONKEY("🐵", "Maymun", "Maymun saati okumaya çalışıyor",
        0xFFD4A574, 0xFFB8956A, 0xFFFFE0B2, 0xFFFFCC80), // Kahve-bej

    ELEPHANT("🐘", "Fil", "Fil saatin kaç olduğunu öğrenmek istiyor",
        0xFF90A4AE, 0xFF78909C, 0xFFB0BEC5, 0xFFCFD8DC); // Gri-mavi

    companion object {
        fun random() = values().random()
    }
}