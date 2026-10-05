package com.game.towerdefense.game.engine

import com.game.towerdefense.domain.model.GameConfig
import com.game.towerdefense.domain.model.LevelConfig
import com.game.towerdefense.domain.model.PlayerBonuses
import com.game.towerdefense.domain.model.TowerConfig
import com.game.towerdefense.game.enemies.Enemy
import com.game.towerdefense.game.entities.Effect
import com.game.towerdefense.game.entities.EffectType
import com.game.towerdefense.game.entities.Projectile
import com.game.towerdefense.game.map.GameMap
import com.game.towerdefense.game.towers.Tower
import com.game.towerdefense.game.waves.WaveSpawner
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Логика одного уровня. Не зависит от Android и UI: вызывается с фиксированным шагом [update],
 * поэтому скорость игры не зависит от FPS устройства.
 */
class GameEngine(
    val level: LevelConfig,
    val config: GameConfig,
    bonuses: PlayerBonuses,
    private val listener: GameEventListener?,
) {
    val map = GameMap(level)

    val enemies = ArrayList<Enemy>(128)
    val towers = ArrayList<Tower>(64)
    val projectiles = ArrayList<Projectile>(128)
    val effects = ArrayList<Effect>(128)

    private val enemyPool = Pool { Enemy() }
    private val projectilePool = Pool { Projectile() }
    private val effectPool = Pool { Effect() }
    private val spawner = WaveSpawner()
    private val towerGrid = arrayOfNulls<Tower>(map.cols * map.rows)
    private val tmp = FloatArray(2)
    private val spawnFn: (String) -> Unit = { id -> spawnEnemy(id) }
    private val damageMultiplier = 1f + bonuses.damageBonus

    val maxBaseHp: Int = level.baseHp + bonuses.extraBaseHp
    var baseHp: Int = maxBaseHp
        private set
    var money: Int = level.startMoney + bonuses.extraStartMoney
        private set
    val totalWaves: Int = level.waves.size
    var wavesStarted = 0
        private set
    var wavesCompleted = 0
        private set
    var waveActive = false
        private set
    var status = GameStatus.PLAYING
        private set
    var kills = 0
        private set
    /** Игровое время (для анимаций). Останавливается на паузе. */
    var time = 0f
        private set

    val hpLost: Int get() = (maxBaseHp - baseHp).coerceAtLeast(0)
    val canStartWave: Boolean get() = status == GameStatus.PLAYING && !waveActive && wavesStarted < totalWaves

    // ---------------------------------------------------------------- Действия игрока

    fun towerAt(col: Int, row: Int): Tower? =
        if (map.isInside(col, row)) towerGrid[row * map.cols + col] else null

    fun canBuildAt(col: Int, row: Int): Boolean =
        map.isBuildable(col, row) && towerGrid[row * map.cols + col] == null

    fun towerDamage(tower: Tower): Float = tower.stats.damage * damageMultiplier

    fun build(cfg: TowerConfig, col: Int, row: Int): Tower? {
        if (status != GameStatus.PLAYING || !canBuildAt(col, row) || money < cfg.buildCost) return null
        val tower = Tower(cfg, col, row)
        money -= cfg.buildCost
        towers.add(tower)
        towerGrid[row * map.cols + col] = tower
        addEffect(EffectType.BUILD, tower.x, tower.y, 45f, 0.5f, COLOR_GOLD)
        listener?.onGameEvent(GameEvent.TOWER_BUILT, cfg.id)
        return tower
    }

    fun upgrade(tower: Tower): Boolean {
        if (status != GameStatus.PLAYING || !tower.canUpgrade) return false
        val cost = tower.upgradeCost
        if (money < cost) return false
        money -= cost
        tower.invested += cost
        tower.level++
        addEffect(EffectType.BUILD, tower.x, tower.y, 55f, 0.6f, COLOR_GOLD)
        listener?.onGameEvent(GameEvent.TOWER_UPGRADED, tower.config.id)
        return true
    }

    fun sell(tower: Tower): Int {
        if (status != GameStatus.PLAYING || !towers.remove(tower)) return 0
        towerGrid[tower.row * map.cols + tower.col] = null
        val value = tower.sellValue(config.sellRatio)
        money += value
        addEffect(EffectType.HIT, tower.x, tower.y, 40f, 0.4f, COLOR_GOLD)
        listener?.onGameEvent(GameEvent.TOWER_SOLD, tower.config.id)
        return value
    }

    fun startWave(): Boolean {
        if (!canStartWave) return false
        spawner.start(level.waves[wavesStarted])
        wavesStarted++
        waveActive = true
        listener?.onGameEvent(GameEvent.WAVE_STARTED, wavesStarted.toString())
        return true
    }

    // ---------------------------------------------------------------- Игровой цикл

    fun update(dt: Float) {
        time += dt
        updateEffects(dt)
        if (status != GameStatus.PLAYING) return
        if (waveActive) spawner.update(dt, spawnFn)
        updateEnemies(dt)
        updateTowers(dt)
        updateProjectiles(dt)
        removeDeadEnemies()
        checkEnd()
    }

    private fun spawnEnemy(id: String) {
        val cfg = config.enemies[id] ?: return
        val e = enemyPool.obtain()
        e.reset(cfg, level.hpMultiplier)
        e.animTime = (enemies.size % 7) * 0.37f
        map.positionAt(0f, tmp)
        e.x = tmp[0]
        e.y = tmp[1]
        enemies.add(e)
    }

    private fun updateEnemies(dt: Float) {
        val baseSpeed = config.baseEnemySpeed
        for (i in enemies.indices) {
            val e = enemies[i]
            if (!e.alive) continue
            if (e.slowTimer > 0f) e.slowTimer -= dt
            if (e.hitFlash > 0f) e.hitFlash -= dt
            e.animTime += dt
            e.distance += e.config.speed * baseSpeed * e.speedMultiplier * dt
            if (e.distance >= map.pathLength) {
                // Враг дошёл до базы: наносит урон и удаляется.
                e.alive = false
                baseHp -= e.config.baseDamage
                addEffect(EffectType.EXPLOSION, map.baseX, map.baseY, 45f, 0.4f, COLOR_RED)
                listener?.onGameEvent(GameEvent.BASE_DAMAGED, e.config.id)
            } else {
                map.positionAt(e.distance, tmp)
                e.x = tmp[0]
                e.y = tmp[1]
            }
        }
    }

    private fun updateTowers(dt: Float) {
        for (i in towers.indices) {
            val t = towers[i]
            if (t.fireAnim > 0f) t.fireAnim -= dt
            t.cooldown -= dt
            if (t.cooldown > 0f) continue
            val target = findTarget(t)
            if (target == null) {
                t.cooldown = 0f
                continue
            }
            fire(t, target)
            t.cooldown += 1f / t.stats.attackSpeed
        }
    }

    /** Приоритет по умолчанию — враг, который продвинулся дальше всех к базе. */
    private fun findTarget(t: Tower): Enemy? {
        val range = t.stats.range
        var best: Enemy? = null
        var bestDistance = -1f
        for (i in enemies.indices) {
            val e = enemies[i]
            if (!e.alive) continue
            val dx = e.x - t.x
            val dy = e.y - t.y
            val r = range + e.config.radius * 0.5f
            if (dx * dx + dy * dy <= r * r && e.distance > bestDistance) {
                best = e
                bestDistance = e.distance
            }
        }
        return best
    }

    private fun fire(t: Tower, target: Enemy) {
        t.angle = atan2(target.y - t.y, target.x - t.x)
        t.fireAnim = Tower.FIRE_ANIM
        val p = projectilePool.obtain()
        p.reset(t.config, towerDamage(t), t.x + cos(t.angle) * 28f, t.y + sin(t.angle) * 28f, target)
        projectiles.add(p)
        listener?.onGameEvent(GameEvent.TOWER_SHOT, t.config.id)
    }

    private fun updateProjectiles(dt: Float) {
        var i = projectiles.size - 1
        while (i >= 0) {
            val p = projectiles[i]
            val target = p.target
            if (target != null) {
                if (target.alive && target.generation == p.targetGeneration) {
                    p.targetX = target.x
                    p.targetY = target.y
                } else {
                    p.target = null // цель погибла — снаряд долетает до последней точки
                }
            }
            val dx = p.targetX - p.x
            val dy = p.targetY - p.y
            val d = sqrt(dx * dx + dy * dy)
            val step = p.speed * dt
            if (d <= step + 2f) {
                p.x = p.targetX
                p.y = p.targetY
                impact(p)
                removeProjectileAt(i)
            } else {
                p.dirX = dx / d
                p.dirY = dy / d
                p.x += p.dirX * step
                p.y += p.dirY * step
            }
            i--
        }
    }

    private fun impact(p: Projectile) {
        val cfg = p.tower
        if (cfg.splashRadius > 0f) {
            val r = cfg.splashRadius
            for (i in enemies.indices) {
                val e = enemies[i]
                if (!e.alive) continue
                val dx = e.x - p.x
                val dy = e.y - p.y
                val rr = r + e.config.radius * 0.5f
                if (dx * dx + dy * dy <= rr * rr) applyHit(e, p.damage, cfg)
            }
            addEffect(EffectType.EXPLOSION, p.x, p.y, r, 0.35f, COLOR_ORANGE)
            listener?.onGameEvent(GameEvent.EXPLOSION, cfg.id)
            return
        }
        val e = p.target
        if (e == null) {
            addEffect(EffectType.HIT, p.x, p.y, 8f, 0.15f, COLOR_WHITE)
            return
        }
        if (cfg.slowFactor > 0f) {
            e.slowTimer = cfg.slowDuration
            e.slowFactor = cfg.slowFactor
            addEffect(EffectType.FREEZE, e.x, e.y, e.config.radius + 18f, 0.35f, COLOR_ICE)
        } else if (cfg.armorPenetration > 0f) {
            addEffect(EffectType.MAGIC, e.x, e.y, 26f, 0.3f, COLOR_MAGIC)
        } else {
            addEffect(EffectType.HIT, e.x, e.y, 18f, 0.2f, COLOR_WHITE)
        }
        applyHit(e, p.damage, cfg)
        listener?.onGameEvent(GameEvent.PROJECTILE_HIT, cfg.id)
    }

    /** damage = towerDamage × (1 − armor), где броня частично игнорируется башнями с armorPenetration. */
    private fun applyHit(e: Enemy, rawDamage: Float, cfg: TowerConfig) {
        if (!e.alive) return
        val armor = e.config.armor * (1f - cfg.armorPenetration)
        e.hp -= rawDamage * (1f - armor)
        e.hitFlash = 0.08f
        if (e.hp <= 0f) {
            e.alive = false
            money += e.config.reward
            kills++
            addEffect(EffectType.DEATH, e.x, e.y, e.config.radius, 0.45f, e.config.color)
            listener?.onGameEvent(GameEvent.ENEMY_KILLED, e.config.id)
        }
    }

    private fun removeDeadEnemies() {
        var i = enemies.size - 1
        while (i >= 0) {
            val e = enemies[i]
            if (!e.alive) {
                val last = enemies.size - 1
                enemies[i] = enemies[last]
                enemies.removeAt(last)
                enemyPool.release(e)
            }
            i--
        }
    }

    private fun removeProjectileAt(index: Int) {
        val p = projectiles[index]
        val last = projectiles.size - 1
        projectiles[index] = projectiles[last]
        projectiles.removeAt(last)
        p.target = null
        projectilePool.release(p)
    }

    private fun checkEnd() {
        if (baseHp <= 0) {
            baseHp = 0
            status = GameStatus.DEFEAT
            waveActive = false
            listener?.onGameEvent(GameEvent.DEFEAT, null)
            return
        }
        if (waveActive && spawner.isFinished && enemies.isEmpty()) {
            waveActive = false
            wavesCompleted++
            listener?.onGameEvent(GameEvent.WAVE_COMPLETED, wavesCompleted.toString())
            if (wavesCompleted >= totalWaves) {
                status = GameStatus.VICTORY
                listener?.onGameEvent(GameEvent.VICTORY, null)
            }
        }
    }

    private fun addEffect(type: EffectType, x: Float, y: Float, radius: Float, duration: Float, color: Int) {
        if (effects.size >= MAX_EFFECTS) return
        val fx = effectPool.obtain()
        fx.type = type
        fx.x = x
        fx.y = y
        fx.radius = radius
        fx.duration = duration
        fx.time = 0f
        fx.color = color
        effects.add(fx)
    }

    private fun updateEffects(dt: Float) {
        var i = effects.size - 1
        while (i >= 0) {
            val fx = effects[i]
            fx.time += dt
            if (fx.time >= fx.duration) {
                val last = effects.size - 1
                effects[i] = effects[last]
                effects.removeAt(last)
                effectPool.release(fx)
            }
            i--
        }
    }

    companion object {
        const val MAX_EFFECTS = 300
        val COLOR_GOLD = 0xFFFFD54F.toInt()
        val COLOR_RED = 0xFFE53935.toInt()
        val COLOR_ORANGE = 0xFFFF9800.toInt()
        val COLOR_WHITE = 0xFFFFFFFF.toInt()
        val COLOR_ICE = 0xFF80DEEA.toInt()
        val COLOR_MAGIC = 0xFFCE93D8.toInt()
    }
}
