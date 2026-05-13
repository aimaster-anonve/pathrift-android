package com.pathrift.anonve.android.core.engine

/**
 * Calculates final run score based on performance metrics.
 */
object ScoreEngine {

    private const val WAVE_CLEAR_BONUS = 500L
    private const val LIVES_BONUS_PER_LIFE = 300L
    private const val ENEMY_KILL_BASE_SCORE = 10L
    private const val GOLD_EFFICIENCY_DIVISOR = 10L

    /**
     * Computes the total score at run end.
     *
     * @param wavesCompleted Number of waves fully cleared
     * @param livesRemaining Lives left at the end (0-3)
     * @param totalEnemiesKilled Total enemies defeated across all waves
     * @param goldSpent Total gold spent on towers (efficiency indicator)
     */
    fun calculate(
        wavesCompleted: Int,
        livesRemaining: Int,
        totalEnemiesKilled: Int,
        goldSpent: Int
    ): Long {
        val waveBonus = wavesCompleted * WAVE_CLEAR_BONUS
        val livesBonus = livesRemaining * LIVES_BONUS_PER_LIFE
        val killBonus = totalEnemiesKilled * ENEMY_KILL_BASE_SCORE
        val goldBonus = goldSpent / GOLD_EFFICIENCY_DIVISOR

        // Multiplier increases with wave count (deeper runs = higher score density)
        val multiplier = 1.0 + (wavesCompleted * 0.05)

        return ((waveBonus + livesBonus + killBonus + goldBonus) * multiplier).toLong()
    }

    /**
     * Assigns a letter rank based on score thresholds.
     */
    fun getRank(score: Long): String = when {
        score >= 50_000L -> "S"
        score >= 30_000L -> "A"
        score >= 15_000L -> "B"
        score >= 7_000L  -> "C"
        score >= 2_000L  -> "D"
        else             -> "F"
    }
}
