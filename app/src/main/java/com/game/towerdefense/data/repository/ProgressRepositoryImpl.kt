package com.game.towerdefense.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.game.towerdefense.data.local.IntMapCodec
import com.game.towerdefense.data.local.ProgressKeys
import com.game.towerdefense.data.local.progressDataStore
import com.game.towerdefense.domain.model.GameSettings
import com.game.towerdefense.domain.model.Progress
import com.game.towerdefense.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class ProgressRepositoryImpl(context: Context) : ProgressRepository {

    private val store: DataStore<Preferences> = context.applicationContext.progressDataStore

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override val progress: Flow<Progress> = data.map { p ->
        Progress(
            currency = p[ProgressKeys.CURRENCY] ?: 0,
            unlockedLevels = max(1, p[ProgressKeys.UNLOCKED_LEVELS] ?: 1),
            stars = IntMapCodec.decode(p[ProgressKeys.STARS])
                .mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }
                .toMap(),
            upgrades = IntMapCodec.decode(p[ProgressKeys.UPGRADES]),
        )
    }.distinctUntilChanged()

    override val settings: Flow<GameSettings> = data.map { p ->
        GameSettings(
            music = p[ProgressKeys.MUSIC] ?: true,
            sound = p[ProgressKeys.SOUND] ?: true,
            vibration = p[ProgressKeys.VIBRATION] ?: true,
        )
    }.distinctUntilChanged()

    override suspend fun completeLevel(levelIndex: Int, stars: Int, reward: Int, totalLevels: Int) {
        store.edit { p ->
            p[ProgressKeys.CURRENCY] = (p[ProgressKeys.CURRENCY] ?: 0) + reward
            val starMap = IntMapCodec.decode(p[ProgressKeys.STARS])
            val key = levelIndex.toString()
            starMap[key] = max(starMap[key] ?: 0, stars)
            p[ProgressKeys.STARS] = IntMapCodec.encode(starMap)
            val unlocked = p[ProgressKeys.UNLOCKED_LEVELS] ?: 1
            p[ProgressKeys.UNLOCKED_LEVELS] = min(totalLevels, max(unlocked, levelIndex + 2))
        }
    }

    override suspend fun addCurrency(amount: Int) {
        if (amount == 0) return
        store.edit { p -> p[ProgressKeys.CURRENCY] = max(0, (p[ProgressKeys.CURRENCY] ?: 0) + amount) }
    }

    override suspend fun purchaseUpgrade(id: String, maxLevel: Int, costForLevel: (Int) -> Int): Boolean {
        var success = false
        store.edit { p ->
            val currency = p[ProgressKeys.CURRENCY] ?: 0
            val upgrades = IntMapCodec.decode(p[ProgressKeys.UPGRADES])
            val level = upgrades[id] ?: 0
            val cost = costForLevel(level)
            if (level < maxLevel && currency >= cost) {
                p[ProgressKeys.CURRENCY] = currency - cost
                upgrades[id] = level + 1
                p[ProgressKeys.UPGRADES] = IntMapCodec.encode(upgrades)
                success = true
            }
        }
        return success
    }

    override suspend fun setMusic(enabled: Boolean) {
        store.edit { it[ProgressKeys.MUSIC] = enabled }
    }

    override suspend fun setSound(enabled: Boolean) {
        store.edit { it[ProgressKeys.SOUND] = enabled }
    }

    override suspend fun setVibration(enabled: Boolean) {
        store.edit { it[ProgressKeys.VIBRATION] = enabled }
    }

    override suspend fun resetProgress() {
        store.edit { p ->
            p.remove(ProgressKeys.CURRENCY)
            p.remove(ProgressKeys.UNLOCKED_LEVELS)
            p.remove(ProgressKeys.STARS)
            p.remove(ProgressKeys.UPGRADES)
        }
    }
}
