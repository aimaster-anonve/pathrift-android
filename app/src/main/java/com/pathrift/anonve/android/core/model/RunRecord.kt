package com.pathrift.anonve.android.core.model

/**
 * Represents a completed run's result for persistence and display.
 */
data class RunRecord(
    val score: Long,
    val wavesCompleted: Int,
    val totalEnemiesKilled: Int,
    val goldSpent: Int
)
