package com.ilkokuluncu.app.data

enum class PuzzlePh { PLAYING, VICTORY, GAME_OVER }

enum class PuzzleTheme(val emoji: String, val label: String) {
    CHICK("🐥", "Civciv"),
    CAR("🚗", "Araba"),
    FROG("🐸", "Kurbağa")
}

data class PuzzleSlot(
    val index: Int,
    val value: Int,
    val isBlank: Boolean,
    val filled: Boolean = false
)

data class PuzzleChar(
    val id: Int,
    val value: Int,
    val x: Float,           // 0..1 normalize
    val direction: Int,     // 1 = sağ, -1 = sol
    val speed: Float,
    val colorIndex: Int
)

data class PatternPuzzleState(
    val phase: PuzzlePh               = PuzzlePh.PLAYING,
    val questionIndex: Int            = 0,
    val slots: List<PuzzleSlot>       = emptyList(),
    val totalBlanks: Int              = 0,
    val filledCorrect: Int            = 0,
    val chars: List<PuzzleChar>       = emptyList(),
    val theme: PuzzleTheme            = PuzzleTheme.CHICK,
    val timeLeft: Float               = 300f,
    val score: Int                    = 0,
    val lives: Int                    = 5,
    val flashCorrectSlot: Int?        = null,
    val flashWrongSlot: Int?          = null,
    val bestScore: Int                = 0,
    val isNewBest: Boolean            = false
)

sealed class PatternPuzzleEvent {
    data class Dropped(val charId: Int, val slotIndex: Int) : PatternPuzzleEvent()
    object Restart : PatternPuzzleEvent()
    object Back    : PatternPuzzleEvent()
}
