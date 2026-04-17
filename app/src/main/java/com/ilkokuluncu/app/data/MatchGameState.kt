package com.ilkokuluncu.app.data

enum class TimeType { FULL_HOUR, HALF_HOUR, QUARTER_PAST, QUARTER_TO }

// ── Türkçe çekim ekleri ──────────────────────────────────────────────────────
// Yönelme: "Bire çeyrek var", "İkiye çeyrek var" …
private fun dative(h: Int) = when (h) {
    1->"Bire"; 2->"İkiye"; 3->"Üçe"; 4->"Dörde"; 5->"Beşe"
    6->"Altıya"; 7->"Yediye"; 8->"Sekize"; 9->"Dokuza"
    10->"Ona"; 11->"Onbire"; 12->"Onikiye"; else->"$h'e"
}
// Belirtme: "Yediyi çeyrek geçiyor", "Üçü çeyrek geçiyor" …
private fun accusative(h: Int) = when (h) {
    1->"Biri"; 2->"İkiyi"; 3->"Üçü"; 4->"Dördü"; 5->"Beşi"
    6->"Altıyı"; 7->"Yediyi"; 8->"Sekizi"; 9->"Dokuzu"
    10->"Onu"; 11->"Onbiri"; 12->"Onikıyi"; else->"$h'i"
}

data class MatchItem(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val type: TimeType,
    val colorIdx: Int           // 0..9 → ITEM_COLORS paleti
) {
    val label: String get() = when (type) {
        TimeType.FULL_HOUR    -> "Saat $hour:00"
        TimeType.HALF_HOUR    -> "Saat $hour:30"
        TimeType.QUARTER_PAST -> "${accusative(hour)} çeyrek geçiyor"
        TimeType.QUARTER_TO   -> {
            val next = if (hour == 12) 1 else hour + 1
            "${dative(next)} çeyrek var"
        }
    }
}

data class MatchGameState(
    val labelItems: List<MatchItem>   = emptyList(),   // sol – sıralı
    val clockItems: List<MatchItem>   = emptyList(),   // sağ – karışık
    val pendingPool: List<MatchItem>  = emptyList(),   // bekleyen sorular
    val selectedLabelId: Int?           = null,
    val wrongFlashIds: Pair<Int,Int>?   = null,        // (labelId, clockId) – kırmızı
    val correctFlashIds: Pair<Int,Int>? = null,        // (labelId, clockId) – yeşil
    val totalTimeLeft: Float          = 1500f,         // toplam oyun süresi (sn)
    val labelSelectedMs: Long         = 0L,            // skor hesabı için
    val totalCorrect: Int             = 0,
    val wrongCount: Int               = 0,
    val score: Int                    = 0,
    val bestScore: Int                = 0,
    val isGameOver: Boolean           = false,
    val isVictory: Boolean            = false
)

sealed class MatchGameEvent {
    data class SelectLabel(val id: Int) : MatchGameEvent()
    data class SelectClock(val id: Int) : MatchGameEvent()
    object RestartGame : MatchGameEvent()
    object BackToMenu  : MatchGameEvent()
}
