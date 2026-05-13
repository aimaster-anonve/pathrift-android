package com.pathrift.anonve.android

import com.pathrift.anonve.android.core.engine.ScoreEngine
import com.pathrift.anonve.android.game.GridSystem
import com.pathrift.anonve.android.game.TileType
import com.pathrift.anonve.android.game.TileCoordinate
import com.pathrift.anonve.android.game.WaveSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GridSystemTest {

    @Test
    fun `grid dimensions are correct`() {
        val grid = GridSystem()
        val tiles = grid.getAllTiles()
        assertEquals(GridSystem.COLS * GridSystem.ROWS, tiles.size)
    }

    @Test
    fun `path tiles cannot have towers placed`() {
        val grid = GridSystem()
        val pathCoord = GridSystem.PATH_NODES.first()
        assertFalse(grid.canPlaceTower(pathCoord))
    }

    @Test
    fun `empty tiles can have towers placed`() {
        val grid = GridSystem()
        val emptyTile = grid.getAllTiles().first { it.type == TileType.EMPTY }
        assertTrue(grid.canPlaceTower(emptyTile.coordinate))
    }

    @Test
    fun `placing tower marks tile as occupied`() {
        val grid = GridSystem()
        val emptyTile = grid.getAllTiles().first { it.type == TileType.EMPTY }
        val coord = emptyTile.coordinate
        assertTrue(grid.placeTower(coord))
        assertFalse(grid.canPlaceTower(coord))
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
        val wave1Count = waveSystem.getTotalEnemyCount(1)
        val wave6Count = waveSystem.getTotalEnemyCount(6)
        assertTrue(wave6Count > wave1Count)
    }

    @Test
    fun `gold reward scales with wave number`() {
        val reward1 = waveSystem.calculateGoldReward(1)
        val reward5 = waveSystem.calculateGoldReward(5)
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
