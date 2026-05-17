package com.pathrift.anonve.android.game

import android.content.res.Resources
import android.graphics.PointF
import com.pathrift.anonve.android.core.engine.EconomyConstants
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
import java.util.Collections
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * GameEngine — full iOS GameScene.swift parity + Phase 2 features.
 *
 * Build 15: Free-form tower placement (DEC-032).
 * - Slot system removed. GridSystem is now a placed-tower tracker only.
 * - isValidPlacement() checks path clearance + tower overlap + screen bounds.
 * - placeTowerFreeform() / moveTowerFreeform() replace slot-based placement.
 * - applyRestore() uses xFrac/yFrac instead of slotId.
 */
class GameEngine(
    private val bridge: GameBridge,
    val diamondStore: DiamondStore,
    private val arsenalStore: ArsenalStore,
    private val premiumStore: PremiumStore
) {

    val grid = GridSystem()
    val waveSystem = WaveSystem()   // internal — exposed for ViewModel preview (PATHRIFT-157)

    // Active tower instances keyed by towerId (= grid PlacedRecord.towerId)
    private val _towers = mutableMapOf<Int, TowerInstance>()
    val towers: Map<Int, TowerInstance> get() = _towers.toMap()

    private val _enemies = mutableListOf<EnemyInstance>()
    val enemies: List<EnemyInstance> get() = synchronized(_enemies) { _enemies.toList() }

    // In-flight projectiles — damage deferred until arrival (PATHRIFT-B7-F001)
    private val _projectiles = Collections.synchronizedList(mutableListOf<InFlightProjectile>())
    val projectiles: List<InFlightProjectile> get() = _projectiles.toList()

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
    private var interWaveCountdownJob: Job? = null
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

    // ---- Free-form placement constants (Build 15 / DEC-032) ----

    private val PATH_CLEARANCE_PX = 38f   // dp units, multiplied by density below
    private val TOWER_CLEARANCE_PX = 22f  // dp units

    /**
     * Returns true if the given screen position is a valid tower placement location.
     * Checks: screen bounds, path clearance, tower overlap.
     * @param excludeTowerId When moving a tower, exclude it from the overlap check.
     */
    fun isValidPlacement(x: Float, y: Float, excludeTowerId: Int? = null): Boolean {
        val density = Resources.getSystem().displayMetrics.density
        val hudTop = PathSystem.hudTopInset
        val hudBot = PathSystem.hudBottomInset
        val margin = 15f * density
        if (x < margin || x > screenWidth - margin) return false
        if (y < hudTop + margin || y > screenHeight - hudBot - margin) return false
        val pathOk = PathSystem.minDistanceToPath(x, y) > PATH_CLEARANCE_PX * density
        val towerOk = grid.minDistanceToTower(x, y, excludeId = excludeTowerId) > TOWER_CLEARANCE_PX * density
        return pathOk && towerOk
    }

    // ---- Lifecycle ----

    fun start(scope: CoroutineScope) {
        gameScope = scope
        startSimulationLoop(scope)
    }

    fun stop() {
        waveJob?.cancel()
        simulationJob?.cancel()
        interWaveCountdownJob?.cancel()
        gameScope = null
    }

    fun reset() {
        waveJob?.cancel()
        simulationJob?.cancel()
        interWaveCountdownJob?.cancel()
        synchronized(_enemies) { _enemies.clear() }
        _projectiles.clear()
        _towers.clear()
        grid.clear()
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
        // Build 15: no slot positions to update — grid is a tracker only
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

        // Build 15: restore using xFrac/yFrac position fractions (version 2 saves)
        for (t in save.towers) {
            val type = TowerType.values().firstOrNull { it.name == t.type } ?: continue
            val x = t.xFrac.toFloat() * screenWidth
            val y = t.yFrac.toFloat() * screenHeight
            val tower = buildTower(type)
            val pos = PointF(x, y)
            val towerId = grid.addTower(type, pos)
            val permDmgBonus = arsenalStore.permDamageBonus(type)
            _towers[towerId] = TowerInstance(
                slotId = towerId, tower = tower, position = pos,
                level = t.level, totalInvested = t.totalInvested,
                lastAttackTime = 0L, facingAngle = 0f
            )
        }
        bridge.onGoldChanged(gold)
        bridge.onStateRestored(currentWave, lives, gold, totalEnemiesKilled)
    }

    // ---- Wave Management ----

    fun startNextWave() {
        cancelInterWaveCountdown()
        startWave()
    }

    /** Internal wave start — call after cancelling countdown. */
    private fun startWave() {
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

    // ---- Inter-wave Countdown (PATHRIFT-B7-002) ----

    private fun startInterWaveCountdown() {
        interWaveCountdownJob?.cancel()
        interWaveCountdownJob = gameScope?.launch(Dispatchers.Default) {
            for (i in 20 downTo 1) {
                if (!isActive) return@launch
                bridge.onInterWaveTimerChanged(i)
                delay((1000L / speedMultiplier).toLong())
            }
            bridge.onInterWaveTimerChanged(0)
            // Auto-start next wave
            if (isActive) startWave()
        }
    }

    fun cancelInterWaveCountdown() {
        interWaveCountdownJob?.cancel()
        interWaveCountdownJob = null
        bridge.onInterWaveTimerChanged(0)
    }

    // ---- Tower Placement (Build 15: free-form) ----

    /** Build 15: place a tower at exact screen coordinates. Returns towerId or null on failure. */
    fun placeTowerFreeform(type: TowerType, x: Float, y: Float): Int? {
        if (!isValidPlacement(x, y)) return null
        val cost = towerCostFor(type)
        if (gold < cost) return null
        if (grid.count >= activeSlotCount(currentWave)) return null

        val position = PointF(x, y)
        val tower = buildTower(type)
        val towerId = grid.addTower(type, position)
        _towers[towerId] = TowerInstance(
            slotId = towerId,
            tower = tower,
            position = position,
            level = 1,
            totalInvested = cost,
            lastAttackTime = 0L,
            facingAngle = 0f
        )
        gold -= cost
        bridge.onGoldChanged(gold)
        return towerId
    }

    /** Build 15: move an existing tower to new free-form coordinates. */
    fun moveTowerFreeform(towerId: Int, toX: Float, toY: Float, goldCost: Int): Boolean {
        val inst = _towers[towerId] ?: return false
        if (!isValidPlacement(toX, toY, excludeTowerId = towerId)) return false
        if (gold < goldCost) return false
        val newPos = PointF(toX, toY)
        grid.moveTower(towerId, newPos)
        _towers[towerId] = inst.copy(position = newPos)
        gold -= goldCost
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
        bridge.onGoldChanged(gold)
    }

    fun towerInstance(slotId: Int): TowerInstance? = _towers[slotId]

    /** Build 16: FIX 2 — returns the screen position of a placed tower by id. */
    fun towerScreenPosition(towerId: Int): android.graphics.PointF? {
        return _towers[towerId]?.position
    }

    // ---- Active Tower Count (DEC-032 — wave-based maximum) ----

    fun activeSlotCount(wave: Int): Int = waveSystem.activeSlotCount(wave)

    // ---- Tower cost lookup ----

    private fun towerCostFor(type: TowerType): Int = when (type) {
        TowerType.BOLT      -> 80
        TowerType.BLAST     -> 100
        TowerType.FROST     -> 100
        TowerType.PIERCE    -> 140
        TowerType.CORE      -> 170
        TowerType.SNIPER    -> 190
        TowerType.ARTILLERY -> 150
        TowerType.INFERNO   -> 210
        TowerType.TESLA     -> 300
        TowerType.NOVA      -> 500
    }

    // ---- Tower factory ----

    fun buildTower(type: TowerType): Tower = when (type) {
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
        updateProjectiles(adjustedDelta)
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
                    // Layer filter — BRIDGE_ONLY falls back to ALL_LAYERS when no bridges exist
                    when (tower.type.targetingMode) {
                        TargetingMode.ALL_LAYERS  -> true
                        TargetingMode.GROUND_ONLY -> e.pathLayer == PathLayer.GROUND
                        TargetingMode.BRIDGE_ONLY -> {
                            if (PathSystem.bridgeSegmentCount == 0) true  // no bridges → target all
                            else e.pathLayer == PathLayer.BRIDGE
                        }
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
                            val ePos = PathSystem.positionAt(e.pathProgress)
                            spawnProjectile(
                                from = inst.position, to = ePos,
                                towerType = tower.type, targetId = e.id,
                                rawDamage = finalDamage, bypassShield = true
                            )
                        }
                    }

                    // F8: Tesla — chain lightning hits primary + 2 nearest (AoE chain = no dodge for chain targets)
                    tower.type == TowerType.TESLA -> {
                        val primary = inRange.maxByOrNull { it.pathProgress } ?: continue
                        val primaryPos = PathSystem.positionAt(primary.pathProgress)
                        targetFacingAngle = atan2(primaryPos.y - inst.position.y, primaryPos.x - inst.position.x)
                        val typeMult = tower.type.damageMultiplier(primary.type)
                        val primaryDamage = (damage * typeMult).toInt()

                        // Chain to 2 nearest other enemies within range of primary
                        val nearbyChain = inRange
                            .filter { it.id != primary.id && it.isAlive }
                            .sortedBy { e ->
                                val ePos = PathSystem.positionAt(e.pathProgress)
                                val dx = ePos.x - primaryPos.x
                                val dy = ePos.y - primaryPos.y
                                dx * dx + dy * dy
                            }
                            .take(2)
                        val chainDamage = (18f * levelMult).toInt()
                        val chainPairs = nearbyChain.map { chainTarget ->
                            val chainMult = tower.type.damageMultiplier(chainTarget.type)
                            Pair(chainTarget.id, (chainDamage * chainMult).toInt())
                        }
                        spawnProjectile(
                            from = inst.position, to = primaryPos,
                            towerType = tower.type, targetId = primary.id,
                            rawDamage = primaryDamage, isAoe = true,
                            chainTargets = chainPairs
                        )
                    }

                    // AoE towers (Blast, Nova): damage all in splash radius — always bypass Phantom dodge
                    tower.aoeRadius > 0f -> {
                        val primary = inRange.maxByOrNull { it.pathProgress } ?: continue
                        val primaryPos = PathSystem.positionAt(primary.pathProgress)
                        targetFacingAngle = atan2(primaryPos.y - inst.position.y, primaryPos.x - inst.position.x)
                        spawnProjectile(
                            from = inst.position, to = primaryPos,
                            towerType = tower.type, targetId = primary.id,
                            rawDamage = (damage * tower.type.damageMultiplier(primary.type)).toInt(),
                            isAoe = true, aoeRadius = tower.aoeRadius
                        )
                    }

                    // Frost: single target + slow
                    tower.slowFactor < 1f -> {
                        val target = inRange.maxByOrNull { it.pathProgress } ?: continue
                        val tPos = PathSystem.positionAt(target.pathProgress)
                        targetFacingAngle = atan2(tPos.y - inst.position.y, tPos.x - inst.position.x)
                        val typeMult = tower.type.damageMultiplier(target.type)
                        val finalDamage = (damage * typeMult).toInt()
                        spawnProjectile(
                            from = inst.position, to = tPos,
                            towerType = tower.type, targetId = target.id,
                            rawDamage = finalDamage
                        )
                        // Apply slow immediately on fire (visual + gameplay parity with iOS)
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
                        spawnProjectile(
                            from = inst.position, to = tPos,
                            towerType = tower.type, targetId = target.id,
                            rawDamage = finalDamage, penetration = penetration
                        )
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

    // ---- In-flight Projectile System (PATHRIFT-B7-F001) ----

    private fun spawnProjectile(
        from: android.graphics.PointF,
        to: android.graphics.PointF,
        towerType: TowerType,
        targetId: Long,
        rawDamage: Int,
        bypassShield: Boolean = false,
        penetration: Float = 0f,
        isAoe: Boolean = false,
        aoeRadius: Float = 0f,
        chainTargets: List<Pair<Long, Int>> = emptyList()
    ) {
        _projectiles.add(InFlightProjectile(
            fromPos = android.graphics.PointF(from.x, from.y),
            toPos = android.graphics.PointF(to.x, to.y),
            towerType = towerType,
            targetEnemyId = targetId,
            rawDamage = rawDamage,
            bypassShield = bypassShield,
            penetration = penetration,
            isAoe = isAoe,
            aoeRadius = aoeRadius,
            chainTargets = chainTargets
        ))
    }

    private fun updateProjectiles(delta: Float) {
        val SPEED = 700f // px/sec
        val ARRIVAL_THRESHOLD = 12f
        val toRemove = mutableListOf<InFlightProjectile>()

        // Snapshot to iterate (list is synchronized, but we modify during iteration via index)
        val snapshot = _projectiles.toList()

        for (proj in snapshot) {
            val dx = proj.toPos.x - proj.fromPos.x
            val dy = proj.toPos.y - proj.fromPos.y
            val dist = sqrt(dx * dx + dy * dy)
            val traveled = SPEED * delta

            if (traveled >= dist - ARRIVAL_THRESHOLD || dist < ARRIVAL_THRESHOLD) {
                // Arrived — apply damage
                if (proj.isAoe && proj.aoeRadius > 0f) {
                    // AoE: damage all enemies in radius around toPos
                    val density = Resources.getSystem().displayMetrics.density
                    val aoePixels = proj.aoeRadius * GridSystem.TILE_SIZE_DP * density
                    synchronized(_enemies) {
                        for (e in _enemies.filter { it.isAlive }) {
                            val ePos = PathSystem.positionAt(e.pathProgress)
                            val edx = ePos.x - proj.toPos.x
                            val edy = ePos.y - proj.toPos.y
                            if (sqrt(edx * edx + edy * edy) <= aoePixels) {
                                val typeMult = proj.towerType.damageMultiplier(e.type)
                                applyDamage(e.id, (proj.rawDamage * typeMult).toInt(), isAoe = true)
                            }
                        }
                    }
                } else {
                    // Single target
                    val enemy = synchronized(_enemies) { _enemies.firstOrNull { it.id == proj.targetEnemyId && it.isAlive } }
                    if (enemy != null) {
                        applyDamage(proj.targetEnemyId, proj.rawDamage, bypassShield = proj.bypassShield, penetration = proj.penetration)
                    }
                }
                // Chain targets (Tesla) — apply on primary arrival
                for ((chainId, chainDmg) in proj.chainTargets) {
                    val chainEnemy = synchronized(_enemies) { _enemies.firstOrNull { it.id == chainId && it.isAlive } }
                    if (chainEnemy != null) applyDamage(chainId, chainDmg, isAoe = true)
                }
                toRemove.add(proj)
            } else {
                // Advance projectile toward target
                val ratio = traveled / dist
                val newFromX = proj.fromPos.x + dx * ratio
                val newFromY = proj.fromPos.y + dy * ratio
                val idx = _projectiles.indexOf(proj)
                if (idx >= 0) {
                    _projectiles[idx] = proj.copy(fromPos = android.graphics.PointF(newFromX, newFromY))
                }
            }
        }
        _projectiles.removeAll(toRemove)
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
            startInterWaveCountdown()

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

    // ---- Rift Shift (Build 15: free-form survivor placement) ----

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

        // Build new path — density already set in initLayout
        PathSystem.buildLayout(screenWidth, screenHeight, currentWave, newIndex)

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

        // Clear all tower-grid assignments
        _towers.clear()
        grid.clear()

        // Re-place survivors at their existing positions (positions stay valid — layout only changes path)
        for (snap in survivors) {
            val pos = snap.position
            val towerId = grid.addTower(snap.tower.type, pos)
            _towers[towerId] = snap.copy(slotId = towerId, position = pos)
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
                maxHp = com.pathrift.anonve.android.game.enemies.PhantomEnemy.HP * hpMult,
                currentHp = com.pathrift.anonve.android.game.enemies.PhantomEnemy.HP * hpMult,
                baseSpeed = com.pathrift.anonve.android.game.enemies.PhantomEnemy.SPEED,
                currentSpeed = com.pathrift.anonve.android.game.enemies.PhantomEnemy.SPEED,
                goldReward = com.pathrift.anonve.android.game.enemies.PhantomEnemy.GOLD_REWARD,
                armorReduction = com.pathrift.anonve.android.game.enemies.PhantomEnemy.ARMOR_REDUCTION
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
 * slotId is now the unique tower ID from GridSystem (Build 15: free-form placement).
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
