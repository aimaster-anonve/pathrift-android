package com.pathrift.anonve.android.game

import com.pathrift.anonve.android.core.engine.EconomyConstants
import com.pathrift.anonve.android.game.enemies.EnemyType

/**
 * Defines the composition of a single wave.
 */
data class WaveDefinition(
    val waveNumber: Int,
    val spawnGroups: List<SpawnGroup>
)

/**
 * A group of enemies to spawn, with optional interval between each spawn.
 */
data class SpawnGroup(
    val type: EnemyType,
    val count: Int,
    val spawnIntervalMs: Long = 800L
)

/**
 * Manages wave progression: defines 5-wave cycle + endless scaling.
 */
class WaveSystem {

    companion object {
        private val BASE_CYCLE = listOf(
            listOf(SpawnGroup(EnemyType.RUNNER, 5)),
            listOf(SpawnGroup(EnemyType.RUNNER, 3), SpawnGroup(EnemyType.TANK, 1)),
            listOf(SpawnGroup(EnemyType.RUNNER, 8)),
            listOf(SpawnGroup(EnemyType.TANK, 2)),
            listOf(SpawnGroup(EnemyType.RUNNER, 6), SpawnGroup(EnemyType.TANK, 2))
        )
    }

    fun getWaveDefinition(waveNumber: Int): WaveDefinition {
        val cycleIndex = (waveNumber - 1) % BASE_CYCLE.size
        val scalingFactor = ((waveNumber - 1) / BASE_CYCLE.size)

        val baseGroups = BASE_CYCLE[cycleIndex]
        val scaledGroups = baseGroups.map { group ->
            val scaledCount = group.count + scalingFactor * 2
            group.copy(count = scaledCount)
        }

        return WaveDefinition(waveNumber = waveNumber, spawnGroups = scaledGroups)
    }

    fun calculateGoldReward(waveNumber: Int): Int {
        return EconomyConstants.GOLD_PER_WAVE_BASE + waveNumber * EconomyConstants.GOLD_PER_WAVE_SCALING
    }

    fun getTotalEnemyCount(waveNumber: Int): Int {
        return getWaveDefinition(waveNumber).spawnGroups.sumOf { it.count }
    }
}
