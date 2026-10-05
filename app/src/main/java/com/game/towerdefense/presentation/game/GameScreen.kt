package com.game.towerdefense.presentation.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.game.towerdefense.domain.model.GameSettings
import com.game.towerdefense.game.engine.GameEngine
import com.game.towerdefense.game.map.CELL
import com.game.towerdefense.game.towers.Tower
import com.game.towerdefense.presentation.ui.GameButton
import com.game.towerdefense.presentation.ui.GameButtonBox
import com.game.towerdefense.presentation.ui.OverlayCard
import com.game.towerdefense.presentation.ui.SettingsToggles
import com.game.towerdefense.presentation.ui.StarsText
import com.game.towerdefense.presentation.ui.StatLine
import com.game.towerdefense.presentation.ui.UiColors
import java.util.Locale
import kotlin.math.floor

@Composable
fun GameScreen(
    vm: GameViewModel,
    settings: GameSettings,
    onMusic: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onVibration: (Boolean) -> Unit,
    onExitToMenu: () -> Unit,
) {
    // Автопауза при сворачивании приложения.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) vm.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler { vm.onBack() }

    val engine = vm.engine
    if (engine == null) {
        Box(Modifier.fillMaxSize().background(UiColors.Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = UiColors.Accent)
        }
    } else {
        // Игровой цикл: синхронизирован с кадрами дисплея, логика — фиксированным шагом во ViewModel.
        LaunchedEffect(engine) {
            var last = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    vm.tick((now - last) / 1_000_000_000f)
                    last = now
                }
            }
        }

        Box(Modifier.fillMaxSize().background(UiColors.Background)) {
            Column(Modifier.fillMaxSize()) {
                TopHud(vm, engine)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    GameCanvas(vm, engine)
                }
                BottomPanel(vm, engine)
            }

            val result = vm.result
            when {
                result != null -> ResultOverlay(
                    result = result,
                    hasNext = vm.levelIndex + 1 < vm.levelCount,
                    onNext = { vm.startLevel(vm.levelIndex + 1) },
                    onRetry = { vm.startLevel(vm.levelIndex) },
                    onMenu = {
                        vm.exit()
                        onExitToMenu()
                    },
                )
                vm.showSettings -> OverlayCard {
                    Text("Настройки", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    SettingsToggles(settings, onMusic, onSound, onVibration)
                    GameButton("Назад", vm::closeSettings, Modifier.fillMaxWidth(), color = UiColors.ButtonDark)
                }
                vm.paused -> OverlayCard {
                    Text("ПАУЗА", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    GameButton("Продолжить", vm::resume, Modifier.fillMaxWidth(), color = UiColors.Green)
                    GameButton("Настройки", vm::openSettings, Modifier.fillMaxWidth())
                    GameButton(
                        "Выйти в меню",
                        {
                            vm.exit()
                            onExitToMenu()
                        },
                        Modifier.fillMaxWidth(),
                        color = UiColors.Red,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCanvas(vm: GameViewModel, engine: GameEngine) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(engine) {
                detectTapGestures { pos ->
                    val t = MapTransform.compute(size.width.toFloat(), size.height.toFloat(), engine.map)
                    val wx = (pos.x - t.offsetX) / t.scale
                    val wy = (pos.y - t.offsetY) / t.scale
                    vm.onMapTap(floor(wx / CELL).toInt(), floor(wy / CELL).toInt())
                }
            }
    ) {
        // Чтение счётчика кадров подписывает Canvas на перерисовку каждый игровой кадр.
        if (vm.frame >= 0L) {
            drawGame(engine, vm.selectedCell, vm.selectedTower)
        }
    }
}

@Composable
private fun TopHud(vm: GameViewModel, engine: GameEngine) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(UiColors.Panel)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HudValue("💰", vm.money.toString(), UiColors.Gold)
        HudValue("❤", vm.baseHp.toString(), Color(0xFFFF8A80))
        HudValue("🌊", "Волна ${vm.wave}/${engine.totalWaves}", Color.White)
        Text(
            "${vm.levelIndex + 1}. ${engine.level.name}",
            color = UiColors.TextDim,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        GameButton(
            if (vm.speed2x) "▶▶ x2" else "▶ x1",
            vm::toggleSpeed,
            color = if (vm.speed2x) UiColors.Orange else UiColors.ButtonDark,
            small = true,
        )
        GameButton("❚❚", vm::pause, color = UiColors.ButtonDark, small = true)
        GameButton("⚙", vm::openSettings, color = UiColors.ButtonDark, small = true)
    }
}

@Composable
private fun HudValue(icon: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(4.dp))
        Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun BottomPanel(vm: GameViewModel, engine: GameEngine) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(UiColors.Panel)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            val tower = vm.selectedTower
            when {
                tower != null -> TowerInfo(vm, engine, tower)
                vm.selectedCell != null -> BuildMenu(vm, engine)
                else -> Text(
                    "Нажмите на свободную клетку, чтобы построить башню, или на башню — чтобы улучшить/продать.",
                    color = UiColors.TextDim,
                    fontSize = 13.sp,
                    maxLines = 2,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        GameButton(
            text = if (vm.waveActive) "ВОЛНА ИДЁТ…" else "НАЧАТЬ ВОЛНУ",
            onClick = vm::startWave,
            enabled = vm.canStartWave,
            color = UiColors.Green,
        )
    }
}

@Composable
private fun BuildMenu(vm: GameViewModel, engine: GameEngine) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (cfg in engine.config.towers) {
            val enabled = vm.money >= cfg.buildCost
            GameButtonBox(
                onClick = { vm.build(cfg) },
                enabled = enabled,
                color = UiColors.CardLight,
                small = true,
            ) {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(cfg.color))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${cfg.shortName} ${cfg.buildCost}",
                    color = if (enabled) Color.White else UiColors.TextDim,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
        }
        GameButton("✕", vm::clearSelection, color = UiColors.ButtonDark, small = true)
    }
}

@Composable
private fun TowerInfo(vm: GameViewModel, engine: GameEngine, tower: Tower) {
    @Suppress("UNUSED_VARIABLE")
    val revision = vm.towerRevision // перерисовать панель после улучшения
    val stats = tower.stats
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "${tower.config.name} · Ур. ${tower.level + 1}/${tower.maxLevel}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
            )
            val extra = when {
                tower.config.splashRadius > 0f -> " · Взрыв ${tower.config.splashRadius.toInt()}"
                tower.config.slowFactor > 0f -> " · Замедл. ${(tower.config.slowFactor * 100).toInt()}%"
                tower.config.armorPenetration > 0f -> " · Пробой брони ${(tower.config.armorPenetration * 100).toInt()}%"
                else -> ""
            }
            Text(
                "Урон ${fmt(engine.towerDamage(tower))} · Дальн. ${stats.range.toInt()} · " +
                    "Скор. ${fmt(stats.attackSpeed)}/с$extra",
                color = UiColors.TextDim,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(6.dp))
        if (tower.canUpgrade) {
            GameButton(
                "Улучшить ${tower.upgradeCost}",
                vm::upgradeSelected,
                enabled = vm.money >= tower.upgradeCost,
                color = UiColors.Accent,
                small = true,
            )
        } else {
            GameButton("МАКС", {}, enabled = false, small = true)
        }
        Spacer(Modifier.width(6.dp))
        GameButton(
            "Продать +${tower.sellValue(engine.config.sellRatio)}",
            vm::sellSelected,
            color = UiColors.Red,
            small = true,
        )
        Spacer(Modifier.width(6.dp))
        GameButton("✕", vm::clearSelection, color = UiColors.ButtonDark, small = true)
    }
}

@Composable
private fun ResultOverlay(
    result: GameResult,
    hasNext: Boolean,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onMenu: () -> Unit,
) {
    OverlayCard {
        if (result.victory) {
            Text("ПОБЕДА!", color = UiColors.Gold, fontSize = 30.sp, fontWeight = FontWeight.Black)
            StarsText(result.stars, 40.sp)
            StatLine("Пройдено волн", "${result.wavesPassed} / ${result.totalWaves}")
            StatLine("Уничтожено врагов", result.kills.toString())
            StatLine("Потеряно HP", result.hpLost.toString())
            StatLine("Награда", "+${result.reward} 💰")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasNext) GameButton("Далее ▶", onNext, color = UiColors.Green)
                GameButton("Повторить", onRetry)
                GameButton("В меню", onMenu, color = UiColors.ButtonDark)
            }
        } else {
            Text("ПОРАЖЕНИЕ", color = UiColors.Red, fontSize = 30.sp, fontWeight = FontWeight.Black)
            StatLine("Достигнута волна", "${result.waveReached} / ${result.totalWaves}")
            StatLine("Уничтожено врагов", result.kills.toString())
            if (result.reward > 0) StatLine("Утешительная награда", "+${result.reward} 💰")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton("Повторить", onRetry, color = UiColors.Green)
                GameButton("В меню", onMenu, color = UiColors.ButtonDark)
            }
        }
    }
}

private fun fmt(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
