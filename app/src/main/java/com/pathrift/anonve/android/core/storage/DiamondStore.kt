package com.pathrift.anonve.android.core.storage

import android.content.Context
import com.pathrift.anonve.android.game.towers.TowerType

/**
 * DiamondStore — persists diamond balance and premium tower unlocks using SharedPreferences.
 * Mirrors iOS DiamondStore pattern.
 */
class DiamondStore(context: Context) {

    private val prefs = context.getSharedPreferences("pathrift_diamonds", Context.MODE_PRIVATE)

    var balance: Int
        get() = prefs.getInt("balance", 0)
        set(value) = prefs.edit().putInt("balance", value).apply()

    fun earn(amount: Int) {
        balance += amount
    }

    fun spend(amount: Int): Boolean {
        if (balance < amount) return false
        balance -= amount
        return true
    }

    private val unlockedKey = "unlocked_towers"

    val unlockedTowers: Set<String>
        get() = prefs.getStringSet(unlockedKey, emptySet()) ?: emptySet()

    fun isUnlocked(type: TowerType): Boolean {
        if (type.diamondCost <= 0) return true
        return unlockedTowers.contains(type.name)
    }

    fun unlock(type: TowerType): Boolean {
        if (type.diamondCost <= 0) return true
        if (!spend(type.diamondCost)) return false
        val set = unlockedTowers.toMutableSet()
        set.add(type.name)
        prefs.edit().putStringSet(unlockedKey, set).apply()
        return true
    }
}
