package com.game.towerdefense.presentation.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.game.towerdefense.TowerDefenseApp
import com.game.towerdefense.data.audio.Sfx
import com.game.towerdefense.domain.model.GridPoint
import com.game.towerdefense.domain.model.TowerConfig
import com.game.towerdefense.domain.service.AnalyticsEvents
import com.game.towerdefense.game.engine.GameEngine
import com.game.towerdefense.game.engine.GameEvent
import com.game.towerdefense.game.engine.GameEventListener
import com.game.towerdefense.game.engine.GameStatus
import com.game.towerdefense.game.towers.Tower
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class GameResult(
    val victory: Boolean,
    val wavesPassed: Int,
    val waveReached: Int,
    val totalWaves: Int,
    val kills: Int,
    val hpLost: Int,
    val reward: Int,
    val stars: Int,
)

class GameViewModel(application: Application) : AndroidViewModel(application), GameEventListener {

    private val container = (application as TowerDefenseApp).container

    var engine by mutableStateOf<GameEngine?>(null)
        private set
    var levelIndex by mutableIntStateOf(0)
        private set

    // Состояние HUD (копируется из движка каждый кадр)
    var money by mutableIntStateOf(0)
        private set
    var baseHp by mutableIntStateOf(0)
        private set
    var wave by mutableIntStateOf(0)
        private set
    var waveActive by mutableStateOf(false)
        private set
    var canStartWave by mutableStateOf(false)
        private set
    var status by mutableStateOf(GameStatus.PLAYING)
        private set

    var paused by mutableStateOf(false)
        private set
    var speed2x by mutableStateOf(false)
        private set
    var showSettings by mutableStateOf(false)
        private set
    /** Счётчик кадров: Canvas читает его, чтобы перерисовываться. */
    var frame by mutableLongStateOf(0L)
        private set

    var selectedCell by mutableStateOf<GridPoint?>(null)
        private set
    var selectedTower by mutableStateOf<Tower?>(null)
        private set
    /** Увеличивается при улучшении башни, чтобы обновилась панель информации. */
    var towerRevision by mutableIntStateOf(0)
        private set
    var result by mutableStateOf<GameResult?>(null)
        private set

    val levelCount: Int get() = container.config.levels.size

    private var accumulator = 0f
    private var endHandled = false
    private var loadJob: Job? = null

    fun startLevel(index: Int) {
        loadJob?.cancel()
        engine = null
        result = null
        paused = false
        showSettings = false
        speed2x = false
        accumulator = 0f
        endHandled = false
        clearSelection()
        levelIndex = index
        loadJob = viewModelScope.launch {
            val progress = container.repository.progress.first()
            val bonuses = container.getPlayerBonuses(progress)
            val level = container.config.levels[index]
            val newEngine = GameEngine(level, container.config, bonuses, this@GameViewModel)
            status = GameStatus.PLAYING
            syncHud(newEngine)
            engine = newEngine
            container.analytics.logEvent(AnalyticsEvents.LEVEL_STARTED, mapOf("level" to index + 1))
        }
    }

    fun exit() {
        loadJob?.cancel()
        engine = null
        result = null
        paused = false
        showSettings = false
        clearSelection()
    }

    /** Вызывается каждый кадр. Логика обновляется фиксированными шагами — независимо от FPS. */
    fun tick(frameSeconds: Float) {
        val e = engine ?: return
        if (paused) return
        val dt = frameSeconds.coerceIn(0f, MAX_FRAME) * (if (speed2x) 2f else 1f)
        accumulator += dt
        var steps = 0
        while (accumulator >= STEP && steps < MAX_STEPS) {
            e.update(STEP)
            accumulator -= STEP
            steps++
        }
        if (steps >= MAX_STEPS) accumulator = 0f
        syncHud(e)
        frame++
    }

    private fun syncHud(e: GameEngine) {
        money = e.money
        baseHp = e.baseHp
        wave = e.wavesStarted
        waveActive = e.waveActive
        canStartWave = e.canStartWave
        if (e.status != status) {
            status = e.status
            if (status != GameStatus.PLAYING) handleEnd(e)
        }
    }

    private fun handleEnd(e: GameEngine) {
        if (endHandled) return
        endHandled = true
        clearSelection()
        paused = false
        showSettings = false
        val analyticsParams = mapOf("level" to levelIndex + 1, "wave" to e.wavesStarted, "kills" to e.kills)
        if (e.status == GameStatus.VICTORY) {
            val stars = container.calculateStars(e.hpLost)
            val reward = e.level.victoryBonus + (stars - 1) * 25
            result = GameResult(true, e.wavesCompleted, e.wavesStarted, e.totalWaves, e.kills, e.hpLost, reward, stars)
            val index = levelIndex
            container.appScope.launch { container.completeLevel(index, stars, reward, levelCount) }
            container.analytics.logEvent(AnalyticsEvents.LEVEL_COMPLETED, analyticsParams + ("stars" to stars))
        } else {
            val reward = e.wavesCompleted * 5
            result = GameResult(false, e.wavesCompleted, e.wavesStarted, e.totalWaves, e.kills, e.hpLost, reward, 0)
            if (reward > 0) container.appScope.launch { container.repository.addCurrency(reward) }
            container.analytics.logEvent(AnalyticsEvents.LEVEL_FAILED, analyticsParams)
        }
    }

    // ---------------------------------------------------------------- Ввод

    fun onMapTap(col: Int, row: Int) {
        val e = engine ?: return
        if (paused || status != GameStatus.PLAYING) return
        val tower = e.towerAt(col, row)
        when {
            tower != null -> {
                selectedTower = if (selectedTower === tower) null else tower
                selectedCell = null
                container.audio.play(Sfx.CLICK)
            }
            e.canBuildAt(col, row) -> {
                val cell = GridPoint(col, row)
                selectedCell = if (selectedCell == cell) null else cell
                selectedTower = null
                container.audio.play(Sfx.CLICK)
            }
            else -> clearSelection()
        }
    }

    fun clearSelection() {
        selectedCell = null
        selectedTower = null
    }

    fun build(cfg: TowerConfig) {
        val e = engine ?: return
        val cell = selectedCell ?: return
        val tower = e.build(cfg, cell.x, cell.y) ?: return
        selectedCell = null
        container.analytics.logEvent(AnalyticsEvents.TOWER_BUILT, mapOf("tower" to tower.config.id))
        syncHud(e)
    }

    fun upgradeSelected() {
        val e = engine ?: return
        val tower = selectedTower ?: return
        if (e.upgrade(tower)) {
            towerRevision++
            container.analytics.logEvent(
                AnalyticsEvents.TOWER_UPGRADED,
                mapOf("tower" to tower.config.id, "level" to tower.level + 1),
            )
            syncHud(e)
        }
    }

    fun sellSelected() {
        val e = engine ?: return
        val tower = selectedTower ?: return
        e.sell(tower)
        selectedTower = null
        syncHud(e)
    }

    fun startWave() {
        val e = engine ?: return
        if (paused) return
        if (e.startWave()) {
            container.analytics.logEvent(AnalyticsEvents.WAVE_STARTED, mapOf("wave" to e.wavesStarted))
            syncHud(e)
        }
    }

    fun toggleSpeed() {
        speed2x = !speed2x
        container.audio.play(Sfx.CLICK)
    }

    fun pause() {
        if (engine != null && status == GameStatus.PLAYING && result == null) paused = true
    }

    fun resume() {
        showSettings = false
        paused = false
    }

    fun openSettings() {
        if (engine == null || status != GameStatus.PLAYING) return
        paused = true
        showSettings = true
    }

    fun closeSettings() {
        showSettings = false
    }

    /** Системная кнопка «Назад» во время игры. */
    fun onBack() {
        when {
            result != null -> Unit
            showSettings -> closeSettings()
            paused -> resume()
            selectedCell != null || selectedTower != null -> clearSelection()
            else -> pause()
        }
    }

    // ---------------------------------------------------------------- События движка

    override fun onGameEvent(event: GameEvent, tag: String?) {
        val audio = container.audio
        val vibration = container.vibration
        when (event) {
            GameEvent.TOWER_BUILT -> {
                audio.play(Sfx.BUILD)
                vibration.vibrate(30)
            }
            GameEvent.TOWER_UPGRADED -> audio.play(Sfx.UPGRADE)
            GameEvent.TOWER_SOLD -> audio.play(Sfx.SELL)
            GameEvent.TOWER_SHOT -> audio.play(
                when (tag) {
                    "cannon" -> Sfx.CANNON
                    "ice" -> Sfx.ICE
                    "magic" -> Sfx.MAGIC
                    else -> Sfx.ARROW
                }
            )
            GameEvent.PROJECTILE_HIT -> audio.play(Sfx.HIT)
            GameEvent.EXPLOSION -> audio.play(Sfx.EXPLOSION)
            GameEvent.ENEMY_KILLED -> {
                audio.play(Sfx.DEATH)
                container.analytics.logEvent(AnalyticsEvents.ENEMY_KILLED, mapOf("enemy" to tag))
            }
            GameEvent.BASE_DAMAGED -> {
                audio.play(Sfx.BASE_HIT)
                vibration.vibrate(120)
            }
            GameEvent.WAVE_STARTED -> audio.play(Sfx.WAVE_START)
            GameEvent.WAVE_COMPLETED -> Unit
            GameEvent.VICTORY -> {
                audio.play(Sfx.VICTORY)
                vibration.vibrate(300)
            }
            GameEvent.DEFEAT -> {
                audio.play(Sfx.DEFEAT)
                vibration.vibrate(500)
            }
        }
    }

    private companion object {
        const val STEP = 1f / 60f
        const val MAX_FRAME = 0.1f
        const val MAX_STEPS = 12
    }
}
