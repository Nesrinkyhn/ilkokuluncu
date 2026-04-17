package com.ilkokuluncu.app.data

/**
 * Saat okuma oyununun durumu
 */
data class ClockGameState(
    val score: Int = 0,
    val level: Int = 1,
    val correctAnswers: Int = 0,
    val currentHour: Int = 1,
    val currentAnimal: AnimalCharacter = AnimalCharacter.CAT,
    val options: List<Int> = emptyList(),
    val isTestMode: Boolean = false,
    val testQuestion: Int = 0,
    val testCorrect: Int = 0,
    val testTotal: Int = 10,
    val showResult: Boolean = false,
    val testPassed: Boolean = false,
    val showCelebration: Boolean = false,
    val showTestReadyDialog: Boolean = false,
    val showVictoryAnimation: Boolean = false, // Geri eklendi
    val showWrongAnswer: Boolean = false,
    val testTimeRemaining: Int = 5,
    val showCorrectAnswerAfterWrong: Boolean = false,
    val correctAnswerToShow: Int? = null
)

/**
 * UI olayları
 */
sealed class ClockGameEvent {
    data class AnswerSelected(val answer: Int) : ClockGameEvent()
    object NextQuestion : ClockGameEvent()
    object StartTest : ClockGameEvent()
    object RetryTest : ClockGameEvent()
    object ContinueToLevel2 : ClockGameEvent()
    object BackToMenu : ClockGameEvent()
    object DismissTestReadyDialog : ClockGameEvent()
    object AcceptTestChallenge : ClockGameEvent()
    object TimeExpired : ClockGameEvent() // YENİ: Süre doldu
}