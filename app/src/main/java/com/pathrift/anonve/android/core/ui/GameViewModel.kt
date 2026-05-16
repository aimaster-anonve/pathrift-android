package com.pathrift.anonve.android.core.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pathrift.anonve.android.app.PathriftApp
import com.pathrift.anonve.android.core.engine.EconomyConstants
import com.pathrift.anonve.android.core.storage.ArsenalStore
import com.pathrift.anonve.android.core.storage.DiamondStore
import com.pathrift.anonve.android.core.storage.PremiumStore
import android.graphics.PointF
import com.pathrift.anonve.android.game.GameBridge
import com.pathrift.anonve.android.game.GameEngine
import com.pathrift.anonve.android.game.GamePhase
import com.pathrift.anonve.android.game.GameRenderer
import com.pathrift.anonve.android.game.GameState
import com.pathrift.anonve.android.game.TowerInfo
import com.pathrift.anonve.android.game.TowerSlotData
import com.pathrift.anonve.android.game.enemies.EnemyInstance
import com.pathrift.anonve.android.game.enemies.EnemyType
import com.pathrift.anonve.android.game.towers.ArtilleryTower
import com.pathrift.anonve.android.game.towers.BoltTower
import com.pathrift.anonve.android.game.towers.BlastTower
import com.pathrift.anonve.android.game.towers.CoreTower
import com.pathrift.anonve.android.game.towers.FrostTower
import com.pathrift.anonve.android.game.towers.InfernoTower
import com.pathrift.anonve.android.game.towers.NovaTower
import com.pathrift.anonve.android.game.towers.PierceTower
import com.pathrift.anonve.android.game.towers.SniperTower
import com.pathrift.anonve.android.game.towers.TeslaTower
import com.pathrift.anonve.android.game.towers.TowerType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    data class RunEnded(val wave: Int, val score: Long, val kills: Int = 0) : GameEvent()
    data object WaveStarted : GameEvent()
    data object WaveCompleted : GameEvent()
    data object RiftShift : GameEvent()
    data object ShowPremiumPrompt : GameEvent()
}

/**
 * GameViewModel — iOS GameViewModel.swift parity.
 * Implements GameBridge. Exposes StateFlow<GameState> to Compose UI.
 * Now extends AndroidViewModel to access Application (DiamondStore).
 */
class GameViewModel(application: Application) : AndroidViewModel(application), GameBridge {

    private val diamondStore: DiamondStore = (application as PathriftApp).diamondStore
    private val premiumStore: PremiumStore = (application as PathriftApp).premiumStore
    private val arsenalStore: ArsenalStore = (application as PathriftApp).arsenalStore
    private val gameSaveStore: com.pathrift.anonve.android.core.storage.GameSaveStore = (application as PathriftApp).gameSaveStore

    private val _state = MutableStateFlow(GameState(diamonds = diamondStore.balance))
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>()
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val _enemies = MutableStateFlow<List<EnemyInstance>>(emptyList())
    val enemies: StateFlow<List<EnemyInstance>> = _enemies.asStateFlow()

    val game = GameEngine(this, diamondStore, arsenalStore, premiumStore)

    // PATHRIFT-157: Next wave info panel state
    var showNextWaveInfo by mutableStateOf(false)

    /** Set by GameCanvasView once the renderer surface is created; used for projectile injection. */
    var renderer: GameRenderer? = null
    // When wave is active show CURRENT wave def (matches progress bar); between waves show NEXT
    val nextWaveDefinition get() = if (_state.value.phase == com.pathrift.anonve.android.game.GamePhase.WAVE_ACTIVE)
        game.waveSystem.waveDefinition(game.currentWave)
    else
        game.waveSystem.waveDefinition(game.currentWave + 1)

    init {
        // If a save exists, restore it (PLAY clears the save first; CONTINUE keeps it)
        gameSaveStore.load()?.let { save -> game.queueRestore(save) }
        game.start(viewModelScope)
        // Poll enemy positions at ~60fps so the renderer stays live
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            while (true) {
                _enemies.value = game.enemies
                kotlinx.coroutines.delay(16L)
            }
        }
    }

    // ---- Player actions ----

    fun initLayout(width: Float, height: Float, topInset: Float = 0f, bottomInset: Float = 0f) {
        game.initLayout(width, height, topInset = topInset, bottomInset = bottomInset)
        _state.update { it.copy(layoutVersion = it.layoutVersion + 1) }
    }

    fun startNextWave() {
        if (_state.value.phase == GamePhase.WAVE_ACTIVE) return
        game.startNextWave()
        _state.update { it.copy(phase = GamePhase.WAVE_ACTIVE) }
    }

    fun placeTower(slotId: Int, type: TowerType) {
        // Check premium tower lock
        if (type.isPremium && !diamondStore.isUnlocked(type)) {
            viewModelScope.launch {
                _events.emit(GameEvent.ShowMessage("${type.displayName} locked! Unlock for ${type.diamondCost}♦"))
            }
            return
        }
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
            // Show tower selection panel for empty slot
            _state.update { it.copy(selectedTowerSlotId = slotId, selectedTowerInfo = null) }
        }
    }

    fun clearTowerSelection() {
        _state.update { it.copy(selectedTowerSlotId = null, selectedTowerInfo = null) }
    }

    fun restartGame() {
        game.reset()
        game.start(viewModelScope)
        _state.value = GameState(diamonds = diamondStore.balance)
        _enemies.value = emptyList()
    }

    /** Unlock a premium tower by spending diamonds. */
    fun unlockTower(type: TowerType) {
        if (diamondStore.unlock(type)) {
            _state.update { it.copy(diamonds = diamondStore.balance) }
            viewModelScope.launch {
                _events.emit(GameEvent.ShowMessage("${type.displayName} unlocked!"))
            }
        } else {
            viewModelScope.launch {
                _events.emit(GameEvent.ShowMessage("Not enough diamonds! Need ${type.diamondCost}♦"))
            }
        }
    }

    /** Returns true if a tower type is available to place (free or unlocked). Premium bypasses all locks. */
    fun isTowerUnlocked(type: TowerType): Boolean = premiumStore.isPremium || diamondStore.isUnlocked(type)

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
        // Save game state after each wave
        val savedTowers = game.towers.values.map { t ->
            com.pathrift.anonve.android.core.storage.SavedTower(
                slotId = t.slotId, type = t.tower.type.name, level = t.level, totalInvested = t.totalInvested
            )
        }
        gameSaveStore.save(
            wave = wave, lives = game.lives, gold = game.gold,
            kills = game.totalEnemiesKilled,
            layoutIndex = com.pathrift.anonve.android.game.PathSystem.currentLayoutIndex,
            towers = savedTowers
        )
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
        gameSaveStore.clear()
        viewModelScope.launch { _events.emit(GameEvent.RunEnded(wave, score, kills = game.totalEnemiesKilled)) }
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

    override fun onDiamondsChanged(balance: Int) {
        _state.update { it.copy(diamonds = balance) }
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
            TowerType.BOLT      -> BoltTower()
            TowerType.BLAST     -> BlastTower()
            TowerType.FROST     -> FrostTower()
            TowerType.PIERCE    -> PierceTower()
            TowerType.CORE      -> CoreTower()
            TowerType.INFERNO   -> InfernoTower()
            TowerType.TESLA     -> TeslaTower()
            TowerType.NOVA      -> NovaTower()
            TowerType.SNIPER    -> SniperTower()
            TowerType.ARTILLERY -> ArtilleryTower()
        }
        val upgradeCost = (EconomyConstants.UPGRADE_BASE_COST *
                EconomyConstants.UPGRADE_GROWTH_RATE.pow((level - 1).toDouble())).toInt()
        val sellValue = (totalInvested * EconomyConstants.SELL_REFUND_PERCENT).toInt()
        // F2: show effective attack speed per level
        val effectiveAttackSpeed = tower.attacksPerSecond * (1f + 0.08f * (level - 1))
        return TowerInfo(
            slotId = slotId,
            type = type,
            level = level,
            damage = tower.damagePerHit * (1f + 0.25f * (level - 1)),
            range = tower.rangeTiles.toFloat(),
            attackSpeed = effectiveAttackSpeed,
            sellValue = sellValue,
            upgradeCost = upgradeCost
        )
    }

    private fun getCost(type: TowerType): Int = when (type) {
        TowerType.BOLT      -> EconomyConstants.TowerCost.BOLT
        TowerType.BLAST     -> EconomyConstants.TowerCost.BLAST
        TowerType.FROST     -> EconomyConstants.TowerCost.FROST
        TowerType.PIERCE    -> 130
        TowerType.CORE      -> 180
        TowerType.INFERNO   -> 200
        TowerType.TESLA     -> 300
        TowerType.NOVA      -> 500
        TowerType.SNIPER    -> 220
        TowerType.ARTILLERY -> 160
    }

    // ---- Speed toggle ----

    fun toggleSpeed() {
        if (!premiumStore.isPremium) {
            viewModelScope.launch {
                _events.emit(GameEvent.ShowPremiumPrompt)
            }
            return
        }
        val newSpeed = if (_state.value.speedMultiplier == 1.0f) 2.0f else 1.0f
        _state.update { it.copy(speedMultiplier = newSpeed) }
        game.setSpeed(newSpeed)
    }

    // ---- Revive ----

    fun acceptRevive() {
        _state.update { it.copy(showRevivePrompt = false) }
        game.acceptRevive()
    }

    fun declineRevive() {
        _state.update { it.copy(showRevivePrompt = false) }
        game.declineRevive()
    }

    private fun startReviveCountdown() {
        viewModelScope.launch {
            repeat(5) { i ->
                kotlinx.coroutines.delay(1000L)
                _state.update { it.copy(reviveCountdown = 4 - i) }
            }
            if (_state.value.showRevivePrompt) {
                _state.update { it.copy(showRevivePrompt = false) }
                game.declineRevive()
            }
        }
    }

    // ---- New GameBridge callbacks ----

    override fun onSpeedChanged(multiplier: Float) {
        // State already updated by toggleSpeed; no additional action needed
    }

    override fun onReviveAvailable() {
        _state.update { it.copy(showRevivePrompt = true, reviveCountdown = 5) }
        startReviveCountdown()
    }

    override fun onLifeRestored(lives: Int) {
        _state.update { it.copy(lives = lives) }
        viewModelScope.launch { _events.emit(GameEvent.ShowMessage("Revived! 1 life restored.")) }
    }

    fun clearSaveOnQuit() { gameSaveStore.clear() }

    override fun onStateRestored(wave: Int, lives: Int, gold: Int, kills: Int) {
        _state.update { it.copy(wave = wave, lives = lives, gold = gold, enemyKills = kills) }
    }

    override fun onProjectileFired(from: PointF, to: PointF, type: TowerType) {
        val proj = GameRenderer.Projectile(
            id = System.nanoTime(),
            fromX = from.x,
            fromY = from.y,
            toX = to.x,
            toY = to.y,
            progress = 0f,
            type = type
        )
        val r = renderer ?: return
        r.projectiles = r.projectiles + proj
    }

    override fun onCleared() {
        super.onCleared()
        game.stop()
    }
}
