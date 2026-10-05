package com.game.towerdefense.game.towers

import com.game.towerdefense.domain.model.TowerConfig
import com.game.towerdefense.domain.model.TowerLevelStats
import com.game.towerdefense.game.map.CELL

class Tower(val config: TowerConfig, val col: Int, val row: Int) {
    /** Индекс уровня: 0..levels.size-1 */
    var level = 0
    /** Сколько потрачено на строительство и улучшения (для продажи). */
    var invested = config.buildCost
    var cooldown = 0f
    var angle = -1.5708f
    var fireAnim = 0f

    val x: Float = col * CELL + CELL / 2f
    val y: Float = row * CELL + CELL / 2f

    val stats: TowerLevelStats get() = config.levels[level]
    val maxLevel: Int get() = config.levels.size
    val canUpgrade: Boolean get() = level < config.levels.size - 1
    val upgradeCost: Int get() = if (canUpgrade) config.levels[level + 1].cost else 0

    fun sellValue(ratio: Float): Int = (invested * ratio).toInt()

    companion object {
        const val FIRE_ANIM = 0.15f
    }
}
