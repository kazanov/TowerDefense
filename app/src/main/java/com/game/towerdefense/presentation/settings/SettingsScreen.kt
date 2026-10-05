package com.game.towerdefense.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.game.towerdefense.domain.model.GameSettings
import com.game.towerdefense.presentation.ui.GameButton
import com.game.towerdefense.presentation.ui.ScreenHeader
import com.game.towerdefense.presentation.ui.SettingsToggles
import com.game.towerdefense.presentation.ui.UiColors

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onMusic: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onVibration: (Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(UiColors.MenuBrush)
            .padding(16.dp)
    ) {
        ScreenHeader("Настройки", onBack)
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsToggles(settings, onMusic, onSound, onVibration)
            Spacer(Modifier.height(20.dp))
            GameButton("Сбросить прогресс", { confirmReset = true }, Modifier.fillMaxWidth(), color = UiColors.Red)
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить прогресс?") },
            text = { Text("Будут удалены открытые уровни, звёзды, монеты и улучшения. Настройки звука сохранятся.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onResetProgress()
                }) { Text("Сбросить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Отмена") }
            },
        )
    }
}
