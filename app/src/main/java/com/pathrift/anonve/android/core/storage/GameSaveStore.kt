package com.pathrift.anonve.android.core.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * SavedTower — Build 15: position stored as fractions of screen size (xFrac, yFrac).
 * Old slotId-based saves (version 1) will not restore towers (wave/gold/lives still restore).
 */
data class SavedTower(
    val xFrac: Double,        // position.x / screenWidth
    val yFrac: Double,        // position.y / screenHeight
    val type: String,
    val level: Int,
    val totalInvested: Int
)

data class GameSaveState(
    val version: Int,
    val savedAt: Long,
    val wave: Int,
    val lives: Int,
    val gold: Int,
    val enemyKills: Int,
    val layoutIndex: Int,
    val towers: List<SavedTower>
)

class GameSaveStore(context: Context) {
    private val prefs = context.getSharedPreferences("pathrift_game_save", Context.MODE_PRIVATE)
    private val currentVersion = 2  // Build 15: changed from slotId to xFrac/yFrac

    fun save(wave: Int, lives: Int, gold: Int, kills: Int, layoutIndex: Int, towers: List<SavedTower>) {
        val towersArr = JSONArray()
        towers.forEach { t ->
            towersArr.put(JSONObject().apply {
                put("xFrac", t.xFrac)
                put("yFrac", t.yFrac)
                put("type", t.type)
                put("level", t.level)
                put("totalInvested", t.totalInvested)
            })
        }
        val obj = JSONObject().apply {
            put("version", currentVersion)
            put("savedAt", System.currentTimeMillis())
            put("wave", wave)
            put("lives", lives)
            put("gold", gold)
            put("enemyKills", kills)
            put("layoutIndex", layoutIndex)
            put("towers", towersArr)
        }
        prefs.edit().putString("save_data", obj.toString()).apply()
    }

    fun load(): GameSaveState? {
        return try {
            val json = prefs.getString("save_data", null) ?: return null
            val obj = JSONObject(json)
            if (obj.getInt("version") != currentVersion) { clear(); return null }
            val arr = obj.getJSONArray("towers")
            val towers = (0 until arr.length()).map { i ->
                val t = arr.getJSONObject(i)
                SavedTower(
                    xFrac = t.getDouble("xFrac"),
                    yFrac = t.getDouble("yFrac"),
                    type = t.getString("type"),
                    level = t.getInt("level"),
                    totalInvested = t.getInt("totalInvested")
                )
            }
            GameSaveState(
                version = obj.getInt("version"),
                savedAt = obj.getLong("savedAt"),
                wave = obj.getInt("wave"),
                lives = obj.getInt("lives"),
                gold = obj.getInt("gold"),
                enemyKills = obj.getInt("enemyKills"),
                layoutIndex = obj.getInt("layoutIndex"),
                towers = towers
            )
        } catch (e: Exception) { null }
    }

    fun hasSave(): Boolean = load() != null
    fun clear() = prefs.edit().remove("save_data").apply()
    val savedWave: Int get() = load()?.wave ?: 0
}
