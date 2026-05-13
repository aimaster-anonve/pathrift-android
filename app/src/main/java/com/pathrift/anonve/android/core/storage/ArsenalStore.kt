package com.pathrift.anonve.android.core.storage

import android.content.Context
import com.pathrift.anonve.android.game.towers.TowerType

class ArsenalStore(context: Context) {
    private val prefs = context.getSharedPreferences("pathrift_arsenal", Context.MODE_PRIVATE)

    fun permDamageLevel(type: TowerType): Int =
        prefs.getInt("dmg_${type.name}", 0).coerceIn(0, 3)

    fun permSpeedLevel(type: TowerType): Int =
        prefs.getInt("spd_${type.name}", 0).coerceIn(0, 3)

    fun setPermDamageLevel(type: TowerType, level: Int) =
        prefs.edit().putInt("dmg_${type.name}", level.coerceIn(0, 3)).apply()

    fun setPermSpeedLevel(type: TowerType, level: Int) =
        prefs.edit().putInt("spd_${type.name}", level.coerceIn(0, 3)).apply()

    fun permDamageBonus(type: TowerType): Float {
        return floatArrayOf(0f, 0.10f, 0.20f, 0.35f)[permDamageLevel(type)]
    }

    fun permSpeedBonus(type: TowerType): Float {
        return floatArrayOf(0f, 0.08f, 0.16f, 0.28f)[permSpeedLevel(type)]
    }

    fun dmgUpgradeCost(type: TowerType): Int? {
        val level = permDamageLevel(type)
        if (level >= 3) return null
        return upgradeCosts(type.tier)[level]
    }

    fun speedUpgradeCost(type: TowerType): Int? {
        val level = permSpeedLevel(type)
        if (level >= 3) return null
        return upgradeCosts(type.tier)[level]
    }

    private fun upgradeCosts(tier: Int): IntArray = when (tier) {
        1    -> intArrayOf(25, 60, 120)
        2    -> intArrayOf(40, 100, 200)
        else -> intArrayOf(60, 150, 300)
    }
}
