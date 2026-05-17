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
/**
 * Holds a single entry describing how many of a given type spawn.
 * Used for the NextWaveInfoPanel (PATHRIFT-157).
 */
data class EnemySpawnEntry(
    val type: EnemyType,
    val count: Int
)

class WaveSystem {

    // Build 5.3: Updated enemy counts per DESIGN_SPEC_BUILD5_3 Section 4 (~60% uplift for early waves)
    private val basePatterns: List<WaveDefinition> = listOf(
        WaveDefinition(1,  listOf(SpawnGroup(EnemyType.RUNNER, 5)), 2500L),                                                              // was runner×3
        WaveDefinition(2,  listOf(SpawnGroup(EnemyType.RUNNER, 7)), 2200L),                                                              // was runner×4
        WaveDefinition(3,  listOf(SpawnGroup(EnemyType.RUNNER, 6), SpawnGroup(EnemyType.TANK, 2)), 2000L),                               // was runner×4, tank×1
        WaveDefinition(4,  listOf(SpawnGroup(EnemyType.RUNNER, 9), SpawnGroup(EnemyType.TANK, 2)), 1800L),                               // was runner×6, tank×1
        WaveDefinition(5,  listOf(SpawnGroup(EnemyType.RUNNER, 9), SpawnGroup(EnemyType.TANK, 3)), 1600L),                               // was runner×5, tank×2
        WaveDefinition(6,  listOf(SpawnGroup(EnemyType.RUNNER, 11), SpawnGroup(EnemyType.TANK, 3)), 1500L),                              // was runner×7, tank×2
        WaveDefinition(7,  listOf(SpawnGroup(EnemyType.RUNNER, 12), SpawnGroup(EnemyType.TANK, 4)), 1400L),                              // was runner×8, tank×3
        WaveDefinition(8,  listOf(SpawnGroup(EnemyType.RUNNER, 7), SpawnGroup(EnemyType.SHIELD, 3)), 1500L),                             // was runner×5, shield×2
        WaveDefinition(9,  listOf(SpawnGroup(EnemyType.RUNNER, 9), SpawnGroup(EnemyType.TANK, 3), SpawnGroup(EnemyType.SHIELD, 2)), 1300L), // was runner×6, tank×2, shield×1
    )

    private val cycleSize = 9

    // Expose CYCLE_SIZE for helpers
    val CYCLE_SIZE: Int get() = cycleSize

    fun isBossWave(wave: Int): Boolean = wave % 10 == 0

    /**
     * Compute cycle number for a given wave number (PATHRIFT-154).
     * Cycle 0/1 = waves 1–18, Cycle 2 = waves 19–27, Cycle 3 = waves 28–36, Cycle 4+ = waves 37+
     */
    fun cycleNumber(waveNumber: Int): Int {
        if (isBossWave(waveNumber)) return 0
        val bossesBeforeThisWave = waveNumber / 10
        val positionInNonBoss = waveNumber - bossesBeforeThisWave
        return (positionInNonBoss - 1) / cycleSize
    }

    /**
     * Synchronize internal wave state to a restored wave number (PATHRIFT-151).
     * Ensures cycleNumber() returns correct values after session restore.
     */
    fun syncWave(waveNumber: Int) {
        // No internal mutable state needed — all cycle math is derived from waveNumber directly.
        // This method is a hook for future stateful additions and fulfills the restore protocol.
    }

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

        // Scale base spawns by cycle depth — Build 5.3: ceil(cycleNumber * 1.3) for more aggressive scaling
        val cycleBoost = kotlin.math.ceil(cycleNumber * 1.3).toInt()
        val scaledGroups = base.spawnGroups.map { group ->
            group.copy(count = group.count + cycleBoost)
        }.toMutableList()

        // Cycle 2+ (wave 19+): inject swarm packs every 3rd pattern slot
        if (cycleNumber >= 2 && patternIndex % 3 == 0) {
            scaledGroups.add(SpawnGroup(EnemyType.SWARM, 5 + cycleNumber))
        }

        // Cycle 3+ (wave 28+): inject ghost enemies every 4th pattern slot (offset by 1)
        if (cycleNumber >= 3 && patternIndex % 4 == 1) {
            scaledGroups.add(SpawnGroup(EnemyType.GHOST, 2 + cycleNumber))
        }

        // Cycle 2+ (wave 19+): inject splitter every 3rd pattern slot (offset 1)
        if (cycleNumber >= 2 && patternIndex % 3 == 1) {
            scaledGroups.add(SpawnGroup(EnemyType.SPLITTER, 2 + cycleNumber))
        }

        // Cycle 2+ (wave 19+): inject jumper every 4th pattern slot (offset 2)
        if (cycleNumber >= 2 && patternIndex % 4 == 2) {
            scaledGroups.add(SpawnGroup(EnemyType.JUMPER, 1 + cycleNumber))
        }

        // Cycle 4+ (wave 37+): inject healer every 4th pattern slot offset 3 (PATHRIFT-159)
        if (cycleNumber >= 4 && patternIndex % 4 == 3) {
            scaledGroups.add(SpawnGroup(EnemyType.HEALER, 1 + cycleNumber - 3))
        }

        // Cycle 4+ (wave 37+): inject phantom every 3rd pattern slot offset 2, not index 2 (PATHRIFT-159)
        if (cycleNumber >= 4 && patternIndex % 3 == 2 && patternIndex != 2) {
            scaledGroups.add(SpawnGroup(EnemyType.PHANTOM, 1 + cycleNumber - 3))
        }

        val interval = maxOf(800L, base.spawnIntervalMs - cycleNumber * 80L)
        return WaveDefinition(wave, scaledGroups, interval)
    }

    /**
     * Returns a wave definition for preview purposes (NextWaveInfoPanel).
     * Delegates to getWaveDefinition (PATHRIFT-157).
     */
    fun waveDefinition(wave: Int): WaveDefinition = getWaveDefinition(wave.coerceAtLeast(1))

    /** Exponential HP scaling — Build 5.1 balance tuning (iOS parity). */
    fun hpScaleMultiplier(waveNumber: Int): Float = when {
        waveNumber <= 5  -> 1.0f
        waveNumber <= 10 -> 1.0f + (waveNumber - 5) * 0.10f   // 1.0→1.5 (was 0.15)
        waveNumber <= 20 -> 1.5f + (waveNumber - 10) * 0.15f  // 1.5→3.0 (was 1.75+0.20)
        waveNumber <= 30 -> 3.0f + (waveNumber - 20) * 0.20f  // 3.0→5.0 (new breakpoint)
        else             -> 5.0f + (waveNumber - 30) * 0.25f  // 5.0+ (was 3.75+0.30 from 20)
    }

    fun goldRewardForWave(wave: Int): Int = EconomyConstants.goldRewardForWave(wave)

    // BUILD7: Extended slot count thresholds (DEC-030, PO_SPEC section 4)
    fun activeSlotCount(wave: Int): Int = when {
        wave < 5  -> 5
        wave < 10 -> 7
        wave < 20 -> 9
        wave < 30 -> 11
        wave < 50 -> 13
        else      -> 15
    }
}
