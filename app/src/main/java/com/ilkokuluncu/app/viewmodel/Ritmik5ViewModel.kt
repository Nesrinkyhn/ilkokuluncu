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

sealed class Ritmik5Sound {
    object Tap     : Ritmik5Sound()
    object Correct : Ritmik5Sound()
    object Wrong   : Ritmik5Sound()
    object CycleWin: Ritmik5Sound()
}

class Ritmik5ViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(Ritmik5State())
    val state: StateFlow<Ritmik5State> = _state.asStateFlow()

    private val _sounds = MutableSharedFlow<Ritmik5Sound>(extraBufferCapacity = 4)
    val sounds: SharedFlow<Ritmik5Sound> = _sounds.asSharedFlow()

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
        _state.value = Ritmik5State(
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
    fun onConeTapped(coneId: Int) {
        val s = _state.value
        if (s.phase != Ritmik5Phase.PLAYING) return
        val cone = s.cones.find { it.id == coneId } ?: return
        if (cone.hitCorrect || cone.hitWrong) return

        _sounds.tryEmit(Ritmik5Sound.Tap)

        when {
            // Doğru hedef ─────────────────────────────────────────────────
            cone.number == s.currentTarget -> {
                _sounds.tryEmit(Ritmik5Sound.Correct)
                val newHits  = s.correctHits + cone.number
                val newScore = s.totalScore + s.pointsPerHit

                val updatedCones = s.cones.map { c ->
                    when {
                        c.id == coneId                           -> c.copy(hitCorrect = true, anim = 0.7f)
                        c.hitCorrect || c.hitWrong               -> c
                        kotlin.math.abs(c.x - cone.x) < 2f      -> c.copy(hitWrong = true, anim = 0.4f)
                        else                                     -> c
                    }
                }

                if (newHits.size >= RITMIK5_SEQUENCE.size) {
                    // 50'ye ulaşıldı → duraksız devam
                    _sounds.tryEmit(Ritmik5Sound.CycleWin)
                    val newSpeed = (s.speed + 20f).coerceAtMost(450f)
                    _state.value = s.copy(
                        cones         = updatedCones,
                        correctHits   = emptyList(),
                        currentTarget = RITMIK5_SEQUENCE[0],
                        totalScore    = newScore,
                        speed         = newSpeed,
                        pointsPerHit  = s.pointsPerHit + 1,
                        cycleCount    = s.cycleCount + 1
                    )
                } else {
                    _state.value = s.copy(
                        cones         = updatedCones,
                        correctHits   = newHits,
                        currentTarget = RITMIK5_SEQUENCE[newHits.size],
                        totalScore    = newScore
                    )
                }
            }

            // Sıradaki 5'in katı: yoksay ─────────────────────────────────
            cone.number in RITMIK5_SEQUENCE -> { /* sessizce yoksay */ }

            // 5'in katı olmayan tuzak → fail ──────────────────────────────
            else -> {
                _sounds.tryEmit(Ritmik5Sound.Wrong)
                val updatedCones = s.cones.map { c ->
                    if (c.id == coneId) c.copy(hitWrong = true, anim = 0.6f) else c
                }
                _state.value = s.copy(
                    cones    = updatedCones,
                    phase    = Ritmik5Phase.FAIL_ANIM,
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
            Ritmik5Phase.COUNTDOWN -> {
                val newCd = s.countdown - dt
                if (newCd <= 0f)
                    _state.value = s.copy(phase = Ritmik5Phase.PLAYING, countdown = 0f, waveTimer = 99f)
                else
                    _state.value = s.copy(countdown = newCd)
            }
            Ritmik5Phase.PLAYING   -> tickPlaying(dt, s)
            Ritmik5Phase.FAIL_ANIM -> {
                val newFail = s.failAnim - dt
                if (newFail <= 0f)
                    startFresh(s.screenW, s.speed, s.totalScore, s.pointsPerHit, s.cycleCount)
                else
                    _state.value = s.copy(failAnim = newFail)
            }
        }
    }

    private fun tickPlaying(dt: Float, s: Ritmik5State) {
        var cones = s.cones.map { c ->
            c.copy(x = c.x - s.speed * dt, anim = (c.anim - dt).coerceAtLeast(0f))
        }

        // Güncel hedef ekranı terk etti → başarısız
        val missedCorrect = cones.any { c ->
            c.x < -R5_ICE_R && c.number == s.currentTarget && !c.hitCorrect && !c.hitWrong
        }

        cones = cones.filter { c ->
            val offScreen   = c.x < -R5_ICE_R * 2f
            val fadeDone    = (c.hitWrong || (c.hitCorrect && c.anim <= 0f)) && offScreen
            val staleTarget = offScreen && c.isCorrect && !c.hitCorrect && c.number != s.currentTarget
            !fadeDone && !staleTarget && !(offScreen && !c.isCorrect && !c.hitCorrect && !c.hitWrong)
        }

        if (missedCorrect) {
            _sounds.tryEmit(Ritmik5Sound.Wrong)
            _state.value = s.copy(cones = cones, phase = Ritmik5Phase.FAIL_ANIM, failAnim = 2.2f)
            return
        }

        val waveInterval = (s.screenW * 0.42f / s.speed).coerceIn(1.5f, 3.2f)
        val newWaveTimer = s.waveTimer + dt
        var nextId = s.nextConeId

        if (newWaveTimer >= waveInterval) {
            val (newCones, newId) = spawnWave(s.currentTarget, nextId, s.screenW, s.cycleCount)
            cones  = cones + newCones
            nextId = newId
            _state.value = s.copy(cones = cones, nextConeId = nextId, waveTimer = 0f)
        } else {
            _state.value = s.copy(cones = cones, waveTimer = newWaveTimer)
        }
    }

    // ── Dalga üretimi ─────────────────────────────────────────────────────────
    private fun spawnWave(
        target: Int, startId: Int, screenW: Float, cycle: Int
    ): Pair<List<Ritmik5Cone>, Int> {
        val tracks   = listOf(0, 1, 2).shuffled()
        val decoys   = generateDecoys(target, cycle)
        val colorPick = (0..5).toList().shuffled()
        var id = startId
        val cones = mutableListOf<Ritmik5Cone>()

        val spawnX = screenW + R5_ICE_R + 10f

        cones.add(Ritmik5Cone(id = id++, x = spawnX, track = tracks[0],
            number = target, isCorrect = true, colorIdx = colorPick[0]))
        decoys.forEachIndexed { i, num ->
            cones.add(Ritmik5Cone(id = id++, x = spawnX, track = tracks[i + 1],
                number = num, isCorrect = false, colorIdx = colorPick[i + 1]))
        }
        return cones to id
    }

    // ── Tuzak üretimi ─────────────────────────────────────────────────────────
    private fun generateDecoys(target: Int, cycle: Int): List<Int> {
        val seq = RITMIK5_SEQUENCE
        val idx = seq.indexOf(target)
        val result = mutableListOf<Int>()

        // 1. Garantili tuzak: 5'in katı olmayan sayı
        val traps = listOf(target - 1, target + 1, target - 2, target + 2, target + 3, target - 3)
            .filter { it > 0 && it % 5 != 0 }
            .shuffled()
        result.add(traps.first())

        // 2. İkinci: ilk turlarda sıra komşusu, ilerleyince tuzak
        if (cycle >= 2 || Random.nextFloat() < 0.45f) {
            val second = traps.filter { it !in result }.firstOrNull() ?: run {
                var r: Int
                do { r = Random.nextInt(1, 54) } while (r == target || r in result || r % 5 == 0)
                r
            }
            result.add(second)
        } else {
            val neighbor = listOfNotNull(
                if (idx > 0) seq[idx - 1] else null,
                if (idx < seq.size - 1) seq[idx + 1] else null
            ).filter { it !in result }.shuffled().firstOrNull()
                ?: if (target + 5 <= 50) target + 5 else target - 5
            result.add(neighbor)
        }

        return result
    }

    override fun onCleared() { super.onCleared(); loopJob?.cancel() }
}
