package com.pathrift.anonve.android.game.enemies

enum class EnemyType {
    RUNNER,
    TANK,
    BOSS,
    SHIELD,
    SWARM,
    GHOST,
    SPLITTER,
    JUMPER
}

/**
 * Runtime instance of an enemy currently active on the battlefield.
 * Mutable data holder — extended to support all 8 enemy types with iOS parity.
 *
 * @param id           Unique identifier (nanosecond timestamp at spawn)
 * @param pathProgress Normalized [0,1] progress along the continuous waypoint path
 * @param armorReduction Fraction of incoming damage absorbed (0f = no armor)
 * @param shieldHp     ShieldEnemy only — remaining shield absorption HP
 * @param shieldBroken ShieldEnemy only — true after shield is destroyed
 * @param slowEndTime  System.currentTimeMillis() timestamp when slow expires (0 = not slowed)
 * @param bossVariant  BossEnemy only — variant index 0-4
 * @param lastJumpTime JumperEnemy only — timestamp of last jump
 */
data class EnemyInstance(
    val id: Long,
    val type: EnemyType,
    val maxHp: Float,
    val currentHp: Float,
    val baseSpeed: Float,
    val currentSpeed: Float,
    val goldReward: Int,
    val armorReduction: Float = 0f,
    val pathProgress: Float = 0f,
    val hasReachedEnd: Boolean = false,
    // Shield enemy
    val shieldHp: Float = 0f,
    val shieldBroken: Boolean = false,
    // Slow state
    val slowEndTime: Long = 0L,
    // Boss
    val bossVariant: Int = 0,
    // Jumper
    val lastJumpTime: Long = 0L
) {
    val isAlive: Boolean get() = currentHp > 0f && !hasReachedEnd
    val isDead: Boolean get() = currentHp <= 0f
}
