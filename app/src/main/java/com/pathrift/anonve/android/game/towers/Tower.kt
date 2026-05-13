package com.pathrift.anonve.android.game.towers

import com.pathrift.anonve.android.game.enemies.EnemyType

enum class TowerType {
    BOLT,
    BLAST,
    FROST,
    PIERCE,
    CORE,
    INFERNO,
    TESLA,
    NOVA;

    fun damageMultiplier(against: EnemyType): Float = when (this) {
        TowerType.BOLT    -> if (against == EnemyType.RUNNER || against == EnemyType.SPLITTER) 1.5f else 1.0f
        TowerType.BLAST   -> if (against == EnemyType.SWARM) 1.6f else 1.0f
        TowerType.FROST   -> 1.0f
        TowerType.PIERCE  -> if (against == EnemyType.SHIELD) 2.0f else 1.0f
        TowerType.CORE    -> if (against == EnemyType.TANK) 1.6f else 1.0f
        TowerType.INFERNO -> if (against == EnemyType.GHOST) 1.75f else 1.0f
        TowerType.TESLA   -> if (against == EnemyType.SWARM) 1.75f else 1.0f
        TowerType.NOVA    -> if (against == EnemyType.BOSS) 1.5f else 1.0f
    }

    val typeAdvantageHint: String? get() = when (this) {
        TowerType.BOLT    -> "+50% vs Runners"
        TowerType.BLAST   -> "+60% vs Swarms"
        TowerType.FROST   -> null
        TowerType.PIERCE  -> "+100% vs Shields"
        TowerType.CORE    -> "+60% vs Tanks"
        TowerType.INFERNO -> "+75% vs Ghosts"
        TowerType.TESLA   -> "+75% vs Swarms"
        TowerType.NOVA    -> "+50% vs Boss"
    }

    val diamondCost: Int get() = when (this) {
        TowerType.INFERNO -> 50
        TowerType.TESLA   -> 150
        TowerType.NOVA    -> 300
        else              -> 0
    }

    val isPremium: Boolean get() = diamondCost > 0
}

/**
 * Base class for all tower types.
 * Tower stats are immutable per type; state (e.g. attack cooldown) is managed by GameEngine.
 */
abstract class Tower {
    abstract val type: TowerType
    abstract val displayName: String
    abstract val cost: Int
    abstract val damagePerHit: Int
    abstract val attacksPerSecond: Float
    abstract val rangeTiles: Float
    abstract val description: String

    /** Slow multiplier applied to enemy speed when this tower hits (1.0 = no slow). */
    open val slowFactor: Float = 1.0f

    /** AoE radius in tiles (0 = single target). */
    open val aoeRadius: Float = 0f
}
