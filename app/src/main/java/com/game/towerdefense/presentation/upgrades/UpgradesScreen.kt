package com.game.towerdefense.presentation.upgrades

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.towerdefense.domain.model.Progress
import com.game.towerdefense.domain.model.UpgradeConfig
import com.game.towerdefense.presentation.ui.GameButton
import com.game.towerdefense.presentation.ui.ScreenHeader
import com.game.towerdefense.presentation.ui.UiColors
import kotlin.math.roundToInt

@Composable
fun UpgradesScreen(
    upgrades: List<UpgradeConfig>,
    progress: Progress,
    onBuy: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        Modifier
            .fillMaxSize()
            .background(UiColors.MenuBrush)
            .padding(16.dp)
    ) {
        ScreenHeader("Улучшения", onBack) {
            Text("💰 ${progress.currency}", color = UiColors.Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            upgrades.forEach { u ->
                val level = progress.upgradeLevel(u.id).coerceAtMost(u.maxLevel)
                val maxed = level >= u.maxLevel
                val cost = u.costForLevel(level)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(UiColors.Card)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(u.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(u.description, color = UiColors.TextDim, fontSize = 13.sp)
                        Text(
                            "Уровень $level / ${u.maxLevel}   ·   Сейчас: ${bonusText(u, level)}",
                            color = UiColors.Accent,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row {
                            repeat(u.maxLevel) { i ->
                                Box(
                                    Modifier
                                        .padding(end = 4.dp)
                                        .size(width = 26.dp, height = 6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (i < level) UiColors.Gold else UiColors.Disabled)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    GameButton(
                        text = if (maxed) "МАКС" else "Купить · $cost 💰",
                        onClick = { onBuy(u.id) },
                        enabled = !maxed && progress.currency >= cost,
                        color = UiColors.Green,
                    )
                }
            }
            Text(
                "Монеты начисляются за победы на уровнях (больше звёзд — больше награда).",
                color = UiColors.TextDim,
                fontSize = 13.sp,
            )
        }
    }
}

private fun bonusText(u: UpgradeConfig, level: Int): String {
    val value = u.valuePerLevel * level
    return if (u.percent) "+${(value * 100).roundToInt()}${u.unit}" else "+${value.roundToInt()}${u.unit}"
}
