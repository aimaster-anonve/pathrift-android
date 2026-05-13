package com.pathrift.anonve.android.game.towers

import com.pathrift.anonve.android.game.enemies.EnemyType

enum class TargetingMode { ALL_LAYERS, GROUND_ONLY, BRIDGE_ONLY }

enum class TowerType {
    BOLT,
    BLAST,
    FROST,
    PIERCE,
    CORE,
    INFERNO,
    TESLA,
    NOVA,
    SNIPER,
    ARTILLERY;

    fun damageMultiplier(against: EnemyType): Float = when (this) {
        TowerType.BOLT      -> if (against == EnemyType.RUNNER || against == EnemyType.SPLITTER) 1.5f else 1.0f
        TowerType.BLAST     -> if (against == EnemyType.SWARM) 1.6f else 1.0f
        TowerType.FROST     -> 1.0f
        TowerType.PIERCE    -> if (against == EnemyType.SHIELD) 2.0f else 1.0f
        TowerType.CORE      -> if (against == EnemyType.TANK) 1.6f else 1.0f
        TowerType.INFERNO   -> if (against == EnemyType.GHOST) 1.75f else 1.0f
        TowerType.TESLA     -> if (against == EnemyType.SWARM) 1.75f else 1.0f
        TowerType.NOVA      -> if (against == EnemyType.BOSS) 1.5f else 1.0f
        TowerType.SNIPER    -> 1.0f
        TowerType.ARTILLERY -> if (against == EnemyType.BOSS) 1.5f else 1.0f
    }

    val typeAdvantageHint: String? get() = when (this) {
        TowerType.BOLT      -> "+50% vs Runners"
        TowerType.BLAST     -> "+60% vs Swarms"
        TowerType.FROST     -> null
        TowerType.PIERCE    -> "+100% vs Shields"
        TowerType.CORE      -> "+60% vs Tanks"
        TowerType.INFERNO   -> "+75% vs Ghosts"
        TowerType.TESLA     -> "+75% vs Swarms"
        TowerType.NOVA      -> "+50% vs Boss"
        TowerType.SNIPER    -> "All layers"
        TowerType.ARTILLERY -> "Bridge enemies only +50% Boss"
    }

    val diamondCost: Int get() = when (this) {
        TowerType.BOLT      -> 0
        TowerType.BLAST     -> 10
        TowerType.FROST     -> 15
        TowerType.PIERCE    -> 30
        TowerType.CORE      -> 50
        TowerType.INFERNO   -> 80
        TowerType.TESLA     -> 150
        TowerType.NOVA      -> 300
        TowerType.SNIPER    -> 0
        TowerType.ARTILLERY -> 0
    }

    val tier: Int get() = when (this) {
        TowerType.BOLT, TowerType.BLAST, TowerType.FROST       -> 1
        TowerType.PIERCE, TowerType.CORE                        -> 2
        TowerType.INFERNO, TowerType.TESLA, TowerType.NOVA      -> 3
        TowerType.SNIPER, TowerType.ARTILLERY                   -> 2
    }

    val displayName: String get() = when (this) {
        TowerType.BOLT      -> "Bolt"
        TowerType.BLAST     -> "Blast"
        TowerType.FROST     -> "Frost"
        TowerType.PIERCE    -> "Pierce"
        TowerType.CORE      -> "Core"
        TowerType.INFERNO   -> "Inferno"
        TowerType.TESLA     -> "Tesla"
        TowerType.NOVA      -> "Nova"
        TowerType.SNIPER    -> "Sniper"
        TowerType.ARTILLERY -> "Artillery"
    }

    val targetingMode: TargetingMode get() = when (this) {
        TowerType.SNIPER    -> TargetingMode.ALL_LAYERS
        TowerType.ARTILLERY -> TargetingMode.BRIDGE_ONLY
        else                -> TargetingMode.GROUND_ONLY
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
