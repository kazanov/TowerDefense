package com.game.towerdefense.data.monetization

import com.game.towerdefense.domain.service.AdsProvider

/** Заглушка для MVP: реклама отключена. */
class NoAdsProvider : AdsProvider {
    override val isAdsDisabled: Boolean = true
    override fun isRewardedAvailable(): Boolean = false
    override fun showRewarded(onReward: () -> Unit, onClosed: () -> Unit) = onClosed()
    override fun showInterstitial(onClosed: () -> Unit) = onClosed()
}
