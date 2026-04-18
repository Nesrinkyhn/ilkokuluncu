package com.ilkokuluncu.app.data

enum class PatternGamePhase {
    INTRO,          // Büyük balon örüntüyü gösteriyor
    PLAYING,        // Oyun aktif
    SEQUENCE_DONE,  // Bir örüntü tamamlandı (kısa kutlama)
    GAME_OVER,
    VICTORY
}

data class FallingBalloon(
    val id: Int,
    val value: Int,
    val x: Float,           // 0..1 normalize (ekran genişliği)
    val y: Float,           // 0..1 normalize (ekran yüksekliği)
    val speed: Float,       // saniyede kaç ekran yüksekliği düşer
    val colorIndex: Int,
    val isCorrect: Boolean
)

data class PatternGameState(
    val phase: PatternGamePhase      = PatternGamePhase.INTRO,
    val currentPattern: Int          = 2,
    val sequence: List<Int>          = emptyList(),
    val nextIndex: Int               = 0,
    val fallingBalloons: List<FallingBalloon> = emptyList(),
    val playerX: Float               = 0.5f,   // 0..1
    val lives: Int                   = 5,
    val score: Int                   = 0,
    val speedMultiplier: Float       = 1.0f,
    val patternsDone: Int            = 0,
    val patternOrder: List<Int>      = listOf(2, 3, 4, 5),
    val introBalloonX: Float         = 1.4f,   // intro balonunun normalize X konumu
    val elapsedSeconds: Float        = 0f,
    val bestScore: Int               = 0,
    val isNewBest: Boolean           = false
)

sealed class PatternGameEvent {
    data class PlayerMoved(val x: Float) : PatternGameEvent()
    object RestartGame : PatternGameEvent()
    object BackToMenu  : PatternGameEvent()
}
