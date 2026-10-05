package com.game.towerdefense.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.game.towerdefense.domain.model.GameSettings

@Composable
fun GameButtonBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = UiColors.Accent,
    small: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) color else UiColors.Disabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (small) 10.dp else 18.dp, vertical = if (small) 7.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = UiColors.Accent,
    small: Boolean = false,
) {
    GameButtonBox(onClick = onClick, modifier = modifier, enabled = enabled, color = color, small = small) {
        Text(
            text = text,
            color = if (enabled) Color.White else UiColors.TextDim,
            fontWeight = FontWeight.Bold,
            fontSize = if (small) 13.sp else 16.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GameButton("← Назад", onBack, color = UiColors.ButtonDark, small = true)
        Spacer(Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun StarsText(stars: Int, fontSize: TextUnit, max: Int = 3) {
    Row {
        for (i in 0 until max) {
            Text("★", color = if (i < stars) UiColors.Gold else Color(0xFF546E7A), fontSize = fontSize)
        }
    }
}

/** Полупрозрачный оверлей с карточкой по центру; блокирует нажатия на игровое поле. */
@Composable
fun OverlayCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(min = 300.dp, max = 480.dp)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(UiColors.Card)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(UiColors.CardLight)
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsToggles(
    settings: GameSettings,
    onMusic: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onVibration: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleRow("🎵  Музыка", settings.music, onMusic)
        ToggleRow("🔊  Звуки", settings.sound, onSound)
        ToggleRow("📳  Вибрация", settings.vibration, onVibration)
    }
}

@Composable
fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = UiColors.TextDim, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
