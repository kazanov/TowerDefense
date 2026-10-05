package com.game.towerdefense.data.audio

enum class Sfx(val volume: Float, val minGapMs: Long) {
    CLICK(0.6f, 40),
    BUILD(0.8f, 50),
    UPGRADE(0.8f, 50),
    SELL(0.7f, 50),
    ARROW(0.35f, 70),
    CANNON(0.55f, 90),
    ICE(0.35f, 90),
    MAGIC(0.35f, 90),
    HIT(0.25f, 60),
    EXPLOSION(0.5f, 90),
    DEATH(0.35f, 60),
    WAVE_START(0.8f, 200),
    VICTORY(0.9f, 500),
    DEFEAT(0.9f, 500),
    BASE_HIT(0.8f, 150),
}
