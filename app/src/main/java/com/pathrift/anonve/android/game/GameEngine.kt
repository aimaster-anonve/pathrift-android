package com.pathrift.anonve.android.game

import android.graphics.PointF
import com.pathrift.anonve.android.core.engine.EconomyConstants
import com.pathrift.anonve.android.game.enemies.BossEnemy
import com.pathrift.anonve.android.game.enemies.EnemyInstance
import com.pathrift.anonve.android.game.enemies.EnemyType
import com.pathrift.anonve.android.game.enemies.GhostEnemy
import com.pathrift.anonve.android.game.enemies.RunnerEnemy
import com.pathrift.anonve.android.game.enemies.ShieldEnemy
import com.pathrift.anonve.android.game.enemies.SwarmEnemy
import com.pathrift.anonve.android.game.enemies.TankEnemy
import com.pathrift.anonve.android.game.towers.BlastTower
import com.pathrift.anonve.android.game.towers.BoltTower
import com.pathrift.anonve.android.game.towers.FrostTower
import com.pathrift.anonve.android.game.towers.Tower
import com.pathrift.anonve.android.game.towers.TowerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * GameEngine — full iOS GameScene.swift parity.
 *
 * Manages:
 * - PathSystem layout selection (12 layouts, random on start)
 * - Wave spawning from WaveSystem (9-wave cycle, boss every 10, swarm/ghost scaling)
 * - EnemyInstance movement along PathSystem waypoints
 * - All 6 enemy types: RUNNER, TANK, SHIELD, SWARM, GHOST, BOSS (5 variants)
 * - Tower placement, sell, upgrade, attack, AoE, slow
 * - Shield absorption mechanic (ShieldEnemy)
 * - Ghost slow immunity (90% immune)
 * - Rift Shift every 5th wave — new layout, 65% survivors (min 2)
 * - slotId reassignment after Rift Shift (iOS fix)
 * - Progressive slot unlocking: 6→8→10→12 by wave range
 * - Economy: gold, score, wave rewards
 * - GameBridge callbacks
 */
class GameEngine(private val bridge: GameBridge) {

    val grid = GridSystem()
    private val waveSystem = WaveSystem()

    // Active tower instances keyed by slotId
    private val _towers = mutableMapOf<Int, TowerInstance>()
    val towers: Map<Int, TowerInstance> get() = _towers.toMap()

    private val _enemies = mutableListOf<EnemyInstance>()
    val enemies: List<EnemyInstance> get() = synchronized(_enemies) { _enemies.toList() }

    var currentWave: Int = 0
        private set
    var lives: Int = EconomyConstants.STARTING_LIVES
        private set
    var gold: Int = EconomyConstants.STARTING_GOLD
        private set
    var score: Long = 0L
        private set
    var totalEnemiesKilled: Int = 0
        private set

    // Wave spawn state
    private var waveJob: Job? = null
    private var simulationJob: Job? = null
    private var gameScope: CoroutineScope? = null

    private var isWaveActive: Boolean = false
    private var waveEnemyTotal: Int = 0
    private var waveEnemiesCleared: Int = 0

    // Screen dimensions (set from renderer/view)
    var screenWidth: Float = 0f
    var screenHeight: Float = 0f

    // ---- Lifecycle ----

    fun start(scope: CoroutineScope) {
        gameScope = scope
        startSimulationLoop(scope)
    }

    fun stop() {
        waveJob?.cancel()
        simulationJob?.cancel()
        gameScope = null
    }

    fun reset() {
        waveJob?.cancel()
        simulationJob?.cancel()
        synchronized(_enemies) { _enemies.clear() }
        _towers.clear()
        grid.updateSlots(emptyList())
        currentWave = 0
        lives = EconomyConstants.STARTING_LIVES
        gold = EconomyConstants.STARTING_GOLD
        score = 0L
        totalEnemiesKilled = 0
        isWaveActive = false
        waveEnemyTotal = 0
        waveEnemiesCleared = 0
    }

    /** Must be called once screen dimensions are known (before first wave). */
    fun initLayout(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        PathSystem.buildLayout(width, height, currentWave = 0, layoutIndex = -1)
        grid.updateSlots(PathSystem.slotPositions)
    }

    // ---- Wave Management ----

    fun startNextWave() {
        if (isWaveActive || lives <= 0) return
        currentWave++
        val waveDef = waveSystem.getWaveDefinition(currentWave)
        val hpMult = waveSystem.hpScaleMultiplier(currentWave)

        waveEnemyTotal = waveDef.totalEnemyCount
        waveEnemiesCleared = 0
        isWaveActive = true

        bridge.onWaveStarted(currentWave, waveEnemyTotal)
        bridge.onWaveProgress(0, waveEnemyTotal)

        waveJob = gameScope?.launch(Dispatchers.Default) {
            for (group in waveDef.spawnGroups) {
                repeat(group.count) {
                    if (!isActive) return@launch
                    spawnEnemy(group.type, hpMult)
                    delay(group.spawnIntervalMs)
                }
            }
        }
    }

    // ---- Tower Placement ----

    fun placeTower(slotId: Int, type: TowerType): Boolean {
        val tower = when (type) {
            TowerType.BOLT  -> BoltTower()
            TowerType.BLAST -> BlastTower()
            TowerType.FROST -> FrostTower()
        }
        if (gold < tower.cost) return false
        val slot = grid.slot(slotId) ?: return false
        if (slot.state.isOccupied) return false

        gold -= tower.cost
        grid.placeTower(type, slotId, tower.cost)
        _towers[slotId] = TowerInstance(
            slotId = slotId,
            tower = tower,
            position = slot.position,
            level = 1,
            totalInvested = tower.cost
        )
        bridge.onGoldChanged(gold)
        return true
    }

    fun sellTower(slotId: Int) {
        val inst = _towers[slotId] ?: return
        val refund = (inst.totalInvested * EconomyConstants.SELL_REFUND_PERCENT).toInt()
        gold += refund
        _towers.remove(slotId)
        grid.removeTower(slotId)
        bridge.onGoldChanged(gold)
    }

    fun upgradeTower(slotId: Int) {
        val inst = _towers[slotId] ?: return
        val upgradeCost = (EconomyConstants.UPGRADE_BASE_COST *
                Math.pow(EconomyConstants.UPGRADE_GROWTH_RATE, (inst.level - 1).toDouble())).toInt()
        if (gold < upgradeCost) return
        gold -= upgradeCost
        _towers[slotId] = inst.copy(level = inst.level + 1, totalInvested = inst.totalInvested + upgradeCost)
        grid.upgradeTower(slotId, upgradeCost)
        bridge.onGoldChanged(gold)
    }

    fun towerInstance(slotId: Int): TowerInstance? = _towers[slotId]

    // ---- Simulation Loop ----

    private fun startSimulationLoop(scope: CoroutineScope) {
        simulationJob = scope.launch(Dispatchers.Default) {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                val deltaMs = (now - lastTime).coerceAtMost(50L)
                lastTime = now
                if (deltaMs > 0) {
                    updateSimulation(deltaMs / 1000f)
                }
                delay(16L)
            }
        }
    }

    private fun updateSimulation(delta: Float) {
        if (lives <= 0) return

        val pathLen = PathSystem.totalPathLength()
        if (pathLen <= 0f) return

        val toKill = mutableListOf<Long>()
        val toEscape = mutableListOf<Long>()
        val now = System.currentTimeMillis()

        synchronized(_enemies) {
            _enemies.replaceAll { enemy ->
                if (!enemy.isAlive) return@replaceAll enemy

                // Restore speed if slow expired
                val spd = if (now > enemy.slowEndTime && enemy.currentSpeed < enemy.baseSpeed) {
                    enemy.baseSpeed
                } else {
                    enemy.currentSpeed
                }

                val distanceToMove = spd * delta
                val newProgress = enemy.pathProgress + distanceToMove / pathLen

                if (newProgress >= 1f) {
                    toEscape.add(enemy.id)
                    return@replaceAll enemy.copy(hasReachedEnd = true, pathProgress = 1f)
                }

                enemy.copy(pathProgress = newProgress, currentSpeed = spd)
            }

            // Remove escaped
            for (id in toEscape) {
                _enemies.removeAll { it.id == id }
                waveEnemiesCleared++
                lives = max(0, lives - 1)
                bridge.onEnemyEscaped()
                bridge.onLifeLost(lives)
                bridge.onWaveProgress(waveEnemiesCleared, waveEnemyTotal)
                if (lives <= 0) {
                    triggerGameOver()
                    return
                }
            }

            // Remove dead (killed by towers — handled in applyTowerAttacks)
            _enemies.removeAll { it.isDead }
        }

        applyTowerAttacks(delta)
        checkWaveCompletion()
    }

    private fun applyTowerAttacks(delta: Float) {
        val now = System.currentTimeMillis()
        synchronized(_enemies) {
            for ((slotId, inst) in _towers) {
                val tower = inst.tower
                // Attack cooldown
                if (now - inst.lastAttackTime < (1000L / tower.attacksPerSecond).toLong()) continue

                // Range in screen pixels (tile-based towers used tile coords; here we use pixel range)
                // rangeTiles * TILE_SIZE_DP gives approximate pixel range
                val rangePixels = tower.rangeTiles * GridSystem.TILE_SIZE_DP

                val inRange = _enemies.filter { e ->
                    if (!e.isAlive) return@filter false
                    val pos = PathSystem.positionAt(e.pathProgress)
                    val dx = pos.x - inst.position.x
                    val dy = pos.y - inst.position.y
                    sqrt(dx * dx + dy * dy) <= rangePixels
                }

                if (inRange.isEmpty()) continue

                // Damage with level scaling: +25% per level above 1
                val levelMult = 1f + 0.25f * (inst.level - 1)
                val damage = tower.damagePerHit * levelMult

                when {
                    tower.aoeRadius > 0f -> {
                        // AoE: damage all in range
                        val aoePixels = tower.aoeRadius * GridSystem.TILE_SIZE_DP
                        // Target furthest along path, then splash from its position
                        val primary = inRange.maxByOrNull { it.pathProgress } ?: continue
                        val primaryPos = PathSystem.positionAt(primary.pathProgress)
                        for (e in inRange) {
                            val ePos = PathSystem.positionAt(e.pathProgress)
                            val dx = ePos.x - primaryPos.x
                            val dy = ePos.y - primaryPos.y
                            if (sqrt(dx * dx + dy * dy) <= aoePixels) {
                                applyDamage(e.id, damage.toInt())
                            }
                        }
                    }
                    tower.slowFactor < 1f -> {
                        // Frost: single target + slow
                        val target = inRange.maxByOrNull { it.pathProgress } ?: continue
                        applyDamage(target.id, damage.toInt())
                        applySlow(target.id, tower.slowFactor, durationMs = 2000L)
                    }
                    else -> {
                        // Single target: furthest along path
                        val target = inRange.maxByOrNull { it.pathProgress } ?: continue
                        applyDamage(target.id, damage.toInt())
                    }
                }

                _towers[slotId] = inst.copy(lastAttackTime = now)
            }
        }
    }

    /** Apply damage to enemy, handling shield absorption and armor reduction. */
    private fun applyDamage(enemyId: Long, rawDamage: Int) {
        val idx = _enemies.indexOfFirst { it.id == enemyId }
        if (idx < 0) return
        val enemy = _enemies[idx]
        if (!enemy.isAlive) return

        val updated: EnemyInstance = when {
            // ShieldEnemy: absorb via shieldHp first
            enemy.type == EnemyType.SHIELD && !enemy.shieldBroken && enemy.shieldHp > 0f -> {
                val dmg = rawDamage.toFloat()
                if (dmg >= enemy.shieldHp) {
                    val overflow = dmg - enemy.shieldHp
                    enemy.copy(shieldHp = 0f, shieldBroken = true, currentHp = max(0f, enemy.currentHp - overflow))
                } else {
                    enemy.copy(shieldHp = enemy.shieldHp - dmg)
                }
            }
            // GhostEnemy: armor reduction already 0, but slow immunity handled in applySlow
            // All others: normal armor reduction
            else -> {
                val actualDmg = rawDamage * (1f - enemy.armorReduction)
                enemy.copy(currentHp = max(0f, enemy.currentHp - actualDmg))
            }
        }

        if (updated.currentHp <= 0f) {
            _enemies.removeAt(idx)
            gold += updated.goldReward
            score += updated.goldReward * 2L
            totalEnemiesKilled++
            waveEnemiesCleared++
            bridge.onEnemyKilled(updated.type, updated.goldReward)
            bridge.onGoldChanged(gold)
            bridge.onWaveProgress(waveEnemiesCleared, waveEnemyTotal)
        } else {
            _enemies[idx] = updated
        }
    }

    /** Apply slow to an enemy — Ghost is 90% immune. */
    private fun applySlow(enemyId: Long, slowFactor: Float, durationMs: Long) {
        val idx = _enemies.indexOfFirst { it.id == enemyId }
        if (idx < 0) return
        val enemy = _enemies[idx]

        val effectiveFactor = if (enemy.type == EnemyType.GHOST) {
            // 90% immune — only 10% of slow applies
            1f - (1f - slowFactor) * GhostEnemy.SLOW_IMMUNITY_FACTOR
        } else {
            slowFactor
        }

        val newSpeed = enemy.baseSpeed * effectiveFactor
        val endTime = System.currentTimeMillis() + durationMs
        _enemies[idx] = enemy.copy(currentSpeed = newSpeed, slowEndTime = endTime)
    }

    private fun checkWaveCompletion() {
        if (!isWaveActive) return
        val waveSpawning = waveJob?.isActive == true
        val anyAlive = synchronized(_enemies) { _enemies.any { it.isAlive } }
        if (!waveSpawning && !anyAlive) {
            isWaveActive = false
            val reward = waveSystem.goldRewardForWave(currentWave)
            gold += reward
            score += reward.toLong() * 5
            bridge.onWaveCompleted(currentWave, reward)
            bridge.onGoldChanged(gold)

            // Rift Shift every 5th wave
            if (currentWave % 5 == 0) {
                performRiftShift()
            }
        }
    }

    // ---- Rift Shift ----

    private fun performRiftShift() {
        if (screenWidth <= 0f || screenHeight <= 0f) return

        // Pick a new layout — never same as current
        val oldIndex = PathSystem.currentLayoutIndex
        var newIndex = (0 until PathSystem.layoutCount).random()
        if (newIndex == oldIndex) {
            newIndex = (newIndex + 1 + (1 until PathSystem.layoutCount).random()) % PathSystem.layoutCount
        }

        // Snapshot surviving towers before layout change
        val towerSnapshot = _towers.values.toList()

        // Build new path + slots
        PathSystem.buildLayout(screenWidth, screenHeight, currentWave, newIndex)
        val newSlotPositions = PathSystem.slotPositions
        grid.updateSlots(newSlotPositions)

        // Determine survivors: 65% survive, min 2 (or all if ≤2), mirrors iOS
        val n = towerSnapshot.size
        val survivorCount = if (n <= 2) n else min(n, max(2, ceil(n * 0.65).toInt()))
        val shuffled = towerSnapshot.shuffled()
        val survivors = shuffled.take(survivorCount)
        val destroyed = shuffled.drop(survivorCount)

        // Refund destroyed towers at rift rate
        for (snap in destroyed) {
            val refund = (snap.totalInvested * EconomyConstants.RIFT_SELL_PERCENT).toInt()
            gold += refund
            _towers.remove(snap.slotId)
            grid.removeTower(snap.slotId)
        }
        bridge.onGoldChanged(gold)

        // Clear old tower-slot assignments — grid was reset by updateSlots
        _towers.clear()

        // Assign survivors to new shuffled slot IDs
        val availableSlotIds = newSlotPositions.indices.toMutableList().also { it.shuffle() }
        for ((i, snap) in survivors.withIndex()) {
            val targetSlotId = if (i < availableSlotIds.size) availableSlotIds[i] else i
            val newSlot = grid.slot(targetSlotId) ?: continue
            val newPos = newSlot.position

            // Re-place in grid
            grid.placeTower(snap.tower.type, targetSlotId, snap.totalInvested)

            // Update tower instance with new slotId and position
            val updatedInst = snap.copy(slotId = targetSlotId, position = newPos)
            _towers[targetSlotId] = updatedInst
        }

        bridge.onRiftShift()
    }

    // ---- Enemy Spawning ----

    private fun spawnEnemy(type: EnemyType, hpMult: Float) {
        val id = System.nanoTime()
        val instance = when (type) {
            EnemyType.RUNNER -> EnemyInstance(
                id = id, type = type,
                maxHp = RunnerEnemy.HP * hpMult, currentHp = RunnerEnemy.HP * hpMult,
                baseSpeed = RunnerEnemy.SPEED, currentSpeed = RunnerEnemy.SPEED,
                goldReward = RunnerEnemy.GOLD_REWARD,
                armorReduction = RunnerEnemy.ARMOR_REDUCTION
            )
            EnemyType.TANK -> EnemyInstance(
                id = id, type = type,
                maxHp = TankEnemy.HP * hpMult, currentHp = TankEnemy.HP * hpMult,
                baseSpeed = TankEnemy.SPEED, currentSpeed = TankEnemy.SPEED,
                goldReward = TankEnemy.GOLD_REWARD,
                armorReduction = TankEnemy.ARMOR_REDUCTION
            )
            EnemyType.SHIELD -> EnemyInstance(
                id = id, type = type,
                maxHp = ShieldEnemy.HP * hpMult, currentHp = ShieldEnemy.HP * hpMult,
                baseSpeed = ShieldEnemy.SPEED, currentSpeed = ShieldEnemy.SPEED,
                goldReward = ShieldEnemy.GOLD_REWARD,
                armorReduction = ShieldEnemy.ARMOR_REDUCTION,
                shieldHp = ShieldEnemy.SHIELD_HP,
                shieldBroken = false
            )
            EnemyType.SWARM -> EnemyInstance(
                id = id, type = type,
                maxHp = SwarmEnemy.HP * hpMult, currentHp = SwarmEnemy.HP * hpMult,
                baseSpeed = SwarmEnemy.SPEED, currentSpeed = SwarmEnemy.SPEED,
                goldReward = SwarmEnemy.GOLD_REWARD,
                armorReduction = SwarmEnemy.ARMOR_REDUCTION
            )
            EnemyType.GHOST -> EnemyInstance(
                id = id, type = type,
                maxHp = GhostEnemy.HP * hpMult, currentHp = GhostEnemy.HP * hpMult,
                baseSpeed = GhostEnemy.SPEED, currentSpeed = GhostEnemy.SPEED,
                goldReward = GhostEnemy.GOLD_REWARD,
                armorReduction = GhostEnemy.ARMOR_REDUCTION
            )
            EnemyType.BOSS -> {
                val variant = BossEnemy.variantIndex(currentWave)
                EnemyInstance(
                    id = id, type = type,
                    maxHp = BossEnemy.hp(currentWave), currentHp = BossEnemy.hp(currentWave),
                    baseSpeed = BossEnemy.speed(variant), currentSpeed = BossEnemy.speed(variant),
                    goldReward = BossEnemy.GOLD_REWARD,
                    armorReduction = BossEnemy.armor(variant),
                    bossVariant = variant
                )
            }
        }
        synchronized(_enemies) { _enemies.add(instance) }
    }

    private fun triggerGameOver() {
        isWaveActive = false
        waveJob?.cancel()
        bridge.onRunEnded(currentWave, score)
    }
}

/**
 * Tower instance in the engine — tracks runtime mutable state alongside immutable Tower definition.
 */
data class TowerInstance(
    val slotId: Int,
    val tower: Tower,
    val position: PointF,
    val level: Int = 1,
    val totalInvested: Int = 0,
    val lastAttackTime: Long = 0L
)
