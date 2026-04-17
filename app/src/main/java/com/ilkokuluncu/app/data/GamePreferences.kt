package com.ilkokuluncu.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Oyun ilerlemesi ve geçmişi kalıcı olarak saklar.
 * Uygulama silinmediği sürece veriler korunur.
 */
class GamePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ilkokuluncu_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_LEVEL_1_PASSED   = "level_1_passed"
        private const val KEY_LEVEL_2_PASSED   = "level_2_passed"
        private const val KEY_SAVED_SCORE      = "saved_score"
        private const val KEY_LEVEL_1_BEST     = "level_1_best_score"
        private const val KEY_LEVEL_2_BEST     = "level_2_best_score"
        private const val KEY_LEVEL_3_BEST     = "level_3_best_score"
        private const val KEY_LEVEL_4_BEST     = "level_4_best_score"
        private const val KEY_LEVEL_3_PASSED   = "level_3_passed"
        private const val KEY_LEVEL_4_PASSED   = "level_4_passed"
        private const val MAX_HISTORY          = 20
        // Her modül için ayrı geçmiş anahtarı
        fun historyKey(moduleId: String) = "score_history_$moduleId"
    }

    // ── Level testi geçildi mi? ────────────────────────────────────────────
    var hasPassedLevel1: Boolean
        get() = prefs.getBoolean(KEY_LEVEL_1_PASSED, false)
        set(value) = prefs.edit().putBoolean(KEY_LEVEL_1_PASSED, value).apply()

    var hasPassedLevel2: Boolean
        get() = prefs.getBoolean(KEY_LEVEL_2_PASSED, false)
        set(value) = prefs.edit().putBoolean(KEY_LEVEL_2_PASSED, value).apply()

    var hasPassedLevel3: Boolean
        get() = prefs.getBoolean(KEY_LEVEL_3_PASSED, false)
        set(value) = prefs.edit().putBoolean(KEY_LEVEL_3_PASSED, value).apply()

    var hasPassedLevel4: Boolean
        get() = prefs.getBoolean(KEY_LEVEL_4_PASSED, false)
        set(value) = prefs.edit().putBoolean(KEY_LEVEL_4_PASSED, value).apply()

    // ── Kaydedilmiş aktif skor ────────────────────────────────────────────
    var savedScore: Int
        get() = prefs.getInt(KEY_SAVED_SCORE, 0)
        set(value) = prefs.edit().putInt(KEY_SAVED_SCORE, value).apply()

    // ── En yüksek skorlar ─────────────────────────────────────────────────
    var level1BestScore: Int
        get() = prefs.getInt(KEY_LEVEL_1_BEST, 0)
        set(value) = prefs.edit().putInt(KEY_LEVEL_1_BEST, value).apply()

    var level2BestScore: Int
        get() = prefs.getInt(KEY_LEVEL_2_BEST, 0)
        set(value) = prefs.edit().putInt(KEY_LEVEL_2_BEST, value).apply()

    var level3BestScore: Int
        get() = prefs.getInt(KEY_LEVEL_3_BEST, 0)
        set(value) = prefs.edit().putInt(KEY_LEVEL_3_BEST, value).apply()

    var level4BestScore: Int
        get() = prefs.getInt(KEY_LEVEL_4_BEST, 0)
        set(value) = prefs.edit().putInt(KEY_LEVEL_4_BEST, value).apply()

    var clockPlacementBestScore: Int
        get() = prefs.getInt("clock_placement_best", 0)
        set(value) = prefs.edit().putInt("clock_placement_best", value).apply()

    var quarterBestScore: Int
        get() = prefs.getInt("quarter_best_score", 0)
        set(value) = prefs.edit().putInt("quarter_best_score", value).apply()

    var matchBestScore: Int
        get() = prefs.getInt("match_best_score", 0)
        set(value) = prefs.edit().putInt("match_best_score", value).apply()

    var timeCalcBestScore: Int
        get() = prefs.getInt("time_calc_best", 0)
        set(value) = prefs.edit().putInt("time_calc_best", value).apply()

    // ── Arka plan müziği: kapalı mı? ─────────────────────────────────────
    var isMusicMuted: Boolean
        get() = prefs.getBoolean("music_muted", false)
        set(value) = prefs.edit().putBoolean("music_muted", value).apply()

    // ── Geçmiş oyun listesi (modüle göre ayrı) ──────────────────────────────
    /**
     * [moduleId] – örn. "clock_reading", "math_addition", "letter_learning"
     * Her modülün geçmişi kendi anahtarında saklanır.
     * Format: "level,score,passed,timestamp" → "|" ile birleştirilmiş satırlar
     */
    fun addToHistory(entry: ScoreEntry, moduleId: String) {
        val key     = historyKey(moduleId)
        val current = getHistory(moduleId).toMutableList()
        current.add(0, entry)
        val trimmed = current.take(MAX_HISTORY)
        val serialized = trimmed.joinToString("|") {
            "${it.level},${it.score},${it.passed},${it.timestamp}"
        }
        prefs.edit().putString(key, serialized).apply()
    }

    fun getHistory(moduleId: String): List<ScoreEntry> {
        val raw = prefs.getString(historyKey(moduleId), "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            try {
                val p = entry.split(",")
                if (p.size != 4) null
                else ScoreEntry(
                    level     = p[0].toInt(),
                    score     = p[1].toInt(),
                    passed    = p[2].toBoolean(),
                    timestamp = p[3].toLong()
                )
            } catch (e: Exception) { null }
        }
    }

    fun shouldShowLevelSkip(): Boolean = hasPassedLevel1

    /** Tüm verileri sıfırla (debug için) */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
