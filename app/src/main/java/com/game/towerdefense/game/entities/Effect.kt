package com.game.towerdefense.game.entities

enum class EffectType { HIT, EXPLOSION, FREEZE, MAGIC, DEATH, BUILD }

class Effect {
    var type = EffectType.HIT
    var x = 0f
    var y = 0f
    var radius = 0f
    var time = 0f
    var duration = 0.3f
    var color = 0

    val progress: Float get() = (time / duration).coerceIn(0f, 1f)
}
