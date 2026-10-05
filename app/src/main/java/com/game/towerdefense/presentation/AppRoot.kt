package com.game.towerdefense.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.game.towerdefense.presentation.game.GameScreen
import com.game.towerdefense.presentation.game.GameViewModel
import com.game.towerdefense.presentation.levels.LevelSelectScreen
import com.game.towerdefense.presentation.menu.MenuScreen
import com.game.towerdefense.presentation.settings.SettingsScreen
import com.game.towerdefense.presentation.ui.UiColors
import com.game.towerdefense.presentation.upgrades.UpgradesScreen

@Composable
fun AppRoot() {
    val appVm: AppViewModel = viewModel()
    val gameVm: GameViewModel = viewModel()
    val progress by appVm.progress.collectAsState()
    val settings by appVm.settings.collectAsState()
    val config = appVm.config

    Box(
        Modifier
            .fillMaxSize()
            .background(UiColors.Background)
            .windowInsetsPadding(WindowInsets.displayCutout)
    ) {
        when (appVm.screen) {
            Screen.Menu -> MenuScreen(
                progress = progress,
                levelCount = config.levels.size,
                onPlay = { appVm.navigate(Screen.Levels) },
                onUpgrades = { appVm.navigate(Screen.Upgrades) },
                onSettings = { appVm.navigate(Screen.Settings) },
            )
            Screen.Levels -> LevelSelectScreen(
                levels = config.levels,
                progress = progress,
                onSelect = { index ->
                    gameVm.startLevel(index)
                    appVm.navigate(Screen.Game)
                },
                onBack = { appVm.navigate(Screen.Menu) },
            )
            Screen.Upgrades -> UpgradesScreen(
                upgrades = config.upgrades,
                progress = progress,
                onBuy = appVm::buyUpgrade,
                onBack = { appVm.navigate(Screen.Menu) },
            )
            Screen.Settings -> SettingsScreen(
                settings = settings,
                onMusic = appVm::setMusic,
                onSound = appVm::setSound,
                onVibration = appVm::setVibration,
                onResetProgress = appVm::resetProgress,
                onBack = { appVm.navigate(Screen.Menu) },
            )
            Screen.Game -> GameScreen(
                vm = gameVm,
                settings = settings,
                onMusic = appVm::setMusic,
                onSound = appVm::setSound,
                onVibration = appVm::setVibration,
                onExitToMenu = { appVm.navigate(Screen.Menu) },
            )
        }
    }
}
