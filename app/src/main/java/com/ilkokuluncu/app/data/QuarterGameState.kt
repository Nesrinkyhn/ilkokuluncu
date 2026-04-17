package com.ilkokuluncu.app.data

enum class QuarterType { PAST, TO }   // çeyrek GEÇİYOR  /  çeyrek VAR

enum class QuarterPhase { ANSWERING, FEEDBACK, SUCCESS }

data class QuarterGameState(
    val questionHour: Int       = 9,
    val questionType: QuarterType = QuarterType.PAST,
    // Saatin fiilen gösterdiği zaman (doğru olabilir ya da yanıltıcı)
    val clockHour: Int          = 9,
    val clockMinute: Int        = 15,
    val isClockCorrect: Boolean = true,   // saat, metinle uyuşuyor mu?
    val score: Int              = 0,
    val bestScore: Int          = 0,
    val timeLeft: Float         = 10f,
    val questionCount: Int      = 0,
    val phase: QuarterPhase     = QuarterPhase.ANSWERING,
    val lastAnswerCorrect: Boolean? = null,
    val pointsDelta: Int        = 0,
    val bgIndex: Int            = 0,
    val newRecord: Boolean      = false
) {
    // ── Soru metni (Türkçe çekim) ─────────────────────────────────────
    fun questionText(): String {
        val pastForm = mapOf(
            1 to "Saat 1'i",  2 to "Saat 2'yi",  3 to "Saat 3'ü",
            4 to "Saat 4'ü",  5 to "Saat 5'i",   6 to "Saat 6'yı",
            7 to "Saat 7'yi", 8 to "Saat 8'i",   9 to "Saat 9'u",
            10 to "Saat 10'u", 11 to "Saat 11'i", 12 to "Saat 12'yi"
        )
        val toForm = mapOf(
            1 to "Saat 1'e",  2 to "Saat 2'ye",  3 to "Saat 3'e",
            4 to "Saat 4'e",  5 to "Saat 5'e",   6 to "Saat 6'ya",
            7 to "Saat 7'ye", 8 to "Saat 8'e",   9 to "Saat 9'a",
            10 to "Saat 10'a", 11 to "Saat 11'e", 12 to "Saat 12'ye"
        )
        return when (questionType) {
            QuarterType.PAST -> "${pastForm[questionHour]} çeyrek geçiyor"
            QuarterType.TO   -> "${toForm[questionHour]} çeyrek var"
        }
    }
}

sealed class QuarterGameEvent {
    object AnswerTrue  : QuarterGameEvent()   // kullanıcı "Doğru" dedi
    object AnswerFalse : QuarterGameEvent()   // kullanıcı "Yanlış" dedi
    object StartFresh  : QuarterGameEvent()
}
