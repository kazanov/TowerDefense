package com.game.towerdefense.di

import android.content.Context
import com.game.towerdefense.data.analytics.LogcatAnalytics
import com.game.towerdefense.data.audio.GameAudio
import com.game.towerdefense.data.config.GameConfigLoader
import com.game.towerdefense.data.monetization.NoAdsProvider
import com.game.towerdefense.data.repository.ProgressRepositoryImpl
import com.game.towerdefense.data.vibration.GameVibration
import com.game.towerdefense.domain.model.GameConfig
import com.game.towerdefense.domain.repository.ProgressRepository
import com.game.towerdefense.domain.service.AdsProvider
import com.game.towerdefense.domain.service.Analytics
import com.game.towerdefense.domain.service.AnalyticsEvents
import com.game.towerdefense.domain.usecase.BuyUpgradeUseCase
import com.game.towerdefense.domain.usecase.CalculateStarsUseCase
import com.game.towerdefense.domain.usecase.CompleteLevelUseCase
import com.game.towerdefense.domain.usecase.GetPlayerBonusesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Ручной DI-контейнер (без Hilt — проще собирать и читать). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val config: GameConfig by lazy { GameConfigLoader(appContext).load() }
    val repository: ProgressRepository = ProgressRepositoryImpl(appContext)
    val audio = GameAudio(appContext)
    val vibration = GameVibration(appContext)
    val analytics: Analytics = LogcatAnalytics()
    val ads: AdsProvider = NoAdsProvider()

    val calculateStars = CalculateStarsUseCase()
    val getPlayerBonuses by lazy { GetPlayerBonusesUseCase(config.upgrades) }
    val buyUpgrade by lazy { BuyUpgradeUseCase(repository, config.upgrades) }
    val completeLevel = CompleteLevelUseCase(repository)

    fun start() {
        audio.init()
        appScope.launch {
            repository.settings.collect { s ->
                audio.setSoundEnabled(s.sound)
                audio.setMusicEnabled(s.music)
                vibration.enabled = s.vibration
            }
        }
        analytics.logEvent(AnalyticsEvents.GAME_STARTED)
    }
}
