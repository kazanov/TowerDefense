package com.game.towerdefense.domain.usecase

import com.game.towerdefense.domain.model.PlayerBonuses
import com.game.towerdefense.domain.model.Progress
import com.game.towerdefense.domain.model.UpgradeConfig
import com.game.towerdefense.domain.model.UpgradeIds
import com.game.towerdefense.domain.repository.ProgressRepository

/** 3 звезды: потеряно 0–2 HP; 2 звезды: 3–7 HP; 1 звезда: 8+ HP. */
class CalculateStarsUseCase {
    operator fun invoke(hpLost: Int): Int = when {
        hpLost <= 2 -> 3
        hpLost <= 7 -> 2
        else -> 1
    }
}

class GetPlayerBonusesUseCase(private val upgrades: List<UpgradeConfig>) {
    operator fun invoke(progress: Progress): PlayerBonuses {
        fun value(id: String): Float {
            val cfg = upgrades.firstOrNull { it.id == id } ?: return 0f
            return cfg.valuePerLevel * progress.upgradeLevel(id).coerceAtMost(cfg.maxLevel)
        }
        return PlayerBonuses(
            extraStartMoney = value(UpgradeIds.START_MONEY).toInt(),
            damageBonus = value(UpgradeIds.TOWER_DAMAGE),
            extraBaseHp = value(UpgradeIds.BASE_HP).toInt(),
        )
    }
}

class BuyUpgradeUseCase(
    private val repository: ProgressRepository,
    private val upgrades: List<UpgradeConfig>,
) {
    suspend operator fun invoke(id: String): Boolean {
        val cfg = upgrades.firstOrNull { it.id == id } ?: return false
        return repository.purchaseUpgrade(id, cfg.maxLevel) { level -> cfg.costForLevel(level) }
    }
}

class CompleteLevelUseCase(private val repository: ProgressRepository) {
    suspend operator fun invoke(levelIndex: Int, stars: Int, reward: Int, totalLevels: Int) {
        repository.completeLevel(levelIndex, stars, reward, totalLevels)
    }
}
