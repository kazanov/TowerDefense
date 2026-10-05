package com.game.towerdefense.game.waves

import com.game.towerdefense.domain.model.WaveConfig

/** Выпускает врагов текущей волны по группам: у каждой группы своя задержка и интервал. */
class WaveSpawner {
    private var wave: WaveConfig? = null
    private var spawned = IntArray(0)
    private var timers = FloatArray(0)

    var isFinished: Boolean = true
        private set

    fun start(config: WaveConfig) {
        wave = config
        spawned = IntArray(config.groups.size)
        timers = FloatArray(config.groups.size) { config.groups[it].delay }
        isFinished = config.groups.all { it.count <= 0 }
    }

    fun update(dt: Float, spawn: (String) -> Unit) {
        val w = wave ?: return
        var done = true
        for (i in w.groups.indices) {
            val g = w.groups[i]
            if (spawned[i] >= g.count) continue
            timers[i] -= dt
            while (timers[i] <= 0f && spawned[i] < g.count) {
                spawn(g.enemyId)
                spawned[i]++
                timers[i] += g.interval
            }
            if (spawned[i] < g.count) done = false
        }
        isFinished = done
        if (done) wave = null
    }
}
