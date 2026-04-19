package com.ilkokuluncu.app.data

enum class MarioPhase { PLAYING, VICTORY, GAME_OVER }

const val MARIO_WORLD_H  = 400f
const val MARIO_WORLD_W  = 3600f
const val MARIO_TILE     = 52f
const val MARIO_PLAYER_W = 34f
const val MARIO_PLAYER_H = 44f

data class MarioPlayer(
    val x: Float,
    val y: Float,
    val vx: Float       = 0f,
    val vy: Float       = 0f,
    val onGround: Boolean   = false,
    val facingRight: Boolean = true,
    val animFrame: Float = 0f,
    val lives: Int      = 3,
    val score: Int      = 0,
    val invTimer: Float = 0f,
    val deadTimer: Float = 0f
)

data class MPlatform(
    val x: Float, val y: Float,
    val w: Float, val h: Float,
    val isGrass: Boolean = true
)

data class MEnemy(
    val id: Int,
    val x: Float, val y: Float,
    val vx: Float        = -60f,
    val alive: Boolean   = true,
    val squished: Boolean = false,
    val squishTimer: Float = 0f,
    val animFrame: Float = 0f
)

data class MCoin(
    val id: Int,
    val x: Float, val y: Float,
    val collected: Boolean = false,
    val collectAnim: Float = 0f
)

data class MarioState(
    val phase: MarioPhase      = MarioPhase.PLAYING,
    val player: MarioPlayer    = MarioPlayer(80f, 300f),
    val platforms: List<MPlatform> = emptyList(),
    val enemies: List<MEnemy>  = emptyList(),
    val coins: List<MCoin>     = emptyList(),
    val cameraX: Float         = 0f,
    val goalX: Float           = MARIO_WORLD_W - 220f
)

sealed class MarioEvent {
    object LeftDown  : MarioEvent()
    object LeftUp    : MarioEvent()
    object RightDown : MarioEvent()
    object RightUp   : MarioEvent()
    object JumpDown  : MarioEvent()
    object JumpUp    : MarioEvent()
    object Restart   : MarioEvent()
    object Back      : MarioEvent()
}
