package com.ilkokuluncu.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ilkokuluncu.app.data.GamePreferences
import com.ilkokuluncu.app.data.QuarterGameEvent
import com.ilkokuluncu.app.data.QuarterGameState
import com.ilkokuluncu.app.data.QuarterPhase
import com.ilkokuluncu.app.data.QuarterType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class QuarterGameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = GamePreferences(application)

    private val _state = MutableStateFlow(QuarterGameState(bestScore = prefs.quarterBestScore))
    val state: StateFlow<QuarterGameState> = _state.asStateFlow()

    private var timerJob: Job? = null

    init { startFresh() }

    // ── Olaylar ───────────────────────────────────────────────────────────
    fun onEvent(event: QuarterGameEvent) {
        when (event) {
            is QuarterGameEvent.AnswerTrue  -> answer(userSaysTrue = true)
            is QuarterGameEvent.AnswerFalse -> answer(userSaysTrue = false)
            is QuarterGameEvent.StartFresh  -> startFresh()
        }
    }

    // ── Sıfırdan başla ────────────────────────────────────────────────────
    fun startFresh() {
        timerJob?.cancel()
        _state.value = QuarterGameState(bestScore = prefs.quarterBestScore)
        generateQuestion()
    }

    // ── Kullanıcı cevapladı ───────────────────────────────────────────────
    private fun answer(userSaysTrue: Boolean) {
        val s = _state.value
        if (s.phase != QuarterPhase.ANSWERING) return
        timerJob?.cancel()

        val isCorrect  = (userSaysTrue == s.isClockCorrect)
        val delta      = if (isCorrect) (20 + (s.timeLeft * 3f).toInt()).coerceAtLeast(20) else -15
        val newScore   = (s.score + delta).coerceAtLeast(0)
        val newBest    = maxOf(newScore, s.bestScore)
        val newRecord  = newBest > prefs.quarterBestScore

        if (newRecord) prefs.quarterBestScore = newBest

        if (newScore >= 300) {
            // ── BAŞARILI ──────────────────────────────────────────────────
            _state.value = s.copy(
                score            = newScore,
                bestScore        = newBest,
                phase            = QuarterPhase.SUCCESS,
                lastAnswerCorrect = isCorrect,
                pointsDelta      = delta,
                newRecord        = newRecord
            )
        } else {
            // ── GERİ BİLDİRİM + sonraki soru ──────────────────────────────
            _state.value = s.copy(
                score            = newScore,
                bestScore        = newBest,
                phase            = QuarterPhase.FEEDBACK,
                lastAnswerCorrect = isCorrect,
                pointsDelta      = delta
            )
            viewModelScope.launch {
                delay(1400)
                if (_state.value.phase == QuarterPhase.FEEDBACK) generateQuestion()
            }
        }
    }

    // ── Soru üret ─────────────────────────────────────────────────────────
    private fun generateQuestion() {
        val hour      = (1..12).random()
        val type      = if (Random.nextBoolean()) QuarterType.PAST else QuarterType.TO
        val isCorrect = Random.nextBoolean()

        // Doğru saat konumu
        val (correctH, correctM) = when (type) {
            QuarterType.PAST -> Pair(hour, 15)
            QuarterType.TO   -> Pair(if (hour == 1) 12 else hour - 1, 45)
        }

        // Yanlışsa farklı bir saat göster
        val (clockH, clockM) = if (isCorrect) {
            Pair(correctH, correctM)
        } else {
            when (Random.nextInt(3)) {
                // Çeyrek tipini çevir: 15 ↔ 45
                0 -> Pair(correctH, if (correctM == 15) 45 else 15)
                // Bir sonraki saat, aynı dakika
                1 -> Pair(if (correctH == 12) 1 else correctH + 1, correctM)
                // Tam yarı saat göster
                else -> Pair(correctH, 30)
            }
        }

        _state.value = _state.value.copy(
            questionHour    = hour,
            questionType    = type,
            clockHour       = clockH,
            clockMinute     = clockM,
            isClockCorrect  = isCorrect,
            phase           = QuarterPhase.ANSWERING,
            lastAnswerCorrect = null,
            pointsDelta     = 0,
            timeLeft        = 10f,
            questionCount   = _state.value.questionCount + 1,
            bgIndex         = (_state.value.bgIndex + 1) % 8
        )
        startTimer()
    }

    // ── Geri sayım ────────────────────────────────────────────────────────
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(100)
                val s = _state.value
                if (s.phase != QuarterPhase.ANSWERING) break

                val newTime = (s.timeLeft - 0.1f).coerceAtLeast(0f)
                _state.value = s.copy(timeLeft = newTime)

                if (newTime <= 0f) {
                    // Süre doldu → yanlış kabul et
                    val newScore  = (s.score - 15).coerceAtLeast(0)
                    val newBest   = maxOf(newScore, s.bestScore)
                    if (newBest > prefs.quarterBestScore) prefs.quarterBestScore = newBest
                    _state.value = s.copy(
                        score            = newScore,
                        bestScore        = newBest,
                        phase            = QuarterPhase.FEEDBACK,
                        lastAnswerCorrect = false,
                        pointsDelta      = -15,
                        timeLeft         = 0f
                    )
                    delay(1400)
                    if (_state.value.phase == QuarterPhase.FEEDBACK) generateQuestion()
                    break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
