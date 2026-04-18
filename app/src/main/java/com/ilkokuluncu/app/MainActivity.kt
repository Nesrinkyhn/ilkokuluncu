package com.ilkokuluncu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ilkokuluncu.app.data.TrainLevelMode
import com.ilkokuluncu.app.ui.screens.ClockGameScreen
import com.ilkokuluncu.app.ui.screens.ClockPlacementScreen
import com.ilkokuluncu.app.ui.screens.LevelSelectionScreen
import com.ilkokuluncu.app.ui.screens.MainMenuScreen
import com.ilkokuluncu.app.ui.screens.SettingsScreen
import com.ilkokuluncu.app.ui.screens.BallGameScreen
import com.ilkokuluncu.app.ui.screens.TrainGameScreen
import com.ilkokuluncu.app.ui.theme.IlkokuluncuTheme
import com.ilkokuluncu.app.viewmodel.BallGameViewModel
import com.ilkokuluncu.app.viewmodel.ClockGameViewModel
import com.ilkokuluncu.app.viewmodel.ClockPlacementViewModel
import com.ilkokuluncu.app.viewmodel.MainViewModel
import com.ilkokuluncu.app.viewmodel.NavigationDestination
import com.ilkokuluncu.app.viewmodel.TrainGameViewModel
import com.ilkokuluncu.app.viewmodel.QuarterGameViewModel
import com.ilkokuluncu.app.data.ClockPlacementEvent
import com.ilkokuluncu.app.ui.screens.QuarterGameScreen
import com.ilkokuluncu.app.ui.screens.MatchGameScreen
import com.ilkokuluncu.app.ui.screens.TimeCalcGameScreen
import com.ilkokuluncu.app.ui.screens.MultiplicationBallGameScreen
import com.ilkokuluncu.app.ui.screens.PatternBalloonGameScreen
import com.ilkokuluncu.app.ui.screens.PatternPuzzleScreen
import com.ilkokuluncu.app.ui.screens.MarioGameScreen
import com.ilkokuluncu.app.viewmodel.MultiplicationGameViewModel
import com.ilkokuluncu.app.viewmodel.PatternGameViewModel
import com.ilkokuluncu.app.viewmodel.PatternPuzzleViewModel
import com.ilkokuluncu.app.viewmodel.MarioGameViewModel
import com.ilkokuluncu.app.data.PatternGameEvent
import com.ilkokuluncu.app.data.MarioEvent
import com.ilkokuluncu.app.viewmodel.MatchGameViewModel
import com.ilkokuluncu.app.viewmodel.TimeCalcGameViewModel
import com.ilkokuluncu.app.data.ClockGameEvent
import com.ilkokuluncu.app.data.TrainGameEvent
import com.ilkokuluncu.app.data.MatchGameEvent
import com.ilkokuluncu.app.data.TimeCalcEvent
import com.ilkokuluncu.app.ui.effects.MusicManager
import com.ilkokuluncu.app.ads.AdManager
import com.ilkokuluncu.app.ads.AdConfig
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

class MainActivity : ComponentActivity() {

    private val mainViewModel:           MainViewModel           by viewModels()
    private val clockGameViewModel:      ClockGameViewModel      by viewModels()
    private val trainGameViewModel:      TrainGameViewModel      by viewModels()
    private val ballGameViewModel:       BallGameViewModel       by viewModels()
    private val clockPlacementViewModel: ClockPlacementViewModel by viewModels()
    private val quarterGameViewModel:    QuarterGameViewModel    by viewModels()
    private val matchGameViewModel:      MatchGameViewModel      by viewModels()
    private val timeCalcGameViewModel:       TimeCalcGameViewModel       by viewModels()
    private val multiplicationGameViewModel: MultiplicationGameViewModel by viewModels()
    private val patternGameViewModel:        PatternGameViewModel        by viewModels()
    private val patternPuzzleViewModel:      PatternPuzzleViewModel      by viewModels()
    private val marioGameViewModel:           MarioGameViewModel          by viewModels()

    lateinit var adManager: AdManager

    private lateinit var musicManager: MusicManager

    // Saat okuma bölümü ekranları → müzik çalmalı
    private fun isClockReadingDestination(dest: NavigationDestination): Boolean = when (dest) {
        is NavigationDestination.LevelSelection -> dest.module.id == "clock_reading"
        is NavigationDestination.Game           -> dest.moduleId  == "clock_reading"
        is NavigationDestination.TestGame       -> dest.moduleId  == "clock_reading"
        is NavigationDestination.Training       -> true
        else                                    -> false
    }

    override fun onPause() {
        super.onPause()
        musicManager.pauseForLifecycle()
    }

    override fun onResume() {
        super.onResume()
        val dest   = mainViewModel.currentDestination.value
        val muted  = mainViewModel.isMusicMuted.value
        if (isClockReadingDestination(dest)) musicManager.resumeForLifecycle(muted)
    }

    override fun onDestroy() {
        super.onDestroy()
        musicManager.release()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicManager = MusicManager(this)

        // ── AdMob başlatma ────────────────────────────────────────────────────
        adManager = AdManager(this)
        adManager.initialize()

        setContent {
            IlkokuluncuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentDestination by mainViewModel.currentDestination.collectAsState()
                    val gameModules        by mainViewModel.gameModules.collectAsState(initial = emptyList())
                    val scoreSummary       by mainViewModel.scoreSummary.collectAsState()
                    val isMusicMuted       by mainViewModel.isMusicMuted.collectAsState()

                    // Navigasyona göre müziği başlat / durdur
                    LaunchedEffect(currentDestination) {
                        if (isClockReadingDestination(currentDestination)) {
                            musicManager.startMusic(isMusicMuted)
                        } else {
                            musicManager.stopMusic()
                        }
                    }

                    // Mute toggle → anında uygula
                    LaunchedEffect(isMusicMuted) {
                        musicManager.applyMute(isMusicMuted)
                    }

                    when (val destination = currentDestination) {

                        is NavigationDestination.MainMenu -> {
                            MainMenuScreen(
                                gameModules     = gameModules,
                                onGameClick     = { moduleId -> mainViewModel.navigateToModule(moduleId) },
                                onSettingsClick = { mainViewModel.navigateToSettings() },
                                isMusicMuted    = isMusicMuted,
                                onToggleMusic   = { mainViewModel.toggleMusic() },
                                adManager       = adManager
                            )
                        }

                        is NavigationDestination.Settings -> {
                            SettingsScreen(
                                onBackClick = { mainViewModel.navigateToMainMenu() }
                            )
                        }

                        is NavigationDestination.LevelSelection -> {
                            LevelSelectionScreen(
                                gameModule      = destination.module,
                                scoreSummary    = if (destination.module.id == "clock_reading") scoreSummary else null,
                                onBackClick     = { mainViewModel.navigateToMainMenu() },
                                onLevelClick    = { level ->
                                    mainViewModel.navigateToLevel(destination.module.id, level.id)
                                },
                                onTrainingClick = { trainId ->
                                    mainViewModel.navigateToTraining(trainId)
                                },
                                onTestClick     = { levelNum ->
                                    val levelId = when (levelNum) {
                                        1 -> "clock_full_hours"
                                        2 -> "clock_half_hours"
                                        else -> "clock_quarter_hours"
                                    }
                                    mainViewModel.navigateToTest(levelId)
                                },
                                adManager       = adManager
                            )
                        }

                        is NavigationDestination.Training -> {
                            when (destination.trainId) {
                                "clock_hand_placement" -> {
                                    val placementState by clockPlacementViewModel.state.collectAsState()
                                    LaunchedEffect(destination) {
                                        clockPlacementViewModel.onEvent(ClockPlacementEvent.StartFresh)
                                    }
                                    ClockPlacementScreen(
                                        state       = placementState,
                                        onEvent     = { clockPlacementViewModel.onEvent(it) },
                                        onBackPress = {
                                            adManager.onGameCompleted(this@MainActivity) {
                                                mainViewModel.refreshHistory()
                                                mainViewModel.navigateBack()
                                            }
                                        }
                                    )
                                }
                                "quarter_game" -> {
                                    val quarterState by quarterGameViewModel.state.collectAsState()
                                    LaunchedEffect(destination) {
                                        quarterGameViewModel.startFresh()
                                    }
                                    QuarterGameScreen(
                                        state       = quarterState,
                                        onEvent     = { quarterGameViewModel.onEvent(it) },
                                        onBackPress = {
                                            adManager.onGameCompleted(this@MainActivity) {
                                                mainViewModel.refreshHistory()
                                                mainViewModel.navigateBack()
                                            }
                                        }
                                    )
                                }
                                "time_calc_game" -> {
                                    val tcState by timeCalcGameViewModel.state.collectAsState()
                                    LaunchedEffect(destination) {
                                        timeCalcGameViewModel.startFresh()
                                    }
                                    TimeCalcGameScreen(
                                        state       = tcState,
                                        onEvent     = { timeCalcGameViewModel.onEvent(it) },
                                        onBackPress = {
                                            adManager.onGameCompleted(this@MainActivity) {
                                                mainViewModel.refreshHistory()
                                                mainViewModel.navigateBack()
                                            }
                                        }
                                    )
                                }
                                "match_game" -> {
                                    val matchState by matchGameViewModel.state.collectAsState()
                                    LaunchedEffect(destination) {
                                        matchGameViewModel.startFresh()
                                    }
                                    MatchGameScreen(
                                        state   = matchState,
                                        onEvent = { ev ->
                                            if (ev is MatchGameEvent.BackToMenu) {
                                                adManager.onGameCompleted(this@MainActivity) {
                                                    mainViewModel.refreshHistory()
                                                    mainViewModel.navigateBack()
                                                }
                                            } else {
                                                matchGameViewModel.onEvent(ev)
                                            }
                                        },
                                        onBackPress = {
                                            adManager.onGameCompleted(this@MainActivity) {
                                                mainViewModel.refreshHistory()
                                                mainViewModel.navigateBack()
                                            }
                                        }
                                    )
                                }
                                else -> mainViewModel.navigateBack()
                            }
                        }

                        is NavigationDestination.TestGame -> {
                            val trainState by trainGameViewModel.gameState.collectAsState()
                            val clockState by clockGameViewModel.gameState.collectAsState()

                            when (destination.levelId) {
                                "clock_full_hours" -> {
                                    LaunchedEffect(destination) {
                                        clockGameViewModel.onEvent(ClockGameEvent.StartTest)
                                    }
                                    ClockGameScreen(
                                        gameState          = clockState,
                                        onEvent            = { clockGameViewModel.onEvent(it) },
                                        onBackPress        = {
                                            clockGameViewModel.saveSessionToHistory()
                                            mainViewModel.refreshHistory()
                                            mainViewModel.navigateBack()
                                        },
                                        onNavigateToLevel2 = {
                                            clockGameViewModel.saveSessionToHistory()
                                            mainViewModel.refreshHistory()
                                            mainViewModel.navigateAfterLevel1Passed()
                                        }
                                    )
                                }
                                "clock_half_hours" -> {
                                    LaunchedEffect(destination) {
                                        trainGameViewModel.startFresh(TrainLevelMode.HALF_HOURS)
                                        trainGameViewModel.onEvent(TrainGameEvent.StartTest)
                                    }
                                    TrainGameScreen(
                                        gameState    = trainState,
                                        onEvent      = { trainGameViewModel.onEvent(it) },
                                        onBackPress  = {
                                            trainGameViewModel.saveSessionToHistory()
                                            mainViewModel.refreshHistory()
                                            mainViewModel.navigateBack()
                                        },
                                        onTestPassed = {
                                            trainGameViewModel.saveSessionToHistory()
                                            mainViewModel.refreshHistory()
                                            mainViewModel.navigateAfterLevel2Passed()
                                        }
                                    )
                                }
                                "clock_quarter_hours" -> {
                                    LaunchedEffect(destination) {
                                        trainGameViewModel.startFresh(TrainLevelMode.QUARTER_HOURS)
                                        trainGameViewModel.onEvent(TrainGameEvent.StartTest)
                                    }
                                    TrainGameScreen(
                                        gameState    = trainState,
                                        onEvent      = { trainGameViewModel.onEvent(it) },
                                        onBackPress  = {
                                            trainGameViewModel.saveSessionToHistory()
                                            mainViewModel.refreshHistory()
                                            mainViewModel.navigateBack()
                                        },
                                        onTestPassed = {
                                            trainGameViewModel.saveSessionToHistory()
                                            mainViewModel.refreshHistory()
                                            mainViewModel.navigateAfterLevel3Passed()
                                        }
                                    )
                                }
                                else -> mainViewModel.navigateBack()
                            }
                        }

                        is NavigationDestination.Game -> {
                            val trainState by trainGameViewModel.gameState.collectAsState()
                            val clockState by clockGameViewModel.gameState.collectAsState()

                            when {
                                destination.moduleId == "clock_reading" && destination.levelId == "clock_half_hours" -> {
                                    LaunchedEffect(destination) {
                                        trainGameViewModel.startFresh(TrainLevelMode.HALF_HOURS)
                                    }
                                    TrainGameScreen(
                                        gameState   = trainState,
                                        onEvent     = { trainGameViewModel.onEvent(it) },
                                        onBackPress = {
                                            trainGameViewModel.saveSessionToHistory()
                                            mainViewModel.refreshHistory()
                                            mainViewModel.navigateBack()
                                        },
                                        onTestPassed = {
                                            trainGameViewModel.saveSessionToHistory()
                                            mainViewModel.refreshHistory()
                                            mainViewModel.navigateAfterLevel2Passed()
                                        }
                                    )
                                }
                                destination.moduleId == "clock_reading" && destination.levelId == "clock_quarter_hours" -> {
                                    LaunchedEffect(destination) {
                                        trainGameViewModel.startFresh(TrainLevelMode.QUARTER_HOURS)
                                    }
                                    TrainGameScreen(
                                        gameState   = trainState,
                                        onEvent     = { trainGameViewModel.onEvent(it) },
                                        onBackPress = {
                                            trainGameViewModel.saveSessionToHistory()
                                            mainViewModel.refreshHistory()
                                            mainViewModel.navigateBack()
                                        },
                                        onTestPassed = {
                                            trainGameViewModel.saveSessionToHistory()
                                            mainViewModel.refreshHistory()
                                            mainViewModel.navigateAfterLevel3Passed()
                                        }
                                    )
                                }
                                destination.moduleId == "clock_reading" && destination.levelId == "clock_minute_expert" -> {
                                    val ballState by ballGameViewModel.state.collectAsState()
                                    LaunchedEffect(destination) {
                                        ballGameViewModel.saveSession()
                                    }
                                    BallGameScreen(
                                        state       = ballState,
                                        onEvent     = { ballGameViewModel.onEvent(it) },
                                        viewModel   = ballGameViewModel,
                                        onBackPress = {
                                            ballGameViewModel.saveSession()
                                            mainViewModel.refreshHistory()
                                            mainViewModel.navigateBack()
                                        }
                                    )
                                }
                                destination.moduleId == "clock_reading" -> {
                                    // Önceki test/zafer state'ini temizle (tekrar girişte "başardın" görünmesin)
                                    LaunchedEffect(destination) {
                                        clockGameViewModel.onEvent(ClockGameEvent.RetryTest)
                                    }
                                    ClockGameScreen(
                                        gameState = clockState,
                                        onEvent   = { clockGameViewModel.onEvent(it) },
                                        onBackPress = {
                                            clockGameViewModel.saveSessionToHistory()
                                            mainViewModel.refreshHistory()
                                            mainViewModel.navigateBack()
                                        },
                                        onNavigateToLevel2 = {
                                            clockGameViewModel.saveSessionToHistory()
                                            mainViewModel.refreshHistory()
                                            mainViewModel.navigateAfterLevel1Passed()
                                        }
                                    )
                                }
                                // ── Çarpma ──────────────────────────────────────
                                destination.moduleId == "carpma" && destination.levelId == "math_multiplication" -> {
                                    val multiState by multiplicationGameViewModel.state.collectAsState()
                                    LaunchedEffect(destination) {
                                        multiplicationGameViewModel.startFresh()
                                    }
                                    MultiplicationBallGameScreen(
                                        state       = multiState,
                                        onEvent     = { multiplicationGameViewModel.onEvent(it) },
                                        onBackPress = { mainViewModel.navigateBack() }
                                    )
                                }
                                // ── Örüntüler ────────────────────────────────────
                                destination.moduleId == "oruntuler" && destination.levelId == "math_patterns" -> {
                                    val patternState by patternGameViewModel.state.collectAsState()
                                    LaunchedEffect(destination) {
                                        patternGameViewModel.startFresh()
                                    }
                                    PatternBalloonGameScreen(
                                        state       = patternState,
                                        onEvent     = { patternGameViewModel.onEvent(it) },
                                        onBackPress = { mainViewModel.navigateBack() }
                                    )
                                }
                                destination.moduleId == "oruntuler" && destination.levelId == "math_pattern_puzzle" -> {
                                    val puzzleState by patternPuzzleViewModel.state.collectAsState()
                                    LaunchedEffect(destination) {
                                        patternPuzzleViewModel.startFresh()
                                    }
                                    PatternPuzzleScreen(
                                        state       = puzzleState,
                                        onEvent     = { patternPuzzleViewModel.onEvent(it) },
                                        onBackPress = { mainViewModel.navigateBack() }
                                    )
                                }
                                // ── Mantar Platformer ────────────────────────────
                                destination.moduleId == "mantar" -> {
                                    val marioState by marioGameViewModel.state.collectAsState()
                                    LaunchedEffect(destination) {
                                        marioGameViewModel.startFresh()
                                    }
                                    MarioGameScreen(
                                        state       = marioState,
                                        onEvent     = { marioGameViewModel.onEvent(it) },
                                        onBackPress = { mainViewModel.navigateBack() }
                                    )
                                }
                                else -> mainViewModel.navigateToMainMenu()
                            }
                        }
                    }
                }
            }
        }
    }
}
