package com.pathrift.anonve.android.core.engine

/**
 * Central registry of all economy constants for Phase 1.
 * Keeping values here makes balancing straightforward without hunting through game logic.
 */
object EconomyConstants {
    /** Gold the player starts the run with. */
    const val STARTING_GOLD = 150

    /** Number of lives a player starts with. */
    const val STARTING_LIVES = 3

    /** Base gold awarded at end of each wave. */
    const val GOLD_PER_WAVE_BASE = 50

    /** Additional gold per wave number (scales: wave 1 → +5, wave 2 → +10, …). */
    const val GOLD_PER_WAVE_SCALING = 5
}
