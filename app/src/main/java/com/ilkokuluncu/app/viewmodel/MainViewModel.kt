package com.ilkokuluncu.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ilkokuluncu.app.data.GameModule
import com.ilkokuluncu.app.data.GamePreferences
import com.ilkokuluncu.app.data.GameRepository
import com.ilkokuluncu.app.data.ScoreEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NavigationDestination {
    object MainMenu         : NavigationDestination()
    object Settings         : NavigationDestination()
    object CarpmaMenu       : NavigationDestination()   // Çarpma ana kartlar
    object CarpmaOyunlar    : NavigationDestination()   // Oyunlar alt menü
    object RitmikSaymaMenu  : NavigationDestination()   // Ritmik sayma alt menü
    object Ritmik2Game      : NavigationDestination()   // 2'li Ritmik Sayma oyunu
    object Ritmik3Game      : NavigationDestination()   // 3'ler Ritmik Sayma oyunu
    object Ritmik4Game      : NavigationDestination()   // 4'ler Safari oyunu
    object Ritmik5Game      : NavigationDestination()   // 5'ler Dondurma oyunu
    data class LevelSelection(val module: GameModule) : NavigationDestination()
    data class Game(val moduleId: String, val levelId: String) : NavigationDestination()
    data class TestGame(val moduleId: String, val levelId: String) : NavigationDestination()
    data class Training(val trainId: String) : NavigationDestination()
}

data class ScoreSummary(
    val level1Best: Int,
    val level2Best: Int,
    val level3Best: Int,
    val level4Best: Int,
    val clockPlacementBest: Int = 0,
    val quarterBest: Int = 0,
    val matchBest: Int = 0,
    val timeCalcBest: Int = 0,
    val history: List<ScoreEntry>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository()
    val gamePrefs = GamePreferences(application)

    private val _currentDestination =
        MutableStateFlow<NavigationDestination>(NavigationDestination.MainMenu)
    val currentDestination: StateFlow<NavigationDestination> = _currentDestination.asStateFlow()

    private val _scoreSummary = MutableStateFlow(
        ScoreSummary(
            level1Best = 0, level2Best = 0, level3Best = 0, level4Best = 0,
            history    = emptyList()
        )
    )
    val scoreSummary: StateFlow<ScoreSummary> = _scoreSummary.asStateFlow()

    // ── Müzik sessiz durumu ───────────────────────────────────────────────
    private val _isMusicMuted = MutableStateFlow(gamePrefs.isMusicMuted)
    val isMusicMuted: StateFlow<Boolean> = _isMusicMuted.asStateFlow()

    fun toggleMusic() {
        val newVal = !_isMusicMuted.value
        _isMusicMuted.value = newVal
        gamePrefs.isMusicMuted = newVal
    }

    val gameModules = repository.gameModules

    init {
        applyUnlocks()
        refreshHistory()
    }

    // ── Geçmiş ────────────────────────────────────────────────────────────
    fun refreshHistory() {
        _scoreSummary.value = ScoreSummary(
            level1Best         = gamePrefs.level1BestScore,
            level2Best         = gamePrefs.level2BestScore,
            level3Best         = gamePrefs.level3BestScore,
            level4Best         = gamePrefs.level4BestScore,
            clockPlacementBest = gamePrefs.clockPlacementBestScore,
            quarterBest        = gamePrefs.quarterBestScore,
            matchBest          = gamePrefs.matchBestScore,
            timeCalcBest       = gamePrefs.timeCalcBestScore,
            history            = gamePrefs.getHistory("clock_reading")
        )
    }

    // ── Kilit açma ────────────────────────────────────────────────────────
    private fun applyUnlocks() {
        if (gamePrefs.hasPassedLevel1) repository.unlockLevel("clock_reading", "clock_half_hours")
        if (gamePrefs.hasPassedLevel2) repository.unlockLevel("clock_reading", "clock_quarter_hours")
        if (gamePrefs.hasPassedLevel3) repository.unlockLevel("clock_reading", "clock_minute_expert")
    }

    // ── Navigasyon ────────────────────────────────────────────────────────
    fun navigateToSettings() {
        _currentDestination.value = NavigationDestination.Settings
    }

    fun navigateToModule(moduleId: String) {
        viewModelScope.launch {
            // Çarpma için özel menü
            if (moduleId == "carpma") {
                _currentDestination.value = NavigationDestination.CarpmaMenu
                return@launch
            }
            val module = repository.getModule(moduleId) ?: return@launch
            if (!module.isInstalled) {
                repository.downloadModule(moduleId).onSuccess {
                    val updatedModule = repository.getModule(moduleId)
                    if (updatedModule != null) {
                        _currentDestination.value = NavigationDestination.LevelSelection(updatedModule)
                    }
                }
            } else {
                _currentDestination.value = NavigationDestination.LevelSelection(module)
            }
        }
    }

    fun navigateToCarpmaMenu()      { _currentDestination.value = NavigationDestination.CarpmaMenu }
    fun navigateToCarpmaOyunlar()   { _currentDestination.value = NavigationDestination.CarpmaOyunlar }
    fun navigateToRitmikSaymaMenu() { _currentDestination.value = NavigationDestination.RitmikSaymaMenu }
    fun navigateToRitmik2()         { _currentDestination.value = NavigationDestination.Ritmik2Game }
    fun navigateToRitmik3()         { _currentDestination.value = NavigationDestination.Ritmik3Game }
    fun navigateToRitmik4()         { _currentDestination.value = NavigationDestination.Ritmik4Game }
    fun navigateToRitmik5()         { _currentDestination.value = NavigationDestination.Ritmik5Game }

    fun navigateToLevel(moduleId: String, levelId: String) {
        _currentDestination.value = NavigationDestination.Game(moduleId, levelId)
    }

    fun navigateToTraining(trainId: String) {
        _currentDestination.value = NavigationDestination.Training(trainId)
    }

    fun navigateToTest(levelId: String) {
        _currentDestination.value = NavigationDestination.TestGame("clock_reading", levelId)
    }

    fun navigateAfterLevel1Passed() {
        applyUnlocks()
        val module = repository.getModule("clock_reading")
        if (module != null) _currentDestination.value = NavigationDestination.LevelSelection(module)
    }

    fun navigateAfterLevel2Passed() {
        applyUnlocks()
        val module = repository.getModule("clock_reading")
        if (module != null) _currentDestination.value = NavigationDestination.LevelSelection(module)
    }

    fun navigateAfterLevel3Passed() {
        gamePrefs.hasPassedLevel3 = true
        applyUnlocks()
        val module = repository.getModule("clock_reading")
        if (module != null) _currentDestination.value = NavigationDestination.LevelSelection(module)
    }

    fun navigateToMainMenu() {
        refreshHistory()
        _currentDestination.value = NavigationDestination.MainMenu
    }

    fun navigateBack() {
        when (val destination = _currentDestination.value) {
            is NavigationDestination.Settings -> {
                _currentDestination.value = NavigationDestination.MainMenu
            }
            is NavigationDestination.Game -> {
                // Çarpışan Balonlar → Çarpma menüsüne dön
                if (destination.moduleId == "matematik" && destination.levelId == "carpisan_balonlar") {
                    _currentDestination.value = NavigationDestination.Game("matematik", "math_multiplication")
                } else if (destination.moduleId == "carpma") {
                    _currentDestination.value = NavigationDestination.CarpmaOyunlar
                } else {
                    applyUnlocks()
                    val module = repository.getModule(destination.moduleId)
                    if (module != null) _currentDestination.value = NavigationDestination.LevelSelection(module)
                }
            }
            is NavigationDestination.LevelSelection -> {
                refreshHistory()
                _currentDestination.value = NavigationDestination.MainMenu
            }
            is NavigationDestination.Training -> {
                refreshHistory()
                val module = repository.getModule("clock_reading")
                if (module != null) _currentDestination.value = NavigationDestination.LevelSelection(module)
            }
            is NavigationDestination.TestGame -> {
                val module = repository.getModule(destination.moduleId)
                if (module != null) _currentDestination.value = NavigationDestination.LevelSelection(module)
            }
            is NavigationDestination.CarpmaOyunlar -> {
                _currentDestination.value = NavigationDestination.CarpmaMenu
            }
            is NavigationDestination.RitmikSaymaMenu -> {
                _currentDestination.value = NavigationDestination.CarpmaMenu
            }
            is NavigationDestination.Ritmik2Game,
            is NavigationDestination.Ritmik3Game,
            is NavigationDestination.Ritmik4Game,
            is NavigationDestination.Ritmik5Game -> {
                _currentDestination.value = NavigationDestination.RitmikSaymaMenu
            }
            is NavigationDestination.CarpmaMenu -> {
                _currentDestination.value = NavigationDestination.MainMenu
            }
            is NavigationDestination.MainMenu -> {
                // Ana menüdeyseniz uygulamadan çıkın (Android handle eder)
                Unit
            }
        }
    }
}
