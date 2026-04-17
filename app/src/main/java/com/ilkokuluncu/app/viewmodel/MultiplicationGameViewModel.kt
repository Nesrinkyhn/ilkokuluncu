package com.ilkokuluncu.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ilkokuluncu.app.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MultiplicationGameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("mult_game_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        MultiplicationGameState(bestScore = prefs.getInt("best_score", 0))
    )
    val state: StateFlow<MultiplicationGameState> = _state.asStateFlow()

    private var timerJob: Job? = null

    // Çarpma tablosu: 2..9, ama ikisi birden 6+ olamaz
    private val fullRange  = (2..9).toList()
    private val smallRange = (2..5).toList()   // en az biri bu aralıkta olmalı

    fun startFresh() {
        stopTimer()
        _state.value = MultiplicationGameState(bestScore = prefs.getInt("best_score", 0))
        generatePickFirst()
    }

    // ── Event yönlendirici ────────────────────────────────────────────────────
    fun onEvent(event: MultiplicationGameEvent) {
        when (event) {
            is MultiplicationGameEvent.FloatingBallTapped -> onFloatingBallTapped(event.ballId)
            is MultiplicationGameEvent.AnswerBallTapped   -> onAnswerBallTapped(event.ballId)
            is MultiplicationGameEvent.TimeExpired        -> handleTimeExpired()
            is MultiplicationGameEvent.RestartGame        -> startFresh()
            is MultiplicationGameEvent.BackToMenu         -> Unit
        }
    }

    // ── Faz 1: Birinci çarpan seçimi ─────────────────────────────────────────
    private fun generatePickFirst() {
        _state.value = _state.value.copy(
            phase              = MultiplicationPhase.PICK_FIRST,
            firstNumber        = null,
            secondNumber       = null,
            floatingBalls      = makeFloatingBalls(),
            answerBalls        = emptyList(),
            wrongBallId        = null,
            correctFlashBallId = null,
            timeLeft           = 10f
        )
    }

    // ── Faz 2: İkinci çarpan seçimi ──────────────────────────────────────────
    private fun generatePickSecond() {
        // Eğer birinci sayı > 5 ise ikinci sayı 2..5 olmak zorunda
        val first = _state.value.firstNumber ?: 2
        val pool  = if (first > 5) smallRange else fullRange
        _state.value = _state.value.copy(
            phase         = MultiplicationPhase.PICK_SECOND,
            secondNumber  = null,
            floatingBalls = makeFloatingBalls(pool),
            answerBalls   = emptyList(),
            wrongBallId   = null
        )
    }

    // ── Faz 3: Cevap seçimi ───────────────────────────────────────────────────
    private fun generateAnswerPhase() {
        val first  = _state.value.firstNumber!!
        val second = _state.value.secondNumber!!
        val correct = first * second

        _state.value = _state.value.copy(
            phase         = MultiplicationPhase.ANSWER,
            floatingBalls = emptyList(),
            answerBalls   = makeAnswerBalls(correct),
            timeLeft      = 10f
        )
        startTimer()
    }

    // ── Uçan balonlara tıklama ────────────────────────────────────────────────
    private fun onFloatingBallTapped(ballId: Int) {
        val s    = _state.value
        val ball = s.floatingBalls.find { it.id == ballId } ?: return

        when (s.phase) {
            MultiplicationPhase.PICK_FIRST -> {
                _state.value = s.copy(firstNumber = ball.value)
                generatePickSecond()
            }
            MultiplicationPhase.PICK_SECOND -> {
                _state.value = s.copy(secondNumber = ball.value)
                generateAnswerPhase()
            }
            else -> Unit
        }
    }

    // ── Cevap balonuna tıklama ────────────────────────────────────────────────
    private fun onAnswerBallTapped(ballId: Int) {
        val s = _state.value
        if (s.phase != MultiplicationPhase.ANSWER) return
        val ball = s.answerBalls.find { it.id == ballId } ?: return
        stopTimer()

        val newQ = s.questionCount + 1

        if (ball.isCorrect) {
            // Süreye göre puan: 10 taban + 0..10 bonus
            val bonus    = (s.timeLeft.coerceIn(0f, 10f)).toInt()
            val newScore = s.score + 10 + bonus

            _state.value = s.copy(
                score              = newScore,
                correctFlashBallId = ballId,
                questionCount      = newQ
            )
            viewModelScope.launch {
                delay(600)
                if (newQ >= 50) finishGame() else generatePickFirst()
            }
        } else {
            val newLives = s.lives - 1
            _state.value = s.copy(
                lives         = newLives,
                wrongBallId   = ballId,
                questionCount = newQ
            )
            viewModelScope.launch {
                delay(800)
                when {
                    newLives <= 0 -> _state.value = _state.value.copy(phase = MultiplicationPhase.GAME_OVER)
                    newQ >= 50   -> finishGame()
                    else         -> generatePickFirst()
                }
            }
        }
    }

    // ── Süre doldu ────────────────────────────────────────────────────────────
    private fun handleTimeExpired() {
        val s = _state.value
        if (s.phase != MultiplicationPhase.ANSWER) return
        stopTimer()

        val newLives = s.lives - 1
        val newQ     = s.questionCount + 1

        _state.value = s.copy(lives = newLives, questionCount = newQ, timeLeft = 0f)

        viewModelScope.launch {
            delay(600)
            when {
                newLives <= 0 -> _state.value = _state.value.copy(phase = MultiplicationPhase.GAME_OVER)
                newQ >= 50   -> finishGame()
                else         -> generatePickFirst()
            }
        }
    }

    // ── Oyun bitti (50 soru tamamlandı) ──────────────────────────────────────
    private fun finishGame() {
        val score     = _state.value.score
        val prevBest  = prefs.getInt("best_score", 0)
        val isNewBest = score > prevBest
        if (isNewBest) prefs.edit().putInt("best_score", score).apply()

        _state.value = _state.value.copy(
            phase     = MultiplicationPhase.VICTORY,
            bestScore = if (isNewBest) score else prevBest,
            isNewBest = isNewBest
        )
    }

    // ── Timer ─────────────────────────────────────────────────────────────────
    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            repeat(100) {
                delay(100)
                val cur = _state.value.timeLeft
                if (cur <= 0.05f) {
                    onEvent(MultiplicationGameEvent.TimeExpired)
                    return@launch
                }
                _state.value = _state.value.copy(timeLeft = (cur - 0.1f).coerceAtLeast(0f))
            }
            onEvent(MultiplicationGameEvent.TimeExpired)
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // ── Balon üreticiler ──────────────────────────────────────────────────────
    private val xStartPool = listOf(0.05f, 0.35f, 0.68f, 0.15f, 0.52f, 0.82f)
    private val yStartPool = listOf(0.12f, 0.55f, 0.22f, 0.70f, 0.38f, 0.62f)
    private val xSpeeds    = listOf(3400, 4000, 2900, 3700, 4300, 3100)
    private val ySpeeds    = listOf(2800, 3800, 4100, 3200, 4400, 3000)

    private fun makeFloatingBalls(pool: List<Int> = fullRange): List<MultBall> {
        val nums = pool.shuffled().take(minOf(6, pool.size))
        return nums.mapIndexed { i, n ->
            MultBall(
                id         = i,
                value      = n,
                colorIndex = i,
                xStart     = xStartPool[i],
                yStart     = yStartPool[i],
                xSpeed     = xSpeeds[i],
                ySpeed     = ySpeeds[i]
            )
        }
    }

    private fun makeAnswerBalls(correct: Int): List<MultBall> {
        val wrongs = mutableListOf<Int>()
        val attempts = mutableSetOf(correct)
        while (wrongs.size < 2) {
            val offset = listOf(-12, -9, -6, -3, 3, 6, 9, 12).random()
            val w = correct + offset
            if (w > 0 && w !in attempts) { wrongs.add(w); attempts.add(w) }
        }
        val options  = (listOf(correct) + wrongs).shuffled()
        val xStarts  = listOf(0.08f, 0.38f, 0.68f)
        val yStarts  = listOf(0.60f, 0.52f, 0.60f)
        val colorIdxs = listOf(2, 5, 8)

        return options.mapIndexed { i, v ->
            MultBall(
                id         = 100 + i,
                value      = v,
                colorIndex = colorIdxs[i],
                xStart     = xStarts[i],
                yStart     = yStarts[i],
                xSpeed     = 5500 + i * 800,
                ySpeed     = 4800 + i * 700,
                isCorrect  = v == correct
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
