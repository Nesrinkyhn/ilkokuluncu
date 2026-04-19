package com.ilkokuluncu.app.data

enum class Ritmik4Phase { COUNTDOWN, PLAYING, FAIL_ANIM }

const val R4_CAR_W = 200f   // araba genişliği (sol kenar baz alınır)
const val R4_CAR_H = 100f   // araba yüksekliği (merkez baz alınır)

// 4, 8, 12 … 40
val RITMIK4_SEQUENCE = (1..10).map { it * 4 }

data class Ritmik4Car(
    val id: Int,
    val x: Float,             // SOL kenar x konumu
    val track: Int,           // 0=alt 1=orta 2=üst
    val number: Int,
    val isCorrect: Boolean,
    val hitCorrect: Boolean = false,
    val hitWrong: Boolean   = false,
    val anim: Float         = 0f
)

data class Ritmik4State(
    val phase: Ritmik4Phase    = Ritmik4Phase.COUNTDOWN,
    val cars: List<Ritmik4Car> = emptyList(),
    val currentTarget: Int     = 4,
    val correctHits: List<Int> = emptyList(),
    val speed: Float           = 200f,
    val countdown: Float       = 3.5f,
    val failAnim: Float        = 0f,
    val waveTimer: Float       = 99f,
    val nextCarId: Int         = 0,
    val totalScore: Int        = 0,
    val pointsPerHit: Int      = 5,
    val cycleCount: Int        = 0,
    val screenW: Float         = 1280f
)
