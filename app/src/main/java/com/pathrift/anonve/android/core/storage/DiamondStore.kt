package com.pathrift.anonve.android.core.storage

import android.content.Context
import com.pathrift.anonve.android.game.towers.TowerType

/**
 * DiamondStore — persists diamond balance and premium tower unlocks using SharedPreferences.
 * Mirrors iOS DiamondStore pattern.
 *
 * @param premiumStore optional reference used to short-circuit unlock checks for premium users.
 *   Passed as a lambda to avoid circular init order with PremiumStore.
 */
class DiamondStore(context: Context, private val premiumStore: PremiumStore? = null) {

    private val prefs = context.getSharedPreferences("pathrift_diamonds", Context.MODE_PRIVATE)

    init {
        // Ensure BOLT is always in the unlocked set (legacy guard — diamondCost is now 0)
        val set = unlockedTowers.toMutableSet()
        if (set.add(TowerType.BOLT.name)) {
            prefs.edit().putStringSet("unlocked_towers", set).apply()
        }
    }

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
        if (premiumStore?.isPremium == true) return true
        if (type.diamondCost == 0) return true   // FREE: Bolt, Blast, Frost
        return unlockedTowers.contains(type.name)
    }

    fun unlock(type: TowerType): Boolean {
        if (type.diamondCost <= 0) return true
        if (!spend(type.diamondCost)) return false
        saveUnlocked(unlockedTowers.toMutableSet().also { it.add(type.name) })
        return true
    }

    /** Mock IAP unlock — no diamonds spent. Real billing in Phase 7. */
    fun iapUnlock(type: TowerType) {
        saveUnlocked(unlockedTowers.toMutableSet().also { it.add(type.name) })
    }

    private fun saveUnlocked(set: MutableSet<String>) {
        prefs.edit().putStringSet(unlockedKey, set).apply()
    }
}
