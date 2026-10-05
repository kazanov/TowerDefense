package com.game.towerdefense.game.engine

/** Простой object pool, чтобы не создавать врагов/снаряды/эффекты каждый кадр. */
class Pool<T>(private val factory: () -> T) {
    private val free = ArrayList<T>(64)

    fun obtain(): T = if (free.isEmpty()) factory() else free.removeAt(free.size - 1)

    fun release(item: T) {
        free.add(item)
    }
}
