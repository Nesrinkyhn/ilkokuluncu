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

sealed class Ritmik2Sound {
    object Tap     : Ritmik2Sound()
    object Correct : Ritmik2Sound()
    object Wrong   : Ritmik2Sound()
    object CycleWin: Ritmik2Sound()
}

class Ritmik2ViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(Ritmik2State())
    val state: StateFlow<Ritmik2State> = _state.asStateFlow()

    private val _sounds = MutableSharedFlow<Ritmik2Sound>(extraBufferCapacity = 4)
    val sounds: SharedFlow<Ritmik2Sound> = _sounds.asSharedFlow()

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
        _state.value = Ritmik2State(
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

    // ── Dokunma olayı ────────────────────────────────────────────────────────
    fun onTileTapped(tileId: Int) {
        val s = _state.value
        if (s.phase != Ritmik2Phase.PLAYING) return
        val tile = s.tiles.find { it.id == tileId } ?: return
        if (tile.hitCorrect || tile.hitWrong) return

        _sounds.tryEmit(Ritmik2Sound.Tap)

        when {
            // ── Doğru ────────────────────────────────────────────────────────
            tile.number == s.currentTarget -> {
                _sounds.tryEmit(Ritmik2Sound.Correct)
                val newHits  = s.correctHits + tile.number
                val newScore = s.totalScore + s.pointsPerHit

                // Aynı dalgadaki karolar solar; farklı dalgalar dokunulmaz
                val updatedTiles = s.tiles.map { t ->
                    when {
                        t.id == tileId             -> t.copy(hitCorrect = true, anim = 0.7f)
                        t.hitCorrect || t.hitWrong -> t
                        kotlin.math.abs(t.x - tile.x) < 2f -> t.copy(hitWrong = true, anim = 0.4f)
                        else                       -> t
                    }
                }

                if (newHits.size >= RITMIK2_SEQUENCE.size) {
                    // 20'ye ulaşıldı → durmadan devam, 2'den başla, hızlan
                    _sounds.tryEmit(Ritmik2Sound.CycleWin)
                    val newSpeed = (s.speed + 20f).coerceAtMost(450f)
                    _state.value = s.copy(
                        tiles         = updatedTiles,
                        correctHits   = emptyList(),
                        currentTarget = RITMIK2_SEQUENCE[0],   // → 2
                        totalScore    = newScore,
                        speed         = newSpeed,
                        pointsPerHit  = s.pointsPerHit + 1,
                        cycleCount    = s.cycleCount + 1
                    )
                } else {
                    val nextTarget = RITMIK2_SEQUENCE[newHits.size]
                    _state.value = s.copy(
                        tiles         = updatedTiles,
                        correctHits   = newHits,
                        currentTarget = nextTarget,
                        totalScore    = newScore
                    )
                }
            }

            // ── Sıradaki bir sayı: yok say, fail etme ────────────────────────
            tile.number in RITMIK2_SEQUENCE -> { /* sessizce yoksay */ }

            // ── Tuzak sayı → fail ─────────────────────────────────────────────
            else -> {
                _sounds.tryEmit(Ritmik2Sound.Wrong)
                val updatedTiles = s.tiles.map { t ->
                    if (t.id == tileId) t.copy(hitWrong = true, anim = 0.6f) else t
                }
                _state.value = s.copy(
                    tiles     = updatedTiles,
                    phase     = Ritmik2Phase.FAIL_ANIM,
                    failAnim  = 2.2f
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
            Ritmik2Phase.COUNTDOWN -> tickCountdown(dt, s)
            Ritmik2Phase.PLAYING   -> tickPlaying(dt, s)
            Ritmik2Phase.FAIL_ANIM -> {
                val newFail = s.failAnim - dt
                if (newFail <= 0f) {
                    // Baştan başla — hız, puan, tur sayısı korunur; skor birikir
                    startFresh(s.screenW, s.speed, s.totalScore, s.pointsPerHit, s.cycleCount)
                } else {
                    _state.value = s.copy(failAnim = newFail)
                }
            }
            Ritmik2Phase.CYCLE_WIN -> {
                // Bu faza artık girilmiyor; güvenlik için PLAYING'e dön
                _state.value = s.copy(phase = Ritmik2Phase.PLAYING)
            }
        }
    }

    private fun tickCountdown(dt: Float, s: Ritmik2State) {
        val newCd = s.countdown - dt
        if (newCd <= 0f) {
            _state.value = s.copy(phase = Ritmik2Phase.PLAYING, countdown = 0f, waveTimer = 99f)
        } else {
            _state.value = s.copy(countdown = newCd)
        }
    }

    private fun tickPlaying(dt: Float, s: Ritmik2State) {
        var tiles = s.tiles.map { t ->
            val newX    = t.x - s.speed * dt
            val newAnim = (t.anim - dt).coerceAtLeast(0f)
            t.copy(x = newX, anim = newAnim)
        }

        // Ekrandan çıkan güncel hedef karo → başarısız
        val missedCorrect = tiles.any { t ->
            t.x + R2_TILE_W < 0f && t.number == s.currentTarget && !t.hitCorrect && !t.hitWrong
        }

        tiles = tiles.filter { t ->
            val offScreen   = t.x + R2_TILE_W < 0f
            val fadeDone    = (t.hitWrong || (t.hitCorrect && t.anim <= 0f)) && offScreen
            val staleTarget = offScreen && t.isCorrect && !t.hitCorrect && t.number != s.currentTarget
            !fadeDone && !staleTarget && !(offScreen && !t.isCorrect && !t.hitCorrect && !t.hitWrong)
        }

        if (missedCorrect) {
            _sounds.tryEmit(Ritmik2Sound.Wrong)
            _state.value = s.copy(tiles = tiles, phase = Ritmik2Phase.FAIL_ANIM, failAnim = 2.2f)
            return
        }

        val waveInterval = (s.screenW * 0.38f / s.speed).coerceIn(1.4f, 3.0f)
        val newWaveTimer = s.waveTimer + dt
        var nextId = s.nextTileId

        if (newWaveTimer >= waveInterval) {
            val (newTiles, newId) = spawnWave(s.currentTarget, nextId, s.screenW, s.cycleCount)
            tiles = tiles + newTiles
            nextId = newId
            _state.value = s.copy(tiles = tiles, nextTileId = nextId, waveTimer = 0f)
        } else {
            _state.value = s.copy(tiles = tiles, waveTimer = newWaveTimer)
        }
    }

    // ── Dalga üretimi ─────────────────────────────────────────────────────────
    private fun spawnWave(
        target: Int, startId: Int, screenW: Float, cycle: Int
    ): Pair<List<RitmikTile>, Int> {
        val tracks = listOf(0, 1, 2).shuffled()
        val decoys = generateDecoys(target, cycle)
        var id = startId
        val tiles = mutableListOf<RitmikTile>()

        tiles.add(RitmikTile(id = id++, x = screenW + 20f, track = tracks[0],
            number = target, isCorrect = true))

        decoys.forEachIndexed { i, num ->
            tiles.add(RitmikTile(id = id++, x = screenW + 20f, track = tracks[i + 1],
                number = num, isCorrect = false))
        }
        return tiles to id
    }

    // ── Tuzak üretimi ─────────────────────────────────────────────────────────
    // Tüm sıra sayıları çift (2,4,6…20) → tek sayılar her zaman tuzak
    private fun generateDecoys(target: Int, cycle: Int): List<Int> {
        val seq = RITMIK2_SEQUENCE
        val idx = seq.indexOf(target)
        val result = mutableListOf<Int>()

        // 1. Tuzak: garanti tek sayı (sıra dışı → basılırsa fail)
        val guaranteedTrap = listOf(target - 1, target + 1, target + 3, target - 3, target + 5)
            .filter { it > 0 && it !in seq }
            .shuffled().first()
        result.add(guaranteedTrap)

        // 2. Tuzak: ilk turlarda sıra komşusu (görsel kafa karıştırıcı ama basılsa safe),
        //           ileriki turlarda ikinci tek-sayı tuzağı
        val useSecondTrap = cycle >= 2 || Random.nextFloat() < 0.4f
        if (useSecondTrap) {
            // İkinci garanti tuzak (farklı tek sayı)
            val secondTrap = listOf(target + 1, target - 1, target + 3, target - 3, target + 5, target - 5)
                .filter { it > 0 && it !in seq && it !in result }
                .shuffled().firstOrNull() ?: run {
                    var r: Int
                    do { r = Random.nextInt(1, 25) } while (r == target || r in result || r in seq)
                    r
                }
            result.add(secondTrap)
        } else {
            // Sıra komşusu (görsel tuzak, basılsa fail değil)
            val seqNeighbor = listOfNotNull(
                if (idx > 0) seq[idx - 1] else null,
                if (idx < seq.size - 1) seq[idx + 1] else null
            ).filter { it !in result }.shuffled().firstOrNull()
                ?: listOf(target - 2, target + 2).filter { it > 0 && it !in result }.first()
            result.add(seqNeighbor)
        }

        return result
    }

    override fun onCleared() { super.onCleared(); loopJob?.cancel() }
}
