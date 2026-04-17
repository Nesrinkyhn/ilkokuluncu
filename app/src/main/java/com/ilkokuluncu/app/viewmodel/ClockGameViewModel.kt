package com.ilkokuluncu.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ilkokuluncu.app.data.AnimalCharacter
import com.ilkokuluncu.app.data.ClockGameEvent
import com.ilkokuluncu.app.data.ClockGameState
import com.ilkokuluncu.app.data.GamePreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClockGameViewModel(application: Application) : AndroidViewModel(application) {

    private val _gameState = MutableStateFlow(ClockGameState())
    val gameState: StateFlow<ClockGameState> = _gameState.asStateFlow()

    private var timerJob: Job? = null
    private val gamePrefs = GamePreferences(application)

    init {

        val savedScore = gamePrefs.savedScore
        if (savedScore > 0) {
            _gameState.value = _gameState.value.copy(score = savedScore)
        }
        generateQuestion()
    }

    fun onEvent(event: ClockGameEvent) {
        when (event) {
            is ClockGameEvent.AnswerSelected -> checkAnswer(event.answer)
            is ClockGameEvent.NextQuestion -> generateQuestion()
            is ClockGameEvent.StartTest -> startTest()
            is ClockGameEvent.RetryTest -> retryTest()
            is ClockGameEvent.ContinueToLevel2 -> continueToLevel2()
            is ClockGameEvent.BackToMenu -> resetGame()
            is ClockGameEvent.DismissTestReadyDialog -> dismissTestReadyDialog()
            is ClockGameEvent.AcceptTestChallenge -> acceptTestChallenge()
            is ClockGameEvent.TimeExpired -> handleTimeExpired()
        }
    }

    private fun generateQuestion() {
        val currentHour = (1..12).random()
        val animal = AnimalCharacter.random()
        val options = generateOptions(currentHour)

        _gameState.value = _gameState.value.copy(
            currentHour = currentHour,
            currentAnimal = animal,
            options = options,
            showCelebration = false,
            testTimeRemaining = 5,
            showCorrectAnswerAfterWrong = false,
            correctAnswerToShow = null
        )

        // Test modunda timer başlat
        if (_gameState.value.isTestMode) {
            startTimer()
        }
    }

    private fun generateOptions(correctAnswer: Int): List<Int> {
        val options = mutableListOf(correctAnswer)

        while (options.size < 3) {
            val wrong = (1..12).random()
            if (wrong !in options) {
                options.add(wrong)
            }
        }

        return options.shuffled()
    }

    private fun checkAnswer(selected: Int) {
        val isCorrect = selected == _gameState.value.currentHour

        if (_gameState.value.isTestMode) {
            handleTestAnswer(isCorrect, selected)
        } else {
            handleNormalAnswer(isCorrect)
        }
    }

    private fun handleNormalAnswer(isCorrect: Boolean) {
        if (isCorrect) {
            val newScore = _gameState.value.score + 10
            val newCorrectAnswers = _gameState.value.correctAnswers + 1

            _gameState.value = _gameState.value.copy(
                score = newScore,
                correctAnswers = newCorrectAnswers,
                showCelebration = true
            )

            // Score'u kaydet – hem aktif hem en yüksek (anında, geri basılmasa da korunsun)
            gamePrefs.savedScore = newScore
            if (newScore > gamePrefs.level1BestScore) {
                gamePrefs.level1BestScore = newScore
            }

            viewModelScope.launch {
                delay(1500)

                // 50 PUANA ULAŞINCA "TESTE HAZIR MISIN?" DİALOGU GÖSTER
                if (newScore >= 200 && !_gameState.value.isTestMode && _gameState.value.level == 1) {
                    _gameState.value = _gameState.value.copy(
                        showTestReadyDialog = true,
                        showCelebration = false
                    )
                } else {
                    generateQuestion()
                }
            }
        } else {
            // YANLIŞ CEVAP - ❌ GÖSTER!
            val newScore = maxOf(0, _gameState.value.score - 2)
            _gameState.value = _gameState.value.copy(
                score = newScore,
                showWrongAnswer = true
            )

            // Score'u kaydet
            gamePrefs.savedScore = newScore

            viewModelScope.launch {
                delay(1200)
                _gameState.value = _gameState.value.copy(
                    showWrongAnswer = false
                )
            }
        }
    }

    private fun dismissTestReadyDialog() {
        _gameState.value = _gameState.value.copy(showTestReadyDialog = false)
        generateQuestion()
    }

    private fun acceptTestChallenge() {
        _gameState.value = _gameState.value.copy(showTestReadyDialog = false)
        startTest()
    }

    private fun handleTestAnswer(isCorrect: Boolean, selectedAnswer: Int) {
        // Timer'ı durdur
        stopTimer()

        if (isCorrect) {
            // DOĞRU CEVAP
            val newTestCorrect = _gameState.value.testCorrect + 1
            val newTestQuestion = _gameState.value.testQuestion + 1

            _gameState.value = _gameState.value.copy(
                testCorrect = newTestCorrect,
                testQuestion = newTestQuestion,
                showCelebration = true
            )

            viewModelScope.launch {
                delay(1500)

                if (newTestQuestion >= 10) {
                    showTestResult()
                } else {
                    generateTestQuestion()
                }
            }
        } else {
            // YANLIŞ CEVAP - 2 SANİYE DOĞRU CEVABI GÖSTER
            val newTestQuestion = _gameState.value.testQuestion + 1

            _gameState.value = _gameState.value.copy(
                testQuestion = newTestQuestion,
                showCorrectAnswerAfterWrong = true,
                correctAnswerToShow = _gameState.value.currentHour
            )

            viewModelScope.launch {
                delay(2000) // 2 saniye bekle

                if (newTestQuestion >= 10) {
                    showTestResult()
                } else {
                    generateTestQuestion()
                }
            }
        }
    }

    private fun handleTimeExpired() {
        // Süre doldu - yanlış cevap gibi işle
        stopTimer()

        val newTestQuestion = _gameState.value.testQuestion + 1

        _gameState.value = _gameState.value.copy(
            testQuestion = newTestQuestion,
            showCorrectAnswerAfterWrong = true,
            correctAnswerToShow = _gameState.value.currentHour
        )

        viewModelScope.launch {
            delay(2000) // 2 saniye doğru cevabı göster

            if (newTestQuestion >= 10) {
                showTestResult()
            } else {
                generateTestQuestion()
            }
        }
    }

    private fun startTest() {
        _gameState.value = _gameState.value.copy(
            isTestMode = true,
            testQuestion = 0,
            testCorrect = 0,
            showResult = false
        )

        generateTestQuestion()
    }

    private fun generateTestQuestion() {
        val currentHour = (1..12).random()
        val animal = AnimalCharacter.random()
        val options = generateOptions(currentHour)

        _gameState.value = _gameState.value.copy(
            currentHour = currentHour,
            currentAnimal = animal,
            options = options,
            showCelebration = false,
            testTimeRemaining = 5,
            showCorrectAnswerAfterWrong = false,
            correctAnswerToShow = null
        )

        startTimer()
    }

    private fun showTestResult() {
        stopTimer()
        val passed = _gameState.value.testCorrect >= 7

        // Test başarılıysa kaydet
        if (passed) {
            gamePrefs.hasPassedLevel1 = true
        }

        _gameState.value = _gameState.value.copy(
            showResult = true,
            testPassed = passed,
            showCelebration = passed,
            showVictoryAnimation = passed
        )
    }

    private fun retryTest() {
        // Test başarısız → Score KALSIN, sadece test modundan çık
        stopTimer()
        _gameState.value = _gameState.value.copy(
            isTestMode = false,
            testQuestion = 0,
            testCorrect = 0,
            showResult = false
            // score ve correctAnswers KORUNUYOR!
        )
        generateQuestion()
    }

    private fun continueToLevel2() {
        // Test başarılı → Normal moda dön (score KALSIN)
        stopTimer()
        _gameState.value = _gameState.value.copy(
            isTestMode = false,
            testQuestion = 0,
            testCorrect = 0,
            showResult = false,
            showVictoryAnimation = false
            // score KORUNUYOR! Kullanıcı isterse tekrar test olabilir
        )
        generateQuestion()
    }

    /** Oyuncu geri tuşuna basarken çağrılır – skoru geçmişe kaydeder */
    fun saveSessionToHistory() {
        val score = _gameState.value.score
        if (score <= 0) return
        val passed = gamePrefs.hasPassedLevel1
        gamePrefs.addToHistory(
            entry    = com.ilkokuluncu.app.data.ScoreEntry(
                level     = 1,
                score     = score,
                passed    = passed,
                timestamp = System.currentTimeMillis()
            ),
            moduleId = "clock_reading"
        )
        if (score > gamePrefs.level1BestScore) {
            gamePrefs.level1BestScore = score
        }
    }

    private fun resetGame() {
        stopTimer()
        _gameState.value = ClockGameState()
        generateQuestion()
    }

    // TIMER FONKSİYONLARI
    private fun startTimer() {
        stopTimer() // Önceki timer varsa durdur

        timerJob = viewModelScope.launch {
            repeat(5) { second ->
                delay(1000)
                _gameState.value = _gameState.value.copy(
                    testTimeRemaining = 4 - second
                )
            }

            // Süre doldu
            onEvent(ClockGameEvent.TimeExpired)
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}