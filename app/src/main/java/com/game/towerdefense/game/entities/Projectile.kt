package com.game.towerdefense.game.entities

import com.game.towerdefense.domain.model.TowerConfig
import com.game.towerdefense.game.enemies.Enemy

class Projectile {
    var x = 0f
    var y = 0f
    var targetX = 0f
    var targetY = 0f
    var dirX = 1f
    var dirY = 0f
    var speed = 0f
    var damage = 0f
    var target: Enemy? = null
    var targetGeneration = 0
    lateinit var tower: TowerConfig
        private set

    fun reset(cfg: TowerConfig, damage: Float, startX: Float, startY: Float, enemy: Enemy) {
        tower = cfg
        this.damage = damage
        x = startX
        y = startY
        target = enemy
        targetGeneration = enemy.generation
        targetX = enemy.x
        targetY = enemy.y
        speed = cfg.projectileSpeed
        dirX = 1f
        dirY = 0f
    }
}
