package com.game.towerdefense.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object UiColors {
    val Background = Color(0xFF14202B)
    val Panel = Color(0xF01B2A38)
    val Card = Color(0xFF223344)
    val CardLight = Color(0xFF2C3E50)
    val Accent = Color(0xFF29B6F6)
    val Green = Color(0xFF43A047)
    val Red = Color(0xFFE53935)
    val Orange = Color(0xFFFB8C00)
    val Gold = Color(0xFFFFD54F)
    val ButtonDark = Color(0xFF37474F)
    val Disabled = Color(0xFF3E474F)
    val TextDim = Color(0xFFB0BEC5)

    val MenuBrush = Brush.verticalGradient(listOf(Color(0xFF1F4430), Color(0xFF14202B)))
}

@Composable
fun TowerDefenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = UiColors.Accent,
            background = UiColors.Background,
            surface = UiColors.Card,
        ),
        content = content,
    )
}
