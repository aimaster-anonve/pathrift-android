package com.pathrift.anonve.android.game

import com.pathrift.anonve.android.core.engine.EconomyConstants
import com.pathrift.anonve.android.game.enemies.EnemyType

/**
 * Wave definition — mirrors iOS WaveDefinition.
 */
data class WaveDefinition(
    val waveNumber: Int,
    val spawnGroups: List<SpawnGroup>,
    val spawnIntervalMs: Long = 1500L
) {
    val totalEnemyCount: Int get() = spawnGroups.sumOf { it.count }
}

/**
 * A group of enemies to spawn within one wave.
 */
data class SpawnGroup(
    val type: EnemyType,
    val count: Int,
    val spawnIntervalMs: Long = 1500L
)

/**
 * Manages wave progression — full iOS WaveSystem.swift parity.
 *
 * - 9-wave base cycle
 * - Boss every 10th wave (wave 10, 20, 30…)
 * - Cycle depth scaling: count +cycleNumber per group
 * - Cycle 2+ (wave 19+): SWARM injected at patternIndex % 3 == 0
 * - Cycle 3+ (wave 28+): GHOST injected at patternIndex % 4 == 1
 * - Spawn interval reduces by 80ms per cycle, minimum 800ms
 * - HP scaling: exponential multiplier per wave tier
 */
class WaveSystem {

    private val basePatterns: List<WaveDefinition> = listOf(
        WaveDefinition(1,  listOf(SpawnGroup(EnemyType.RUNNER, 3)), 2500L),
        WaveDefinition(2,  listOf(SpawnGroup(EnemyType.RUNNER, 4)), 2200L),
        WaveDefinition(3,  listOf(SpawnGroup(EnemyType.RUNNER, 4), SpawnGroup(EnemyType.TANK, 1)), 2000L),
        WaveDefinition(4,  listOf(SpawnGroup(EnemyType.RUNNER, 6), SpawnGroup(EnemyType.TANK, 1)), 1800L),
        WaveDefinition(5,  listOf(SpawnGroup(EnemyType.RUNNER, 5), SpawnGroup(EnemyType.TANK, 2)), 1600L),
        WaveDefinition(6,  listOf(SpawnGroup(EnemyType.RUNNER, 7), SpawnGroup(EnemyType.TANK, 2)), 1500L),
        WaveDefinition(7,  listOf(SpawnGroup(EnemyType.RUNNER, 8), SpawnGroup(EnemyType.TANK, 3)), 1400L),
        WaveDefinition(8,  listOf(SpawnGroup(EnemyType.RUNNER, 5), SpawnGroup(EnemyType.SHIELD, 2)), 1500L),
        WaveDefinition(9,  listOf(SpawnGroup(EnemyType.RUNNER, 6), SpawnGroup(EnemyType.TANK, 2), SpawnGroup(EnemyType.SHIELD, 1)), 1300L),
    )

    private val cycleSize = 9

    fun isBossWave(wave: Int): Boolean = wave % 10 == 0

    fun getWaveDefinition(wave: Int): WaveDefinition {
        if (isBossWave(wave)) {
            return WaveDefinition(wave, listOf(SpawnGroup(EnemyType.BOSS, 1)), 8000L)
        }

        // Map non-boss wave into 9-wave cycle.
        // Skip every 10th number: wave 1-9 → index 0-8, wave 11-19 → index 0-8, etc.
        // Mirrors iOS: bossesBeforeThisWave = wave / 10, positionInNonBoss = wave - bossesBeforeThisWave
        val bossesBeforeThisWave = wave / 10
        val positionInNonBoss = wave - bossesBeforeThisWave
        val patternIndex = (positionInNonBoss - 1) % cycleSize
        val cycleNumber = (positionInNonBoss - 1) / cycleSize
        val base = basePatterns[patternIndex]

        if (cycleNumber == 0) {
            return WaveDefinition(wave, base.spawnGroups, base.spawnIntervalMs)
        }

        // Scale base spawns by cycle depth
        val scaledGroups = base.spawnGroups.map { group ->
            group.copy(count = group.count + cycleNumber)
        }.toMutableList()

        // Cycle 2+ (wave 19+): inject swarm packs every 3rd pattern slot
        if (cycleNumber >= 2 && patternIndex % 3 == 0) {
            scaledGroups.add(SpawnGroup(EnemyType.SWARM, 5 + cycleNumber))
        }

        // Cycle 3+ (wave 28+): inject ghost enemies every 4th pattern slot (offset by 1)
        if (cycleNumber >= 3 && patternIndex % 4 == 1) {
            scaledGroups.add(SpawnGroup(EnemyType.GHOST, 2 + cycleNumber))
        }

        val interval = maxOf(800L, base.spawnIntervalMs - cycleNumber * 80L)
        return WaveDefinition(wave, scaledGroups, interval)
    }

    /** Exponential HP scaling — mirrors iOS hpScaleMultiplier(for:). */
    fun hpScaleMultiplier(wave: Int): Float = when {
        wave <= 5  -> 1.0f
        wave <= 10 -> 1.0f + (wave - 5) * 0.15f
        wave <= 20 -> 1.75f + (wave - 10) * 0.20f
        else       -> 3.75f + (wave - 20) * 0.30f
    }

    fun goldRewardForWave(wave: Int): Int = EconomyConstants.goldRewardForWave(wave)

    fun activeSlotCount(wave: Int): Int = when {
        wave < 5  -> 6
        wave < 10 -> 8
        wave < 15 -> 10
        else      -> 12
    }
}
