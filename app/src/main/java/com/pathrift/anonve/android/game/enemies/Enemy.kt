package com.pathrift.anonve.android.game.enemies

import com.pathrift.anonve.android.game.PathLayer

enum class EnemyType {
    RUNNER,
    TANK,
    BOSS,
    SHIELD,
    SWARM,
    GHOST,
    SPLITTER,
    JUMPER,
    HEALER,   // PATHRIFT-159: support/heal aura enemy
    PHANTOM   // PATHRIFT-159: 40% dodge single-target projectiles
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
    val lastJumpTime: Long = 0L,
    // Z-layer path system
    val pathLayer: PathLayer = PathLayer.GROUND,
    // Healer: last time aura heal fired (ms timestamp)
    val lastHealTime: Long = 0L,
    // Phantom: brief dodge flash visual flag
    val dodgeFlashing: Boolean = false,
    // Boss ability state
    val bossAbilityTriggered: Boolean = false,
    val bossShellActive: Boolean = false
) {
    val isAlive: Boolean get() = currentHp > 0f && !hasReachedEnd
    val isDead: Boolean get() = currentHp <= 0f
}
