package com.ilkokuluncu.app.data

// ── Oyun fazları ──────────────────────────────────────────────────────────────
enum class MultiplicationPhase {
    PICK_FIRST,   // Birinci çarpan seçiliyor
    PICK_SECOND,  // İkinci çarpan seçiliyor
    ANSWER,       // Sonuç balonlarından seçim (10 sn)
    GAME_OVER,    // Can bitti
    VICTORY       // 50 soru tamamlandı
}

// ── Balon modeli ──────────────────────────────────────────────────────────────
data class MultBall(
    val id: Int,
    val value: Int,
    val colorIndex: Int,
    val xStart: Float,   // 0..1 normalize başlangıç
    val xEnd: Float,     // 0..1 normalize hedef (tam ekran boyunca)
    val yStart: Float,
    val yEnd: Float,
    val xSpeed: Int,     // animasyon periyodu (ms)
    val ySpeed: Int,
    val isCorrect: Boolean = false
)

// ── Ana oyun state ────────────────────────────────────────────────────────────
data class MultiplicationGameState(
    val phase: MultiplicationPhase    = MultiplicationPhase.PICK_FIRST,
    val firstNumber: Int?             = null,
    val secondNumber: Int?            = null,
    val floatingBalls: List<MultBall> = emptyList(),
    val answerBalls: List<MultBall>   = emptyList(),
    val questionCount: Int            = 0,   // şu ana kadar cevaplanan soru sayısı
    val score: Int                    = 0,
    val lives: Int                    = 5,
    val timeLeft: Float               = 10f,
    val wrongBallId: Int?             = null,
    val correctFlashBallId: Int?      = null,
    val selectedBallId: Int?          = null,   // tıklanan uçan balon (sayı göster + yukarı çık)
    val bestScore: Int                = 0,
    val isNewBest: Boolean            = false
)

// ── Event'ler ─────────────────────────────────────────────────────────────────
sealed class MultiplicationGameEvent {
    data class FloatingBallTapped(val ballId: Int) : MultiplicationGameEvent()
    data class AnswerBallTapped(val ballId: Int)   : MultiplicationGameEvent()
    object TimeExpired  : MultiplicationGameEvent()
    object RestartGame  : MultiplicationGameEvent()
    object BackToMenu   : MultiplicationGameEvent()
}
