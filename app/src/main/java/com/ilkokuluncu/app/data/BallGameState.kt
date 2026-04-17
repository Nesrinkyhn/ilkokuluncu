package com.ilkokuluncu.app.data

enum class BallPhase { HOUR, MINUTE, AFERIN }

data class BallItem(
    val id: Int,
    val label: String,
    val isCorrect: Boolean,
    val colorIndex: Int,
    val xStart: Float,
    val yStart: Float,
    val xSpeed: Int,
    val ySpeed: Int
)

data class BallGameState(
    // ── Normal mod ───────────────────────────────────────────────────────────
    val currentHour: Int       = 3,
    val currentMinute: Int     = 25,
    val phase: BallPhase       = BallPhase.HOUR,
    val balls: List<BallItem>  = emptyList(),
    val score: Int             = 0,
    val bgIndex: Int           = 0,
    val showAferin: Boolean    = false,
    val questionCount: Int     = 0,
    val wrongBallId: Int?      = null,
    // ── Test modu ────────────────────────────────────────────────────────────
    val showTestReadyDialog: Boolean = false,
    val isTestMode: Boolean          = false,
    val testQuestion: Int            = 0,     // 0–9
    val testCorrect: Int             = 0,
    val testShownCorrect: Int?       = null,  // yanlış sonrası doğru topun id'si
    val showTestResult: Boolean      = false,
    val testPassed: Boolean          = false
)

sealed class BallGameEvent {
    data class BallTapped(val ballId: Int) : BallGameEvent()
    object AcceptTestChallenge             : BallGameEvent()
    object DismissTestDialog               : BallGameEvent()
    object RetryTest                       : BallGameEvent()
    object BackToMenu                      : BallGameEvent()
}
