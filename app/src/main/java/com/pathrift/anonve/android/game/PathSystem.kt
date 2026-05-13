package com.pathrift.anonve.android.game

/**
 * Utility for querying path information.
 * Delegates to GridSystem.PATH_NODES for the canonical path definition.
 */
object PathSystem {

    fun getPathNodes(): List<TileCoordinate> = GridSystem.PATH_NODES

    fun isOnPath(coord: TileCoordinate): Boolean = GridSystem.PATH_NODES.contains(coord)

    fun getPathLength(): Int = GridSystem.PATH_NODES.size
}
