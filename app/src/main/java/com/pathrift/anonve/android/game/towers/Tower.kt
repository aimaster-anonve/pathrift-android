package com.pathrift.anonve.android.game.towers

enum class TowerType {
    BOLT,
    BLAST,
    FROST
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
