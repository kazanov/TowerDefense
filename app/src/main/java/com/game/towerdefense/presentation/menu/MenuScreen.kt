package com.game.towerdefense.presentation.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.towerdefense.domain.model.Progress
import com.game.towerdefense.presentation.ui.GameButton
import com.game.towerdefense.presentation.ui.UiColors
import kotlin.math.min

@Composable
fun MenuScreen(
    progress: Progress,
    levelCount: Int,
    onPlay: () -> Unit,
    onUpgrades: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxSize()
            .background(UiColors.MenuBrush)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🏰", fontSize = 60.sp)
            Text("TOWER DEFENSE", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text("💰 ${progress.currency}", color = UiColors.Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "★ ${progress.totalStars} / ${levelCount * 3}   ·   Открыто уровней: " +
                    "${min(progress.unlockedLevels, levelCount)} / $levelCount",
                color = UiColors.TextDim,
                fontSize = 15.sp,
            )
        }
        Column(Modifier.width(250.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GameButton("Играть", onPlay, Modifier.fillMaxWidth(), color = UiColors.Green)
            GameButton("Улучшения", onUpgrades, Modifier.fillMaxWidth())
            GameButton("Настройки", onSettings, Modifier.fillMaxWidth(), color = UiColors.ButtonDark)
        }
    }
}
