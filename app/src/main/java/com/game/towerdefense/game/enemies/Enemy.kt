package com.game.towerdefense.game.enemies

import com.game.towerdefense.domain.model.EnemyConfig

/** Враг. Объект переиспользуется через пул; [generation] меняется при каждом новом появлении. */
class Enemy {
    lateinit var config: EnemyConfig
        private set
    var generation = 0
        private set
    var alive = false
    var hp = 0f
    var maxHp = 0f
    /** Пройденная по маршруту дистанция — она же «продвинутость к базе». */
    var distance = 0f
    var x = 0f
    var y = 0f
    var slowTimer = 0f
    var slowFactor = 0f
    var hitFlash = 0f
    var animTime = 0f

    val speedMultiplier: Float get() = if (slowTimer > 0f) 1f - slowFactor else 1f
    val isSlowed: Boolean get() = slowTimer > 0f

    fun reset(cfg: EnemyConfig, hpMultiplier: Float) {
        config = cfg
        generation++
        alive = true
        maxHp = cfg.hp * hpMultiplier
        hp = maxHp
        distance = 0f
        slowTimer = 0f
        slowFactor = 0f
        hitFlash = 0f
        animTime = 0f
    }
}
