package com.game.towerdefense.game.engine

enum class GameStatus { PLAYING, VICTORY, DEFEAT }

enum class GameEvent {
    TOWER_BUILT,
    TOWER_UPGRADED,
    TOWER_SOLD,
    TOWER_SHOT,
    PROJECTILE_HIT,
    EXPLOSION,
    ENEMY_KILLED,
    BASE_DAMAGED,
    WAVE_STARTED,
    WAVE_COMPLETED,
    VICTORY,
    DEFEAT,
}

/** Движок сообщает о событиях (звук, вибрация, аналитика) через этот интерфейс. */
fun interface GameEventListener {
    /** @param tag дополнительный идентификатор (например, id башни или врага) */
    fun onGameEvent(event: GameEvent, tag: String?)
}
