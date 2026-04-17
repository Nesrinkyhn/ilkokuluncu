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

private const val VISIBLE_COUNT   = 5
private const val GOAL_CORRECT    = 50
private const val MAX_WRONG       = 5
private const val BASE_POINTS     = 10
private const val TOTAL_TIME_SECS = 750f   // 50 soru × 15 sn
private const val TIMER_STEP_MS   = 100L

class MatchGameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs  = GamePreferences(application)
    private val _state = MutableStateFlow(MatchGameState())
    val state: StateFlow<MatchGameState> = _state.asStateFlow()

    private var nextId     = 0
    private var timerJob: Job? = null

    // ── Başlat ────────────────────────────────────────────────────────────────
    fun startFresh() {
        timerJob?.cancel()
        nextId = 0
        val pool    = buildPool(GOAL_CORRECT)
        val initial = pool.take(VISIBLE_COUNT)
        val pending = pool.drop(VISIBLE_COUNT)
        _state.value = MatchGameState(
            labelItems    = initial,
            clockItems    = initial.shuffled(),
            pendingPool   = pending,
            bestScore     = prefs.matchBestScore,
            totalTimeLeft = TOTAL_TIME_SECS
        )
        startGameTimer()
    }

    // ── Toplam oyun sayacı ────────────────────────────────────────────────────
    private fun startGameTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(TIMER_STEP_MS)
                val s = _state.value
                if (s.isGameOver || s.isVictory) break
                val newTime = (s.totalTimeLeft - TIMER_STEP_MS / 1000f).coerceAtLeast(0f)
                if (newTime <= 0f) {
                    _state.value = s.copy(totalTimeLeft = 0f, isGameOver = true)
                    break
                }
                _state.value = s.copy(totalTimeLeft = newTime)
            }
        }
    }

    // ── Event yöneticisi ──────────────────────────────────────────────────────
    fun onEvent(event: MatchGameEvent) {
        when (event) {
            is MatchGameEvent.SelectLabel -> selectLabel(event.id)
            is MatchGameEvent.SelectClock -> selectClock(event.id)
            MatchGameEvent.RestartGame    -> startFresh()
            MatchGameEvent.BackToMenu     -> timerJob?.cancel()
        }
    }

    // ── Etiket seçimi ─────────────────────────────────────────────────────────
    private fun selectLabel(id: Int) {
        val s = _state.value
        if (s.isGameOver || s.isVictory) return
        _state.value = s.copy(
            selectedLabelId = if (s.selectedLabelId == id) null else id,
            labelSelectedMs = System.currentTimeMillis()
        )
    }

    // ── Kadran seçimi ─────────────────────────────────────────────────────────
    private fun selectClock(clockId: Int) {
        val s       = _state.value
        val labelId = s.selectedLabelId ?: return
        if (s.isGameOver || s.isVictory) return

        val isCorrect = labelId == clockId

        if (isCorrect) {
            // Önce yeşil flash göster, 350 ms sonra kartı kaldır
            _state.value = s.copy(
                correctFlashIds = Pair(labelId, clockId),
                selectedLabelId = null,
                wrongFlashIds   = null
            )
            viewModelScope.launch {
                delay(350)
                val cur = _state.value

                val elapsedSec = (System.currentTimeMillis() - cur.labelSelectedMs) / 1000f
                val timeBonus  = (BASE_POINTS - elapsedSec).coerceIn(0f, BASE_POINTS.toFloat()).toInt()
                val gained     = BASE_POINTS + timeBonus
                val newCorrect = cur.totalCorrect + 1
                val newScore   = cur.score + gained

                var newLabels = cur.labelItems.filter { it.id != labelId }
                var newClocks = cur.clockItems.filter { it.id != clockId }
                var newPool   = cur.pendingPool

                if (newPool.isNotEmpty()) {
                    val next  = newPool.first()
                    newPool   = newPool.drop(1)
                    newLabels = newLabels + next
                    val insertAt = (0..newClocks.size).random()
                    newClocks = newClocks.toMutableList().also { it.add(insertAt, next) }
                }

                val isVictory = newCorrect >= GOAL_CORRECT
                val newBest   = if (newScore > cur.bestScore) newScore else cur.bestScore
                if (isVictory) {
                    timerJob?.cancel()
                    if (newBest > prefs.matchBestScore) prefs.matchBestScore = newBest
                }

                _state.value = cur.copy(
                    labelItems      = newLabels,
                    clockItems      = newClocks,
                    pendingPool     = newPool,
                    correctFlashIds = null,
                    totalCorrect    = newCorrect,
                    score           = newScore,
                    bestScore       = newBest,
                    isVictory       = isVictory
                )
            }
        } else {
            val newWrong = s.wrongCount + 1
            val newScore = (s.score - 5).coerceAtLeast(0)
            val isGameOver = newWrong >= MAX_WRONG
            if (isGameOver) timerJob?.cancel()

            _state.value = s.copy(
                wrongFlashIds   = Pair(labelId, clockId),
                selectedLabelId = null,
                wrongCount      = newWrong,
                score           = newScore,
                isGameOver      = isGameOver
            )
            viewModelScope.launch {
                delay(700)
                if (_state.value.wrongFlashIds == Pair(labelId, clockId)) {
                    _state.value = _state.value.copy(wrongFlashIds = null)
                }
            }
        }
    }

    // ── Soru havuzu ──────────────────────────────────────────────────────────
    private fun buildPool(count: Int): List<MatchItem> {
        val allPairs = buildList {
            for (type in TimeType.values())
                for (hour in 1..12)
                    add(type to hour)
        }.shuffled().toMutableList()

        val result   = mutableListOf<MatchItem>()
        var colorIdx = 0

        for ((type, hour) in allPairs) {
            if (result.size >= count) break
            result += makeItem(hour, type, colorIdx++)
        }
        var ri = 0
        while (result.size < count) {
            val (type, hour) = allPairs[ri++ % allPairs.size]
            result += makeItem(hour, type, colorIdx++)
        }
        return result.shuffled()
    }

    private fun makeItem(hour: Int, type: TimeType, colorCounter: Int) = MatchItem(
        id       = nextId++,
        hour     = hour,
        minute   = when (type) {
            TimeType.FULL_HOUR    -> 0
            TimeType.HALF_HOUR    -> 30
            TimeType.QUARTER_PAST -> 15
            TimeType.QUARTER_TO   -> 45
        },
        type     = type,
        colorIdx = colorCounter % 10
    )
}
