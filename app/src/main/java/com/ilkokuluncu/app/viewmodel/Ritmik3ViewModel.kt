package com.ilkokuluncu.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ilkokuluncu.app.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed class Ritmik3Sound {
    object Tap     : Ritmik3Sound()
    object Correct : Ritmik3Sound()
    object Wrong   : Ritmik3Sound()
    object CycleWin: Ritmik3Sound()
}

class Ritmik3ViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(Ritmik3State())
    val state: StateFlow<Ritmik3State> = _state.asStateFlow()

    private val _sounds = MutableSharedFlow<Ritmik3Sound>(extraBufferCapacity = 4)
    val sounds: SharedFlow<Ritmik3Sound> = _sounds.asSharedFlow()

    private var loopJob: Job? = null

    // ── Başlangıç ────────────────────────────────────────────────────────────
    fun startFresh(
        screenW: Float    = 1280f,
        speed: Float      = 200f,
        score: Int        = 0,
        pointsPerHit: Int = 5,
        cycleCount: Int   = 0
    ) {
        loopJob?.cancel()
        _state.value = Ritmik3State(
            screenW      = screenW,
            speed        = speed,
            totalScore   = score,
            pointsPerHit = pointsPerHit,
            cycleCount   = cycleCount,
            countdown    = if (cycleCount == 0) 3.5f else 1.2f
        )
        startLoop()
    }

    fun setScreenWidth(w: Float) {
        if (_state.value.screenW != w) _state.value = _state.value.copy(screenW = w)
    }

    // ── Dokunma ──────────────────────────────────────────────────────────────
    fun onStarTapped(starId: Int) {
        val s = _state.value
        if (s.phase != Ritmik3Phase.PLAYING) return
        val star = s.stars.find { it.id == starId } ?: return
        if (star.hitCorrect || star.hitWrong) return

        _sounds.tryEmit(Ritmik3Sound.Tap)

        when {
            // Doğru hedef ──────────────────────────────────────────────────
            star.number == s.currentTarget -> {
                _sounds.tryEmit(Ritmik3Sound.Correct)
                val newHits  = s.correctHits + star.number
                val newScore = s.totalScore + s.pointsPerHit

                val updatedStars = s.stars.map { t ->
                    when {
                        t.id == starId             -> t.copy(hitCorrect = true, anim = 0.7f)
                        t.hitCorrect || t.hitWrong -> t
                        kotlin.math.abs(t.x - star.x) < 2f -> t.copy(hitWrong = true, anim = 0.4f)
                        else                       -> t
                    }
                }

                if (newHits.size >= RITMIK3_SEQUENCE.size) {
                    // 30'a ulaşıldı → durmadan devam, 3'ten başla
                    _sounds.tryEmit(Ritmik3Sound.CycleWin)
                    val newSpeed = (s.speed + 20f).coerceAtMost(450f)
                    _state.value = s.copy(
                        stars         = updatedStars,
                        correctHits   = emptyList(),
                        currentTarget = RITMIK3_SEQUENCE[0],
                        totalScore    = newScore,
                        speed         = newSpeed,
                        pointsPerHit  = s.pointsPerHit + 1,
                        cycleCount    = s.cycleCount + 1
                    )
                } else {
                    val nextTarget = RITMIK3_SEQUENCE[newHits.size]
                    _state.value = s.copy(
                        stars         = updatedStars,
                        correctHits   = newHits,
                        currentTarget = nextTarget,
                        totalScore    = newScore
                    )
                }
            }

            // Sıradaki bir sayı: yoksay ────────────────────────────────────
            star.number in RITMIK3_SEQUENCE -> { /* sessizce yoksay */ }

            // 3'ün katı olmayan tuzak → fail ───────────────────────────────
            else -> {
                _sounds.tryEmit(Ritmik3Sound.Wrong)
                val updatedStars = s.stars.map { t ->
                    if (t.id == starId) t.copy(hitWrong = true, anim = 0.6f) else t
                }
                _state.value = s.copy(
                    stars    = updatedStars,
                    phase    = Ritmik3Phase.FAIL_ANIM,
                    failAnim = 2.2f
                )
            }
        }
    }

    // ── Döngü ────────────────────────────────────────────────────────────────
    private fun startLoop() {
        loopJob = viewModelScope.launch {
            var last = System.currentTimeMillis()
            while (true) {
                delay(16)
                val now = System.currentTimeMillis()
                val dt  = ((now - last) / 1000f).coerceAtMost(0.05f)
                last = now
                tick(dt)
            }
        }
    }

    private fun tick(dt: Float) {
        val s = _state.value
        when (s.phase) {
            Ritmik3Phase.COUNTDOWN -> {
                val newCd = s.countdown - dt
                if (newCd <= 0f)
                    _state.value = s.copy(phase = Ritmik3Phase.PLAYING, countdown = 0f, waveTimer = 99f)
                else
                    _state.value = s.copy(countdown = newCd)
            }
            Ritmik3Phase.PLAYING   -> tickPlaying(dt, s)
            Ritmik3Phase.FAIL_ANIM -> {
                val newFail = s.failAnim - dt
                if (newFail <= 0f)
                    startFresh(s.screenW, s.speed, s.totalScore, s.pointsPerHit, s.cycleCount)
                else
                    _state.value = s.copy(failAnim = newFail)
            }
        }
    }

    private fun tickPlaying(dt: Float, s: Ritmik3State) {
        var stars = s.stars.map { t ->
            t.copy(x = t.x - s.speed * dt, anim = (t.anim - dt).coerceAtLeast(0f))
        }

        // Ekrandan çıkan güncel hedef → başarısız
        val missedCorrect = stars.any { t ->
            t.x < 0f && t.number == s.currentTarget && !t.hitCorrect && !t.hitWrong
        }

        stars = stars.filter { t ->
            val offScreen   = t.x < -R3_STAR_R
            val fadeDone    = (t.hitWrong || (t.hitCorrect && t.anim <= 0f)) && offScreen
            val staleTarget = offScreen && t.isCorrect && !t.hitCorrect && t.number != s.currentTarget
            !fadeDone && !staleTarget && !(offScreen && !t.isCorrect && !t.hitCorrect && !t.hitWrong)
        }

        if (missedCorrect) {
            _sounds.tryEmit(Ritmik3Sound.Wrong)
            _state.value = s.copy(stars = stars, phase = Ritmik3Phase.FAIL_ANIM, failAnim = 2.2f)
            return
        }

        val waveInterval = (s.screenW * 0.42f / s.speed).coerceIn(1.5f, 3.2f)
        val newWaveTimer = s.waveTimer + dt
        var nextId = s.nextStarId

        if (newWaveTimer >= waveInterval) {
            val (newStars, newId) = spawnWave(s.currentTarget, nextId, s.screenW, s.cycleCount)
            stars = stars + newStars
            nextId = newId
            _state.value = s.copy(stars = stars, nextStarId = nextId, waveTimer = 0f)
        } else {
            _state.value = s.copy(stars = stars, waveTimer = newWaveTimer)
        }
    }

    // ── Dalga üretimi ─────────────────────────────────────────────────────────
    private fun spawnWave(
        target: Int, startId: Int, screenW: Float, cycle: Int
    ): Pair<List<Ritmik3Star>, Int> {
        val tracks = listOf(0, 1, 2).shuffled()
        val decoys = generateDecoys(target, cycle)
        var id = startId
        val stars = mutableListOf<Ritmik3Star>()

        val spawnX = screenW + R3_STAR_R + 20f

        stars.add(Ritmik3Star(id = id++, x = spawnX, track = tracks[0],
            number = target, isCorrect = true))
        decoys.forEachIndexed { i, num ->
            stars.add(Ritmik3Star(id = id++, x = spawnX, track = tracks[i + 1],
                number = num, isCorrect = false))
        }
        return stars to id
    }

    // ── Tuzak üretimi ─────────────────────────────────────────────────────────
    // Tüm sıra sayıları 3'ün katı → 3'ün katı olmayanlar tuzak
    private fun generateDecoys(target: Int, cycle: Int): List<Int> {
        val seq = RITMIK3_SEQUENCE
        val idx = seq.indexOf(target)
        val result = mutableListOf<Int>()

        // 1. Garanti tuzak: 3'ün katı olmayan sayı
        val traps = listOf(target - 1, target + 1, target - 2, target + 2, target + 4, target - 4)
            .filter { it > 0 && it % 3 != 0 }
            .shuffled()
        result.add(traps.first())

        // 2. İkinci: ilk turlarda sıra komşusu (görsel), ilerleyince ikinci tuzak
        if (cycle >= 2 || Random.nextFloat() < 0.45f) {
            val second = traps.filter { it !in result }.firstOrNull() ?: run {
                var r: Int
                do { r = Random.nextInt(1, 33) } while (r == target || r in result || r % 3 == 0)
                r
            }
            result.add(second)
        } else {
            val neighbor = listOfNotNull(
                if (idx > 0) seq[idx - 1] else null,
                if (idx < seq.size - 1) seq[idx + 1] else null
            ).filter { it !in result }.shuffled().firstOrNull()
                ?: if (target + 3 <= 30) target + 3 else target - 3
            result.add(neighbor)
        }

        return result
    }

    override fun onCleared() { super.onCleared(); loopJob?.cancel() }
}
