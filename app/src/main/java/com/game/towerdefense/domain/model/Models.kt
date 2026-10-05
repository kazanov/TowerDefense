package com.game.towerdefense.domain.model

/** Клетка игрового поля. */
data class GridPoint(val x: Int, val y: Int)

data class TowerLevelStats(
    val damage: Float,
    val range: Float,
    val attackSpeed: Float,
    /** Для 1-го уровня — стоимость строительства, для остальных — стоимость улучшения. */
    val cost: Int,
)

data class TowerConfig(
    val id: String,
    val name: String,
    val shortName: String,
    val color: Int,
    val levels: List<TowerLevelStats>,
    val projectileSpeed: Float,
    val splashRadius: Float,
    val slowFactor: Float,
    val slowDuration: Float,
    val armorPenetration: Float,
) {
    val buildCost: Int get() = levels.first().cost
}

data class EnemyConfig(
    val id: String,
    val name: String,
    val hp: Float,
    val speed: Float,
    val reward: Int,
    /** Доля снижения урона (0.5 = −50%). */
    val armor: Float,
    val baseDamage: Int,
    val radius: Float,
    val color: Int,
)

data class WaveGroup(
    val enemyId: String,
    val count: Int,
    val interval: Float,
    val delay: Float,
)

data class WaveConfig(val groups: List<WaveGroup>)

data class LevelConfig(
    val index: Int,
    val name: String,
    val cols: Int,
    val rows: Int,
    val path: List<GridPoint>,
    val obstacles: List<GridPoint>,
    val waves: List<WaveConfig>,
    val hpMultiplier: Float,
    val startMoney: Int,
    val baseHp: Int,
    val victoryBonus: Int,
)

data class UpgradeConfig(
    val id: String,
    val name: String,
    val description: String,
    val maxLevel: Int,
    val baseCost: Int,
    val costStep: Int,
    val valuePerLevel: Float,
    val unit: String,
    val percent: Boolean,
) {
    fun costForLevel(currentLevel: Int): Int = baseCost + costStep * currentLevel
}

data class GameConfig(
    val towers: List<TowerConfig>,
    val sellRatio: Float,
    val enemies: Map<String, EnemyConfig>,
    val baseEnemySpeed: Float,
    val levels: List<LevelConfig>,
    val upgrades: List<UpgradeConfig>,
)

data class Progress(
    val currency: Int = 0,
    val unlockedLevels: Int = 1,
    val stars: Map<Int, Int> = emptyMap(),
    val upgrades: Map<String, Int> = emptyMap(),
) {
    val totalStars: Int get() = stars.values.sum()
    fun upgradeLevel(id: String): Int = upgrades[id] ?: 0
    fun starsFor(levelIndex: Int): Int = stars[levelIndex] ?: 0
    fun isUnlocked(levelIndex: Int): Boolean = levelIndex < unlockedLevels
}

data class GameSettings(
    val music: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true,
)

/** Бонусы от постоянных улучшений. */
data class PlayerBonuses(
    val extraStartMoney: Int = 0,
    val damageBonus: Float = 0f,
    val extraBaseHp: Int = 0,
)

object UpgradeIds {
    const val START_MONEY = "start_money"
    const val TOWER_DAMAGE = "tower_damage"
    const val BASE_HP = "base_hp"
}
