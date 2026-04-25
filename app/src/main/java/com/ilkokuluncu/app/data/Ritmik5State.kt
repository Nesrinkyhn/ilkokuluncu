package com.ilkokuluncu.app.data

enum class Ritmik5Phase { COUNTDOWN, PLAYING, FAIL_ANIM }

const val R5_ICE_R  = 60f   // dondurma topu yarıçapı (%50 büyütüldü)
const val R5_CONE_H = 93f   // külah yüksekliği (%50 büyütüldü)

// 5, 10, 15 … 50
val RITMIK5_SEQUENCE = (1..10).map { it * 5 }

data class Ritmik5Cone(
    val id: Int,
    val x: Float,              // topu MERKEZ x konumu
    val track: Int,            // 0=alt 1=orta 2=üst
    val number: Int,
    val isCorrect: Boolean,
    val hitCorrect: Boolean = false,
    val hitWrong: Boolean   = false,
    val anim: Float         = 0f,
    val colorIdx: Int       = 0    // 0–5 arası renk
)

data class Ritmik5State(
    val phase: Ritmik5Phase      = Ritmik5Phase.COUNTDOWN,
    val cones: List<Ritmik5Cone> = emptyList(),
    val currentTarget: Int       = 5,
    val correctHits: List<Int>   = emptyList(),
    val speed: Float             = 200f,
    val countdown: Float         = 3.5f,
    val failAnim: Float          = 0f,
    val waveTimer: Float         = 99f,
    val nextConeId: Int          = 0,
    val totalScore: Int          = 0,
    val pointsPerHit: Int        = 5,
    val cycleCount: Int          = 0,
    val screenW: Float           = 1280f,
    val totalCorrectCatches: Int = 0
)
