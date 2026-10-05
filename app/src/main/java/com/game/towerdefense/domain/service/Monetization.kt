package com.game.towerdefense.domain.service

/**
 * Точки расширения для монетизации (в MVP не реализованы).
 * Позже: rewarded video, межстраничная реклама, покупка валюты, отключение рекламы.
 */
interface AdsProvider {
    val isAdsDisabled: Boolean
    fun isRewardedAvailable(): Boolean
    fun showRewarded(onReward: () -> Unit, onClosed: () -> Unit = {})
    fun showInterstitial(onClosed: () -> Unit = {})
}

interface PurchaseProvider {
    fun buyCurrencyPack(packId: String, onResult: (Boolean) -> Unit)
    fun buyNoAds(onResult: (Boolean) -> Unit)
}
