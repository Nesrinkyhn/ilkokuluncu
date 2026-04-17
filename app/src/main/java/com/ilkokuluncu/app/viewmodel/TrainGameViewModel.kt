package com.ilkokuluncu.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ilkokuluncu.app.data.*
import com.ilkokuluncu.app.ui.components.MAX_WAGONS
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrainGameViewModel(application: Application) : AndroidViewModel(application) {

    private val _gameState = MutableStateFlow(TrainGameState())
    val gameState: StateFlow<TrainGameState> = _gameState.asStateFlow()

    private var timerJob: Job? = null
    private val gamePrefs = GamePreferences(application)

    init {
        generateQuestion()
    }

    // ─── Event dispatcher ─────────────────────────────────────────────────────
    fun onEvent(event: TrainGameEvent) {
        when (event) {
            is TrainGameEvent.AnswerSelected         -> checkAnswer(event.answer)
            is TrainGameEvent.WagonSelected          -> checkWagonAnswer(event.hour)
            is TrainGameEvent.NextQuestion           -> generateQuestion()
            is TrainGameEvent.StartTest              -> startTest()
            is TrainGameEvent.RetryTest              -> retryTest()
            is TrainGameEvent.ContinueToLevel3       -> continueAfterTest()
            is TrainGameEvent.BackToMenu             -> resetGame()
            is TrainGameEvent.DismissTestReadyDialog -> dismissTestReadyDialog()
            is TrainGameEvent.AcceptTestChallenge    -> acceptTestChallenge()
            is TrainGameEvent.TimeExpired            -> handleTimeExpired()
        }
    }

    // ─── Normal mod – soru üretimi ────────────────────────────────────────────
    private fun generateQuestion() {
        val hour   = (1..12).random()
        val minute = randomMinute()
        val train  = TrainCharacter.random()

        val style = if (_gameState.value.levelMode == TrainLevelMode.QUARTER_HOURS)
            (0..1).random() else 0

        _gameState.value = _gameState.value.copy(
            currentHour                 = hour,
            currentMinute               = minute,
            currentTrain                = train,
            options                     = generateTextOptions(hour, minute),
            questionStyle               = style,
            showCelebration             = false,
            showCorrectAnswerAfterWrong = false,
            correctAnswerToShow         = null,
            trainIsHappy                = true,
            testWagonOptions            = emptyList(),
            testCorrectWagonShown       = null
        )
    }

    private fun randomMinute(): Int = when (_gameState.value.levelMode) {
        TrainLevelMode.HALF_HOURS    -> 30
        TrainLevelMode.QUARTER_HOURS -> if ((0..1).random() == 0) 15 else 45
    }

    private fun generateTextOptions(correctHour: Int, correctMinute: Int): List<String> {
        val ms      = correctMinute.toString().padStart(2, '0')
        val correct = "$correctHour:$ms"
        val opts    = mutableListOf(correct)

        while (opts.size < 3) {
            val wh = (1..12).random()
            val wm = when (_gameState.value.levelMode) {
                TrainLevelMode.HALF_HOURS    -> if ((0..1).random() == 0) "00" else "30"
                TrainLevelMode.QUARTER_HOURS -> if ((0..1).random() == 0) "15" else "45"
            }
            val w = "$wh:$wm"
            if (w !in opts) opts.add(w)
        }
        return opts.shuffled()
    }

    // ─── Normal mod – cevap kontrolü ─────────────────────────────────────────
    private fun checkAnswer(selected: String) {
        val ms        = _gameState.value.currentMinute.toString().padStart(2, '0')
        val isCorrect = selected == "${_gameState.value.currentHour}:$ms"
        handleNormalAnswer(isCorrect)
    }

    private fun handleNormalAnswer(isCorrect: Boolean) {
        if (isCorrect) {
            val newScore      = _gameState.value.score + 10
            val newCorrect    = _gameState.value.correctAnswers + 1
            val newWagonCount = (_gameState.value.wagonCount + 1) % (MAX_WAGONS + 1)

            _gameState.value = _gameState.value.copy(
                score           = newScore,
                correctAnswers  = newCorrect,
                wagonCount      = newWagonCount,
                showCelebration = true,
                trainIsHappy    = true
            )

            // Anında kaydet – geri basılmasa da korunsun
            when (_gameState.value.levelMode) {
                TrainLevelMode.HALF_HOURS    -> { if (newScore > gamePrefs.level2BestScore) gamePrefs.level2BestScore = newScore }
                TrainLevelMode.QUARTER_HOURS -> { if (newScore > gamePrefs.level3BestScore) gamePrefs.level3BestScore = newScore }
            }

            viewModelScope.launch {
                delay(1500)
                // Her iki level için 300 puana ulaşınca test hazır diyaloğu
                val testThreshold = 300
                if (newScore >= testThreshold && !_gameState.value.isTestMode) {
                    _gameState.value = _gameState.value.copy(
                        showTestReadyDialog = true,
                        showCelebration     = false
                    )
                } else {
                    generateQuestion()
                }
            }
        } else {
            val newScore = maxOf(0, _gameState.value.score - 2)
            _gameState.value = _gameState.value.copy(
                score           = newScore,
                showWrongAnswer = true,
                trainIsHappy    = false
            )
            viewModelScope.launch {
                delay(1200)
                _gameState.value = _gameState.value.copy(
                    showWrongAnswer = false,
                    trainIsHappy    = true
                )
            }
        }
    }

    // ─── Test modu – başlatma ────────────────────────────────────────────────
    private fun dismissTestReadyDialog() {
        _gameState.value = _gameState.value.copy(showTestReadyDialog = false)
        generateQuestion()
    }

    private fun acceptTestChallenge() {
        _gameState.value = _gameState.value.copy(showTestReadyDialog = false)
        startTest()
    }

    private fun startTest() {
        _gameState.value = _gameState.value.copy(
            isTestMode   = true,
            testQuestion = 0,
            testCorrect  = 0,
            showResult   = false,
            wagonCount   = 0
        )
        generateTestQuestion()
    }

    // ─── Test modu – vagon seçme sorusu ─────────────────────────────────────
    private fun generateTestQuestion() {
        val hour   = (1..12).random()
        val minute = randomMinute()
        val train  = TrainCharacter.random()
        val style  = if (_gameState.value.levelMode == TrainLevelMode.QUARTER_HOURS)
            (0..1).random() else 0

        _gameState.value = _gameState.value.copy(
            currentHour                 = hour,
            currentMinute               = minute,
            currentTrain                = train,
            questionStyle               = style,
            testWagonOptions            = generateWagonHourOptions(hour),
            testCorrectHour             = hour,
            testCorrectWagonShown       = null,
            showCelebration             = false,
            testTimeRemaining           = 5,
            showCorrectAnswerAfterWrong = false,
            correctAnswerToShow         = null,
            trainIsHappy                = true
        )
        startTimer()
    }

    private fun generateWagonHourOptions(correctHour: Int): List<Int> {
        val opts = mutableListOf(correctHour)
        while (opts.size < 3) {
            val w = (1..12).random()
            if (w !in opts) opts.add(w)
        }
        return opts.shuffled()
    }

    // ─── Test modu – vagon cevap kontrolü ───────────────────────────────────
    private fun checkWagonAnswer(selectedHour: Int) {
        stopTimer()
        val isCorrect = selectedHour == _gameState.value.testCorrectHour

        if (isCorrect) {
            val newTestCorrect  = _gameState.value.testCorrect + 1
            val newTestQuestion = _gameState.value.testQuestion + 1
            val newWagonCount   = minOf(_gameState.value.wagonCount + 1, MAX_WAGONS)

            _gameState.value = _gameState.value.copy(
                testCorrect     = newTestCorrect,
                testQuestion    = newTestQuestion,
                wagonCount      = newWagonCount,
                showCelebration = true,
                trainIsHappy    = true
            )

            viewModelScope.launch {
                delay(1200)
                if (newTestQuestion >= 10) showTestResult() else generateTestQuestion()
            }
        } else {
            val newTestQuestion = _gameState.value.testQuestion + 1
            _gameState.value = _gameState.value.copy(
                testQuestion                = newTestQuestion,
                showCorrectAnswerAfterWrong = true,
                testCorrectWagonShown       = _gameState.value.testCorrectHour,
                trainIsHappy                = false
            )

            viewModelScope.launch {
                delay(2000)
                if (newTestQuestion >= 10) showTestResult() else generateTestQuestion()
            }
        }
    }

    // ─── Süre doldu ──────────────────────────────────────────────────────────
    private fun handleTimeExpired() {
        stopTimer()
        val newTestQuestion = _gameState.value.testQuestion + 1
        _gameState.value = _gameState.value.copy(
            testQuestion                = newTestQuestion,
            showCorrectAnswerAfterWrong = true,
            testCorrectWagonShown       = _gameState.value.testCorrectHour,
            trainIsHappy                = false
        )
        viewModelScope.launch {
            delay(2000)
            if (newTestQuestion >= 10) showTestResult() else generateTestQuestion()
        }
    }

    // ─── Test sonucu ─────────────────────────────────────────────────────────
    private fun showTestResult() {
        stopTimer()
        val passed = _gameState.value.testCorrect >= 7

        // Level 2 testini geçtiyse Level 3'ü aç
        if (passed && _gameState.value.levelMode == TrainLevelMode.HALF_HOURS) {
            gamePrefs.hasPassedLevel2 = true
        }

        _gameState.value = _gameState.value.copy(
            showResult           = true,
            testPassed           = passed,
            showCelebration      = passed,
            showVictoryAnimation = passed
        )
    }

    // ─── Tekrar dene / devam et ───────────────────────────────────────────────
    private fun retryTest() {
        stopTimer()
        _gameState.value = _gameState.value.copy(
            isTestMode   = false,
            testQuestion = 0,
            testCorrect  = 0,
            showResult   = false,
            wagonCount   = 0
        )
        generateQuestion()
    }

    private fun continueAfterTest() {
        stopTimer()
        _gameState.value = _gameState.value.copy(
            isTestMode           = false,
            testQuestion         = 0,
            testCorrect          = 0,
            showResult           = false,
            showVictoryAnimation = false
        )
        generateQuestion()
    }

    private fun resetGame() {
        stopTimer()
        val mode = _gameState.value.levelMode
        _gameState.value = TrainGameState(levelMode = mode)
        generateQuestion()
    }

    // ─── Geçmişe kaydet ──────────────────────────────────────────────────────
    fun saveSessionToHistory() {
        val score = _gameState.value.score
        if (score <= 0) return
        val level  = if (_gameState.value.levelMode == TrainLevelMode.HALF_HOURS) 2 else 3
        val passed = _gameState.value.testPassed
        gamePrefs.addToHistory(
            entry    = ScoreEntry(level = level, score = score, passed = passed, timestamp = System.currentTimeMillis()),
            moduleId = "clock_reading"
        )
        when (_gameState.value.levelMode) {
            TrainLevelMode.HALF_HOURS    -> { if (score > gamePrefs.level2BestScore) gamePrefs.level2BestScore = score }
            TrainLevelMode.QUARTER_HOURS -> { if (score > gamePrefs.level3BestScore) gamePrefs.level3BestScore = score }
        }
    }

    /** MainActivity çağırır – seviye başında sıfırdan başlatır */
    fun startFresh(mode: TrainLevelMode = TrainLevelMode.HALF_HOURS) {
        stopTimer()
        _gameState.value = TrainGameState(levelMode = mode)
        generateQuestion()
    }

    // ─── Zamanlayıcı ─────────────────────────────────────────────────────────
    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            repeat(5) { second ->
                delay(1000)
                _gameState.value = _gameState.value.copy(testTimeRemaining = 4 - second)
            }
            onEvent(TrainGameEvent.TimeExpired)
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
