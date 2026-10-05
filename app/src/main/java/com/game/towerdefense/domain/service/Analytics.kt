package com.game.towerdefense.domain.service

/** Абстракция аналитики. В MVP — запись в Logcat, позже можно подключить Firebase и т.п. */
interface Analytics {
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
}

object AnalyticsEvents {
    const val GAME_STARTED = "game_started"
    const val LEVEL_STARTED = "level_started"
    const val WAVE_STARTED = "wave_started"
    const val TOWER_BUILT = "tower_built"
    const val TOWER_UPGRADED = "tower_upgraded"
    const val ENEMY_KILLED = "enemy_killed"
    const val LEVEL_COMPLETED = "level_completed"
    const val LEVEL_FAILED = "level_failed"
    const val AD_WATCHED = "ad_watched"
}
