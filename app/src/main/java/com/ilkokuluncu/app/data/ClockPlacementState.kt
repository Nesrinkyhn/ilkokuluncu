package com.ilkokuluncu.app.data

enum class PlacementPhase { PLACING, CORRECT, TIMEOUT }

data class ClockPlacementState(
    val targetHour: Int        = 9,
    val targetMinute: Int      = 15,
    val isHourPhase: Boolean   = true,
    val activeHandAngle: Float = 180f,
    val otherHandAngle: Float  = 60f,
    val phase: PlacementPhase  = PlacementPhase.PLACING,
    val score: Int             = 0,
    val bestScore: Int         = 0,
    val questionCount: Int     = 0,
    val timeLeft: Float        = 10f,
    val lives: Int             = 3,       // kalan can (şeker)
    val showGameOver: Boolean  = false,
    val newRecord: Boolean     = false,
    val lastPoints: Int        = 0,
    val bgIndex: Int           = 0
)

sealed class ClockPlacementEvent {
    data class AngleDragged(val angle: Float) : ClockPlacementEvent()
    object StartFresh : ClockPlacementEvent()
}
