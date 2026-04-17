package com.ilkokuluncu.app.data

/** Oyun seviyesi modu */
enum class TrainLevelMode { HALF_HOURS, QUARTER_HOURS }

/**
 * Tren oyunu durumu (Level 2 – Yarım Saatler / Level 3 – Çeyrek Saatler)
 */
data class TrainGameState(
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val wagonCount: Int = 0,
    val currentHour: Int = 1,
    val currentMinute: Int = 30,
    val currentTrain: TrainCharacter = TrainCharacter.EXPRESS,
    val options: List<String> = emptyList(),
    val isTestMode: Boolean = false,
    val testQuestion: Int = 0,
    val testCorrect: Int = 0,
    val testTotal: Int = 10,
    val showResult: Boolean = false,
    val testPassed: Boolean = false,
    val showCelebration: Boolean = false,
    val showTestReadyDialog: Boolean = false,
    val showVictoryAnimation: Boolean = false,
    val showWrongAnswer: Boolean = false,
    val testTimeRemaining: Int = 5,
    val showCorrectAnswerAfterWrong: Boolean = false,
    val correctAnswerToShow: String? = null,
    val trainIsHappy: Boolean = true,
    // ── Seviye modu ─────────────────────────────────────────────────────────
    val levelMode: TrainLevelMode = TrainLevelMode.HALF_HOURS,
    // ── Test: vagon seçme (doğru vagonu bul) ────────────────────────────────
    val testWagonOptions: List<Int> = emptyList(),
    val testCorrectHour: Int = 0,
    val testCorrectWagonShown: Int? = null,
    // ── Level 3: soru stili (0=dijital "10:15", 1=sözlü "çeyrek geçiyor/var") ─
    val questionStyle: Int = 0
)

sealed class TrainGameEvent {
    data class AnswerSelected(val answer: String) : TrainGameEvent()
    data class WagonSelected(val hour: Int) : TrainGameEvent()
    object NextQuestion : TrainGameEvent()
    object StartTest : TrainGameEvent()
    object RetryTest : TrainGameEvent()
    object ContinueToLevel3 : TrainGameEvent()
    object BackToMenu : TrainGameEvent()
    object DismissTestReadyDialog : TrainGameEvent()
    object AcceptTestChallenge : TrainGameEvent()
    object TimeExpired : TrainGameEvent()
}
