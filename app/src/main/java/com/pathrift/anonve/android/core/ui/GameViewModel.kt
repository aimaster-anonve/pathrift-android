package com.pathrift.anonve.android.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pathrift.anonve.android.core.engine.EconomyConstants
import com.pathrift.anonve.android.game.GameBridge
import com.pathrift.anonve.android.game.GameState
import com.pathrift.anonve.android.game.GameEngine
import com.pathrift.anonve.android.game.enemies.EnemyInstance
import com.pathrift.anonve.android.game.enemies.EnemyType
import com.pathrift.anonve.android.game.GridSystem
import com.pathrift.anonve.android.game.GridTile
import com.pathrift.anonve.android.game.TileCoordinate
import com.pathrift.anonve.android.game.towers.Tower
import com.pathrift.anonve.android.game.towers.BoltTower
import com.pathrift.anonve.android.game.towers.BlastTower
import com.pathrift.anonve.android.game.towers.FrostTower
import com.pathrift.anonve.android.game.towers.TowerType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---- State Definitions ----

enum class GamePhase {
    PRE_WAVE,
    WAVE_ACTIVE,
    BETWEEN_WAVES,
    RUN_ENDED
}

data class UiGameState(
    val wave: Int = 0,
    val lives: Int = EconomyConstants.STARTING_LIVES,
    val gold: Int = EconomyConstants.STARTING_GOLD,
    val score: Long = 0L,
    val phase: GamePhase = GamePhase.PRE_WAVE,
    val towers: Map<TileCoordinate, Tower> = emptyMap(),
    val enemies: List<EnemyInstance> = emptyList(),
    val gridTiles: List<GridTile> = emptyList(),
    val selectedTowerType: TowerType? = null,
    val selectedTile: TileCoordinate? = null,
    val totalEnemiesKilled: Int = 0
)

// One-shot UI events
sealed class GameEvent {
    data class ShowMessage(val message: String) : GameEvent()
    data class RunEnded(val wave: Int, val score: Long) : GameEvent()
    data object WaveStarted : GameEvent()
    data object WaveCompleted : GameEvent()
}

// ---- ViewModel ----

class GameViewModel : ViewModel(), GameBridge {

    private val _state = MutableStateFlow(UiGameState())
    val state: StateFlow<UiGameState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>()
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val game = GameEngine(this)

    init {
        game.start(viewModelScope)
        _state.update { it.copy(gridTiles = game.grid.getAllTiles()) }
    }

    // ---- Player Actions ----

    fun selectTowerType(type: TowerType) {
        _state.update { it.copy(selectedTowerType = type, selectedTile = null) }
    }

    fun selectTile(coord: TileCoordinate) {
        val currentType = _state.value.selectedTowerType ?: run {
            _state.update { it.copy(selectedTile = coord) }
            return
        }

        val success = game.placeTower(coord, currentType)
        if (success) {
            _state.update { current ->
                current.copy(
                    towers = game.towers,
                    gold = game.gold,
                    gridTiles = game.grid.getAllTiles(),
                    selectedTile = null,
                    selectedTowerType = null
                )
            }
        } else {
            val reason = when {
                game.gold < getTowerCost(currentType) -> "Not enough gold!"
                !game.grid.canPlaceTower(coord) -> "Cannot place tower here."
                else -> "Invalid placement."
            }
            viewModelScope.launch {
                _events.emit(GameEvent.ShowMessage(reason))
            }
        }
    }

    fun startNextWave() {
        if (_state.value.phase == GamePhase.WAVE_ACTIVE) return
        game.startNextWave()
        _state.update { it.copy(phase = GamePhase.WAVE_ACTIVE, wave = game.currentWave) }
    }

    fun clearTowerSelection() {
        _state.update { it.copy(selectedTowerType = null, selectedTile = null) }
    }

    fun restartGame() {
        game.reset()
        game.start(viewModelScope)
        _state.value = UiGameState(gridTiles = game.grid.getAllTiles())
    }

    // ---- GameBridge Callbacks (called from game thread) ----

    override fun onWaveStarted(wave: Int) {
        _state.update { it.copy(wave = wave, phase = GamePhase.WAVE_ACTIVE) }
        viewModelScope.launch { _events.emit(GameEvent.WaveStarted) }
    }

    override fun onWaveCompleted(wave: Int, goldEarned: Int) {
        _state.update { current ->
            current.copy(
                phase = GamePhase.BETWEEN_WAVES,
                gold = game.gold,
                score = game.score
            )
        }
        viewModelScope.launch {
            _events.emit(GameEvent.WaveCompleted)
            _events.emit(GameEvent.ShowMessage("Wave $wave complete! +$goldEarned gold"))
        }
    }

    override fun onEnemyKilled(type: EnemyType, gold: Int) {
        _state.update { current ->
            current.copy(
                gold = game.gold,
                score = game.score,
                enemies = game.enemies,
                totalEnemiesKilled = game.totalEnemiesKilled
            )
        }
    }

    override fun onLifeLost(livesRemaining: Int) {
        _state.update { it.copy(lives = livesRemaining) }
        if (livesRemaining <= 0) return
        viewModelScope.launch {
            _events.emit(GameEvent.ShowMessage("Life lost! $livesRemaining remaining"))
        }
    }

    override fun onRunEnded(wave: Int, score: Long) {
        _state.update { it.copy(phase = GamePhase.RUN_ENDED, lives = 0) }
        viewModelScope.launch {
            _events.emit(GameEvent.RunEnded(wave, score))
        }
    }

    // ---- Periodic state sync (called from Compose side on each frame) ----
    fun syncEnemyPositions() {
        _state.update { it.copy(enemies = game.enemies) }
    }

    // ---- Helpers ----

    private fun getTowerCost(type: TowerType): Int = when (type) {
        TowerType.BOLT -> BoltTower().cost
        TowerType.BLAST -> BlastTower().cost
        TowerType.FROST -> FrostTower().cost
    }

    override fun onCleared() {
        super.onCleared()
        game.stop()
    }
}
