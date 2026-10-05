package com.game.towerdefense.domain.repository

import com.game.towerdefense.domain.model.GameSettings
import com.game.towerdefense.domain.model.Progress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    val progress: Flow<Progress>
    val settings: Flow<GameSettings>

    suspend fun completeLevel(levelIndex: Int, stars: Int, reward: Int, totalLevels: Int)
    suspend fun addCurrency(amount: Int)

    /** Атомарная покупка: проверяет баланс и уровень внутри одной транзакции. */
    suspend fun purchaseUpgrade(id: String, maxLevel: Int, costForLevel: (Int) -> Int): Boolean

    suspend fun setMusic(enabled: Boolean)
    suspend fun setSound(enabled: Boolean)
    suspend fun setVibration(enabled: Boolean)
    suspend fun resetProgress()
}
