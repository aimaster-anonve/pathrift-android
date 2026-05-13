package com.pathrift.anonve.android.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pathrift_runs")

/**
 * Persists best run data across sessions using Jetpack DataStore.
 * Stores: best score, best wave reached, total runs played.
 */
class LocalProgressStore(private val context: Context) {

    companion object {
        private val KEY_BEST_SCORE = longPreferencesKey("best_score")
        private val KEY_BEST_WAVE = intPreferencesKey("best_wave")
        private val KEY_TOTAL_RUNS = intPreferencesKey("total_runs")
    }

    val bestScore: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_BEST_SCORE] ?: 0L
    }

    val bestWave: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_BEST_WAVE] ?: 0
    }

    val totalRuns: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_TOTAL_RUNS] ?: 0
    }

    suspend fun saveRunResult(score: Long, wave: Int) {
        context.dataStore.edit { prefs ->
            val currentBest = prefs[KEY_BEST_SCORE] ?: 0L
            if (score > currentBest) {
                prefs[KEY_BEST_SCORE] = score
            }

            val currentBestWave = prefs[KEY_BEST_WAVE] ?: 0
            if (wave > currentBestWave) {
                prefs[KEY_BEST_WAVE] = wave
            }

            prefs[KEY_TOTAL_RUNS] = (prefs[KEY_TOTAL_RUNS] ?: 0) + 1
        }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { it.clear() }
    }
}
