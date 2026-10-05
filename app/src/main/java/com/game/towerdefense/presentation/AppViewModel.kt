package com.game.towerdefense.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.game.towerdefense.TowerDefenseApp
import com.game.towerdefense.data.audio.Sfx
import com.game.towerdefense.domain.model.GameConfig
import com.game.towerdefense.domain.model.GameSettings
import com.game.towerdefense.domain.model.Progress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Menu : Screen
    data object Levels : Screen
    data object Upgrades : Screen
    data object Settings : Screen
    data object Game : Screen
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as TowerDefenseApp).container

    val config: GameConfig get() = container.config

    val progress: StateFlow<Progress> =
        container.repository.progress.stateIn(viewModelScope, SharingStarted.Eagerly, Progress())

    val settings: StateFlow<GameSettings> =
        container.repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, GameSettings())

    var screen by mutableStateOf<Screen>(Screen.Menu)
        private set

    fun navigate(target: Screen) {
        container.audio.play(Sfx.CLICK)
        screen = target
    }

    fun buyUpgrade(id: String) {
        viewModelScope.launch {
            if (container.buyUpgrade(id)) container.audio.play(Sfx.UPGRADE)
        }
    }

    fun setMusic(enabled: Boolean) {
        viewModelScope.launch { container.repository.setMusic(enabled) }
    }

    fun setSound(enabled: Boolean) {
        viewModelScope.launch { container.repository.setSound(enabled) }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch { container.repository.setVibration(enabled) }
        if (enabled) container.vibration.vibrate(40)
    }

    fun resetProgress() {
        viewModelScope.launch { container.repository.resetProgress() }
    }
}
