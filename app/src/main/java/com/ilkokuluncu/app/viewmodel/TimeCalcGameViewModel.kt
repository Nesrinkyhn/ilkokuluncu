package com.ilkokuluncu.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ilkokuluncu.app.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val GOAL_SCORE    = 500
private const val TIMER_SECS    = 10f
private const val BASE_POINTS   = 10
private const val TIME_BONUS    = 10   // max ekstra
private const val WRONG_PENALTY = 4

class TimeCalcGameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = GamePreferences(application)

    private val _state = MutableStateFlow(TimeCalcGameState())
    val state: StateFlow<TimeCalcGameState> = _state.asStateFlow()

    private var timerJob: Job? = null

    fun startFresh() {
        timerJob?.cancel()
        _state.value = TimeCalcGameState(
            bestScore = prefs.timeCalcBestScore,
            lives     = 3,
            score     = 0
        )
        generateQuestion()
    }

    private fun generateQuestion() {
        val hour   = (1..12).random()
        val minute = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55).random()
        val offset = TimeOffset.values().toList().random()

        val correct = addMinutes(hour, minute, offset.minutes)
        val opts    = generateOptions(correct)

        _state.value = _state.value.copy(
            clockHour      = hour,
            clockMinute    = minute,
            offset         = offset,
            correctAnswer  = correct,
            options        = opts,
            selectedAnswer = null,
            timeLeft       = TIMER_SECS,
            phase          = TimeCalcPhase.QUESTION,
            lastCorrect    = null,
            pointsDelta    = 0
        )
        startTimer()
    }

    private fun addMinutes(h: Int, m: Int, deltaMin: Int): Pair<Int, Int> {
        val totalMin = h * 60 + m + deltaMin
        val wrapped  = ((totalMin % (12 * 60)) + 12 * 60) % (12 * 60)
        val rh = wrapped / 60
        val rm = wrapped % 60
        return (if (rh == 0) 12 else rh) to rm
    }

    private fun generateOptions(correct: Pair<Int,Int>): List<Pair<Int,Int>> {
        val opts = mutableListOf(correct)
        val deltas = listOf(-60, -45, -30, -15, 15, 30, 45, 60, 90, -90).shuffled()
        for (d in deltas) {
            if (opts.size == 3) break
            val candidate = addMinutes(correct.first, correct.second, d)
            if (candidate !in opts) opts += candidate
        }
        while (opts.size < 3) {
            val candidate = addMinutes(correct.first, correct.second, listOf(20, -20, 40, -40).random())
            if (candidate !in opts) opts += candidate
        }
        return opts.shuffled()
    }

    fun onEvent(event: TimeCalcEvent) {
        when (event) {
            is TimeCalcEvent.Answer      -> handleAnswer(event.hour, event.minute)
            is TimeCalcEvent.NextQuestion -> {
                val s = _state.value
                if (s.phase == TimeCalcPhase.VICTORY || s.phase == TimeCalcPhase.GAME_OVER) return
                generateQuestion()
            }
            is TimeCalcEvent.RestartGame  -> startFresh()
            is TimeCalcEvent.TimeExpired  -> handleTimeExpired()
        }
    }

    private fun handleAnswer(h: Int, m: Int) {
        timerJob?.cancel()
        val s         = _state.value
        if (s.phase != TimeCalcPhase.QUESTION) return

        val selected  = h to m
        val isCorrect = selected == s.correctAnswer
        val timeFrac  = (s.timeLeft / TIMER_SECS).coerceIn(0f, 1f)
        val gained    = if (isCorrect) BASE_POINTS + (TIME_BONUS * timeFrac).toInt() else 0
        val penalty   = if (!isCorrect) WRONG_PENALTY else 0
        val newScore  = (s.score + gained - penalty).coerceAtLeast(0)
        val newLives  = if (!isCorrect) s.lives - 1 else s.lives

        val isVictory  = newScore >= GOAL_SCORE
        val isGameOver = newLives <= 0 && !isVictory

        val newRecord = isVictory && newScore > s.bestScore
        if (newRecord) prefs.timeCalcBestScore = newScore

        val nextPhase = when {
            isVictory  -> TimeCalcPhase.VICTORY
            isGameOver -> TimeCalcPhase.GAME_OVER
            else       -> TimeCalcPhase.FEEDBACK
        }

        _state.value = s.copy(
            score          = newScore,
            lives          = newLives,
            phase          = nextPhase,
            selectedAnswer = selected,
            lastCorrect    = isCorrect,
            pointsDelta    = if (isCorrect) gained else -penalty,
            questionCount  = s.questionCount + 1,
            newRecord      = newRecord,
            bestScore      = if (newRecord) newScore else s.bestScore
        )

        if (nextPhase == TimeCalcPhase.FEEDBACK) {
            viewModelScope.launch {
                delay(1100)
                generateQuestion()
            }
        }
    }

    private fun handleTimeExpired() {
        val s = _state.value
        if (s.phase != TimeCalcPhase.QUESTION) return
        timerJob?.cancel()
        val newLives = s.lives - 1
        val newScore = (s.score - WRONG_PENALTY).coerceAtLeast(0)
        val isGameOver = newLives <= 0

        _state.value = s.copy(
            score         = newScore,
            lives         = newLives,
            phase         = if (isGameOver) TimeCalcPhase.GAME_OVER else TimeCalcPhase.FEEDBACK,
            lastCorrect   = false,
            pointsDelta   = -WRONG_PENALTY,
            questionCount = s.questionCount + 1
        )

        if (!isGameOver) {
            viewModelScope.launch {
                delay(1100)
                generateQuestion()
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val steps = 100
            val stepMs = (TIMER_SECS * 1000 / steps).toLong()
            repeat(steps) {
                delay(stepMs)
                val cur = _state.value
                if (cur.phase == TimeCalcPhase.QUESTION) {
                    val newTime = cur.timeLeft - TIMER_SECS / steps
                    if (newTime <= 0f) {
                        onEvent(TimeCalcEvent.TimeExpired)
                        return@launch
                    }
                    _state.value = cur.copy(timeLeft = newTime)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
