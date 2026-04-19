package com.ilkokuluncu.app.data

enum class Ritmik2Phase { COUNTDOWN, PLAYING, FAIL_ANIM, CYCLE_WIN }

const val R2_TILE_W = 200f
const val R2_TILE_H = 110f

// Sayı dizisi: 2, 4, 6 … 20
val RITMIK2_SEQUENCE = (1..10).map { it * 2 }  // [2,4,6,8,10,12,14,16,18,20]

data class RitmikTile(
    val id: Int,
    val x: Float,               // ekran X — her frame sola kayar
    val track: Int,             // 0=alt, 1=orta, 2=üst
    val number: Int,
    val isCorrect: Boolean,
    val hitCorrect: Boolean = false,
    val hitWrong: Boolean   = false,
    val anim: Float         = 0f    // animasyon sayacı
)

data class Ritmik2State(
    val phase: Ritmik2Phase     = Ritmik2Phase.COUNTDOWN,
    val tiles: List<RitmikTile> = emptyList(),
    val currentTarget: Int      = 2,
    val correctHits: List<Int>  = emptyList(), // bu turda topladıkları
    val speed: Float            = 200f,        // px/sn
    val countdown: Float        = 3.5f,
    val failAnim: Float         = 0f,
    val cycleWinAnim: Float     = 0f,
    val waveTimer: Float        = 99f,
    val nextTileId: Int         = 0,
    val totalScore: Int         = 0,
    val pointsPerHit: Int       = 5,           // tur başına artıyor
    val cycleCount: Int         = 0,           // kaç tur tamamlandı
    val screenW: Float          = 1280f
)
