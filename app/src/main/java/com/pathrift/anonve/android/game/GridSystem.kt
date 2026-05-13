package com.pathrift.anonve.android.game

/**
 * Represents a position on the game grid.
 * @param col Column index (0-based, 0 to COLS-1)
 * @param row Row index (0-based, 0 to ROWS-1)
 */
data class TileCoordinate(val col: Int, val row: Int)

/**
 * Represents a tile on the grid.
 */
data class GridTile(
    val coordinate: TileCoordinate,
    val type: TileType = TileType.EMPTY
)

enum class TileType {
    EMPTY,      // Can place a tower
    PATH,       // Enemy path - cannot place tower
    OCCUPIED    // Tower placed here
}

/**
 * Core grid system for the 12x8 game board.
 * Manages tile state, path definition, and placement validation.
 */
class GridSystem {

    companion object {
        const val COLS = 12
        const val ROWS = 8
        const val TILE_SIZE_DP = 64

        /**
         * Hardcoded L-shaped path for Phase 1.
         * Enemies enter from the left (col=0, row=4) and exit at the right (col=11, row=1).
         * The path forms an L: straight right to col 6, then up to row 1, then right to col 11.
         */
        val PATH_NODES: List<TileCoordinate> = buildList {
            for (col in 0..6) add(TileCoordinate(col, 4))
            for (row in 3 downTo 1) add(TileCoordinate(6, row))
            for (col in 6..11) add(TileCoordinate(col, 1))
        }
    }

    private val grid: Array<Array<GridTile>> = Array(ROWS) { row ->
        Array(COLS) { col ->
            GridTile(TileCoordinate(col, row))
        }
    }

    init {
        markPathTiles()
    }

    private fun markPathTiles() {
        PATH_NODES.forEach { coord ->
            if (isValid(coord)) {
                grid[coord.row][coord.col] = grid[coord.row][coord.col].copy(type = TileType.PATH)
            }
        }
    }

    fun getTile(coord: TileCoordinate): GridTile? {
        if (!isValid(coord)) return null
        return grid[coord.row][coord.col]
    }

    fun canPlaceTower(coord: TileCoordinate): Boolean {
        val tile = getTile(coord) ?: return false
        return tile.type == TileType.EMPTY
    }

    fun placeTower(coord: TileCoordinate): Boolean {
        if (!canPlaceTower(coord)) return false
        grid[coord.row][coord.col] = grid[coord.row][coord.col].copy(type = TileType.OCCUPIED)
        return true
    }

    fun removeTower(coord: TileCoordinate) {
        if (!isValid(coord)) return
        if (grid[coord.row][coord.col].type == TileType.OCCUPIED) {
            grid[coord.row][coord.col] = grid[coord.row][coord.col].copy(type = TileType.EMPTY)
        }
    }

    fun getAllTiles(): List<GridTile> = grid.flatMap { it.toList() }

    fun reset() {
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                val coord = TileCoordinate(col, row)
                val tileType = if (PATH_NODES.contains(coord)) TileType.PATH else TileType.EMPTY
                grid[row][col] = GridTile(coord, tileType)
            }
        }
    }

    private fun isValid(coord: TileCoordinate): Boolean {
        return coord.col in 0 until COLS && coord.row in 0 until ROWS
    }
}
