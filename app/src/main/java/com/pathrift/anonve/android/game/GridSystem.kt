package com.pathrift.anonve.android.game

import android.graphics.PointF
import com.pathrift.anonve.android.game.towers.TowerType

/**
 * Represents a position on the legacy grid (kept for backward compatibility).
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
 * Tower slot state — mirrors iOS TowerSlotState.
 */
sealed class TowerSlotState {
    object Empty : TowerSlotState()
    data class Occupied(val towerType: TowerType, val level: Int = 1, val totalInvested: Int = 0) : TowerSlotState()
    object Locked : TowerSlotState()

    val isOccupied: Boolean get() = this is Occupied
    val occupiedTowerType: TowerType? get() = (this as? Occupied)?.towerType
}

/**
 * A tower slot — position-based, not tile-based (mirrors iOS TowerSlot).
 */
data class TowerSlot(
    val id: Int,
    val position: PointF,
    val state: TowerSlotState = TowerSlotState.Empty
)

/**
 * GridSystem — manages tower slot positions derived from PathSystem layout.
 * Also keeps the legacy tile grid for background rendering.
 *
 * iOS parity: updateSlots(), placeTower(), removeTower(), slot(at:).
 */
class GridSystem {

    companion object {
        const val COLS = 16   // was 12 (Build 5.3 scale reduction)
        const val ROWS = 26   // was 20 (Build 5.3 scale reduction)
        const val TILE_SIZE_DP = 64
    }

    private var _slots: MutableList<TowerSlot> = mutableListOf()
    val slots: List<TowerSlot> get() = _slots.toList()

    // ---- Slot API (mirrors iOS GridSystem) ----

    /** Replace all slots with positions derived from PathSystem layout. */
    fun updateSlots(positions: List<PointF>) {
        _slots = positions.mapIndexed { idx, pos ->
            TowerSlot(id = idx, position = pos, state = TowerSlotState.Empty)
        }.toMutableList()
    }

    fun slot(at: Int): TowerSlot? = _slots.firstOrNull { it.id == at }

    private fun slotIndex(id: Int): Int = _slots.indexOfFirst { it.id == id }

    fun placeTower(type: TowerType, at: Int, cost: Int = 0): Boolean {
        val idx = slotIndex(at)
        if (idx < 0) return false
        if (_slots[idx].state.isOccupied) return false
        _slots[idx] = _slots[idx].copy(state = TowerSlotState.Occupied(type, level = 1, totalInvested = cost))
        return true
    }

    fun removeTower(at: Int) {
        val idx = slotIndex(at)
        if (idx < 0) return
        _slots[idx] = _slots[idx].copy(state = TowerSlotState.Empty)
    }

    fun upgradeTower(at: Int, upgradeCost: Int) {
        val idx = slotIndex(at)
        if (idx < 0) return
        val cur = _slots[idx].state as? TowerSlotState.Occupied ?: return
        _slots[idx] = _slots[idx].copy(
            state = cur.copy(
                level = cur.level + 1,
                totalInvested = cur.totalInvested + upgradeCost
            )
        )
    }

    fun availableSlots(): List<TowerSlot> = _slots.filter { it.state == TowerSlotState.Empty }
    fun occupiedSlots(): List<TowerSlot> = _slots.filter { it.state.isOccupied }

    // ---- Legacy background tile grid (for renderer) ----

    fun getAllTiles(): List<GridTile> = buildList {
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                add(GridTile(TileCoordinate(col, row), TileType.EMPTY))
            }
        }
    }
}
