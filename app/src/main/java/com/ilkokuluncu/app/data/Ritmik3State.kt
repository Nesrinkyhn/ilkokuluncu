package com.ilkokuluncu.app.data

enum class Ritmik3Phase { COUNTDOWN, PLAYING, FAIL_ANIM }

const val R3_STAR_R    = 32f    // yıldız baş yarıçapı
const val R3_TRAIL_LEN = 150f   // kuyruk uzunluğu (sağa doğru)

// 3, 6, 9 … 30
val RITMIK3_SEQUENCE = (1..10).map { it * 3 }

data class Ritmik3Star(
    val id: Int,
    val x: Float,             // baş MERKEZİ x konumu
    val track: Int,           // 0=alt 1=orta 2=üst
    val number: Int,
    val isCorrect: Boolean,
    val hitCorrect: Boolean = false,
    val hitWrong: Boolean   = false,
    val anim: Float         = 0f
)

data class Ritmik3State(
    val phase: Ritmik3Phase      = Ritmik3Phase.COUNTDOWN,
    val stars: List<Ritmik3Star> = emptyList(),
    val currentTarget: Int       = 3,
    val correctHits: List<Int>   = emptyList(),
    val speed: Float             = 200f,
    val countdown: Float         = 3.5f,
    val failAnim: Float          = 0f,
    val waveTimer: Float         = 99f,
    val nextStarId: Int          = 0,
    val totalScore: Int          = 0,
    val pointsPerHit: Int        = 5,
    val cycleCount: Int          = 0,
    val screenW: Float           = 1280f
)
