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

class PatternGameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("pattern_game_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        PatternGameState(bestScore = prefs.getInt("best_score", 0))
    )
    val state: StateFlow<PatternGameState> = _state.asStateFlow()

    private var gameLoopJob: Job? = null
    private var spawnJob:    Job? = null
    private var introJob:    Job? = null
    private var nextId = 0

    // Oyuncunun sabit Y konumu (normalize)
    private val PLAYER_Y = 0.78f

    // Örüntü dizileri
    private fun buildSequence(pattern: Int): List<Int> {
        val end = when (pattern) { 2 -> 30; 3 -> 30; 4 -> 40; else -> 50 }
        return (pattern..end step pattern).toList()
    }

    // ── Oyunu sıfırla ────────────────────────────────────────────────────────
    fun startFresh() {
        stopAllJobs()
        val order = listOf(2, 3, 4, 5).shuffled()
        val first = order[0]
        _state.value = PatternGameState(
            phase         = PatternGamePhase.INTRO,
            currentPattern = first,
            sequence      = buildSequence(first),
            patternOrder  = order,
            bestScore     = prefs.getInt("best_score", 0)
        )
        startIntro()
    }

    // ── Intro animasyonu ──────────────────────────────────────────────────────
    private fun startIntro() {
        introJob = viewModelScope.launch {
            _state.value = _state.value.copy(introBalloonX = 1.4f)
            val startMs = System.currentTimeMillis()
            // Sağdan ortaya gel: ~1.2 saniyede
            while (true) {
                delay(16)
                val elapsed = (System.currentTimeMillis() - startMs) / 1000f
                val targetX = 1.4f - elapsed * 1.1f
                if (targetX <= 0.35f) {
                    _state.value = _state.value.copy(introBalloonX = 0.35f)
                    break
                }
                _state.value = _state.value.copy(introBalloonX = targetX)
            }
            // Ortada bekle
            delay(1800)
            // Oyunu başlat
            startPlaying()
        }
    }

    private fun startPlaying() {
        _state.value = _state.value.copy(
            phase          = PatternGamePhase.PLAYING,
            fallingBalloons = emptyList(),
            playerX        = 0.5f
        )
        startGameLoop()
        startSpawner()
    }

    // ── Ana oyun döngüsü (~60 fps) ───────────────────────────────────────────
    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch {
            var lastMs = System.currentTimeMillis()
            while (_state.value.phase == PatternGamePhase.PLAYING) {
                delay(16)
                val now = System.currentTimeMillis()
                val dt  = (now - lastMs) / 1000f
                lastMs  = now
                tick(dt)
            }
        }
    }

    // ── Balon yağdırma (aralıklarla) ─────────────────────────────────────────
    private fun startSpawner() {
        spawnJob = viewModelScope.launch {
            while (_state.value.phase == PatternGamePhase.PLAYING) {
                val interval = (1800f / _state.value.speedMultiplier).toLong().coerceAtLeast(350)
                delay(interval)
                if (_state.value.phase == PatternGamePhase.PLAYING) spawnBalloon()
            }
        }
    }

    private fun spawnBalloon() {
        val s = _state.value
        val correctValue = s.sequence[s.nextIndex]

        // Doğru sayı ekranda yoksa onu yağdır, yoksa %45 ihtimalle yine de yağdır
        val correctOnScreen = s.fallingBalloons.any { it.value == correctValue }
        val spawnCorrect    = !correctOnScreen || Random.nextFloat() < 0.45f

        val value: Int
        val isCorrect: Boolean
        if (spawnCorrect) {
            value = correctValue; isCorrect = true
        } else {
            value = generateDecoy(correctValue, s.currentPattern, s.sequence)
            isCorrect = false
        }

        val x     = 0.07f + Random.nextFloat() * 0.86f
        val speed = (0.13f + Random.nextFloat() * 0.09f) * s.speedMultiplier

        val balloon = FallingBalloon(
            id = nextId++, value = value, x = x, y = -0.08f,
            speed = speed, colorIndex = nextId % 10, isCorrect = isCorrect
        )
        _state.value = s.copy(fallingBalloons = s.fallingBalloons + balloon)
    }

    private fun generateDecoy(correct: Int, pattern: Int, seq: List<Int>): Int {
        val candidates = mutableListOf<Int>()
        for (off in listOf(-pattern, pattern, -(pattern * 2), pattern * 2, -1, 1, -2, 2)) {
            val c = correct + off
            if (c > 0 && c != correct) candidates.add(c)
        }
        return candidates.filter { it !in seq || Random.nextFloat() < 0.3f }
            .ifEmpty { listOf(correct + 1, correct - 1) }
            .random()
    }

    // ── Tick: fizik + çarpışma ────────────────────────────────────────────────
    private fun tick(dt: Float) {
        val s = _state.value
        if (s.phase != PatternGamePhase.PLAYING) return

        val px = s.playerX

        // Balonları aşağı taşı
        val moved = s.fallingBalloons.map { it.copy(y = it.y + it.speed * dt) }

        // Çarpışma & ekran dışı kontrolü
        val toRemove = mutableSetOf<Int>()
        var newLives = s.lives
        var newScore = s.score
        var newIndex = s.nextIndex
        var seqDone  = false

        for (fb in moved) {
            if (fb.y > 1.12f) { toRemove.add(fb.id); continue }
            // Çarpışma: yatay 12%, dikey 9%
            if (abs(fb.x - px) < 0.12f && abs(fb.y - PLAYER_Y) < 0.09f) {
                toRemove.add(fb.id)
                if (fb.isCorrect && fb.value == s.sequence[newIndex]) {
                    newScore += (10 + (s.speedMultiplier * 5f).toInt())
                    newIndex++
                    if (newIndex >= s.sequence.size) seqDone = true
                } else {
                    newLives--
                }
            }
        }

        val remaining = moved.filter { it.id !in toRemove }
        val newElapsed = s.elapsedSeconds + dt
        val newSpeed   = (1f + newElapsed / 30f).coerceAtMost(3.0f)

        _state.value = s.copy(
            fallingBalloons = remaining,
            lives           = newLives,
            score           = newScore,
            nextIndex       = newIndex,
            elapsedSeconds  = newElapsed,
            speedMultiplier = newSpeed
        )

        when {
            newLives <= 0 -> gameOver()
            seqDone       -> patternComplete()
        }
    }

    // ── Örüntü tamamlandı → direkt sonraki intro'ya geç ─────────────────────
    private fun patternComplete() {
        stopAllJobs()
        val s = _state.value
        val done = s.patternsDone + 1

        if (done >= s.patternOrder.size) {
            finishGame(); return
        }

        val nextPat = s.patternOrder[done]
        val nextSeq = buildSequence(nextPat)

        _state.value = s.copy(
            phase           = PatternGamePhase.INTRO,   // direkt intro, bekleme yok
            patternsDone    = done,
            currentPattern  = nextPat,
            sequence        = nextSeq,
            nextIndex       = 0,
            fallingBalloons = emptyList(),
            speedMultiplier = (s.speedMultiplier + 0.2f).coerceAtMost(3.0f)
        )
        startIntro()
    }

    private fun finishGame() {
        val score    = _state.value.score
        val prevBest = prefs.getInt("best_score", 0)
        val isNew    = score > prevBest
        if (isNew) prefs.edit().putInt("best_score", score).apply()

        _state.value = _state.value.copy(
            phase     = PatternGamePhase.VICTORY,
            bestScore = if (isNew) score else prevBest,
            isNewBest = isNew
        )
    }

    private fun gameOver() {
        stopAllJobs()
        _state.value = _state.value.copy(phase = PatternGamePhase.GAME_OVER)
    }

    // ── Event ─────────────────────────────────────────────────────────────────
    fun onEvent(event: PatternGameEvent) {
        when (event) {
            is PatternGameEvent.PlayerMoved -> {
                _state.value = _state.value.copy(
                    playerX = event.x.coerceIn(0.05f, 0.95f)
                )
            }
            is PatternGameEvent.RestartGame -> startFresh()
            is PatternGameEvent.BackToMenu  -> Unit
        }
    }

    private fun stopAllJobs() {
        gameLoopJob?.cancel(); gameLoopJob = null
        spawnJob?.cancel();    spawnJob    = null
        introJob?.cancel();    introJob    = null
    }

    override fun onCleared() { super.onCleared(); stopAllJobs() }
}
