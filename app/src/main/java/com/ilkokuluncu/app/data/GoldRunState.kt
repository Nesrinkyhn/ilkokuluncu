package com.ilkokuluncu.app.data

enum class GoldRunPhase { PLAYING, VICTORY, GAME_OVER }

const val GOLD_WORLD_H  = 400f
const val GOLD_WORLD_W  = 4400f
const val GOLD_TILE     = 50f
const val GOLD_BALL_R   = 20f
const val GOLD_BALL_D   = GOLD_BALL_R * 2
const val GOLD_COIN_R   = 19f
const val GOLD_NORMAL_COIN_R = 14f   // normal coin daha küçük
const val GOLD_THORN_W  = 50f
const val GOLD_THORN_H  = 56f
const val GOLD_GROUND_Y = GOLD_WORLD_H - GOLD_TILE

data class GoldBall(
    val x: Float, val y: Float,
    val vx: Float         = 0f,
    val vy: Float         = 0f,
    val onGround: Boolean = false,
    val angle: Float      = 0f,
    val invTimer: Float   = 0f,
    val deadTimer: Float  = 0f
)

data class GRPlatform(
    val x: Float, val y: Float,
    val w: Float, val h: Float
)

data class GRThorn(
    val id: Int,
    val x: Float, val y: Float,
    val vx: Float        = -55f,
    val animFrame: Float = 0f
)

data class GRCoin(
    val id: Int,
    val x: Float, val y: Float,
    val value: Int,
    val isCorrect: Boolean,
    val isNormal: Boolean    = false,   // normal altın: +10 puan, numarasız
    val collected: Boolean   = false,
    val wrong: Boolean       = false,
    val anim: Float          = 0f
)

data class GRQuestion(val a: Int, val b: Int) {
    val answer: Int     = a * b
    val display: String = "$a × $b = ?"
}

data class GoldRunState(
    val phase: GoldRunPhase          = GoldRunPhase.PLAYING,
    val ball: GoldBall               = GoldBall(80f, GOLD_GROUND_Y - GOLD_BALL_D),
    val platforms: List<GRPlatform>  = emptyList(),
    val thorns: List<GRThorn>        = emptyList(),
    val coins: List<GRCoin>          = emptyList(),
    val question: GRQuestion?        = null,
    val cameraX: Float               = 0f,
    val lives: Int                   = 5,
    val thornHits: Int               = 0,
    val score: Int                   = 0,
    val questionsAnswered: Int       = 0,
    val totalQuestions: Int          = 10,
    val nextCoinId: Int              = 0
)

sealed class GoldRunEvent {
    object LeftDown  : GoldRunEvent()
    object LeftUp    : GoldRunEvent()
    object RightDown : GoldRunEvent()
    object RightUp   : GoldRunEvent()
    object JumpDown  : GoldRunEvent()
    object JumpUp    : GoldRunEvent()
    object Restart   : GoldRunEvent()
    object Back      : GoldRunEvent()
}

sealed class GoldRunSound {
    object CoinCorrect : GoldRunSound()
    object CoinNormal  : GoldRunSound()
    object CoinWrong   : GoldRunSound()
    object ThornHit    : GoldRunSound()
    object PitFall     : GoldRunSound()
    object Victory     : GoldRunSound()
    object GameOver    : GoldRunSound()
}
