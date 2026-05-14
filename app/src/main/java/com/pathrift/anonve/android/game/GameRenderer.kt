package com.pathrift.anonve.android.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.pathrift.anonve.android.game.enemies.EnemyInstance
import com.pathrift.anonve.android.game.enemies.EnemyType
import com.pathrift.anonve.android.game.towers.TowerType
import com.pathrift.anonve.android.game.PathLayer
import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * GameRenderer — Canvas-based SurfaceView renderer. iOS parity for all 6 enemy types,
 * level badges, range circles, boss variants, Rift Shift flash, and path rendering.
 */
class GameRenderer(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    // ---- Game state (updated externally) ----
    var enemies: List<EnemyInstance> = emptyList()
    var towerInstances: Map<Int, TowerInstance> = emptyMap()
    var slotPositions: List<PointF> = emptyList()
    var slotOccupied: Map<Int, Boolean> = emptyMap()
    var selectedSlotId: Int? = null
    var riftShiftActive: Boolean = false

    // ---- Paints ----
    private val backgroundPaint = Paint().apply { color = Color.parseColor("#0A0A0F") }

    // Grid tiles (checkerboard)
    private val gridPaint1 = Paint().apply { color = Color.parseColor("#11111A"); style = Paint.Style.FILL }
    private val gridPaint2 = Paint().apply { color = Color.parseColor("#16161F"); style = Paint.Style.FILL }
    private val gridBorderPaint = Paint().apply {
        color = Color.parseColor("#262633")
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
        alpha = 76
    }

    // Path
    private val pathFillPaint = Paint().apply {
        color = Color.parseColor("#473820")
        style = Paint.Style.FILL
    }
    private val pathStrokePaint = Paint().apply {
        color = Color.parseColor("#7F6128")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        alpha = 178
    }

    // Tower slot
    private val slotEmptyPaint = Paint().apply {
        color = Color.parseColor("#0D2640")
        style = Paint.Style.FILL
        alpha = 216
    }
    private val slotBorderPaint = Paint().apply {
        color = Color.parseColor("#00C7FF")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        alpha = 153
    }
    private val slotCrossPaint = Paint().apply {
        color = Color.parseColor("#00C7FF")
        style = Paint.Style.FILL
        alpha = 178
    }

    // Tower colors
    private val towerBoltPaint      = Paint().apply { color = Color.parseColor("#00C8FF"); style = Paint.Style.FILL }
    private val towerBlastPaint     = Paint().apply { color = Color.parseColor("#FF6B00"); style = Paint.Style.FILL }
    private val towerFrostPaint     = Paint().apply { color = Color.parseColor("#8B4FFF"); style = Paint.Style.FILL }
    private val towerPiercePaint    = Paint().apply { color = Color.parseColor("#CCFF00"); style = Paint.Style.FILL }
    private val towerCorePaint      = Paint().apply { color = Color.parseColor("#FF4400"); style = Paint.Style.FILL }
    private val towerInfernoPaint   = Paint().apply { color = Color.parseColor("#FF2200"); style = Paint.Style.FILL }
    private val towerTeslaPaint     = Paint().apply { color = Color.parseColor("#00AAFF"); style = Paint.Style.FILL }
    private val towerNovaPaint      = Paint().apply { color = Color.parseColor("#FFD700"); style = Paint.Style.FILL }
    private val towerSniperPaint    = Paint().apply { color = Color.parseColor("#66FFFF"); style = Paint.Style.FILL }
    private val towerArtilleryPaint = Paint().apply { color = Color.parseColor("#CC8800"); style = Paint.Style.FILL }

    // Tower range ring — semi-transparent blue halo
    private val towerRangePaint = Paint().apply {
        color = Color.parseColor("#00C7FF")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 60
    }
    private val towerRangeFillPaint = Paint().apply {
        color = Color.parseColor("#00C7FF")
        style = Paint.Style.FILL
        alpha = 15
    }

    // Level badge
    private val badgeBgPaint = Paint().apply {
        color = Color.parseColor("#0D0D26")
        style = Paint.Style.FILL
        alpha = 242
    }
    private val badgeBorderPaint = Paint().apply {
        color = Color.parseColor("#00C8FF")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val badgeTextPaint = Paint().apply {
        color = Color.parseColor("#00C8FF")
        textSize = 18f
        isAntiAlias = true
        isFakeBoldText = true
    }

    // Enemy paints
    private val enemyRunnerPaint = Paint().apply { color = Color.parseColor("#FF2D55"); style = Paint.Style.FILL }
    private val enemyTankPaint = Paint().apply { color = Color.parseColor("#FF6B00"); style = Paint.Style.FILL }
    private val enemyShieldBodyPaint = Paint().apply { color = Color.parseColor("#3380E6"); style = Paint.Style.FILL }
    private val enemyShieldRingPaint = Paint().apply {
        color = Color.parseColor("#7FCCFF")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        alpha = 229
    }
    private val enemySwarmPaint = Paint().apply { color = Color.parseColor("#FFBF00"); style = Paint.Style.FILL }
    private val enemyGhostPaint = Paint().apply {
        color = Color.parseColor("#B3E6B3")
        style = Paint.Style.FILL
        alpha = 191   // 0.75 alpha
    }
    private val enemySplitterPaint = Paint().apply { color = Color.parseColor("#FF66FF"); style = Paint.Style.FILL }
    private val enemyJumperPaint = Paint().apply { color = Color.parseColor("#00FF99"); style = Paint.Style.FILL }
    private val enemyBossVariantPaints = listOf(
        Paint().apply { color = Color.parseColor("#6633FF"); style = Paint.Style.FILL },  // 0 Rift Guardian — purple
        Paint().apply { color = Color.parseColor("#666678"); style = Paint.Style.FILL },  // 1 Iron Colossus — grey
        Paint().apply { color = Color.parseColor("#CC4D00"); style = Paint.Style.FILL },  // 2 Swarm Queen — dark orange
        Paint().apply { color = Color.parseColor("#0099CC"); style = Paint.Style.FILL },  // 3 Phase Runner — cyan
        Paint().apply { color = Color.parseColor("#330066"); style = Paint.Style.FILL },  // 4 Void Titan — dark purple
    )
    private val bossStrokePaints = listOf(
        Paint().apply { color = Color.parseColor("#9966FF"); style = Paint.Style.STROKE; strokeWidth = 3f },
        Paint().apply { color = Color.parseColor("#B3B3CC"); style = Paint.Style.STROKE; strokeWidth = 3f },
        Paint().apply { color = Color.parseColor("#FF9933"); style = Paint.Style.STROKE; strokeWidth = 3f },
        Paint().apply { color = Color.parseColor("#00CCFF"); style = Paint.Style.STROKE; strokeWidth = 2f },
        Paint().apply { color = Color.parseColor("#7F00CC"); style = Paint.Style.STROKE; strokeWidth = 4f },
    )

    // Health bar
    private val hpBgPaint = Paint().apply { color = Color.parseColor("#333333"); style = Paint.Style.FILL }
    private val hpGreenPaint = Paint().apply { color = Color.parseColor("#30D158"); style = Paint.Style.FILL }
    private val hpYellowPaint = Paint().apply { color = Color.parseColor("#FFD60A"); style = Paint.Style.FILL }
    private val hpRedPaint = Paint().apply { color = Color.parseColor("#FF2D55"); style = Paint.Style.FILL }

    // Rift Shift overlay
    private val riftFlashPaint = Paint().apply {
        color = Color.parseColor("#8C4FFF")
        style = Paint.Style.FILL
        alpha = 38
    }

    private val indicatorTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 20f
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private var renderThread: RenderThread? = null
    private var isRunning = false

    init {
        holder.addCallback(this)
        setZOrderOnTop(false)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        renderThread = RenderThread(holder).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        renderThread?.join()
        renderThread = null
    }

    // ---- Drawing ----

    private fun drawFrame(canvas: Canvas) {
        val W = canvas.width.toFloat()
        val H = canvas.height.toFloat()

        canvas.drawRect(0f, 0f, W, H, backgroundPaint)

        drawCheckerGrid(canvas, W, H)
        drawPath(canvas, W, H)
        drawTowerSlots(canvas)
        drawTowers(canvas)
        drawEnemies(canvas)

        if (riftShiftActive) {
            canvas.drawRect(0f, 0f, W, H, riftFlashPaint)
        }
    }

    // Checkerboard grid background
    private fun drawCheckerGrid(canvas: Canvas, W: Float, H: Float) {
        val cols = GridSystem.COLS
        val rows = GridSystem.ROWS
        val tileW = W / cols
        val tileH = H / rows
        for (col in 0 until cols) {
            for (row in 0 until rows) {
                val left = col * tileW
                val top = row * tileH
                val paint = if ((col + row) % 2 == 0) gridPaint1 else gridPaint2
                canvas.drawRect(left, top, left + tileW, top + tileH, paint)
                canvas.drawRect(left, top, left + tileW, top + tileH, gridBorderPaint)
            }
        }
    }

    // Render the Z-shaped waypoint path as thick segments + joints, with bridge overlay pass
    private fun drawPath(canvas: Canvas, W: Float, H: Float) {
        val wps = PathSystem.waypoints
        if (wps.size < 2) return
        val thickness = 24f

        // First pass: draw all ground path segments
        for (i in 1 until wps.size) {
            val from = wps[i - 1]
            val to = wps[i]
            val dx = to.x - from.x
            val dy = to.y - from.y
            val len = sqrt(dx * dx + dy * dy)
            if (len < 1f) continue

            canvas.save()
            canvas.translate((from.x + to.x) / 2f, (from.y + to.y) / 2f)
            val angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
            canvas.rotate(angle)
            canvas.drawRect(-len / 2f, -thickness / 2f, len / 2f, thickness / 2f, pathFillPaint)
            canvas.drawRect(-len / 2f, -thickness / 2f, len / 2f, thickness / 2f, pathStrokePaint)
            canvas.restore()
        }

        // Corner joints
        for (wp in wps) {
            canvas.drawCircle(wp.x, wp.y, thickness / 2f, pathFillPaint)
        }

        // Second pass: draw bridge segments with elevated visual
        val bridgeShadowPaint = Paint().apply {
            color = Color.argb(77, 0, 0, 0)   // black ~30% alpha
            style = Paint.Style.STROKE
            strokeWidth = thickness + 6f
            strokeCap = Paint.Cap.ROUND
        }
        val bridgeSurfacePaint = Paint().apply {
            color = Color.parseColor("#594D33")  // lighter brown
            style = Paint.Style.STROKE
            strokeWidth = thickness
            strokeCap = Paint.Cap.ROUND
        }
        val bridgeRailPaint = Paint().apply {
            color = Color.argb(179, 153, 122, 77)  // brass ~70% alpha
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
        }

        for (i in 1 until wps.size) {
            val fromLayer = PathSystem.layerAt(i - 1)
            val toLayer = PathSystem.layerAt(i)
            if (fromLayer == PathLayer.BRIDGE || toLayer == PathLayer.BRIDGE) {
                val from = wps[i - 1]
                val to = wps[i]
                // Shadow (offset down 3px)
                canvas.drawLine(from.x, from.y + 3f, to.x, to.y + 3f, bridgeShadowPaint)
                // Bridge surface
                canvas.drawLine(from.x, from.y, to.x, to.y, bridgeSurfacePaint)
                // Rails
                val perp = computePerpendicular(from, to, thickness / 2f - 2f)
                canvas.drawLine(from.x + perp.x, from.y + perp.y, to.x + perp.x, to.y + perp.y, bridgeRailPaint)
                canvas.drawLine(from.x - perp.x, from.y - perp.y, to.x - perp.x, to.y - perp.y, bridgeRailPaint)
            }
        }

        // Elite entry / exit indicators — clamped to screen edges so they are always visible
        if (wps.isNotEmpty()) {
            val entryPos = PointF(24f, wps[0].y)
            val exitPos = PointF(canvas.width.toFloat() - 24f, wps[wps.size - 1].y)
            drawEntryIndicator(canvas, entryPos)
            drawExitIndicator(canvas, exitPos)
        }
    }

    private fun drawEntryIndicator(canvas: Canvas, pos: PointF) {
        val t = ((System.currentTimeMillis() % 2000L) / 2000f)
        val pulse = (0.5f + 0.5f * kotlin.math.sin(t * 2 * Math.PI)).toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Outer pulsing ring — neon blue
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = android.graphics.Color.argb((230 * (0.25f + 0.75f * (1f - pulse))).toInt().coerceIn(0, 255), 0, 200, 255)
        canvas.drawCircle(pos.x, pos.y, 13f + 6f * pulse, paint)

        // Inner ring — neon blue steady
        paint.strokeWidth = 1.5f
        paint.color = android.graphics.Color.argb(178, 0, 200, 255)
        canvas.drawCircle(pos.x, pos.y, 7f, paint)

        // Right-pointing chevron arrow
        val ap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(255, 0, 200, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
        }
        val path = android.graphics.Path().apply {
            moveTo(pos.x + 4f, pos.y)
            lineTo(pos.x - 2f, pos.y - 5f)
            moveTo(pos.x + 4f, pos.y)
            lineTo(pos.x - 2f, pos.y + 5f)
        }
        canvas.drawPath(path, ap)
    }

    private fun drawExitIndicator(canvas: Canvas, pos: PointF) {
        val t = ((System.currentTimeMillis() % 800L) / 800f)
        val pulse = (0.5f + 0.5f * kotlin.math.sin(t * 2 * Math.PI)).toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 6 dashed arc segments — red danger ring
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = android.graphics.Color.argb(217, 255, 43, 84)
        val r = 13f
        val oval = RectF(pos.x - r, pos.y - r, pos.x + r, pos.y + r)
        val dashCount = 6
        val sweep = 360f / dashCount
        for (i in 0 until dashCount) {
            canvas.drawArc(oval, i * sweep, sweep * 0.55f, false, paint)
        }

        // Pulsing core dot — danger
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.argb((89 + (89 * pulse).toInt()).coerceIn(0, 255), 255, 43, 84)
        canvas.drawCircle(pos.x, pos.y, 5f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = android.graphics.Color.argb(204, 255, 43, 84)
        canvas.drawCircle(pos.x, pos.y, 5f, paint)

        // Right-pointing chevron arrow — red
        val ap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(255, 255, 43, 84)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
        }
        val path = android.graphics.Path().apply {
            moveTo(pos.x + 4f, pos.y)
            lineTo(pos.x - 2f, pos.y - 5f)
            moveTo(pos.x + 4f, pos.y)
            lineTo(pos.x - 2f, pos.y + 5f)
        }
        canvas.drawPath(path, ap)
    }

    /** Compute perpendicular offset vector for bridge rail rendering. */
    private fun computePerpendicular(from: PointF, to: PointF, distance: Float): PointF {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val len = sqrt(dx * dx + dy * dy)
        return if (len > 0) PointF(-dy / len * distance, dx / len * distance) else PointF(0f, distance)
    }

    // Tower slot backgrounds (only unoccupied)
    private fun drawTowerSlots(canvas: Canvas) {
        for ((idx, pos) in slotPositions.withIndex()) {
            if (slotOccupied[idx] == true) continue
            val half = 23f
            canvas.drawRoundRect(
                RectF(pos.x - half, pos.y - half, pos.x + half, pos.y + half),
                8f, 8f, slotEmptyPaint
            )
            canvas.drawRoundRect(
                RectF(pos.x - half, pos.y - half, pos.x + half, pos.y + half),
                8f, 8f, slotBorderPaint
            )
            // Cross
            canvas.drawRect(pos.x - 1f, pos.y - 9f, pos.x + 1f, pos.y + 9f, slotCrossPaint)
            canvas.drawRect(pos.x - 9f, pos.y - 1f, pos.x + 9f, pos.y + 1f, slotCrossPaint)
        }
    }

    // Towers with diamond shape, range ring for selected, level badge.
    // Each tower rotates its barrel toward the current target via facingAngle.
    private fun drawTowers(canvas: Canvas) {
        for ((slotId, inst) in towerInstances) {
            val cx = inst.position.x
            val cy = inst.position.y
            val radius = 22f

            // Range ring for selected tower (drawn before rotation so it stays axis-aligned)
            if (selectedSlotId == slotId) {
                val rangePixels = inst.tower.rangeTiles * GridSystem.TILE_SIZE_DP
                canvas.drawCircle(cx, cy, rangePixels, towerRangeFillPaint)
                canvas.drawCircle(cx, cy, rangePixels, towerRangePaint)
            }

            // Rotate canvas so barrel points toward current target.
            // facingAngle=0 means target is to the right (+x). Drawing assumes barrel points up (-y),
            // so we rotate by (angle_degrees - 90) to align the barrel to the target direction.
            val rotationDeg = Math.toDegrees(inst.facingAngle.toDouble()).toFloat() - 90f
            canvas.save()
            canvas.rotate(rotationDeg, cx, cy)

            // Tower body (diamond)
            val towerPaint = when (inst.tower.type) {
                TowerType.BOLT      -> towerBoltPaint
                TowerType.BLAST     -> towerBlastPaint
                TowerType.FROST     -> towerFrostPaint
                TowerType.PIERCE    -> towerPiercePaint
                TowerType.CORE      -> towerCorePaint
                TowerType.INFERNO   -> towerInfernoPaint
                TowerType.TESLA     -> towerTeslaPaint
                TowerType.NOVA      -> towerNovaPaint
                TowerType.SNIPER    -> towerSniperPaint
                TowerType.ARTILLERY -> towerArtilleryPaint
            }
            val path = Path().apply {
                moveTo(cx, cy - radius)
                lineTo(cx + radius, cy)
                lineTo(cx, cy + radius)
                lineTo(cx - radius, cy)
                close()
            }
            canvas.drawPath(path, towerPaint)

            // Type label
            val label = inst.tower.type.name.first().toString()
            val labelPaint = Paint(indicatorTextPaint).apply { textSize = radius * 0.85f }
            canvas.drawText(label, cx, cy + radius * 0.30f, labelPaint)

            canvas.restore()

            // Level badge (drawn after restore so it stays in fixed position)
            if (inst.level > 1) {
                drawLevelBadge(canvas, cx + 14f, cy + 14f, inst.level)
            }
        }
    }

    private fun drawLevelBadge(canvas: Canvas, cx: Float, cy: Float, level: Int) {
        val r = 9f
        canvas.drawCircle(cx, cy, r, badgeBgPaint)

        // Color escalation: blue → purple → orange
        val (border, text) = when {
            level < 3  -> Color.parseColor("#00C8FF") to Color.parseColor("#00C8FF")
            level < 6  -> Color.parseColor("#8C50FF") to Color.parseColor("#8C50FF")
            else       -> Color.parseColor("#FF6B00") to Color.parseColor("#FF6B00")
        }
        val bp = Paint(badgeBorderPaint).apply { color = border }
        canvas.drawCircle(cx, cy, r, bp)

        val tp = Paint(badgeTextPaint).apply {
            color = text
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(level.toString(), cx, cy + 5f, tp)
    }

    // All enemy types with distinct visuals; bridge enemies drawn 1.15× larger
    private fun drawEnemies(canvas: Canvas) {
        for (enemy in enemies) {
            if (!enemy.isAlive) continue
            val pos = PathSystem.positionAt(enemy.pathProgress)
            val px = pos.x
            val py = pos.y
            val scale = if (enemy.pathLayer == PathLayer.BRIDGE) 1.15f else 1.0f

            if (scale != 1.0f) {
                canvas.save()
                canvas.scale(scale, scale, px, py)
            }

            when (enemy.type) {
                EnemyType.RUNNER   -> drawRunnerEnemy(canvas, px, py, enemy)
                EnemyType.TANK     -> drawTankEnemy(canvas, px, py, enemy)
                EnemyType.SHIELD   -> drawShieldEnemy(canvas, px, py, enemy)
                EnemyType.SWARM    -> drawSwarmEnemy(canvas, px, py, enemy)
                EnemyType.GHOST    -> drawGhostEnemy(canvas, px, py, enemy)
                EnemyType.BOSS     -> drawBossEnemy(canvas, px, py, enemy)
                EnemyType.SPLITTER -> drawSplitterEnemy(canvas, px, py, enemy)
                EnemyType.JUMPER   -> drawJumperEnemy(canvas, px, py, enemy)
            }

            if (scale != 1.0f) {
                canvas.restore()
            }
        }
    }

    private fun drawRunnerEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        val r = 11f
        canvas.drawCircle(cx, cy, r, enemyRunnerPaint)
        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = 24f, barHeight = 5f)
    }

    private fun drawTankEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        val r = 17f
        canvas.drawCircle(cx, cy, r, enemyTankPaint)
        // Armor indicator — inner darker ring
        val innerPaint = Paint(enemyTankPaint).apply { color = Color.parseColor("#4D1F00") }
        canvas.drawCircle(cx, cy, r * 0.55f, innerPaint)
        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = 34f, barHeight = 6f)
    }

    private fun drawShieldEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        val r = 12f
        // Body — blue
        val bodyColor = if (e.shieldBroken) Color.parseColor("#E64D1A") else Color.parseColor("#3380E6")
        canvas.drawCircle(cx, cy, r, Paint().apply { color = bodyColor; style = Paint.Style.FILL })

        // Shield ring — cyan halo (only if shield active)
        if (!e.shieldBroken && e.shieldHp > 0f) {
            canvas.drawCircle(cx, cy, r + 5f, enemyShieldRingPaint)
        }
        drawHealthBar(canvas, cx, cy, r + 5f, e.currentHp, e.maxHp, barWidth = 28f, barHeight = 5f)
    }

    private fun drawSwarmEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        val r = 7f
        canvas.drawCircle(cx, cy, r, enemySwarmPaint)
        // Swarm: small orange-yellow dot — tiny health bar
        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = 16f, barHeight = 3f)
    }

    private fun drawGhostEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        val r = 13f
        // Semi-transparent green
        canvas.drawCircle(cx, cy, r, enemyGhostPaint)
        // Inner glow
        val glowPaint = Paint().apply {
            color = Color.parseColor("#CCFFCC")
            style = Paint.Style.FILL
            alpha = 153
        }
        canvas.drawCircle(cx, cy, r * 0.54f, glowPaint)
        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = 28f, barHeight = 5f)
    }

    private fun drawBossEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        val variant = e.bossVariant
        val bodyPaint = enemyBossVariantPaints[variant]
        val strokePaint = bossStrokePaints[variant]

        when (variant) {
            1 -> {
                // Iron Colossus — grey square
                val half = 19f
                val rect = RectF(cx - half, cy - half, cx + half, cy + half)
                canvas.drawRoundRect(rect, 5f, 5f, bodyPaint)
                canvas.drawRoundRect(rect, 5f, 5f, strokePaint)
            }
            2 -> {
                // Swarm Queen — orange pulsing circle with sacs
                canvas.drawCircle(cx, cy, 20f, bodyPaint)
                canvas.drawCircle(cx, cy, 20f, strokePaint)
                // Spawn sac indicators
                val sacPaint = Paint().apply { color = Color.parseColor("#FFAA00"); style = Paint.Style.FILL; alpha = 204 }
                for (angle in listOf(0f, 90f, 180f, 270f)) {
                    val rad = Math.toRadians(angle.toDouble())
                    val sx = cx + (Math.cos(rad) * 18).toFloat()
                    val sy = cy + (Math.sin(rad) * 18).toFloat()
                    canvas.drawCircle(sx, sy, 5f, sacPaint)
                }
            }
            3 -> {
                // Phase Runner — cyan angular body
                val path = Path().apply {
                    moveTo(cx, cy - 15f)
                    lineTo(cx + 10f, cy)
                    lineTo(cx, cy + 15f)
                    lineTo(cx - 10f, cy)
                    close()
                }
                canvas.drawPath(path, bodyPaint)
                canvas.drawPath(path, strokePaint)
            }
            4 -> {
                // Void Titan — dark purple concentric circles
                canvas.drawCircle(cx, cy, 26f, bodyPaint)
                canvas.drawCircle(cx, cy, 26f, strokePaint)
                val innerPaint = Paint().apply { color = Color.parseColor("#4D0080"); style = Paint.Style.FILL }
                canvas.drawCircle(cx, cy, 14f, innerPaint)
            }
            else -> {
                // 0: Rift Guardian — purple circle with outer ring
                canvas.drawCircle(cx, cy, 22f, bodyPaint)
                canvas.drawCircle(cx, cy, 22f, strokePaint)
                val corePaint = Paint().apply { color = Color.parseColor("#CC66FF"); style = Paint.Style.FILL }
                canvas.drawCircle(cx, cy, 10f, corePaint)
            }
        }

        // Boss wide health bar
        drawHealthBar(canvas, cx, cy, 26f, e.currentHp, e.maxHp, barWidth = 56f, barHeight = 7f)
    }

    private fun drawSplitterEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        val r = 10f
        // Magenta diamond shape — splits on death
        val path = android.graphics.Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r, cy)
            lineTo(cx, cy + r)
            lineTo(cx - r, cy)
            close()
        }
        canvas.drawPath(path, enemySplitterPaint)
        // Split indicators — two small dots
        val dotPaint = Paint().apply { color = Color.parseColor("#FF99FF"); style = Paint.Style.FILL; alpha = 204 }
        canvas.drawCircle(cx - 5f, cy + 1f, 3f, dotPaint)
        canvas.drawCircle(cx + 5f, cy + 1f, 3f, dotPaint)
        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = 22f, barHeight = 4f)
    }

    private fun drawJumperEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        val r = 12f
        // Teal-green hexagon shape — jumps forward
        canvas.drawCircle(cx, cy, r, enemyJumperPaint)
        // Jump arrow indicator
        val arrowPaint = Paint().apply { color = Color.parseColor("#004422"); style = Paint.Style.FILL; isFakeBoldText = true }
        val arrowPath = android.graphics.Path().apply {
            moveTo(cx - 4f, cy)
            lineTo(cx + 4f, cy)
            lineTo(cx, cy - 6f)
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)
        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = 26f, barHeight = 5f)
    }

    private fun drawHealthBar(
        canvas: Canvas,
        cx: Float, cy: Float,
        enemyRadius: Float,
        currentHp: Float, maxHp: Float,
        barWidth: Float, barHeight: Float
    ) {
        val barLeft = cx - barWidth / 2f
        val barTop = cy - enemyRadius - barHeight - 4f

        canvas.drawRect(barLeft, barTop, barLeft + barWidth, barTop + barHeight, hpBgPaint)

        val ratio = if (maxHp > 0f) (currentHp / maxHp).coerceIn(0f, 1f) else 0f
        val fgPaint = when {
            ratio > 0.6f -> hpGreenPaint
            ratio > 0.3f -> hpYellowPaint
            else         -> hpRedPaint
        }
        canvas.drawRect(barLeft, barTop, barLeft + barWidth * ratio, barTop + barHeight, fgPaint)
    }

    inner class RenderThread(private val surfaceHolder: SurfaceHolder) : Thread("PathriftRenderThread") {
        override fun run() {
            while (isRunning) {
                val canvas = surfaceHolder.lockCanvas() ?: continue
                try {
                    synchronized(surfaceHolder) { drawFrame(canvas) }
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
                sleep(16L)
            }
        }
    }
}
