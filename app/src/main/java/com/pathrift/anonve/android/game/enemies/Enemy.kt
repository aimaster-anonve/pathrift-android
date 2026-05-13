package com.pathrift.anonve.android.game.enemies

enum class EnemyType {
    RUNNER,
    TANK
}

/**
 * Runtime instance of an enemy currently active on the battlefield.
 * Immutable data class — all state mutations produce new copies.
 *
 * @param id Unique identifier (nanosecond timestamp at spawn)
 * @param pathNodeIndex Current target node in GridSystem.PATH_NODES
 * @param pathProgress Current X position (in tile units) along the path
 * @param pathRow Current Y position (in tile units) along the path
 * @param armorReduction Fraction of incoming damage absorbed (0f = no armor)
 */
data class EnemyInstance(
    val id: Long,
    val type: EnemyType,
    val maxHp: Int,
    val currentHp: Int,
    val speed: Float,
    val goldReward: Int,
    val armorReduction: Float = 0f,
    val pathNodeIndex: Int,
    val pathProgress: Float,
    val pathRow: Float
)
