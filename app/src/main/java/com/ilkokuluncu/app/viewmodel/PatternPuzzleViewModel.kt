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
import kotlin.math.abs
import kotlin.random.Random

class PatternPuzzleViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("puzzle_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        PatternPuzzleState(bestScore = prefs.getInt("best", 0))
    )
    val state: StateFlow<PatternPuzzleState> = _state.asStateFlow()

    private var loopJob: Job? = null
    private var nextId  = 0

    private val STEPS = listOf(-10, -5, -4, -3, -2, 2, 3, 4, 5, 10)

    fun startFresh() {
        loopJob?.cancel()
        _state.value = PatternPuzzleState(bestScore = prefs.getInt("best", 0))
        generateQuestion(0)
        startLoop()
    }

    // ── Soru üret ────────────────────────────────────────────────────────────
    private fun generateQuestion(index: Int) {
        // Tema tamamen rastgele
        val theme = PuzzleTheme.values().random()

        val step    = STEPS.random()
        val absStep = abs(step)
        val len     = 7

        // start aralığı: tüm değerler 1..99 içinde kalsın
        val start = if (step > 0) {
            val lo = 5
            val hi = (99 - step * (len - 1)).coerceAtLeast(lo + 1)
            Random.nextInt(lo, hi)
        } else {
            val lo = (1 + absStep * (len - 1)).coerceAtMost(93)
            val hi = 95
            if (lo >= hi) lo else Random.nextInt(lo, hi)
        }
        val values = (0 until len).map { start + it * step }

        // Boşluk sayısı rastgele: 1, 2 ya da 3
        val numBlanks      = Random.nextInt(1, 4)
        val blankPositions = pickBlanks(len, numBlanks)
        val slots = values.mapIndexed { i, v ->
            PuzzleSlot(index = i, value = v, isBlank = i in blankPositions)
        }

        val answers  = blankPositions.map { values[it] }
        // Sahte seçenek sayısı: ekranda toplamda en az 4 top olsun
        val decoyCount = (4 - answers.size).coerceAtLeast(2)
        val decoys   = generateDecoys(answers, step, values, decoyCount)
        val allVals  = (answers + decoys).shuffled()

        // Topları eşit aralıklı yerleştir
        val spacing = 1f / (allVals.size + 1)
        val chars = allVals.mapIndexed { i, v ->
            PuzzleChar(
                id         = nextId++,
                value      = v,
                x          = spacing * (i + 1),
                direction  = if (i % 2 == 0) 1 else -1,
                speed      = 0.028f + Random.nextFloat() * 0.025f,
                colorIndex = i
            )
        }

        val s = _state.value
        _state.value = s.copy(
            phase            = PuzzlePh.PLAYING,
            questionIndex    = index,
            slots            = slots,
            totalBlanks      = numBlanks,
            filledCorrect    = 0,
            chars            = chars,
            theme            = theme,
            flashCorrectSlot = null,
            flashWrongSlot   = null
        )
    }

    private fun pickBlanks(len: Int, count: Int): List<Int> {
        val candidates = (1 until len - 1).shuffled()   // ilk ve son hariç
        val result = mutableListOf<Int>()
        for (c in candidates) {
            if (result.none { abs(it - c) <= 1 }) result.add(c)
            if (result.size == count) break
        }
        // İstenen sayıya ulaşamazsak geri kalanı bitişik olmaksızın ekle
        for (c in candidates) {
            if (result.size == count) break
            if (c !in result) result.add(c)
        }
        return result.sorted()
    }

    private fun generateDecoys(correct: List<Int>, step: Int, all: List<Int>, count: Int = 3): List<Int> {
        val used   = (correct + all).toMutableSet()
        val result = mutableListOf<Int>()
        val absS   = abs(step)
        var attempt = 0
        while (result.size < count && attempt < 80) {
            attempt++
            val base = correct.random()
            val off  = listOf(-absS * 2, -absS, absS, absS * 2, -1, 1, -2, 2, -3, 3).random()
            val c    = base + off
            if (c > 0 && c !in used) { result.add(c); used.add(c) }
        }
        var extra = 1
        while (result.size < count) { result.add(correct.first() + extra * 7); extra++ }
        return result
    }

    // ── Oyun döngüsü ──────────────────────────────────────────────────────────
    private fun startLoop() {
        loopJob = viewModelScope.launch {
            var lastMs = System.currentTimeMillis()
            while (true) {
                delay(16)
                val now = System.currentTimeMillis()
                val dt  = (now - lastMs) / 1000f
                lastMs  = now
                tick(dt)
            }
        }
    }

    private fun tick(dt: Float) {
        val s = _state.value
        if (s.phase != PuzzlePh.PLAYING) return

        val newTime  = (s.timeLeft - dt).coerceAtLeast(0f)
        val newChars = s.chars.map { c ->
            var nx = c.x + c.direction * c.speed * dt
            var nd = c.direction
            if (nx < 0.03f) { nx = 0.03f; nd = 1 }
            if (nx > 0.88f) { nx = 0.88f; nd = -1 }
            c.copy(x = nx, direction = nd)
        }

        _state.value = s.copy(chars = newChars, timeLeft = newTime)
        if (newTime <= 0f) {
            loopJob?.cancel()
            _state.value = _state.value.copy(phase = PuzzlePh.GAME_OVER)
        }
    }

    // ── Sürükle-bırak ─────────────────────────────────────────────────────────
    fun onEvent(event: PatternPuzzleEvent) {
        when (event) {
            is PatternPuzzleEvent.Dropped -> handleDrop(event.charId, event.slotIndex)
            is PatternPuzzleEvent.Restart -> startFresh()
            is PatternPuzzleEvent.Back    -> Unit
        }
    }

    private fun handleDrop(charId: Int, slotIndex: Int) {
        val s    = _state.value
        val char = s.chars.find { it.id == charId } ?: return
        val slot = s.slots.getOrNull(slotIndex)    ?: return
        if (!slot.isBlank || slot.filled) return

        if (char.value == slot.value) {
            // Doğru
            val newSlots  = s.slots.map { if (it.index == slotIndex) it.copy(filled = true) else it }
            val newChars  = s.chars.filter { it.id != charId }
            val newFilled = s.filledCorrect + 1
            val newScore  = s.score + 15

            _state.value = s.copy(
                slots = newSlots, chars = newChars,
                filledCorrect = newFilled, score = newScore,
                flashCorrectSlot = slotIndex, flashWrongSlot = null
            )
            viewModelScope.launch {
                delay(500)
                _state.value = _state.value.copy(flashCorrectSlot = null)
                if (newFilled >= s.totalBlanks) {
                    delay(200)
                    val next = s.questionIndex + 1
                    if (next >= 30) finishGame() else generateQuestion(next)
                }
            }
        } else {
            // Yanlış
            val newLives = s.lives - 1
            _state.value = s.copy(
                lives = newLives,
                flashWrongSlot = slotIndex, flashCorrectSlot = null
            )
            viewModelScope.launch {
                delay(500)
                _state.value = _state.value.copy(flashWrongSlot = null)
                if (newLives <= 0) {
                    loopJob?.cancel()
                    _state.value = _state.value.copy(phase = PuzzlePh.GAME_OVER)
                }
            }
        }
    }

    private fun finishGame() {
        loopJob?.cancel()
        val score   = _state.value.score
        val prev    = prefs.getInt("best", 0)
        val isNew   = score > prev
        if (isNew) prefs.edit().putInt("best", score).apply()
        _state.value = _state.value.copy(
            phase     = PuzzlePh.VICTORY,
            bestScore = if (isNew) score else prev,
            isNewBest = isNew
        )
    }

    override fun onCleared() { super.onCleared(); loopJob?.cancel() }
}
