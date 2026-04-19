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

sealed class Ritmik4Sound {
    object Tap     : Ritmik4Sound()
    object Correct : Ritmik4Sound()
    object Wrong   : Ritmik4Sound()
    object CycleWin: Ritmik4Sound()
}

class Ritmik4ViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(Ritmik4State())
    val state: StateFlow<Ritmik4State> = _state.asStateFlow()

    private val _sounds = MutableSharedFlow<Ritmik4Sound>(extraBufferCapacity = 4)
    val sounds: SharedFlow<Ritmik4Sound> = _sounds.asSharedFlow()

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
        _state.value = Ritmik4State(
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
    fun onCarTapped(carId: Int) {
        val s = _state.value
        if (s.phase != Ritmik4Phase.PLAYING) return
        val car = s.cars.find { it.id == carId } ?: return
        if (car.hitCorrect || car.hitWrong) return

        _sounds.tryEmit(Ritmik4Sound.Tap)

        when {
            // Doğru hedef ─────────────────────────────────────────────────
            car.number == s.currentTarget -> {
                _sounds.tryEmit(Ritmik4Sound.Correct)
                val newHits  = s.correctHits + car.number
                val newScore = s.totalScore + s.pointsPerHit

                val updatedCars = s.cars.map { c ->
                    when {
                        c.id == carId                              -> c.copy(hitCorrect = true, anim = 0.7f)
                        c.hitCorrect || c.hitWrong                 -> c
                        kotlin.math.abs(c.x - car.x) < 2f         -> c.copy(hitWrong = true, anim = 0.4f)
                        else                                       -> c
                    }
                }

                if (newHits.size >= RITMIK4_SEQUENCE.size) {
                    // 40'a ulaşıldı → duraksız devam, 4'ten başla
                    _sounds.tryEmit(Ritmik4Sound.CycleWin)
                    val newSpeed = (s.speed + 20f).coerceAtMost(450f)
                    _state.value = s.copy(
                        cars          = updatedCars,
                        correctHits   = emptyList(),
                        currentTarget = RITMIK4_SEQUENCE[0],
                        totalScore    = newScore,
                        speed         = newSpeed,
                        pointsPerHit  = s.pointsPerHit + 1,
                        cycleCount    = s.cycleCount + 1
                    )
                } else {
                    val nextTarget = RITMIK4_SEQUENCE[newHits.size]
                    _state.value = s.copy(
                        cars          = updatedCars,
                        correctHits   = newHits,
                        currentTarget = nextTarget,
                        totalScore    = newScore
                    )
                }
            }

            // Sıradaki bir sayı: yoksay ───────────────────────────────────
            car.number in RITMIK4_SEQUENCE -> { /* sessizce yoksay */ }

            // 4'ün katı olmayan tuzak → fail ──────────────────────────────
            else -> {
                _sounds.tryEmit(Ritmik4Sound.Wrong)
                val updatedCars = s.cars.map { c ->
                    if (c.id == carId) c.copy(hitWrong = true, anim = 0.6f) else c
                }
                _state.value = s.copy(
                    cars     = updatedCars,
                    phase    = Ritmik4Phase.FAIL_ANIM,
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
            Ritmik4Phase.COUNTDOWN -> {
                val newCd = s.countdown - dt
                if (newCd <= 0f)
                    _state.value = s.copy(phase = Ritmik4Phase.PLAYING, countdown = 0f, waveTimer = 99f)
                else
                    _state.value = s.copy(countdown = newCd)
            }
            Ritmik4Phase.PLAYING   -> tickPlaying(dt, s)
            Ritmik4Phase.FAIL_ANIM -> {
                val newFail = s.failAnim - dt
                if (newFail <= 0f)
                    startFresh(s.screenW, s.speed, s.totalScore, s.pointsPerHit, s.cycleCount)
                else
                    _state.value = s.copy(failAnim = newFail)
            }
        }
    }

    private fun tickPlaying(dt: Float, s: Ritmik4State) {
        var cars = s.cars.map { c ->
            c.copy(x = c.x - s.speed * dt, anim = (c.anim - dt).coerceAtLeast(0f))
        }

        // Ekrandan çıkan güncel hedef → başarısız
        val missedCorrect = cars.any { c ->
            (c.x + R4_CAR_W) < 0f && c.number == s.currentTarget && !c.hitCorrect && !c.hitWrong
        }

        cars = cars.filter { c ->
            val offScreen   = (c.x + R4_CAR_W) < 0f
            val fadeDone    = (c.hitWrong || (c.hitCorrect && c.anim <= 0f)) && offScreen
            val staleTarget = offScreen && c.isCorrect && !c.hitCorrect && c.number != s.currentTarget
            !fadeDone && !staleTarget && !(offScreen && !c.isCorrect && !c.hitCorrect && !c.hitWrong)
        }

        if (missedCorrect) {
            _sounds.tryEmit(Ritmik4Sound.Wrong)
            _state.value = s.copy(cars = cars, phase = Ritmik4Phase.FAIL_ANIM, failAnim = 2.2f)
            return
        }

        val waveInterval = (s.screenW * 0.42f / s.speed).coerceIn(1.5f, 3.2f)
        val newWaveTimer = s.waveTimer + dt
        var nextId = s.nextCarId

        if (newWaveTimer >= waveInterval) {
            val (newCars, newId) = spawnWave(s.currentTarget, nextId, s.screenW, s.cycleCount)
            cars = cars + newCars
            nextId = newId
            _state.value = s.copy(cars = cars, nextCarId = nextId, waveTimer = 0f)
        } else {
            _state.value = s.copy(cars = cars, waveTimer = newWaveTimer)
        }
    }

    // ── Dalga üretimi ─────────────────────────────────────────────────────────
    private fun spawnWave(
        target: Int, startId: Int, screenW: Float, cycle: Int
    ): Pair<List<Ritmik4Car>, Int> {
        val tracks = listOf(0, 1, 2).shuffled()
        val decoys = generateDecoys(target, cycle)
        var id = startId
        val cars = mutableListOf<Ritmik4Car>()

        val spawnX = screenW + 20f

        cars.add(Ritmik4Car(id = id++, x = spawnX, track = tracks[0],
            number = target, isCorrect = true))
        decoys.forEachIndexed { i, num ->
            cars.add(Ritmik4Car(id = id++, x = spawnX, track = tracks[i + 1],
                number = num, isCorrect = false))
        }
        return cars to id
    }

    // ── Tuzak üretimi ─────────────────────────────────────────────────────────
    private fun generateDecoys(target: Int, cycle: Int): List<Int> {
        val seq  = RITMIK4_SEQUENCE
        val idx  = seq.indexOf(target)
        val result = mutableListOf<Int>()

        // 1. Garantili tuzak: 4'ün katı olmayan sayı
        val traps = listOf(target - 1, target + 1, target - 2, target + 2, target + 3, target - 3)
            .filter { it > 0 && it % 4 != 0 }
            .shuffled()
        result.add(traps.first())

        // 2. İkinci: ilk turlarda sıra komşusu, ilerleyince ikinci tuzak
        if (cycle >= 2 || Random.nextFloat() < 0.45f) {
            val second = traps.filter { it !in result }.firstOrNull() ?: run {
                var r: Int
                do { r = Random.nextInt(1, 44) } while (r == target || r in result || r % 4 == 0)
                r
            }
            result.add(second)
        } else {
            val neighbor = listOfNotNull(
                if (idx > 0) seq[idx - 1] else null,
                if (idx < seq.size - 1) seq[idx + 1] else null
            ).filter { it !in result }.shuffled().firstOrNull()
                ?: if (target + 4 <= 40) target + 4 else target - 4
            result.add(neighbor)
        }

        return result
    }

    override fun onCleared() { super.onCleared(); loopJob?.cancel() }
}
