package com.pathrift.anonve.android.game

import com.pathrift.anonve.android.core.engine.EconomyConstants
import com.pathrift.anonve.android.game.enemies.EnemyInstance
import com.pathrift.anonve.android.game.enemies.EnemyType
import com.pathrift.anonve.android.game.enemies.RunnerEnemy
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

/**
 * Core game simulation engine for Pathrift.
 *
 * Manages:
 * - Grid state and tower placement
 * - Wave spawning and progression
 * - Enemy AI movement along path
 * - Tower targeting and damage
 * - Economy (gold) tracking
 * - Life management
 * - Score accumulation
 * - Communication back to ViewModel via GameBridge
 */
class GameEngine(private val bridge: GameBridge) {

    val grid = GridSystem()
    private val waveSystem = WaveSystem()

    private val _towers = mutableMapOf<TileCoordinate, Tower>()
    val towers: Map<TileCoordinate, Tower> get() = _towers.toMap()

    private val _enemies = mutableListOf<EnemyInstance>()
    val enemies: List<EnemyInstance> get() = _enemies.toList()

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
    var goldSpent: Int = 0
        private set

    private var gameScope: CoroutineScope? = null
    private var waveJob: Job? = null
    private var simulationJob: Job? = null

    fun start(scope: CoroutineScope) {
        gameScope = scope
        startSimulationLoop(scope)
    }

    fun startNextWave() {
        currentWave++
        val waveDef = waveSystem.getWaveDefinition(currentWave)
        bridge.onWaveStarted(currentWave)

        waveJob = gameScope?.launch(Dispatchers.Default) {
            for (group in waveDef.spawnGroups) {
                repeat(group.count) {
                    if (!isActive) return@launch
                    spawnEnemy(group.type)
                    delay(group.spawnIntervalMs)
                }
                delay(1000L)
            }
        }
    }

    fun placeTower(coord: TileCoordinate, type: TowerType): Boolean {
        val tower = when (type) {
            TowerType.BOLT -> BoltTower()
            TowerType.BLAST -> BlastTower()
            TowerType.FROST -> FrostTower()
        }
        if (gold < tower.cost) return false
        if (!grid.placeTower(coord)) return false

        gold -= tower.cost
        goldSpent += tower.cost
        _towers[coord] = tower
        return true
    }

    fun stop() {
        waveJob?.cancel()
        simulationJob?.cancel()
        gameScope = null
    }

    fun reset() {
        waveJob?.cancel()
        simulationJob?.cancel()
        _towers.clear()
        _enemies.clear()
        grid.reset()
        currentWave = 0
        lives = EconomyConstants.STARTING_LIVES
        gold = EconomyConstants.STARTING_GOLD
        score = 0L
        totalEnemiesKilled = 0
        goldSpent = 0
    }

    private fun spawnEnemy(type: EnemyType) {
        val instance = when (type) {
            EnemyType.RUNNER -> EnemyInstance(
                id = System.nanoTime(),
                type = EnemyType.RUNNER,
                maxHp = RunnerEnemy.HP,
                currentHp = RunnerEnemy.HP,
                speed = RunnerEnemy.SPEED,
                goldReward = RunnerEnemy.GOLD_REWARD,
                pathNodeIndex = 0,
                pathProgress = 0f,
                pathRow = GridSystem.PATH_NODES.first().row.toFloat()
            )
            EnemyType.TANK -> EnemyInstance(
                id = System.nanoTime(),
                type = EnemyType.TANK,
                maxHp = TankEnemy.HP,
                currentHp = TankEnemy.HP,
                speed = TankEnemy.SPEED,
                goldReward = TankEnemy.GOLD_REWARD,
                armorReduction = TankEnemy.ARMOR_REDUCTION,
                pathNodeIndex = 0,
                pathProgress = 0f,
                pathRow = GridSystem.PATH_NODES.first().row.toFloat()
            )
        }
        synchronized(_enemies) { _enemies.add(instance) }
    }

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
        val pathNodes = GridSystem.PATH_NODES
        val toRemove = mutableListOf<EnemyInstance>()

        synchronized(_enemies) {
            _enemies.replaceAll { enemy ->
                if (enemy.pathNodeIndex >= pathNodes.size) {
                    return@replaceAll enemy.copy(pathNodeIndex = pathNodes.size)
                }

                val targetNode = pathNodes[enemy.pathNodeIndex]
                val targetX = targetNode.col.toFloat()
                val targetY = targetNode.row.toFloat()

                val dx = targetX - enemy.pathProgress
                val dy = targetY - enemy.pathRow
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val moveAmount = enemy.speed * delta / GridSystem.TILE_SIZE_DP

                if (dist <= moveAmount) {
                    val nextIndex = enemy.pathNodeIndex + 1
                    if (nextIndex >= pathNodes.size) {
                        toRemove.add(enemy)
                        val newLives = (lives - 1).coerceAtLeast(0)
                        lives = newLives
                        bridge.onLifeLost(newLives)
                        if (newLives <= 0) {
                            triggerRunEnd()
                        }
                        enemy
                    } else {
                        enemy.copy(
                            pathNodeIndex = nextIndex,
                            pathProgress = targetX,
                            pathRow = targetY
                        )
                    }
                } else {
                    val step = moveAmount / dist
                    enemy.copy(
                        pathProgress = enemy.pathProgress + dx * step,
                        pathRow = enemy.pathRow + dy * step
                    )
                }
            }

            _enemies.removeAll(toRemove.toSet())
        }

        applyTowerAttacks(delta)
        checkWaveCompletion()
    }

    private fun applyTowerAttacks(delta: Float) {
        synchronized(_enemies) {
            _towers.forEach { (coord, tower) ->
                val towerX = coord.col.toFloat()
                val towerY = coord.row.toFloat()
                val range = tower.rangeTiles

                val inRange = _enemies.filter { enemy ->
                    val dx = enemy.pathProgress - towerX
                    val dy = enemy.pathRow - towerY
                    Math.sqrt((dx * dx + dy * dy).toDouble()) <= range
                }

                if (inRange.isEmpty()) return@forEach

                val dps = tower.damagePerHit * tower.attacksPerSecond
                val damageTick = dps * delta

                when (tower) {
                    is BlastTower -> {
                        inRange.forEach { target ->
                            applyDamage(target, damageTick, tower)
                        }
                    }
                    is FrostTower -> {
                        val target = inRange.firstOrNull() ?: return@forEach
                        applyDamage(target, damageTick, tower)
                    }
                    else -> {
                        val target = inRange.firstOrNull() ?: return@forEach
                        applyDamage(target, damageTick, tower)
                    }
                }
            }
        }
    }

    private fun applyDamage(target: EnemyInstance, damage: Float, tower: Tower) {
        val idx = _enemies.indexOfFirst { it.id == target.id }
        if (idx < 0) return

        val actualDamage = damage * (1f - target.armorReduction)
        val newHp = _enemies[idx].currentHp - actualDamage.toInt().coerceAtLeast(1)

        if (newHp <= 0) {
            val killed = _enemies[idx]
            _enemies.removeAt(idx)
            gold += killed.goldReward
            score += killed.goldReward * 2L
            totalEnemiesKilled++
            bridge.onEnemyKilled(killed.type, killed.goldReward)
        } else {
            _enemies[idx] = _enemies[idx].copy(currentHp = newHp)
        }
    }

    private fun checkWaveCompletion() {
        if (currentWave == 0) return
        val waveActive = waveJob?.isActive ?: false
        if (!waveActive && _enemies.isEmpty()) {
            val reward = waveSystem.calculateGoldReward(currentWave)
            gold += reward
            score += reward.toLong() * 5
            bridge.onWaveCompleted(currentWave, reward)
        }
    }

    private fun triggerRunEnd() {
        waveJob?.cancel()
        bridge.onRunEnded(currentWave, score)
    }
}
