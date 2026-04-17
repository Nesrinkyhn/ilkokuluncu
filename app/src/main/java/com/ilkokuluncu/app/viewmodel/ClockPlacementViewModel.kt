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
import kotlin.math.abs

class ClockPlacementViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ClockPlacementState())
    val state: StateFlow<ClockPlacementState> = _state.asStateFlow()

    private val prefs = GamePreferences(application)

    private val validMinutes  = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
    private val HOUR_TOL      = 25f   // ±25° akrep toleransı
    private val MINUTE_TOL    = 20f   // ±20° yelkovan toleransı

    private var timerJob: Job? = null

    init {
        _state.value = _state.value.copy(bestScore = prefs.clockPlacementBestScore)
        nextQuestion()
    }

    fun onEvent(event: ClockPlacementEvent) {
        when (event) {
            is ClockPlacementEvent.AngleDragged -> onAngleDragged(event.angle)
            is ClockPlacementEvent.StartFresh   -> startFresh()
        }
    }

    fun startFresh() {
        timerJob?.cancel()
        _state.value = ClockPlacementState(bestScore = prefs.clockPlacementBestScore)
        nextQuestion()
    }

    // ─── Parmak sürükleme ────────────────────────────────────────────────────
    private fun onAngleDragged(angle: Float) {
        val s = _state.value
        if (s.phase != PlacementPhase.PLACING) return
        _state.value = s.copy(activeHandAngle = angle)

        val target    = targetAngle(s)
        val tolerance = if (s.isHourPhase) HOUR_TOL else MINUTE_TOL
        if (angularDist(angle, target) <= tolerance) {
            if (s.isHourPhase) onHourCorrect(target) else onBothCorrect(target)
        }
    }

    // ─── Akrep doğru → yelkovan fazına geç ──────────────────────────────────
    private fun onHourCorrect(snapAngle: Float) {
        val s = _state.value
        // Akrep snap'lenir; yelkovanın yanlış pozisyonu aktif el olur
        _state.value = s.copy(
            activeHandAngle = s.otherHandAngle,  // yelkovan yanlış pozisyonu
            otherHandAngle  = snapAngle,          // akrep doğru pozisyonda sabit
            isHourPhase     = false
        )
        // Timer durmadan devam eder
    }

    // ─── İkisi de doğru → puan kazan ────────────────────────────────────────
    private fun onBothCorrect(snapAngle: Float) {
        timerJob?.cancel()
        val s        = _state.value
        val points   = (5f * s.timeLeft).toInt().coerceAtLeast(1)
        val newScore = s.score + points
        val isRecord = newScore > s.bestScore
        if (isRecord) prefs.clockPlacementBestScore = newScore

        _state.value = s.copy(
            activeHandAngle = snapAngle,
            phase           = PlacementPhase.CORRECT,
            score           = newScore,
            bestScore       = if (isRecord) newScore else s.bestScore,
            newRecord       = isRecord,
            lastPoints      = points
        )
        viewModelScope.launch {
            delay(900)
            nextQuestion()
        }
    }

    // ─── Süre doldu ──────────────────────────────────────────────────────────
    private fun onTimeout() {
        timerJob?.cancel()
        val s        = _state.value
        val newLives = s.lives - 1
        _state.value = s.copy(
            phase    = PlacementPhase.TIMEOUT,
            timeLeft = 0f,
            lives    = newLives
        )
        viewModelScope.launch {
            delay(900)
            if (newLives <= 0) {
                _state.value = _state.value.copy(showGameOver = true)
            } else {
                nextQuestion()
            }
        }
    }

    // ─── Yeni soru ───────────────────────────────────────────────────────────
    private fun nextQuestion() {
        val hour   = (1..12).random()
        val minute = validMinutes.random()

        val hourCorrectAngle   = (hour % 12) * 30f + minute * 0.5f
        val minuteCorrectAngle = minute * 6f

        // Akrep yanlış pozisyonu: kendi doğrusundan uzak
        val wrongHour = randomWrongAngle(hourCorrectAngle, minDiff = 60f)

        // Yelkovan yanlış pozisyonu: hem kendi doğrusundan hem akrebin
        // doğru pozisyonundan uzak — akrep yerleşince yanına gelmesin
        var wrongMinute: Float
        do { wrongMinute = (Math.random() * 360).toFloat() }
        while (angularDist(wrongMinute, minuteCorrectAngle) < 60f ||
               angularDist(wrongMinute, hourCorrectAngle)   < 55f)

        _state.value = _state.value.copy(
            targetHour      = hour,
            targetMinute    = minute,
            isHourPhase     = true,         // her zaman önce akrep
            activeHandAngle = wrongHour,    // kullanıcı önce akrebi yerleştirir
            otherHandAngle  = wrongMinute,  // yelkovan yanlış pozisyonda bekler
            phase           = PlacementPhase.PLACING,
            timeLeft        = 10f,
            newRecord       = false,
            lastPoints      = 0,
            questionCount   = _state.value.questionCount + 1,
            bgIndex         = (_state.value.bgIndex + 1) % 8
        )
        startTimer()
    }

    // ─── Zamanlayıcı ─────────────────────────────────────────────────────────
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(100)
                val s = _state.value
                if (s.phase != PlacementPhase.PLACING) break
                val newTime = (s.timeLeft - 0.1f).coerceAtLeast(0f)
                _state.value = s.copy(timeLeft = newTime)
                if (newTime <= 0f) { onTimeout(); break }
            }
        }
    }

    // ─── Yardımcılar ─────────────────────────────────────────────────────────
    private fun targetAngle(s: ClockPlacementState): Float =
        if (s.isHourPhase) ((s.targetHour % 12) * 30f + s.targetMinute * 0.5f + 360f) % 360f
        else (s.targetMinute * 6f + 360f) % 360f

    private fun angularDist(a: Float, b: Float): Float {
        val d = ((a - b + 360f) % 360f)
        return if (d > 180f) 360f - d else d
    }

    private fun randomWrongAngle(correct: Float, minDiff: Float): Float {
        var angle: Float
        do { angle = (Math.random() * 360).toFloat() }
        while (angularDist(angle, correct) < minDiff)
        return angle
    }
}
