package com.pathrift.anonve.android

import com.pathrift.anonve.android.core.engine.ScoreEngine
import com.pathrift.anonve.android.game.GridSystem
import com.pathrift.anonve.android.game.TileType
import com.pathrift.anonve.android.game.WaveSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridSystemTest {

    @Test
    fun `grid tile count matches dimensions`() {
        val grid = GridSystem()
        val tiles = grid.getAllTiles()
        assertEquals(GridSystem.COLS * GridSystem.ROWS, tiles.size)
    }

    @Test
    fun `all tiles are initially empty`() {
        val grid = GridSystem()
        assertTrue(grid.getAllTiles().all { it.type == TileType.EMPTY })
    }

    @Test
    fun `no slots exist before updateSlots called`() {
        val grid = GridSystem()
        assertEquals(0, grid.slots.size)
    }

    @Test
    fun `available slots returns empty list when no slots configured`() {
        val grid = GridSystem()
        assertEquals(0, grid.availableSlots().size)
    }
}

class WaveSystemTest {

    private val waveSystem = WaveSystem()

    @Test
    fun `wave 1 contains 5 runners`() {
        val def = waveSystem.getWaveDefinition(1)
        val runnerGroup = def.spawnGroups.first()
        assertEquals(5, runnerGroup.count)
    }

    @Test
    fun `wave scaling increases enemy count`() {
        val wave1Count = waveSystem.getWaveDefinition(1).totalEnemyCount
        val wave6Count = waveSystem.getWaveDefinition(6).totalEnemyCount
        assertTrue(wave6Count > wave1Count)
    }

    @Test
    fun `gold reward scales with wave number`() {
        val reward1 = waveSystem.goldRewardForWave(1)
        val reward5 = waveSystem.goldRewardForWave(5)
        assertTrue(reward5 > reward1)
    }
}

class ScoreEngineTest {

    @Test
    fun `score is zero with no waves and no kills`() {
        val score = ScoreEngine.calculate(
            wavesCompleted = 0,
            livesRemaining = 3,
            totalEnemiesKilled = 0,
            goldSpent = 0
        )
        assertEquals(900L, score)
    }

    @Test
    fun `score increases with more waves`() {
        val low = ScoreEngine.calculate(1, 2, 10, 100)
        val high = ScoreEngine.calculate(10, 2, 10, 100)
        assertTrue(high > low)
    }

    @Test
    fun `rank S requires 50000 score`() {
        assertEquals("S", ScoreEngine.getRank(50_000L))
    }

    @Test
    fun `rank F for very low scores`() {
        assertEquals("F", ScoreEngine.getRank(100L))
    }
}
