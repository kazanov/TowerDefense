package com.game.towerdefense.presentation.levels

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.towerdefense.domain.model.LevelConfig
import com.game.towerdefense.domain.model.Progress
import com.game.towerdefense.presentation.ui.ScreenHeader
import com.game.towerdefense.presentation.ui.StarsText
import com.game.towerdefense.presentation.ui.UiColors

@Composable
fun LevelSelectScreen(
    levels: List<LevelConfig>,
    progress: Progress,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        Modifier
            .fillMaxSize()
            .background(UiColors.MenuBrush)
            .padding(16.dp)
    ) {
        ScreenHeader("Выбор уровня", onBack) {
            Text("★ ${progress.totalStars} / ${levels.size * 3}", color = UiColors.Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        levels.indices.chunked(5).forEach { rowIndices ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowIndices.forEach { i ->
                    LevelCard(
                        number = i + 1,
                        name = levels[i].name,
                        unlocked = progress.isUnlocked(i),
                        stars = progress.starsFor(i),
                        onClick = { onSelect(i) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                repeat(5 - rowIndices.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LevelCard(
    number: Int,
    name: String,
    unlocked: Boolean,
    stars: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (unlocked) UiColors.Card else Color(0xFF1A232C))
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (unlocked) "$number" else "🔒",
                color = if (unlocked) Color.White else UiColors.TextDim,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                name,
                color = if (unlocked) UiColors.TextDim else Color(0xFF607D8B),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (unlocked) StarsText(stars, 18.sp)
        }
    }
}
