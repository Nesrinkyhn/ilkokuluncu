package com.ilkokuluncu.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ilkokuluncu.app.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BallGameViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(BallGameState())
    val state: StateFlow<BallGameState> = _state.asStateFlow()

    private val gamePrefs = GamePreferences(application)

    val validMinutes = listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)

    // 3 top için başlangıç pozisyonları — ekrana iyice yayılmış
    private val xStarts = listOf(0.04f, 0.40f, 0.75f)
    private val yStarts = listOf(0.08f, 0.65f, 0.25f)
    // Daha yavaş ve birbirinden farklı hızlar → toplar ayrı kalır
    private val xSpeeds = listOf(3800, 3100, 4200)
    private val ySpeeds = listOf(2900, 4000, 3400)

    init { newQuestion() }

    fun onEvent(event: BallGameEvent) {
        when (event) {
            is BallGameEvent.BallTapped          -> onBallTapped(event.ballId)
            is BallGameEvent.AcceptTestChallenge -> acceptTest()
            is BallGameEvent.DismissTestDialog   -> dismissTest()
            is BallGameEvent.RetryTest           -> retryTest()
            is BallGameEvent.BackToMenu          -> Unit
        }
    }

    // ─── Top tıklama yönlendirici ────────────────────────────────────────────
    private fun onBallTapped(ballId: Int) {
        val s = _state.value
        if (s.phase == BallPhase.AFERIN) return
        if (s.testShownCorrect != null) return           // yanlış sonrası bekleniyor
        val ball = s.balls.find { it.id == ballId } ?: return

        if (s.isTestMode) handleTestTap(ball) else handleNormalTap(ball)
    }

    // ─── Normal mod ──────────────────────────────────────────────────────────
    private fun newQuestion() {
        val hour   = (1..12).random()
        val minute = validMinutes.random()
        _state.value = _state.value.copy(
            currentHour   = hour,
            currentMinute = minute,
            phase         = BallPhase.HOUR,
            balls         = makeHourBalls(hour),
            showAferin    = false,
            wrongBallId   = null,
            bgIndex       = (_state.value.bgIndex + 1) % 8,
            questionCount = _state.value.questionCount + 1
        )
    }

    private fun handleNormalTap(ball: BallItem) {
        val s = _state.value
        if (ball.isCorrect) {
            when (s.phase) {
                BallPhase.HOUR -> {
                    // Akrep bulundu → yelkovan fazına geç
                    _state.value = s.copy(
                        score       = s.score + 5,
                        phase       = BallPhase.MINUTE,
                        balls       = makeMinuteBalls(s.currentMinute),
                        wrongBallId = null
                    )
                }
                BallPhase.MINUTE -> {
                    // Yelkovan da bulundu → Aferin!
                    val newScore = s.score + 10
                    _state.value = s.copy(
                        score       = newScore,
                        phase       = BallPhase.AFERIN,
                        balls       = emptyList(),
                        showAferin  = true,
                        wrongBallId = null
                    )
                    viewModelScope.launch {
                        delay(1800)
                        if (newScore >= 300) {
                            _state.value = _state.value.copy(
                                showAferin          = false,
                                showTestReadyDialog = true
                            )
                        } else {
                            newQuestion()
                        }
                    }
                }
                BallPhase.AFERIN -> Unit
            }
        } else {
            _state.value = s.copy(
                score       = maxOf(0, s.score - 2),
                wrongBallId = ball.id
            )
            viewModelScope.launch {
                delay(700)
                _state.value = _state.value.copy(wrongBallId = null)
            }
        }
    }

    // ─── Test modu ───────────────────────────────────────────────────────────
    private fun acceptTest() {
        _state.value = _state.value.copy(
            showTestReadyDialog = false,
            isTestMode          = true,
            testQuestion        = 0,
            testCorrect         = 0,
            showTestResult      = false,
            testShownCorrect    = null
        )
        generateTestQuestion()
    }

    private fun dismissTest() {
        _state.value = _state.value.copy(showTestReadyDialog = false)
        newQuestion()
    }

    private fun retryTest() {
        _state.value = _state.value.copy(
            isTestMode       = false,
            testQuestion     = 0,
            testCorrect      = 0,
            showTestResult   = false,
            testPassed       = false,
            testShownCorrect = null,
            score            = 0
        )
        newQuestion()
    }

    private fun generateTestQuestion() {
        val hour   = (1..12).random()
        val minute = validMinutes.random()
        _state.value = _state.value.copy(
            currentHour      = hour,
            currentMinute    = minute,
            phase            = BallPhase.HOUR,
            balls            = makeHourBalls(hour),
            testShownCorrect = null,
            wrongBallId      = null,
            bgIndex          = (_state.value.bgIndex + 1) % 8
        )
    }

    private fun handleTestTap(ball: BallItem) {
        val s = _state.value
        if (ball.isCorrect) {
            when (s.phase) {
                BallPhase.HOUR -> {
                    // Akrep bulundu → yelkovan fazına geç (puan yok, hayat yok)
                    _state.value = s.copy(
                        phase            = BallPhase.MINUTE,
                        balls            = makeMinuteBalls(s.currentMinute),
                        wrongBallId      = null,
                        testShownCorrect = null
                    )
                }
                BallPhase.MINUTE -> {
                    // İkisi de bulundu → soru doğru
                    val newCorrect = s.testCorrect + 1
                    val newQ       = s.testQuestion + 1
                    _state.value = s.copy(
                        testCorrect      = newCorrect,
                        testQuestion     = newQ,
                        wrongBallId      = null,
                        testShownCorrect = null
                    )
                    viewModelScope.launch {
                        delay(600)
                        if (newQ >= 10) finishTest() else generateTestQuestion()
                    }
                }
                BallPhase.AFERIN -> Unit
            }
        } else {
            // Yanlış → doğruyu göster, soruyu say (yanlış)
            val correctBallId = s.balls.find { it.isCorrect }?.id
            val newQ          = s.testQuestion + 1
            _state.value = s.copy(
                wrongBallId      = ball.id,
                testShownCorrect = correctBallId,
                testQuestion     = newQ
            )
            viewModelScope.launch {
                delay(1500)
                if (newQ >= 10) finishTest() else generateTestQuestion()
            }
        }
    }

    private fun finishTest() {
        val passed = _state.value.testCorrect >= 7
        if (passed) gamePrefs.hasPassedLevel4 = true
        saveSession(passed)
        _state.value = _state.value.copy(
            showTestResult   = true,
            testPassed       = passed,
            isTestMode       = false,
            testShownCorrect = null
        )
    }

    // ─── Top üreticiler ──────────────────────────────────────────────────────
    private fun makeHourBalls(correct: Int): List<BallItem> {
        val opts = mutableListOf(correct)
        while (opts.size < 3) { val h = (1..12).random(); if (h !in opts) opts.add(h) }
        opts.shuffle()
        return opts.mapIndexed { i, h ->
            BallItem(i, "$h", h == correct, i, xStarts[i], yStarts[i], xSpeeds[i], ySpeeds[i])
        }
    }

    private fun makeMinuteBalls(correct: Int): List<BallItem> {
        val opts = mutableListOf(correct)
        while (opts.size < 3) { val m = validMinutes.random(); if (m !in opts) opts.add(m) }
        opts.shuffle()
        return opts.mapIndexed { i, m ->
            BallItem(
                id         = i,
                label      = "$m",
                isCorrect  = m == correct,
                colorIndex = i + 3,
                xStart     = listOf(0.05f, 0.42f, 0.76f)[i],
                yStart     = listOf(0.60f, 0.10f, 0.70f)[i],
                xSpeed     = listOf(3300, 4100, 3700)[i],
                ySpeed     = listOf(4000, 3200, 4400)[i]
            )
        }
    }

    fun saveSession(passed: Boolean = _state.value.testPassed) {
        val score = _state.value.score
        if (score <= 0) return
        gamePrefs.addToHistory(
            entry    = ScoreEntry(level = 4, score = score, passed = passed, timestamp = System.currentTimeMillis()),
            moduleId = "clock_reading"
        )
        if (score > gamePrefs.level4BestScore) gamePrefs.level4BestScore = score
    }
}
