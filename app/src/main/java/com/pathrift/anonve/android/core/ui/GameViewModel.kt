package com.pathrift.anonve.android.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pathrift.anonve.android.core.engine.EconomyConstants
import com.pathrift.anonve.android.game.GameBridge
import com.pathrift.anonve.android.game.GameEngine
import com.pathrift.anonve.android.game.GamePhase
import com.pathrift.anonve.android.game.GameState
import com.pathrift.anonve.android.game.TowerInfo
import com.pathrift.anonve.android.game.TowerSlotData
import com.pathrift.anonve.android.game.enemies.EnemyInstance
import com.pathrift.anonve.android.game.enemies.EnemyType
import com.pathrift.anonve.android.game.towers.TowerType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.pow

// ---- One-shot UI events ----

sealed class GameEvent {
    data class ShowMessage(val message: String) : GameEvent()
    data class RunEnded(val wave: Int, val score: Long) : GameEvent()
    data object WaveStarted : GameEvent()
    data object WaveCompleted : GameEvent()
    data object RiftShift : GameEvent()
}

/**
 * GameViewModel — iOS GameViewModel.swift parity.
 * Implements GameBridge. Exposes StateFlow<GameState> to Compose UI.
 */
class GameViewModel : ViewModel(), GameBridge {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>()
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val _enemies = MutableStateFlow<List<EnemyInstance>>(emptyList())
    val enemies: StateFlow<List<EnemyInstance>> = _enemies.asStateFlow()

    val game = GameEngine(this)

    init {
        game.start(viewModelScope)
    }

    // ---- Player actions ----

    fun initLayout(width: Float, height: Float) {
        game.initLayout(width, height)
    }

    fun startNextWave() {
        if (_state.value.phase == GamePhase.WAVE_ACTIVE) return
        game.startNextWave()
        _state.update { it.copy(phase = GamePhase.WAVE_ACTIVE) }
    }

    fun placeTower(slotId: Int, type: TowerType) {
        val success = game.placeTower(slotId, type)
        if (!success) {
            viewModelScope.launch {
                val reason = when {
                    game.gold < getCost(type) -> "Not enough gold!"
                    game.grid.slot(slotId)?.state?.isOccupied == true -> "Slot already occupied."
                    else -> "Cannot place tower here."
                }
                _events.emit(GameEvent.ShowMessage(reason))
            }
        } else {
            syncTowerSlots()
        }
    }

    fun sellSelectedTower() {
        val slotId = _state.value.selectedTowerSlotId ?: return
        game.sellTower(slotId)
        syncTowerSlots()
        _state.update { it.copy(selectedTowerSlotId = null, selectedTowerInfo = null) }
    }

    fun upgradeSelectedTower() {
        val slotId = _state.value.selectedTowerSlotId ?: return
        game.upgradeTower(slotId)
        syncTowerSlots()
        // Refresh tower info panel
        val inst = game.towerInstance(slotId)
        if (inst != null) {
            _state.update { cur ->
                cur.copy(
                    selectedTowerInfo = buildTowerInfo(slotId, inst.tower.type, inst.level, inst.totalInvested)
                )
            }
        }
    }

    fun tapTowerSlot(slotId: Int) {
        val slot = game.grid.slot(slotId) ?: return
        if (slot.state.isOccupied) {
            bridge.onTowerTapped(slotId)
        } else {
            _state.update { it.copy(selectedTowerSlotId = null, selectedTowerInfo = null) }
        }
    }

    fun clearTowerSelection() {
        _state.update { it.copy(selectedTowerSlotId = null, selectedTowerInfo = null) }
    }

    fun restartGame() {
        game.reset()
        game.start(viewModelScope)
        _state.value = GameState()
        _enemies.value = emptyList()
    }

    /** Called ~60fps from the rendering side to keep enemy positions live. */
    fun syncEnemyPositions() {
        _enemies.value = game.enemies
    }

    // ---- GameBridge callbacks (called from game thread → dispatch to main) ----

    private val bridge: GameBridge get() = this   // self-reference for readability

    override fun onWaveStarted(wave: Int, totalEnemies: Int) {
        _state.update { it.copy(wave = wave, phase = GamePhase.WAVE_ACTIVE, waveEnemyTotal = maxOf(1, totalEnemies), waveEnemiesCleared = 0) }
        viewModelScope.launch { _events.emit(GameEvent.WaveStarted) }
    }

    override fun onWaveCompleted(wave: Int, goldEarned: Int) {
        _state.update { cur ->
            cur.copy(
                phase = GamePhase.DECISION,
                gold = game.gold,
                score = game.score,
                waveCompleteMessage = "Wave $wave cleared! +$goldEarned gold"
            )
        }
        viewModelScope.launch {
            _events.emit(GameEvent.WaveCompleted)
            _events.emit(GameEvent.ShowMessage("Wave $wave complete! +$goldEarned gold"))
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000L)
            _state.update { it.copy(waveCompleteMessage = null) }
        }
    }

    override fun onEnemyKilled(type: EnemyType, gold: Int) {
        _state.update { cur ->
            cur.copy(
                gold = game.gold,
                score = game.score,
                enemyKills = game.totalEnemiesKilled
            )
        }
    }

    override fun onEnemyEscaped() {
        // Life deduction handled by onLifeLost
    }

    override fun onLifeLost(livesRemaining: Int) {
        _state.update { it.copy(lives = livesRemaining) }
        if (livesRemaining > 0) {
            viewModelScope.launch { _events.emit(GameEvent.ShowMessage("Life lost! $livesRemaining remaining")) }
        }
    }

    override fun onRunEnded(wave: Int, score: Long) {
        _state.update { it.copy(phase = GamePhase.GAME_OVER, isGameOver = true, lives = 0) }
        viewModelScope.launch { _events.emit(GameEvent.RunEnded(wave, score)) }
    }

    override fun onRiftShift() {
        _state.update { cur ->
            cur.copy(
                riftShiftActive = true,
                gold = game.gold,
                activeTowerSlots = buildTowerSlotMap()
            )
        }
        viewModelScope.launch {
            _events.emit(GameEvent.RiftShift)
            _events.emit(GameEvent.ShowMessage("RIFT SHIFT! Layout changed."))
            kotlinx.coroutines.delay(2000L)
            _state.update { it.copy(riftShiftActive = false) }
        }
    }

    override fun onWaveProgress(cleared: Int, total: Int) {
        _state.update { it.copy(waveEnemiesCleared = cleared, waveEnemyTotal = maxOf(1, total)) }
    }

    override fun onTowerTapped(slotId: Int) {
        val inst = game.towerInstance(slotId) ?: return
        _state.update { cur ->
            cur.copy(
                selectedTowerSlotId = slotId,
                selectedTowerInfo = buildTowerInfo(slotId, inst.tower.type, inst.level, inst.totalInvested)
            )
        }
    }

    override fun onGoldChanged(gold: Int) {
        _state.update { it.copy(gold = gold) }
    }

    // ---- Helpers ----

    private fun syncTowerSlots() {
        _state.update { it.copy(gold = game.gold, activeTowerSlots = buildTowerSlotMap()) }
    }

    private fun buildTowerSlotMap(): Map<Int, TowerSlotData> {
        return game.towers.mapValues { (_, inst) ->
            TowerSlotData(inst.tower.type, inst.level, inst.totalInvested)
        }
    }

    private fun buildTowerInfo(slotId: Int, type: TowerType, level: Int, totalInvested: Int): TowerInfo {
        val inst = game.towerInstance(slotId)
        val tower = inst?.tower ?: when (type) {
            TowerType.BOLT  -> com.pathrift.anonve.android.game.towers.BoltTower()
            TowerType.BLAST -> com.pathrift.anonve.android.game.towers.BlastTower()
            TowerType.FROST -> com.pathrift.anonve.android.game.towers.FrostTower()
        }
        val upgradeCost = (EconomyConstants.UPGRADE_BASE_COST *
                EconomyConstants.UPGRADE_GROWTH_RATE.pow((level - 1).toDouble())).toInt()
        val sellValue = (totalInvested * EconomyConstants.SELL_REFUND_PERCENT).toInt()
        return TowerInfo(
            slotId = slotId,
            type = type,
            level = level,
            damage = tower.damagePerHit * (1f + 0.25f * (level - 1)),
            range = tower.rangeTiles.toFloat(),
            attackSpeed = tower.attacksPerSecond,
            sellValue = sellValue,
            upgradeCost = upgradeCost
        )
    }

    private fun getCost(type: TowerType): Int = when (type) {
        TowerType.BOLT  -> EconomyConstants.TowerCost.BOLT
        TowerType.BLAST -> EconomyConstants.TowerCost.BLAST
        TowerType.FROST -> EconomyConstants.TowerCost.FROST
    }

    override fun onCleared() {
        super.onCleared()
        game.stop()
    }
}
