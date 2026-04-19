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

class MarioGameViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(buildState())
    val state: StateFlow<MarioState> = _state.asStateFlow()

    private var loopJob: Job? = null
    private var leftDown  = false
    private var rightDown = false
    private var jumpDown  = false
    private var jumpConsumed = false

    private val GRAVITY  = 1400f
    private val JUMP_VEL = -540f
    private val WALK_SPD = 175f
    private val T  = MARIO_TILE
    private val PW = MARIO_PLAYER_W
    private val PH = MARIO_PLAYER_H
    private val GROUND_Y = MARIO_WORLD_H - T   // platform top y

    fun startFresh() {
        loopJob?.cancel()
        leftDown = false; rightDown = false; jumpDown = false; jumpConsumed = false
        _state.value = buildState()
        startLoop()
    }

    // ── Level ─────────────────────────────────────────────────────────────────
    private fun buildState(): MarioState {
        val gy = GROUND_Y
        return MarioState(
            player    = MarioPlayer(x = 80f, y = gy - PH),
            platforms = buildPlatforms(gy),
            enemies   = buildEnemies(gy),
            coins     = buildCoins(gy),
            goalX     = MARIO_WORLD_W - 220f
        )
    }

    private fun buildPlatforms(gy: Float) = listOf(
        // ── Ground segments (gaps at 740-860, 1460-1600, 2240-2360, 2940-3060)
        MPlatform(0f,    gy, 740f,  T * 4),
        MPlatform(860f,  gy, 600f,  T * 4),
        MPlatform(1600f, gy, 640f,  T * 4),
        MPlatform(2360f, gy, 580f,  T * 4),
        MPlatform(3060f, gy, 540f,  T * 4),
        // ── Elevated bricks
        MPlatform(920f,  gy - T * 2.8f, 180f, T, isGrass = false),
        MPlatform(1140f, gy - T * 2.0f, 160f, T, isGrass = false),
        MPlatform(1660f, gy - T * 2.5f, 150f, T, isGrass = false),
        MPlatform(1860f, gy - T * 3.5f, 190f, T, isGrass = false),
        MPlatform(2420f, gy - T * 2.5f, 160f, T, isGrass = false),
        MPlatform(2620f, gy - T * 2.0f, 160f, T, isGrass = false),
        MPlatform(3120f, gy - T * 2.8f, 180f, T, isGrass = false),
        MPlatform(3320f, gy - T * 2.0f, 160f, T, isGrass = false),
    )

    private fun buildEnemies(gy: Float): List<MEnemy> {
        val eh = T * 1.10f
        fun onGround(x: Float) = MEnemy(0, x, gy - eh, vx = -65f)
        fun onPlat(x: Float, platY: Float) = MEnemy(0, x, platY - eh, vx = -55f)
        val platY1 = gy - T * 2.8f; val platY2 = gy - T * 2.0f; val platY3 = gy - T * 3.5f
        return listOf(
            onGround(280f), onGround(520f),
            onPlat(930f, platY1), onPlat(1155f, platY2),
            onGround(1100f),
            onGround(1700f), onPlat(1870f, platY3),
            onGround(1960f),
            onGround(2460f), onPlat(2630f, platY2),
            onGround(2550f),
            onGround(3150f), onGround(3350f),
        ).mapIndexed { i, e -> e.copy(id = i) }
    }

    private fun buildCoins(gy: Float): List<MCoin> {
        var id = 0
        val list = mutableListOf<MCoin>()
        fun row(startX: Float, count: Int, y: Float) =
            repeat(count) { list.add(MCoin(id++, startX + it * 34f, y)) }
        row(140f,  4, gy - T * 2.6f)
        row(430f,  3, gy - T * 2.6f)
        row(930f,  3, gy - T * 5.3f)
        row(1155f, 3, gy - T * 4.5f)
        row(1670f, 3, gy - T * 5.0f)
        row(1870f, 3, gy - T * 6.0f)
        row(2430f, 3, gy - T * 5.0f)
        row(2630f, 3, gy - T * 4.5f)
        row(3130f, 3, gy - T * 5.3f)
        row(3340f, 4, gy - T * 2.6f)
        return list
    }

    // ── Events ────────────────────────────────────────────────────────────────
    fun onEvent(event: MarioEvent) {
        when (event) {
            MarioEvent.LeftDown  -> leftDown  = true
            MarioEvent.LeftUp    -> leftDown  = false
            MarioEvent.RightDown -> rightDown = true
            MarioEvent.RightUp   -> rightDown = false
            MarioEvent.JumpDown  -> jumpDown  = true
            MarioEvent.JumpUp    -> { jumpDown = false; jumpConsumed = false }
            MarioEvent.Restart   -> startFresh()
            MarioEvent.Back      -> loopJob?.cancel()
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
        if (s.phase != MarioPhase.PLAYING) return
        var p = s.player

        // ── Death animation ────────────────────────────────────────────
        if (p.deadTimer > 0f) {
            p = p.copy(y = p.y + p.vy * dt, vy = p.vy + GRAVITY * dt, deadTimer = p.deadTimer - dt)
            if (p.deadTimer <= 0f) {
                if (p.lives > 1) {
                    p = MarioPlayer(x = 80f, y = GROUND_Y - PH, lives = p.lives - 1, score = p.score)
                    _state.value = s.copy(player = p, cameraX = 0f); return
                } else {
                    _state.value = s.copy(phase = MarioPhase.GAME_OVER, player = p.copy(lives = 0)); return
                }
            }
            _state.value = s.copy(player = p); return
        }

        // ── Input ─────────────────────────────────────────────────────
        val targetVx = when {
            leftDown && !rightDown  -> -WALK_SPD
            rightDown && !leftDown  ->  WALK_SPD
            else                    ->  0f
        }
        val facingRight = if (targetVx > 0) true else if (targetVx < 0) false else p.facingRight
        var vy = p.vy + GRAVITY * dt
        if (jumpDown && !jumpConsumed && p.onGround) { vy = JUMP_VEL; jumpConsumed = true }

        // ── Move & collide ─────────────────────────────────────────────
        var nx = (p.x + targetVx * dt).coerceIn(0f, MARIO_WORLD_W - PW)
        var ny = p.y + vy * dt
        var onGround = false
        for (plat in s.platforms) {
            val overX = nx + PW > plat.x && nx < plat.x + plat.w
            if (!overX) continue
            if (vy >= 0 && p.y + PH <= plat.y + 4f && ny + PH >= plat.y) {
                ny = plat.y - PH; vy = 0f; onGround = true
            }
            if (vy < 0 && p.y >= plat.y + plat.h - 4f && ny < plat.y + plat.h) {
                ny = plat.y + plat.h; vy = 2f
            }
        }

        // ── Fell into pit ──────────────────────────────────────────────
        if (ny > MARIO_WORLD_H + 40f) {
            p = p.copy(deadTimer = 1.5f, vy = JUMP_VEL * 0.55f, y = ny)
            _state.value = s.copy(player = p); return
        }

        val animFrame = if (targetVx != 0f) (p.animFrame + dt * 9f) % 2f else p.animFrame
        val invTimer  = (p.invTimer - dt).coerceAtLeast(0f)
        p = p.copy(x = nx, y = ny, vx = targetVx, vy = vy,
            onGround = onGround, facingRight = facingRight,
            animFrame = animFrame, invTimer = invTimer)

        // ── Camera ────────────────────────────────────────────────────
        val newCam = (p.x - 220f).coerceAtLeast(0f)

        // ── Enemies ───────────────────────────────────────────────────
        var enemies = s.enemies.map { e ->
            if (!e.alive) {
                if (e.squishTimer > 0f) e.copy(squishTimer = e.squishTimer - dt) else e
            } else {
                val EW = T * 1.10f; val EH = T * 1.10f
                var ex  = e.x + e.vx * dt
                var evx = e.vx
                if (ex < 0f || ex + EW > MARIO_WORLD_W) { evx = -evx; ex = e.x }

                // Reverse at platform edges
                val leadX = if (evx < 0f) ex - 2f else ex + EW + 2f
                val footY = e.y + EH + 3f
                var leadOnPlat = false
                for (plat in s.platforms) {
                    if (leadX >= plat.x && leadX <= plat.x + plat.w &&
                        footY >= plat.y && footY <= plat.y + T) { leadOnPlat = true; break }
                }
                var curOnPlat = false
                for (plat in s.platforms) {
                    val midX = e.x + EW * 0.5f
                    if (midX >= plat.x && midX <= plat.x + plat.w &&
                        footY >= plat.y && footY <= plat.y + T) { curOnPlat = true; break }
                }
                if (curOnPlat && !leadOnPlat) { evx = -evx; ex = e.x }

                e.copy(x = ex, vx = evx, animFrame = (e.animFrame + dt * 5f) % 2f)
            }
        }

        // ── Player ↔ Enemy ─────────────────────────────────────────────
        if (invTimer <= 0f && p.deadTimer <= 0f) {
            for (i in enemies.indices) {
                val e = enemies[i]; if (!e.alive) continue
                val EW = T * 1.10f; val EH = T * 1.10f
                if (p.x < e.x + EW && p.x + PW > e.x && p.y < e.y + EH && p.y + PH > e.y) {
                    if (p.vy > 30f && p.y + PH < e.y + EH * 0.55f) {
                        // Stomp!
                        enemies = enemies.toMutableList().also {
                            it[i] = e.copy(alive = false, squished = true, squishTimer = 0.4f)
                        }
                        p = p.copy(vy = JUMP_VEL * 0.5f, score = p.score + 200)
                    } else {
                        if (p.lives <= 1) {
                            p = p.copy(deadTimer = 1.5f, vy = JUMP_VEL * 0.55f)
                            _state.value = s.copy(player = p, enemies = enemies, cameraX = newCam); return
                        }
                        p = p.copy(invTimer = 2.2f, vy = JUMP_VEL * 0.45f,
                            score = (p.score - 50).coerceAtLeast(0))
                    }
                    break
                }
            }
        }

        // ── Coins ─────────────────────────────────────────────────────
        var scoreAdd = 0
        val coins = s.coins.map { c ->
            if (c.collected) {
                if (c.collectAnim > 0f) c.copy(collectAnim = c.collectAnim - dt) else c
            } else {
                val CW = 20f; val CH = 20f
                if (p.x < c.x + CW && p.x + PW > c.x && p.y < c.y + CH && p.y + PH > c.y) {
                    scoreAdd += 50; c.copy(collected = true, collectAnim = 0.5f)
                } else c
            }
        }
        p = p.copy(score = p.score + scoreAdd)

        // ── Goal ──────────────────────────────────────────────────────
        if (p.x + PW >= s.goalX) {
            _state.value = s.copy(phase = MarioPhase.VICTORY, player = p, enemies = enemies, coins = coins, cameraX = newCam)
            return
        }

        _state.value = s.copy(player = p, enemies = enemies, coins = coins, cameraX = newCam)
    }

    override fun onCleared() { super.onCleared(); loopJob?.cancel() }
}
