package com.game.towerdefense.data.config

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.game.towerdefense.domain.model.EnemyConfig
import com.game.towerdefense.domain.model.GameConfig
import com.game.towerdefense.domain.model.GridPoint
import com.game.towerdefense.domain.model.LevelConfig
import com.game.towerdefense.domain.model.TowerConfig
import com.game.towerdefense.domain.model.TowerLevelStats
import com.game.towerdefense.domain.model.UpgradeConfig
import com.game.towerdefense.domain.model.WaveConfig
import com.game.towerdefense.domain.model.WaveGroup
import org.json.JSONArray
import org.json.JSONObject

/** Загружает баланс игры из JSON-файлов в папке assets/config. */
class GameConfigLoader(private val context: Context) {

    fun load(): GameConfig {
        val towersRoot = JSONObject(read("config/towers.json"))
        val enemiesRoot = JSONObject(read("config/enemies.json"))
        val levelsRoot = JSONObject(read("config/levels.json"))
        val upgradesRoot = JSONObject(read("config/upgrades.json"))

        val towers = towersRoot.getJSONArray("towers").objects().map(::parseTower)
        val enemies = enemiesRoot.getJSONArray("enemies").objects().map(::parseEnemy).associateBy { it.id }

        val defaults = levelsRoot.optJSONObject("defaults") ?: JSONObject()
        val waveSets = HashMap<String, List<WaveConfig>>()
        levelsRoot.optJSONObject("waveSets")?.let { sets ->
            sets.keys().forEach { key -> waveSets[key] = parseWaves(sets.getJSONArray(key), enemies) }
        }
        val levels = levelsRoot.getJSONArray("levels").objects().mapIndexed { index, obj ->
            parseLevel(index, obj, defaults, waveSets, enemies)
        }
        val upgrades = upgradesRoot.getJSONArray("upgrades").objects().map(::parseUpgrade)

        return GameConfig(
            towers = towers,
            sellRatio = towersRoot.optDouble("sellRatio", 0.7).toFloat(),
            enemies = enemies,
            baseEnemySpeed = enemiesRoot.optDouble("baseSpeed", 70.0).toFloat(),
            levels = levels,
            upgrades = upgrades,
        )
    }

    private fun parseTower(o: JSONObject): TowerConfig {
        val levels = o.getJSONArray("levels").objects().map { l ->
            TowerLevelStats(
                damage = l.getDouble("damage").toFloat(),
                range = l.getDouble("range").toFloat(),
                attackSpeed = l.getDouble("attackSpeed").toFloat().coerceAtLeast(0.05f),
                cost = l.getInt("cost"),
            )
        }
        require(levels.isNotEmpty()) { "Tower ${o.optString("id")} has no levels" }
        return TowerConfig(
            id = o.getString("id"),
            name = o.getString("name"),
            shortName = o.optString("shortName", o.getString("name")),
            color = color(o.optString("color", "#9E9E9E")),
            levels = levels,
            projectileSpeed = o.optDouble("projectileSpeed", 500.0).toFloat(),
            splashRadius = o.optDouble("splashRadius", 0.0).toFloat(),
            slowFactor = o.optDouble("slowFactor", 0.0).toFloat(),
            slowDuration = o.optDouble("slowDuration", 0.0).toFloat(),
            armorPenetration = o.optDouble("armorPenetration", 0.0).toFloat(),
        )
    }

    private fun parseEnemy(o: JSONObject) = EnemyConfig(
        id = o.getString("id"),
        name = o.optString("name", o.getString("id")),
        hp = o.getDouble("hp").toFloat(),
        speed = o.getDouble("speed").toFloat(),
        reward = o.getInt("reward"),
        armor = o.optDouble("armor", 0.0).toFloat().coerceIn(0f, 0.95f),
        baseDamage = o.optInt("baseDamage", 1),
        radius = o.optDouble("radius", 18.0).toFloat(),
        color = color(o.optString("color", "#E57373")),
    )

    private fun parseWaves(arr: JSONArray, enemies: Map<String, EnemyConfig>): List<WaveConfig> =
        arr.objects().map { w ->
            val groups = w.getJSONArray("groups").objects().mapNotNull { g ->
                val enemyId = g.getString("enemy")
                if (!enemies.containsKey(enemyId)) {
                    Log.w(TAG, "Unknown enemy '$enemyId' in wave config — skipped")
                    null
                } else {
                    WaveGroup(
                        enemyId = enemyId,
                        count = g.getInt("count"),
                        interval = g.optDouble("interval", 1.0).toFloat().coerceAtLeast(0f),
                        delay = g.optDouble("delay", 0.0).toFloat().coerceAtLeast(0f),
                    )
                }
            }
            WaveConfig(groups)
        }

    private fun parseLevel(
        index: Int,
        o: JSONObject,
        defaults: JSONObject,
        waveSets: Map<String, List<WaveConfig>>,
        enemies: Map<String, EnemyConfig>,
    ): LevelConfig {
        fun int(key: String, fallback: Int) = o.optInt(key, defaults.optInt(key, fallback))
        val waves = if (o.has("waves")) {
            parseWaves(o.getJSONArray("waves"), enemies)
        } else {
            val setName = o.optString("waveSet", "standard")
            waveSets[setName] ?: error("Wave set '$setName' not found for level ${index + 1}")
        }
        val path = o.getJSONArray("path").points()
        require(path.size >= 2) { "Level ${index + 1}: path must contain at least 2 points" }
        return LevelConfig(
            index = index,
            name = o.optString("name", "Уровень ${index + 1}"),
            cols = int("cols", 16),
            rows = int("rows", 9),
            path = path,
            obstacles = o.optJSONArray("obstacles")?.points() ?: emptyList(),
            waves = waves,
            hpMultiplier = o.optDouble("hpMultiplier", 1.0).toFloat(),
            startMoney = int("startMoney", 250),
            baseHp = int("baseHp", 20),
            victoryBonus = int("victoryBonus", 100),
        )
    }

    private fun parseUpgrade(o: JSONObject) = UpgradeConfig(
        id = o.getString("id"),
        name = o.getString("name"),
        description = o.optString("description", ""),
        maxLevel = o.getInt("maxLevel"),
        baseCost = o.getInt("baseCost"),
        costStep = o.optInt("costStep", 0),
        valuePerLevel = o.getDouble("valuePerLevel").toFloat(),
        unit = o.optString("unit", ""),
        percent = o.optBoolean("percent", false),
    )

    private fun read(name: String): String =
        context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONArray.points(): List<GridPoint> = (0 until length()).map {
        val p = getJSONArray(it)
        GridPoint(p.getInt(0), p.getInt(1))
    }

    private fun color(value: String): Int = try {
        Color.parseColor(value)
    } catch (e: IllegalArgumentException) {
        Color.GRAY
    }

    private companion object {
        const val TAG = "GameConfigLoader"
    }
}
