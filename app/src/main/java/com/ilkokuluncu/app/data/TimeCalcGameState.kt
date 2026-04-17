package com.ilkokuluncu.app.data

enum class TimeOffset(val minutes: Int, val label: String) {
    PLUS_15  ( 15,  "15 dakika sonra"),
    PLUS_30  ( 30,  "30 dakika sonra"),
    PLUS_45  ( 45,  "45 dakika sonra"),
    PLUS_60  ( 60,  "1 saat sonra"),
    PLUS_90  ( 90,  "1 saat 30 dakika sonra"),
    PLUS_120 (120,  "2 saat sonra"),
    MINUS_15 (-15,  "15 dakika önce"),
    MINUS_30 (-30,  "30 dakika önce"),
    MINUS_60 (-60,  "1 saat önce"),
}

enum class TimeCalcPhase { QUESTION, FEEDBACK, VICTORY, GAME_OVER }

data class TimeCalcGameState(
    val clockHour: Int              = 10,
    val clockMinute: Int            = 0,
    val offset: TimeOffset          = TimeOffset.PLUS_60,
    val options: List<Pair<Int,Int>> = emptyList(),   // (hour, minute)
    val correctAnswer: Pair<Int,Int> = 11 to 0,
    val selectedAnswer: Pair<Int,Int>? = null,
    val timeLeft: Float             = 10f,
    val score: Int                  = 0,
    val bestScore: Int              = 0,
    val lives: Int                  = 3,
    val phase: TimeCalcPhase        = TimeCalcPhase.QUESTION,
    val lastCorrect: Boolean?       = null,
    val pointsDelta: Int            = 0,
    val questionCount: Int          = 0,
    val newRecord: Boolean          = false
)

sealed class TimeCalcEvent {
    data class Answer(val hour: Int, val minute: Int) : TimeCalcEvent()
    object NextQuestion  : TimeCalcEvent()
    object RestartGame   : TimeCalcEvent()
    object TimeExpired   : TimeCalcEvent()
}
