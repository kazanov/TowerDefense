package com.game.towerdefense.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(name = "td_progress")

object ProgressKeys {
    val CURRENCY = intPreferencesKey("currency")
    val UNLOCKED_LEVELS = intPreferencesKey("unlocked_levels")
    val STARS = stringPreferencesKey("stars")
    val UPGRADES = stringPreferencesKey("upgrades")
    val MUSIC = booleanPreferencesKey("music")
    val SOUND = booleanPreferencesKey("sound")
    val VIBRATION = booleanPreferencesKey("vibration")
}

/** Простой формат "key:value,key:value" для хранения словарей в Preferences. */
object IntMapCodec {
    fun decode(raw: String?): MutableMap<String, Int> {
        val result = LinkedHashMap<String, Int>()
        if (raw.isNullOrBlank()) return result
        raw.split(',').forEach { entry ->
            val parts = entry.split(':')
            if (parts.size == 2) {
                val v = parts[1].trim().toIntOrNull()
                if (v != null) result[parts[0].trim()] = v
            }
        }
        return result
    }

    fun encode(map: Map<String, Int>): String = map.entries.joinToString(",") { "${it.key}:${it.value}" }
}
