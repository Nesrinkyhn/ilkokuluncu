package com.ilkokuluncu.app.data

data class ScoreEntry(
    val level: Int,       // 1 veya 2
    val score: Int,
    val passed: Boolean,  // test geçildi mi
    val timestamp: Long   // System.currentTimeMillis()
)
