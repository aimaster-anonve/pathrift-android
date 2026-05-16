package com.pathrift.anonve.android.game

import android.content.res.Resources
import android.graphics.PointF
import com.pathrift.anonve.android.core.engine.EconomyConstants
import com.pathrift.anonve.android.game.PathLayer
import com.pathrift.anonve.android.core.storage.ArsenalStore
import com.pathrift.anonve.android.core.storage.DiamondStore
import com.pathrift.anonve.android.core.storage.PremiumStore
import com.pathrift.anonve.android.game.enemies.BossEnemy
import com.pathrift.anonve.android.game.enemies.EnemyInstance
import com.pathrift.anonve.android.game.enemies.EnemyType
import com.pathrift.anonve.android.game.enemies.GhostEnemy
import com.pathrift.anonve.android.game.enemies.HealerEnemy
import com.pathrift.anonve.android.game.enemies.JumperEnemy
import com.pathrift.anonve.android.game.enemies.PhantomEnemy
import com.pathrift.anonve.android.game.enemies.RunnerEnemy
import com.pathrift.anonve.android.game.enemies.ShieldEnemy
import com.pathrift.anonve.android.game.enemies.SplitterEnemy
import com.pathrift.anonve.android.game.enemies.SwarmEnemy
import com.pathrift.anonve.android.game.enemies.TankEnemy
import com.pathrift.anonve.android.game.towers.ArtilleryTower
import com.pathrift.anonve.android.game.towers.BlastTower
import com.pathrift.anonve.android.game.towers.BoltTower
import com.pathrift.anonve.android.game.towers.CoreTower
import com.pathrift.anonve.android.game.towers.FrostTower
import com.pathrift.anonve.android.game.towers.InfernoTower
import com.pathrift.anonve.android.game.towers.NovaTower
import com.pathrift.anonve.android.game.towers.PierceTower
import com.pathrift.anonve.android.game.towers.SniperTower
import com.pathrift.anonve.android.game.towers.TargetingMode
import com.pathrift.anonve.android.game.towers.TeslaTower
import com.pathrift.anonve.android.game.towers.Tower
import com.pathrift.anonve.android.game.towers.TowerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * GameEngine — full iOS GameScene.swift parity + Phase 2 features.
 *
 * Phase 2 additions:
 * - 5 new tower types: Pierce, Core, Inferno, Tesla, Nova
 * - Attack speed scaling per upgrade level (+8% per level)
 * - Tower type advantage multipliers
 * - Pierce shield bypass + Core armor penetration
 * - Tesla chain lightning
 * - DiamondStore integration: +2/3/4 per 10-wave milestone
 * - Splitter enemy (splits into 2 Swarm on death)
 * - Jumper enemy (teleports forward every 3s)
 */
class GameEngine(
    private val bridge: GameBridge,
    val diamondStore: DiamondStore,
    private val arsenalStore: ArsenalStore,
    private val premiumStore: PremiumStore
) {

    val grid = GridSystem()
    val waveSystem = WaveSystem()   // internal — exposed for ViewModel preview (PATHRIFT-157)

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

    // Speed multiplier (1.0 = normal, 2.0 = fast)
    var speedMultiplier: Float = 1.0f

    // Boss ability state (PATHRIFT-155)
    /** When > currentTime: all towers fire at 40% attack rate (Rift Pulse debuff). */
    var riftPulseEndTime: Double = 0.0
    /** When > currentTime: all towers within radius are disabled (Gravity Well). */
    var gravityWellEndTime: Double = 0.0

    // Revive state
    var hasUsedRevive: Boolean = false

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
        speedMultiplier = 1.0f
        hasUsedRevive = false
        riftPulseEndTime = 0.0
        gravityWellEndTime = 0.0
    }

    // Set before initLayout to restore a saved game
    private var pendingSave: com.pathrift.anonve.android.core.storage.GameSaveState? = null

    fun queueRestore(save: com.pathrift.anonve.android.core.storage.GameSaveState) {
        pendingSave = save
    }

    /** Must be called once screen dimensions are known (before first wave).
     *  topInset / bottomInset are in pixels and include status bar + HUD height and
     *  nav bar + bottom bar height respectively, as measured from Compose onGloballyPositioned.
     */
    fun initLayout(width: Float, height: Float, topInset: Float = 0f, bottomInset: Float = 0f) {
        screenWidth = width
        screenHeight = height
        val density = Resources.getSystem().displayMetrics.density
        // Use real insets from Compose when provided; fall back to sensible defaults
        PathSystem.hudTopInset = if (topInset > 0f) topInset else 48f * density
        PathSystem.hudBottomInset = if (bottomInset > 0f) bottomInset else 46f * density
        PathSystem.hudHorizontalInset = 8f * density
        PathSystem.screenDensity = density  // Build 8: density-aware path clearance (PATHRIFT-160 fix)
        val layoutIdx = pendingSave?.layoutIndex ?: -1
        PathSystem.buildLayout(width, height, currentWave = 0, layoutIndex = layoutIdx)
        grid.updateSlots(PathSystem.slotPositions)
        pendingSave?.let { applyRestore(it) }
        pendingSave = null
    }

    private fun applyRestore(save: com.pathrift.anonve.android.core.storage.GameSaveState) {
        currentWave = save.wave
        waveSystem.syncWave(save.wave)   // PATHRIFT-151: keep wave system in sync
        lives = save.lives
        gold = save.gold
        totalEnemiesKilled = save.enemyKills
        score = 0L

        for (t in save.towers) {
            val type = TowerType.values().firstOrNull { it.name == t.type } ?: continue
            val slot = grid.slot(t.slotId) ?: continue
            if (slot.state.isOccupied) continue
            val tower = when (type) {
                TowerType.BOLT      -> com.pathrift.anonve.android.game.towers.BoltTower()
                TowerType.BLAST     -> com.pathrift.anonve.android.game.towers.BlastTower()
                TowerType.FROST     -> com.pathrift.anonve.android.game.towers.FrostTower()
                TowerType.PIERCE    -> com.pathrift.anonve.android.game.towers.PierceTower()
                TowerType.CORE      -> com.pathrift.anonve.android.game.towers.CoreTower()
                TowerType.INFERNO   -> com.pathrift.anonve.android.game.towers.InfernoTower()
                TowerType.TESLA     -> com.pathrift.anonve.android.game.towers.TeslaTower()
                TowerType.NOVA      -> com.pathrift.anonve.android.game.towers.NovaTower()
                TowerType.SNIPER    -> com.pathrift.anonve.android.game.towers.SniperTower()
                TowerType.ARTILLERY -> com.pathrift.anonve.android.game.towers.ArtilleryTower()
            }
            grid.placeTower(type, t.slotId, t.totalInvested)
            _towers[t.slotId] = TowerInstance(
                slotId = t.slotId, tower = tower, position = slot.position,
                level = t.level, totalInvested = t.totalInvested
            )
        }
        bridge.onGoldChanged(gold)
        bridge.onStateRestored(currentWave, lives, gold, totalEnemiesKilled)
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
                    delay((group.spawnIntervalMs / speedMultiplier).toLong())
                }
            }
        }
    }

    // ---- Tower Placement ----

    fun placeTower(slotId: Int, type: TowerType): Boolean {
        val tower = when (type) {
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

        val adjustedDelta = delta * speedMultiplier
        val pathLen = PathSystem.totalPathLength()
        if (pathLen <= 0f) return

        val toKill = mutableListOf<Long>()
        val toEscape = mutableListOf<Long>()
        val now = System.currentTimeMillis()
        val nowSec = now / 1000.0

        synchronized(_enemies) {
            _enemies.replaceAll { enemy ->
                if (!enemy.isAlive) return@replaceAll enemy

                // Restore speed if slow expired
                val spd = if (now > enemy.slowEndTime && enemy.currentSpeed < enemy.baseSpeed) {
                    enemy.baseSpeed
                } else {
                    enemy.currentSpeed
                }

                // Jumper jump mechanic
                val isJumper = enemy.type == EnemyType.JUMPER
                val shouldJump = isJumper && (now - enemy.lastJumpTime) >= JumperEnemy.JUMP_INTERVAL_MS

                val newProgress = if (shouldJump) {
                    minOf(1f, enemy.pathProgress + JumperEnemy.JUMP_DISTANCE)
                } else {
                    val distanceToMove = spd * adjustedDelta
                    enemy.pathProgress + distanceToMove / pathLen
                }

                val updatedLastJump = if (shouldJump) now else enemy.lastJumpTime

                if (newProgress >= 1f) {
                    toEscape.add(enemy.id)
                    return@replaceAll enemy.copy(hasReachedEnd = true, pathProgress = 1f, lastJumpTime = updatedLastJump)
                }

                // Update pathLayer based on current waypoint segment
                val waypointLayerCount = PathSystem.waypointLayers.size
                val waypointIndex = if (waypointLayerCount > 1) {
                    (newProgress * (waypointLayerCount - 1)).toInt().coerceIn(0, waypointLayerCount - 1)
                } else {
                    0
                }
                val newLayer = PathSystem.layerAt(waypointIndex)

                enemy.copy(pathProgress = newProgress, currentSpeed = spd, lastJumpTime = updatedLastJump, pathLayer = newLayer)
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
                    if (premiumStore.isPremium && !hasUsedRevive) {
                        bridge.onReviveAvailable()
                    } else {
                        triggerGameOver()
                    }
                    return
                }
            }

            // Remove dead (killed by towers — handled in applyTowerAttacks)
            _enemies.removeAll { it.isDead }
        }

        // Boss ability tick — PATHRIFT-155
        tickBossAbilities(nowSec)

        // Healer aura tick — PATHRIFT-159
        tickHealerAuras(now)

        applyTowerAttacks(delta, nowSec)
        checkWaveCompletion()
    }

    private fun applyTowerAttacks(delta: Float, nowSec: Double = System.currentTimeMillis() / 1000.0) {
        val now = System.currentTimeMillis()
        val riftPulseActive = nowSec < riftPulseEndTime
        val gravityWellActive = nowSec < gravityWellEndTime
        synchronized(_enemies) {
            for ((slotId, inst) in _towers) {
                val tower = inst.tower

                // Permanent bonuses from Arsenal
                val permDmgBonus = arsenalStore.permDamageBonus(tower.type)
                val permSpdBonus = arsenalStore.permSpeedBonus(tower.type)

                // F2: Attack speed scaling — +8% per level above 1, plus permanent speed bonus
                var effectiveAttacksPerSecond = tower.attacksPerSecond * (1f + 0.08f * (inst.level - 1)) * (1f + permSpdBonus)

                // PATHRIFT-155: Rift Pulse debuff reduces attack rate to 40%
                if (riftPulseActive) {
                    effectiveAttacksPerSecond *= 0.40f
                }

                // PATHRIFT-155: Gravity Well disables towers
                if (gravityWellActive) continue

                // Attack cooldown
                if (now - inst.lastAttackTime < (1000L / effectiveAttacksPerSecond).toLong()) continue

                // Range in screen pixels — TILE_SIZE_DP must be scaled by display density
                val rangePixels = tower.rangeTiles * GridSystem.TILE_SIZE_DP * Resources.getSystem().displayMetrics.density

                val inRange = _enemies.filter { e ->
                    if (!e.isAlive) return@filter false
                    val pos = PathSystem.positionAt(e.pathProgress)
                    val dx = pos.x - inst.position.x
                    val dy = pos.y - inst.position.y
                    val distOk = sqrt(dx * dx + dy * dy) <= rangePixels
                    if (!distOk) return@filter false
                    // Layer filter based on targeting mode
                    when (tower.type.targetingMode) {
                        TargetingMode.ALL_LAYERS   -> true
                        TargetingMode.GROUND_ONLY  -> e.pathLayer == PathLayer.GROUND
                        TargetingMode.BRIDGE_ONLY  -> e.pathLayer == PathLayer.BRIDGE
                    }
                }

                if (inRange.isEmpty()) continue

                // Damage with level scaling: +25% per level above 1, plus permanent damage bonus
                val levelMult = (1f + 0.25f * (inst.level - 1)) * (1f + permDmgBonus)
                val damage = tower.damagePerHit * levelMult

                var targetFacingAngle = inst.facingAngle

                when {
                    // F5: Pierce — hits all enemies in range, bypasses shield
                    tower.type == TowerType.PIERCE -> {
                        val primary = inRange.maxByOrNull { it.pathProgress }
                        if (primary != null) {
                            val tPos = PathSystem.positionAt(primary.pathProgress)
                            targetFacingAngle = atan2(tPos.y - inst.position.y, tPos.x - inst.position.x)
                        }
                        for (e in inRange) {
                            val typeMult = tower.type.damageMultiplier(e.type)
                            val finalDamage = (damage * typeMult).toInt()
                            applyDamage(e.id, finalDamage, bypassShield = true)
                        }
                    }

                    // F8: Tesla — chain lightning hits primary + 2 nearest (AoE chain = no dodge for chain targets)
                    tower.type == TowerType.TESLA -> {
                        val primary = inRange.maxByOrNull { it.pathProgress } ?: continue
                        val primaryPos = PathSystem.positionAt(primary.pathProgress)
                        targetFacingAngle = atan2(primaryPos.y - inst.position.y, primaryPos.x - inst.position.x)
                        val typeMult = tower.type.damageMultiplier(primary.type)
                        val primaryDamage = (damage * typeMult).toInt()
                        applyDamage(primary.id, primaryDamage, isAoe = true)  // Tesla is chain = AoE bypass

                        // Chain to 2 nearest other enemies within 150px of primary
                        val chainTargets = inRange
                            .filter { it.id != primary.id && it.isAlive }
                            .sortedBy { e ->
                                val ePos = PathSystem.positionAt(e.pathProgress)
                                val dx = ePos.x - primaryPos.x
                                val dy = ePos.y - primaryPos.y
                                dx * dx + dy * dy
                            }
                            .take(2)
                        val chainDamage = (18f * levelMult).toInt()
                        for (chainTarget in chainTargets) {
                            val chainMult = tower.type.damageMultiplier(chainTarget.type)
                            applyDamage(chainTarget.id, (chainDamage * chainMult).toInt(), isAoe = true)
                        }
                    }

                    // AoE towers (Blast, Nova): damage all in splash radius — always bypass Phantom dodge
                    tower.aoeRadius > 0f -> {
                        val aoePixels = tower.aoeRadius * GridSystem.TILE_SIZE_DP
                        val primary = inRange.maxByOrNull { it.pathProgress } ?: continue
                        val primaryPos = PathSystem.positionAt(primary.pathProgress)
                        targetFacingAngle = atan2(primaryPos.y - inst.position.y, primaryPos.x - inst.position.x)
                        for (e in inRange) {
                            val ePos = PathSystem.positionAt(e.pathProgress)
                            val dx = ePos.x - primaryPos.x
                            val dy = ePos.y - primaryPos.y
                            if (sqrt(dx * dx + dy * dy) <= aoePixels) {
                                val typeMult = tower.type.damageMultiplier(e.type)
                                val finalDamage = (damage * typeMult).toInt()
                                applyDamage(e.id, finalDamage, isAoe = true)
                            }
                        }
                    }

                    // Frost: single target + slow
                    tower.slowFactor < 1f -> {
                        val target = inRange.maxByOrNull { it.pathProgress } ?: continue
                        val tPos = PathSystem.positionAt(target.pathProgress)
                        targetFacingAngle = atan2(tPos.y - inst.position.y, tPos.x - inst.position.x)
                        val typeMult = tower.type.damageMultiplier(target.type)
                        val finalDamage = (damage * typeMult).toInt()
                        applyDamage(target.id, finalDamage)
                        applySlow(target.id, tower.slowFactor, durationMs = 2000L)
                    }

                    // Single target: furthest along path
                    else -> {
                        val target = inRange.maxByOrNull { it.pathProgress } ?: continue
                        val tPos = PathSystem.positionAt(target.pathProgress)
                        targetFacingAngle = atan2(tPos.y - inst.position.y, tPos.x - inst.position.x)
                        val typeMult = tower.type.damageMultiplier(target.type)
                        val penetration = if (tower.type == TowerType.CORE) 0.5f else 0f
                        val finalDamage = (damage * typeMult).toInt()
                        applyDamage(target.id, finalDamage, penetration = penetration)
                    }
                }

                _towers[slotId] = inst.copy(lastAttackTime = now, facingAngle = targetFacingAngle)

                // Fire projectile visual — pick the primary target position for the line/AoE effect
                val primaryTarget = inRange.maxByOrNull { it.pathProgress }
                if (primaryTarget != null) {
                    val targetPos = PathSystem.positionAt(primaryTarget.pathProgress)
                    bridge.onProjectileFired(inst.position, targetPos, tower.type)
                }
            }
        }
    }

    /**
     * Apply damage to enemy, handling shield absorption and armor reduction.
     * @param bypassShield When true (Pierce/AoE), skips shield absorption and Phantom dodge.
     * @param penetration  Armor penetration fraction (0..1). Core uses 0.5 = 50% armor ignored.
     * @param isAoe        When true, bypasses Phantom dodge check.
     */
    private fun applyDamage(
        enemyId: Long, rawDamage: Int,
        bypassShield: Boolean = false,
        penetration: Float = 0f,
        isAoe: Boolean = false
    ) {
        val idx = _enemies.indexOfFirst { it.id == enemyId }
        if (idx < 0) return
        val enemy = _enemies[idx]
        if (!enemy.isAlive) return

        // PATHRIFT-155: Boss Iron Colossus shell immunity
        if (enemy.bossShellActive) return

        // PATHRIFT-159: Phantom dodge — single-target hits have 40% miss chance
        if (!isAoe && enemy.type == EnemyType.PHANTOM && Math.random() < PhantomEnemy.DODGE_CHANCE) {
            // Dodge: mark flash and return without damage
            _enemies[idx] = enemy.copy(dodgeFlashing = true)
            return
        }

        val updated: EnemyInstance = when {
            // ShieldEnemy: absorb via shieldHp first (unless bypassed by Pierce)
            enemy.type == EnemyType.SHIELD && !enemy.shieldBroken && enemy.shieldHp > 0f && !bypassShield -> {
                val dmg = rawDamage.toFloat()
                if (dmg >= enemy.shieldHp) {
                    val overflow = dmg - enemy.shieldHp
                    val effectiveArmor = enemy.armorReduction * (1f - penetration)
                    val armoredOverflow = overflow * (1f - effectiveArmor)
                    enemy.copy(shieldHp = 0f, shieldBroken = true, currentHp = max(0f, enemy.currentHp - armoredOverflow))
                } else {
                    enemy.copy(shieldHp = enemy.shieldHp - dmg)
                }
            }
            // All others (or bypass): armor reduction with optional penetration
            else -> {
                val effectiveArmor = enemy.armorReduction * (1f - penetration)
                val actualDmg = rawDamage * (1f - effectiveArmor)
                enemy.copy(currentHp = max(0f, enemy.currentHp - actualDmg), dodgeFlashing = false)
            }
        }

        if (updated.currentHp <= 0f) {
            _enemies.removeAt(idx)
            // PATHRIFT-154: cycle-based kill gold scaling
            val cycle = waveSystem.cycleNumber(currentWave)
            val cycleScale = EconomyConstants.killGoldMultiplier(cycle)
            val scaledGold = maxOf(1, (updated.goldReward * cycleScale).toInt())
            gold += scaledGold
            score += scaledGold * 2L
            totalEnemiesKilled++
            waveEnemiesCleared++
            bridge.onEnemyKilled(updated.type, scaledGold)
            bridge.onGoldChanged(gold)
            bridge.onWaveProgress(waveEnemiesCleared, waveEnemyTotal)

            // F12: Splitter death — spawn 2 SWARM at same pathProgress
            if (updated.type == EnemyType.SPLITTER) {
                val hpMult = waveSystem.hpScaleMultiplier(currentWave)
                repeat(2) {
                    spawnEnemyAtProgress(EnemyType.SWARM, hpMult, updated.pathProgress)
                }
                // Increase wave total to account for spawned swarms
                waveEnemyTotal += 2
                bridge.onWaveProgress(waveEnemiesCleared, waveEnemyTotal)
            }
            // PATHRIFT-159: Healer death — no special behavior
            // PATHRIFT-159: Phantom death — no special behavior
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

            // F10: Diamond reward every 10 waves
            if (currentWave % 10 == 0) {
                val diamondReward = when {
                    currentWave <= 30 -> 2
                    currentWave <= 60 -> 3
                    else -> 4
                }
                diamondStore.earn(diamondReward)
                bridge.onDiamondsChanged(diamondStore.balance)
            }

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

        // Build new path + slots — density already set in initLayout
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

    // ---- Boss Abilities (PATHRIFT-155) ----

    private fun tickBossAbilities(nowSec: Double) {
        synchronized(_enemies) {
            for (i in _enemies.indices) {
                val e = _enemies[i]
                if (e.type != EnemyType.BOSS || !e.isAlive) continue
                val hpRatio = if (e.maxHp > 0f) e.currentHp / e.maxHp else 0f

                val (triggered, shellActive) = when (e.bossVariant) {
                    0 -> { // Rift Guardian — Rift Pulse once at 50% HP
                        if (!e.bossAbilityTriggered && hpRatio <= 0.5f) {
                            triggerRiftPulse(nowSec, duration = 4.0)
                            Pair(true, false)
                        } else Pair(e.bossAbilityTriggered, false)
                    }
                    1 -> { // Iron Colossus — Shell Mode
                        if (!e.bossAbilityTriggered && hpRatio <= 0.5f) {
                            // First trigger
                            val shell = (nowSec % 8.0) < 2.0
                            Pair(true, shell)
                        } else if (e.bossAbilityTriggered) {
                            val shell = (nowSec % 8.0) < 2.0
                            Pair(true, shell)
                        } else Pair(false, false)
                    }
                    2 -> { // Swarm Queen — Brood Burst once at 50% HP
                        if (!e.bossAbilityTriggered && hpRatio <= 0.5f) {
                            spawnBroodBurst(e.pathProgress, count = 6)
                            Pair(true, false)
                        } else Pair(e.bossAbilityTriggered, false)
                    }
                    3 -> { // Phase Runner — Overdrive once at 50% HP
                        if (!e.bossAbilityTriggered && hpRatio <= 0.5f) {
                            // Speed doubled — handled via currentSpeed override
                            val overdriveSpeed = e.baseSpeed * 2.0f
                            _enemies[i] = e.copy(currentSpeed = overdriveSpeed, bossAbilityTriggered = true)
                            scheduleSpeedReset(e.id, after = 5.0)
                            return@synchronized  // Updated in-place, skip end-of-loop copy
                        } else Pair(e.bossAbilityTriggered, false)
                    }
                    4 -> { // Void Titan — Gravity Well every 10s
                        val lastGravity = e.lastJumpTime / 1000.0
                        if (nowSec - lastGravity >= 10.0) {
                            triggerGravityWell(nowSec, duration = 2.0)
                            _enemies[i] = e.copy(lastJumpTime = (nowSec * 1000.0).toLong())
                            return@synchronized
                        } else Pair(e.bossAbilityTriggered, e.bossShellActive)
                    }
                    else -> Pair(e.bossAbilityTriggered, false)
                }

                if (triggered != e.bossAbilityTriggered || shellActive != e.bossShellActive) {
                    _enemies[i] = e.copy(bossAbilityTriggered = triggered, bossShellActive = shellActive)
                }
            }
        }
    }

    /** Rift Pulse: slow all tower attack rates to 40% for duration seconds. */
    fun triggerRiftPulse(nowSec: Double, duration: Double) {
        riftPulseEndTime = nowSec + duration
    }

    /** Brood Burst: spawn count SWARM enemies at given path progress. */
    fun spawnBroodBurst(pathProgress: Float, count: Int) {
        val hpMult = waveSystem.hpScaleMultiplier(currentWave)
        synchronized(_enemies) {
            repeat(count) {
                val id = System.nanoTime()
                _enemies.add(EnemyInstance(
                    id = id, type = EnemyType.SWARM,
                    maxHp = com.pathrift.anonve.android.game.enemies.SwarmEnemy.HP * hpMult,
                    currentHp = com.pathrift.anonve.android.game.enemies.SwarmEnemy.HP * hpMult,
                    baseSpeed = com.pathrift.anonve.android.game.enemies.SwarmEnemy.SPEED,
                    currentSpeed = com.pathrift.anonve.android.game.enemies.SwarmEnemy.SPEED,
                    goldReward = com.pathrift.anonve.android.game.enemies.SwarmEnemy.GOLD_REWARD,
                    armorReduction = com.pathrift.anonve.android.game.enemies.SwarmEnemy.ARMOR_REDUCTION,
                    pathProgress = pathProgress
                ))
            }
            waveEnemyTotal += count
            bridge.onWaveProgress(waveEnemiesCleared, waveEnemyTotal)
        }
    }

    /** Schedule speed reset for Phase Runner after overdrive expires. */
    private fun scheduleSpeedReset(enemyId: Long, after: Double) {
        gameScope?.launch(Dispatchers.Default) {
            delay((after * 1000).toLong())
            synchronized(_enemies) {
                val idx = _enemies.indexOfFirst { it.id == enemyId }
                if (idx >= 0) {
                    val e = _enemies[idx]
                    _enemies[idx] = e.copy(currentSpeed = e.baseSpeed)
                }
            }
        }
    }

    /** Gravity Well: disable all towers and clear in-flight conceptual projectiles for duration. */
    fun triggerGravityWell(nowSec: Double, duration: Double) {
        gravityWellEndTime = nowSec + duration
        // In the engine we have no projectile list — tower attacks simply miss during this window.
        // Tower disable is handled in applyTowerAttacks via gravityWellEndTime check.
    }

    // ---- Healer Aura Tick (PATHRIFT-159) ----

    private fun tickHealerAuras(now: Long) {
        synchronized(_enemies) {
            val healers = _enemies.filter { it.type == EnemyType.HEALER && it.isAlive }
            if (healers.isEmpty()) return

            for (i in _enemies.indices) {
                val healer = _enemies[i]
                if (healer.type != EnemyType.HEALER || !healer.isAlive) continue
                if (now - healer.lastHealTime < HealerEnemy.HEAL_INTERVAL_MS) continue

                // Heal enemies within radius (progress-based)
                for (j in _enemies.indices) {
                    val target = _enemies[j]
                    if (!target.isAlive) continue
                    if (target.id == healer.id) continue
                    if (kotlin.math.abs(target.pathProgress - healer.pathProgress) > HealerEnemy.HEAL_RADIUS_PROGRESS) continue
                    val healed = minOf(target.maxHp, target.currentHp + HealerEnemy.HEAL_AMOUNT)
                    if (healed > target.currentHp) {
                        _enemies[j] = target.copy(currentHp = healed)
                    }
                }
                _enemies[i] = healer.copy(lastHealTime = now)
            }
        }
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
            EnemyType.SPLITTER -> EnemyInstance(
                id = id, type = type,
                maxHp = SplitterEnemy.HP * hpMult, currentHp = SplitterEnemy.HP * hpMult,
                baseSpeed = SplitterEnemy.SPEED, currentSpeed = SplitterEnemy.SPEED,
                goldReward = SplitterEnemy.GOLD_REWARD,
                armorReduction = SplitterEnemy.ARMOR_REDUCTION
            )
            EnemyType.JUMPER -> EnemyInstance(
                id = id, type = type,
                maxHp = JumperEnemy.HP * hpMult, currentHp = JumperEnemy.HP * hpMult,
                baseSpeed = JumperEnemy.SPEED, currentSpeed = JumperEnemy.SPEED,
                goldReward = JumperEnemy.GOLD_REWARD,
                armorReduction = JumperEnemy.ARMOR_REDUCTION
            )
            EnemyType.HEALER -> EnemyInstance(
                id = id, type = type,
                maxHp = HealerEnemy.HP * hpMult, currentHp = HealerEnemy.HP * hpMult,
                baseSpeed = HealerEnemy.SPEED, currentSpeed = HealerEnemy.SPEED,
                goldReward = HealerEnemy.GOLD_REWARD,
                armorReduction = HealerEnemy.ARMOR_REDUCTION
            )
            EnemyType.PHANTOM -> EnemyInstance(
                id = id, type = type,
                maxHp = PhantomEnemy.HP * hpMult, currentHp = PhantomEnemy.HP * hpMult,
                baseSpeed = PhantomEnemy.SPEED, currentSpeed = PhantomEnemy.SPEED,
                goldReward = PhantomEnemy.GOLD_REWARD,
                armorReduction = PhantomEnemy.ARMOR_REDUCTION
            )
        }
        synchronized(_enemies) { _enemies.add(instance) }
    }

    /** Spawn an enemy at a specific path progress (used for Splitter death spawns). */
    private fun spawnEnemyAtProgress(type: EnemyType, hpMult: Float, pathProgress: Float) {
        val id = System.nanoTime()
        val instance = when (type) {
            EnemyType.SWARM -> EnemyInstance(
                id = id, type = type,
                maxHp = SwarmEnemy.HP * hpMult, currentHp = SwarmEnemy.HP * hpMult,
                baseSpeed = SwarmEnemy.SPEED, currentSpeed = SwarmEnemy.SPEED,
                goldReward = SwarmEnemy.GOLD_REWARD,
                armorReduction = SwarmEnemy.ARMOR_REDUCTION,
                pathProgress = pathProgress
            )
            else -> return // Only SWARM supported for Splitter splits
        }
        _enemies.add(instance)
    }

    fun setSpeed(mult: Float) {
        speedMultiplier = mult
        bridge.onSpeedChanged(mult)
    }

    fun acceptRevive() {
        hasUsedRevive = true
        lives = 1
        bridge.onLifeRestored(lives)
    }

    fun declineRevive() {
        triggerGameOver()
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
    val lastAttackTime: Long = 0L,
    val facingAngle: Float = 0f  // radians toward current target; updated each attack
)
