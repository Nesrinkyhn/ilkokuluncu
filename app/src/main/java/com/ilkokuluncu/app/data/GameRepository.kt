package com.ilkokuluncu.app.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRepository {

    private val _gameModules = MutableStateFlow<List<GameModule>>(emptyList())
    val gameModules: Flow<List<GameModule>> = _gameModules.asStateFlow()

    init {
        initializeModules()
    }

    private fun initializeModules() {
        _gameModules.value = listOf(

            // ── Saat Okuma – yerleşik, saat skoruyla kilit sistemi ────────────
            GameModule(
                id          = "clock_reading",
                title       = "Saat Okuma",
                description = "Saatleri öğren ve eğlen!",
                icon        = "⏰",
                isInstalled = true,
                version     = "1.0",
                sizeInMB    = 0f,
                levels      = listOf(
                    GameLevel(
                        id            = "clock_full_hours",
                        levelNumber   = 1,
                        title         = "Tam Saatler",
                        description   = "1:00, 2:00, 3:00 gibi tam saatleri öğren",
                        icon          = "🕐",
                        isUnlocked    = true,
                        requiredScore = 0
                    ),
                    GameLevel(
                        id            = "clock_half_hours",
                        levelNumber   = 2,
                        title         = "Yarım Saatler",
                        description   = "1:30, 2:30, 3:30 gibi yarım saatleri öğren",
                        icon          = "🕜",
                        isUnlocked    = true,
                        requiredScore = 0
                    ),
                    GameLevel(
                        id            = "clock_quarter_hours",
                        levelNumber   = 3,
                        title         = "Çeyrek Saatler",
                        description   = "1:15, 1:45 gibi çeyrek saatleri öğren",
                        icon          = "🕒",
                        isUnlocked    = true,
                        requiredScore = 0
                    ),
                    GameLevel(
                        id            = "clock_minute_expert",
                        levelNumber   = 4,
                        title         = "Dakika Uzmanı",
                        description   = "5 geçe, 10 kala… doğru topu yakala!",
                        icon          = "🎯",
                        isUnlocked    = true,
                        requiredScore = 0
                    )
                )
            ),

            // ── Çarpma ────────────────────────────────────────────────────────
            GameModule(
                id          = "carpma",
                title       = "Çarpma",
                description = "Ritmik sayma ve çarpma oyunlarıyla öğren!",
                icon        = "✖️",
                isInstalled = true,
                version     = "1.0",
                sizeInMB    = 0f,
                levels      = listOf(
                    // Oyunlar kartı
                    GameLevel(
                        id            = "math_multiplication",
                        levelNumber   = 1,
                        title         = "Çarpışan Balonlar",
                        description   = "Balonlara dokunarak çarpma işlemlerini çöz!",
                        icon          = "🎈",
                        isUnlocked    = true,
                        requiredScore = 0
                    ),
                    GameLevel(
                        id            = "gold_run",
                        levelNumber   = 2,
                        title         = "Altınları Topla",
                        description   = "Koş, zıpla, altınları topla!",
                        icon          = "⚽",
                        isUnlocked    = true,
                        requiredScore = 0
                    ),
                    // Ritmik Sayma kartı
                    GameLevel(
                        id            = "ritmik_2",
                        levelNumber   = 3,
                        title         = "2'li Ritmik Sayma",
                        description   = "2, 4, 6, 8… ikişer ikişer say!",
                        icon          = "2️⃣",
                        isUnlocked    = true,
                        requiredScore = 0,
                        isComingSoon  = true
                    ),
                    GameLevel(
                        id            = "ritmik_3",
                        levelNumber   = 4,
                        title         = "3'ler",
                        description   = "3, 6, 9, 12… üçer üçer say!",
                        icon          = "3️⃣",
                        isUnlocked    = true,
                        requiredScore = 0,
                        isComingSoon  = true
                    )
                )
            ),


            // ── Örüntüler ─────────────────────────────────────────────────────
            GameModule(
                id          = "oruntuler",
                title       = "Örüntüler",
                description = "Sayı örüntülerini keşfet ve boşlukları doldur!",
                icon        = "🔢",
                isInstalled = true,
                version     = "1.0",
                sizeInMB    = 0f,
                levels      = listOf(
                    GameLevel(
                        id            = "math_patterns",
                        levelNumber   = 1,
                        title         = "Uçan Gaz Balonları",
                        description   = "2'ler, 3'ler, 4'ler, 5'ler! Doğru balonları yakala!",
                        icon          = "🎈",
                        isUnlocked    = true,
                        requiredScore = 0
                    ),
                    GameLevel(
                        id            = "math_pattern_puzzle",
                        levelNumber   = 2,
                        title         = "Boşluklara Sürükle",
                        description   = "Boşluklara doğru sayıyı sürükleyip bırak! 30 soru, 300 saniye.",
                        icon          = "🧩",
                        isUnlocked    = true,
                        requiredScore = 0
                    )
                )
            ),

            // ── Oyunlar ───────────────────────────────────────────────────────

        )
    }

    suspend fun downloadModule(moduleId: String): Result<Boolean> {
        val currentModules = _gameModules.value.toMutableList()
        val moduleIndex = currentModules.indexOfFirst { it.id == moduleId }

        if (moduleIndex == -1) return Result.failure(Exception("Modül bulunamadı"))

        currentModules[moduleIndex] = currentModules[moduleIndex].copy(
            isDownloading    = true,
            downloadProgress = 0f
        )
        _gameModules.value = currentModules

        for (progress in 0..100 step 10) {
            delay(200)
            val updated = _gameModules.value.toMutableList()
            val idx     = updated.indexOfFirst { it.id == moduleId }
            updated[idx] = updated[idx].copy(downloadProgress = progress / 100f)
            _gameModules.value = updated
        }

        delay(500)
        val final    = _gameModules.value.toMutableList()
        val finalIdx = final.indexOfFirst { it.id == moduleId }
        final[finalIdx] = final[finalIdx].copy(
            isInstalled      = true,
            isDownloading    = false,
            downloadProgress = 1f
        )
        _gameModules.value = final

        return Result.success(true)
    }

    fun getModule(moduleId: String): GameModule? =
        _gameModules.value.find { it.id == moduleId }

    fun isModuleInstalled(moduleId: String): Boolean =
        _gameModules.value.find { it.id == moduleId }?.isInstalled ?: false

    fun unlockLevel(moduleId: String, levelId: String) {
        val modules     = _gameModules.value.toMutableList()
        val moduleIndex = modules.indexOfFirst { it.id == moduleId }
        if (moduleIndex == -1) return

        val module = modules[moduleIndex]
        val levels = module.levels.map { level ->
            if (level.id == levelId) level.copy(isUnlocked = true) else level
        }
        modules[moduleIndex] = module.copy(levels = levels)
        _gameModules.value = modules
    }
}
