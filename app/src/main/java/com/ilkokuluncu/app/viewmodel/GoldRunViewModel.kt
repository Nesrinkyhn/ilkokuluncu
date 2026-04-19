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
import kotlin.math.abs
import kotlin.random.Random

class GoldRunViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(buildInitialState())
    val state: StateFlow<GoldRunState> = _state.asStateFlow()

    private val _sounds = MutableSharedFlow<GoldRunSound>(extraBufferCapacity = 8)
    val sounds: SharedFlow<GoldRunSound> = _sounds.asSharedFlow()

    private fun emitSound(s: GoldRunSound) { _sounds.tryEmit(s) }

    private var loopJob: Job? = null
    private var leftDown     = false
    private var jumpDown     = false
    private var jumpConsumed = false
    @Volatile private var jumpPending = false

    private val GRAVITY   = 1300f
    private val JUMP_VEL  = -520f
    private val WALK_SPD  = 155f
    private val BR        = GOLD_BALL_R
    private val BD        = GOLD_BALL_D

    fun startFresh() {
        loopJob?.cancel()
        leftDown = false; jumpDown = false; jumpConsumed = false; jumpPending = false
        _state.value = buildInitialState()
        startLoop()
    }

    // ── Level ────────────────────────────────────────────────────────────────
    private fun buildInitialState(): GoldRunState {
        val gy = GOLD_GROUND_Y
        val platforms = buildPlatforms(gy)
        val q = generateQuestion()
        val pool = makeAnswerPool(q)
        val (coins, remainingPool) = buildMixedCoins(150f, 4200f, 0, pool, q)
        return GoldRunState(
            ball       = GoldBall(80f, gy - BD),
            platforms  = platforms,
            thorns     = buildThorns(),
            coins      = coins,
            question   = q,
            answerPool = remainingPool,
            nextCoinId = coins.size
        )
    }

    private fun buildPlatforms(gy: Float) = listOf(
        GRPlatform(0f,    gy, 820f, GOLD_TILE * 3),
        GRPlatform(940f,  gy, 720f, GOLD_TILE * 3),
        GRPlatform(1780f, gy, 700f, GOLD_TILE * 3),
        GRPlatform(2600f, gy, 680f, GOLD_TILE * 3),
        GRPlatform(3400f, gy, 700f, GOLD_TILE * 3),
        GRPlatform(4220f, gy, 180f, GOLD_TILE * 3)
    )

    private fun buildThorns(): List<GRThorn> {
        return listOf(800f, 2200f).mapIndexed { i, x ->
            GRThorn(id = i, x = x, y = GOLD_GROUND_Y - GOLD_THORN_H, vx = -44f)
        }
    }

    // ── Soru ve cevap havuzu ─────────────────────────────────────────────────
    private fun generateQuestion(): GRQuestion {
        val a = Random.nextInt(1, 6)
        val b = Random.nextInt(1, 6)
        return GRQuestion(a, b)
    }

    private fun generateDecoys(answer: Int): List<Int> {
        val set = mutableSetOf<Int>()
        while (set.size < 4) {
            val d = (answer + Random.nextInt(-6, 7)).coerceIn(1, 25)
            if (d != answer) set.add(d)
        }
        return set.toList()
    }

    /** Bir soru için karıştırılmış 5 değer: 4 yanlış + 1 doğru */
    private fun makeAnswerPool(q: GRQuestion): List<Int> =
        (generateDecoys(q.answer) + q.answer).shuffled()

    /**
     * [fromX, toX] aralığında zemine coin dizisi üretir.
     * Her ~8 normal coinden sonra pool'dan 1 cevap coini yerleştirilir.
     * Pool bitiverirse aynı sorudan yenilenir (cevap hâlâ toplanmamış olabilir).
     * Döndürür: (coins listesi, kullanılmayan pool kalıntısı)
     */
    private fun buildMixedCoins(
        fromX: Float, toX: Float, startId: Int,
        pool: List<Int>, question: GRQuestion?
    ): Pair<List<GRCoin>, List<Int>> {
        val gy = GOLD_GROUND_Y
        val coins = mutableListOf<GRCoin>()
        val mutablePool = pool.toMutableList()
        var id = startId
        var x = fromX
        var sinceLastAnswer = 0

        while (x < toX) {
            // Her 3 normal coinden sonra %80 ihtimalle cevap coini → beklenen aralık ~3.25 coin
            val placeAnswer = question != null
                    && sinceLastAnswer >= 3
                    && Random.nextFloat() < 0.80f

            if (placeAnswer) {
                // Pool bitince aynı sorudan yenile (cevap hâlâ aranıyor)
                if (mutablePool.isEmpty()) mutablePool.addAll(makeAnswerPool(question!!))
                val value = mutablePool.removeFirst()
                // Zıplayarak toplanacak yükseklik: yerden ~85 birim yukarı
                // (max zıplama ≈104 birim; yerden zıplamadan ulaşılamaz)
                coins.add(
                    GRCoin(
                        id        = id++,
                        x         = x,
                        y         = gy - 85f,
                        value     = value,
                        isCorrect = value == question!!.answer,
                        isNormal  = false
                    )
                )
                sinceLastAnswer = 0
            } else {
                // Normal coin: yerden ~72 birim yukarı — zıplamayı zorunlu kılar
                coins.add(
                    GRCoin(
                        id        = id++,
                        x         = x,
                        y         = gy - 72f,
                        value     = 10,
                        isCorrect = false,
                        isNormal  = true
                    )
                )
                sinceLastAnswer++
            }
            x += 110f + Random.nextFloat() * 60f
        }
        return coins to mutablePool
    }

    // ── Events ───────────────────────────────────────────────────────────────
    fun onEvent(event: GoldRunEvent) {
        when (event) {
            GoldRunEvent.LeftDown  -> leftDown = true
            GoldRunEvent.LeftUp    -> leftDown = false
            GoldRunEvent.RightDown -> {}
            GoldRunEvent.RightUp   -> {}
            GoldRunEvent.JumpDown  -> { jumpDown = true; jumpPending = true }
            GoldRunEvent.JumpUp    -> { jumpDown = false; jumpConsumed = false }
            GoldRunEvent.Restart   -> startFresh()
            GoldRunEvent.Back      -> loopJob?.cancel()
        }
    }

    // ── Game loop ─────────────────────────────────────────────────────────────
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
        if (s.phase != GoldRunPhase.PLAYING) return
        var ball = s.ball

        // ── Ölüm animasyonu ───────────────────────────────────────────────────
        if (ball.deadTimer > 0f) {
            ball = ball.copy(
                y = ball.y + ball.vy * dt,
                vy = ball.vy + GRAVITY * dt,
                deadTimer = ball.deadTimer - dt
            )
            if (ball.deadTimer <= 0f) {
                val newLives = s.lives - 1
                if (newLives > 0) {
                    ball = GoldBall(80f, GOLD_GROUND_Y - BD)
                    // Başlangıç platformlarını yeniden kur — aksi hâlde top platfor
                    // msuz zemine iner ve tekrar çukura düşer (sonsuz döngü).
                    val respawnPlatforms = buildPlatforms(GOLD_GROUND_Y)
                    _state.value = s.copy(
                        ball = ball, lives = newLives, cameraX = 0f,
                        thornHits = 0, platforms = respawnPlatforms
                    )
                } else {
                    emitSound(GoldRunSound.GameOver)
                    _state.value = s.copy(phase = GoldRunPhase.GAME_OVER, ball = ball, lives = 0)
                }
                return
            }
            _state.value = s.copy(ball = ball); return
        }

        // ── Hareket ───────────────────────────────────────────────────────────
        val targetVx = if (leftDown) -WALK_SPD * 0.7f else WALK_SPD
        var vy = ball.vy + GRAVITY * dt
        if ((jumpDown || jumpPending) && !jumpConsumed && ball.onGround) {
            vy = JUMP_VEL; jumpConsumed = true; jumpPending = false
        }

        // ── Platform çarpışması ───────────────────────────────────────────────
        var nx = (ball.x + targetVx * dt).coerceAtLeast(0f)
        var ny = ball.y + vy * dt
        var onGround = false
        for (plat in s.platforms) {
            val overX = nx + BD > plat.x && nx < plat.x + plat.w
            if (!overX) continue
            if (vy >= 0 && ball.y + BD <= plat.y + 4f && ny + BD >= plat.y) {
                ny = plat.y - BD; vy = 0f; onGround = true
            }
            if (vy < 0 && ball.y >= plat.y + plat.h - 4f && ny < plat.y + plat.h) {
                ny = plat.y + plat.h; vy = 2f
            }
        }

        // ── Çukura düşme ──────────────────────────────────────────────────────
        if (ny > GOLD_WORLD_H + 40f) {
            emitSound(GoldRunSound.PitFall)
            ball = ball.copy(deadTimer = 1.2f, vy = JUMP_VEL * 0.5f, y = ny)
            _state.value = s.copy(ball = ball); return
        }

        val angle    = (ball.angle + targetVx * dt * (180f / (Math.PI.toFloat() * BR))) % 360f
        val invTimer = (ball.invTimer - dt).coerceAtLeast(0f)
        ball = ball.copy(x = nx, y = ny, vx = targetVx, vy = vy,
            onGround = onGround, angle = angle, invTimer = invTimer)

        val newCam = (ball.x - 220f).coerceAtLeast(0f)

        // ── Çalılar ───────────────────────────────────────────────────────────
        val thornXsBefore = s.thorns.map { it.x }
        var thorns = s.thorns.map { th ->
            var nx2 = th.x + th.vx * dt
            if (nx2 + GOLD_THORN_W < newCam - 100f) {
                var candidate: Float
                var tries = 0
                do {
                    candidate = newCam + 1000f + Random.nextFloat() * 1200f
                    tries++
                } while (tries < 8 && thornXsBefore.any { abs(it - candidate) < 500f })
                nx2 = candidate
            }
            th.copy(x = nx2, animFrame = (th.animFrame + dt * 6f) % 2f)
        }

        // Çalı ↔ top çarpışması
        var thornHits = s.thornHits
        var lives = s.lives
        if (invTimer <= 0f && ball.deadTimer <= 0f) {
            for (th in thorns) {
                val overlapX = ball.x + BD > th.x + 8f && ball.x < th.x + GOLD_THORN_W - 8f
                val overlapY = ball.y + BD > th.y + 12f && ball.y < th.y + GOLD_THORN_H
                if (overlapX && overlapY) {
                    thornHits++
                    emitSound(GoldRunSound.ThornHit)
                    if (thornHits >= 3) {
                        thornHits = 0; lives--
                        if (lives <= 0) {
                            emitSound(GoldRunSound.GameOver)
                            _state.value = s.copy(phase = GoldRunPhase.GAME_OVER, ball = ball,
                                thorns = thorns, lives = 0)
                            return
                        }
                    }
                    ball = ball.copy(invTimer = 1.8f, vy = JUMP_VEL * 0.35f)
                    break
                }
            }
        }

        // ── Coin toplama ──────────────────────────────────────────────────────
        var scoreAdd = 0
        var questionsAnswered = s.questionsAnswered
        var currentQuestion   = s.question
        var currentPool       = s.answerPool
        var nextCoinId        = s.nextCoinId

        val updatedCoins = s.coins.map { c ->
            if (c.collected || c.wrong) {
                if (c.anim > 0f) c.copy(anim = c.anim - dt) else c
            } else {
                val r  = if (c.isNormal) GOLD_NORMAL_COIN_R else GOLD_COIN_R
                val overlapX = ball.x < c.x + r * 2 && ball.x + BD > c.x
                val overlapY = ball.y < c.y + r * 2 && ball.y + BD > c.y
                if (overlapX && overlapY) {
                    when {
                        c.isNormal -> {
                            emitSound(GoldRunSound.CoinNormal)
                            scoreAdd += 10
                            c.copy(collected = true, anim = 0.35f)
                        }
                        c.isCorrect -> {
                            emitSound(GoldRunSound.CoinCorrect)
                            scoreAdd += 20
                            questionsAnswered++
                            c.copy(collected = true, anim = 0.6f)
                        }
                        else -> {
                            emitSound(GoldRunSound.CoinWrong)
                            scoreAdd -= 5
                            c.copy(wrong = true, anim = 0.4f)
                        }
                    }
                } else c
            }
        }

        var finalCoins = updatedCoins

        // Doğru cevap toplandı → yeni soru + yeni pool
        if (questionsAnswered > s.questionsAnswered) {
            if (questionsAnswered >= s.totalQuestions) {
                emitSound(GoldRunSound.Victory)
                _state.value = s.copy(
                    phase = GoldRunPhase.VICTORY,
                    ball = ball, thorns = thorns, coins = finalCoins,
                    score = (s.score + scoreAdd).coerceAtLeast(0),
                    questionsAnswered = questionsAnswered, cameraX = newCam
                )
                return
            }
            val newQ = generateQuestion()
            currentQuestion = newQ
            currentPool = makeAnswerPool(newQ)   // yeni soru için taze pool
            // Yeni cevap coinleri platform üretiminde otomatik yerleşecek
        }

        // ── Sonsuz platform uzatma ────────────────────────────────────────────
        val gy = GOLD_GROUND_Y
        var platforms = s.platforms
        val frontier = platforms.maxOfOrNull { it.x + it.w } ?: 0f
        if (ball.x + 1000f > frontier) {
            val gap   = 110f + Random.nextFloat() * 80f
            val width = 640f + Random.nextFloat() * 220f
            val newPlat = GRPlatform(frontier + gap, gy, width, GOLD_TILE * 3)
            platforms = platforms + newPlat

            // Platforma karışık coin dizisi ekle (normal + ara sıra cevap)
            val (newCoins, remainingPool) = buildMixedCoins(
                newPlat.x + 50f, newPlat.x + newPlat.w - 50f,
                nextCoinId, currentPool, currentQuestion
            )
            finalCoins = finalCoins + newCoins
            nextCoinId += newCoins.size
            currentPool = remainingPool
        }

        // Kameranın gerisinde kalan eski veri temizliği
        platforms  = platforms.filter { it.x + it.w > newCam - 600f }
        finalCoins = finalCoins.filter { c ->
            !((c.collected || c.wrong) && c.anim <= 0f) && c.x > newCam - 600f
        }

        _state.value = s.copy(
            ball = ball, thorns = thorns, coins = finalCoins, platforms = platforms,
            question = currentQuestion, answerPool = currentPool, cameraX = newCam,
            lives = lives, thornHits = thornHits,
            score = (s.score + scoreAdd).coerceAtLeast(0),
            questionsAnswered = questionsAnswered, nextCoinId = nextCoinId
        )
    }

    override fun onCleared() { super.onCleared(); loopJob?.cancel() }
}
