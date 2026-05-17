package com.pathrift.anonve.android.game

import android.graphics.PointF
import com.pathrift.anonve.android.game.towers.TowerType

/**
 * Represents a position on the legacy grid (kept for background rendering).
 */
data class TileCoordinate(val col: Int, val row: Int)

/**
 * Tile type for the background grid rendering.
 */
data class GridTile(
    val coordinate: TileCoordinate,
    val type: TileType = TileType.EMPTY
)

enum class TileType {
    EMPTY,
    PATH,
    OCCUPIED
}

/**
 * PlacedRecord — tracks a placed tower by unique ID, position, and type.
 * Replaces slot-based system (Build 15 / DEC-032).
 */
data class PlacedRecord(
    val towerId: Int,
    val position: PointF,
    val type: TowerType
)

/**
 * GridSystem — Build 15: repurposed as placed-tower tracker only.
 * No predefined slot positions. Free-form placement support.
 *
 * Legacy TILE_SIZE_DP kept for range pixel calculation in GameEngine/GameRenderer.
 */
class GridSystem {

    companion object {
        const val COLS = 16
        const val ROWS = 26
        const val TILE_SIZE_DP = 64
    }

    private val _placed = mutableMapOf<Int, PlacedRecord>()
    val placed: Map<Int, PlacedRecord> get() = _placed.toMap()
    private var nextId = 0

    val count: Int get() = _placed.size

    fun addTower(type: TowerType, position: PointF): Int {
        val id = nextId++
        _placed[id] = PlacedRecord(id, PointF(position.x, position.y), type)
        return id
    }

    fun removeTower(id: Int) {
        _placed.remove(id)
    }

    fun moveTower(id: Int, to: PointF) {
        _placed[id]?.let { _placed[id] = it.copy(position = PointF(to.x, to.y)) }
    }

    fun record(id: Int): PlacedRecord? = _placed[id]

    fun clear() {
        _placed.clear()
        nextId = 0
    }

    /** Min distance from point to any placed tower center, optionally excluding one tower. */
    fun minDistanceToTower(x: Float, y: Float, excludeId: Int? = null): Float {
        return _placed.values
            .filter { it.towerId != excludeId }
            .map { r ->
                val dx = r.position.x - x
                val dy = r.position.y - y
                kotlin.math.sqrt(dx * dx + dy * dy)
            }
            .minOrNull() ?: Float.MAX_VALUE
    }

    // ---- Legacy background tile grid (for renderer) ----

    fun getAllTiles(): List<GridTile> = buildList {
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                add(GridTile(TileCoordinate(col, row), TileType.EMPTY))
            }
        }
    }
}
