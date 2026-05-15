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
import kotlin.math.PI
import kotlin.math.abs
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
    private val enemyHealerPaint = Paint().apply { color = Color.parseColor("#2ECC71"); style = Paint.Style.FILL }
    private val enemyHealerStrokePaint = Paint().apply {
        color = Color.parseColor("#AAFFD0")
        style = Paint.Style.STROKE; strokeWidth = 2f
    }
    private val enemyHealerAuraPaint = Paint().apply {
        color = Color.parseColor("#2ECC71"); style = Paint.Style.FILL; alpha = 51 // ~0.2 alpha
    }
    private val enemyPhantomPaint = Paint().apply {
        color = Color.parseColor("#8B00FF"); style = Paint.Style.FILL; alpha = 191 // 0.75 alpha
    }
    private val enemyPhantomStrokePaint = Paint().apply {
        color = Color.parseColor("#CC66FF"); style = Paint.Style.STROKE; strokeWidth = 1.5f
    }
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

    // Path fill and edge paints — DESIGN_SPEC_BUILD5 Section 3 (PATHRIFT-163)
    private val pathCorridorFillPaint = Paint().apply {
        color = Color.argb(255, 26, 26, 40)   // (0.10, 0.10, 0.16) — path.fill slate
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val pathEdgePaint = Paint().apply {
        color = Color.argb(76, 0, 200, 255)   // (0.00, 0.78, 1.00, 0.30) — muted cyan edge
        style = Paint.Style.STROKE
        strokeWidth = 1.0f
        isAntiAlias = true
    }
    private val pathTexturePaint = Paint().apply {
        color = Color.argb(127, 41, 41, 61)   // (0.16, 0.16, 0.24, 0.50) texture lines
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
        isAntiAlias = true
    }

    /** Build a filled corridor path for a single waypoint segment. */
    private fun buildCorridorSegment(from: PointF, to: PointF, halfWidth: Float): Path {
        val dx = to.x - from.x; val dy = to.y - from.y
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1f) return Path()
        val px = -dy / len * halfWidth; val py = dx / len * halfWidth
        val path = Path()
        path.moveTo(from.x + px, from.y + py)
        path.lineTo(to.x + px, to.y + py)
        path.lineTo(to.x - px, to.y - py)
        path.lineTo(from.x - px, from.y - py)
        path.close()
        return path
    }

    // Render the path as filled corridor with edge glow lines and texture (PATHRIFT-163)
    private fun drawPath(canvas: Canvas, W: Float, H: Float) {
        val wps = PathSystem.waypoints
        if (wps.size < 2) return
        val halfWidth = 16f  // 32pt corridor = 16pt each side

        // First pass: draw all ground path segments as filled corridors
        for (i in 1 until wps.size) {
            val fromLayer = PathSystem.layerAt(i - 1)
            val toLayer = PathSystem.layerAt(i)
            if (fromLayer == PathLayer.BRIDGE || toLayer == PathLayer.BRIDGE) continue

            val from = wps[i - 1]; val to = wps[i]
            val seg = buildCorridorSegment(from, to, halfWidth)
            canvas.drawPath(seg, pathCorridorFillPaint)

            // Edge lines
            val dx = to.x - from.x; val dy = to.y - from.y
            val len = sqrt(dx * dx + dy * dy)
            if (len > 0f) {
                val px = -dy / len * halfWidth; val py = dx / len * halfWidth
                canvas.drawLine(from.x + px, from.y + py, to.x + px, to.y + py, pathEdgePaint)
                canvas.drawLine(from.x - px, from.y - py, to.x - px, to.y - py, pathEdgePaint)

                // Texture lines — perpendicular, every 4pt along length
                val steps = (len / 4f).toInt().coerceAtMost(32)
                for (s in 0..steps) {
                    val t = if (steps > 0) s.toFloat() / steps else 0f
                    val mx = from.x + dx * t; val my = from.y + dy * t
                    canvas.drawLine(mx + px, my + py, mx - px, my - py, pathTexturePaint)
                }
            }
        }

        // Waypoint joints (circular caps) — ground
        val jointFillPaint = Paint(pathCorridorFillPaint)
        for (i in wps.indices) {
            if (PathSystem.layerAt(i) == PathLayer.BRIDGE) continue
            canvas.drawCircle(wps[i].x, wps[i].y, halfWidth, jointFillPaint)
            // Accent dot
            val accentPaint = Paint().apply {
                color = Color.argb(102, 0, 200, 255); style = Paint.Style.STROKE; strokeWidth = 1f
            }
            canvas.drawCircle(wps[i].x, wps[i].y, 4f, accentPaint)
        }

        // Second pass: bridge segments with elevated visual
        val bridgeFillPaint = Paint().apply {
            color = Color.argb(255, 36, 36, 56)  // (0.14, 0.14, 0.22) lighter
            style = Paint.Style.FILL; isAntiAlias = true
        }
        val bridgeEdgePaint = Paint().apply {
            color = Color.argb(140, 0, 200, 255) // stronger cyan edge
            style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true
        }
        val bridgeRailPaint = Paint().apply {
            color = Color.argb(255, 77, 77, 115)  // (0.30, 0.30, 0.45) guard rail
            style = Paint.Style.STROKE; strokeWidth = 2f; strokeCap = Paint.Cap.ROUND; isAntiAlias = true
        }
        val bridgeShadowPaint = Paint().apply {
            color = Color.argb(102, 0, 0, 0)
            style = Paint.Style.FILL; isAntiAlias = true
        }

        for (i in 1 until wps.size) {
            val fromLayer = PathSystem.layerAt(i - 1)
            val toLayer = PathSystem.layerAt(i)
            if (fromLayer != PathLayer.BRIDGE && toLayer != PathLayer.BRIDGE) continue
            val from = wps[i - 1]; val to = wps[i]

            // Shadow (offset down 3px)
            val shadowSeg = buildCorridorSegment(
                PointF(from.x, from.y + 3f), PointF(to.x, to.y + 3f), halfWidth
            )
            canvas.drawPath(shadowSeg, bridgeShadowPaint)

            // Bridge fill
            val bridgeSeg = buildCorridorSegment(from, to, halfWidth)
            canvas.drawPath(bridgeSeg, bridgeFillPaint)

            // Bridge edge lines
            val dx = to.x - from.x; val dy = to.y - from.y
            val len = sqrt(dx * dx + dy * dy)
            if (len > 0f) {
                val px = -dy / len * halfWidth; val py = dx / len * halfWidth
                canvas.drawLine(from.x + px, from.y + py, to.x + px, to.y + py, bridgeEdgePaint)
                canvas.drawLine(from.x - px, from.y - py, to.x - px, to.y - py, bridgeEdgePaint)
                // Rails (inner edge)
                val railOff = halfWidth - 2f
                val rpx = -dy / len * railOff; val rpy = dx / len * railOff
                canvas.drawLine(from.x + rpx, from.y + rpy, to.x + rpx, to.y + rpy, bridgeRailPaint)
                canvas.drawLine(from.x - rpx, from.y - rpy, to.x - rpx, to.y - rpy, bridgeRailPaint)
            }
        }

        // Entry / exit indicators
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

    // Build an octagon path centered at (cx, cy) with given radius (PATHRIFT-161)
    private fun buildOctagonPath(cx: Float, cy: Float, radius: Float): Path {
        val path = Path()
        for (i in 0 until 8) {
            val angle = (i * 45f - 22.5f) * (PI / 180f).toFloat()
            val x = cx + cos(angle) * radius
            val y = cy + sin(angle) * radius
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    // Tower slot backgrounds — octagon shape (PATHRIFT-161, DESIGN_SPEC_BUILD5 Section 1)
    private fun drawTowerSlots(canvas: Canvas) {
        val slotRadius = 24f
        val innerRingRadius = 8f
        val dotRadius = 2f
        val pulseT = ((System.currentTimeMillis() % 2400L) / 2400f)
        val pulseAlpha = (102 + (40 * sin(pulseT * 2 * PI.toFloat())).toInt()).coerceIn(62, 166)

        val fillPaint = Paint().apply {
            color = Color.argb(255, 13, 23, 36)   // (0.05, 0.09, 0.14) = very dark navy
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val strokePaint = Paint().apply {
            color = Color.argb(pulseAlpha, 0, 200, 255)  // accent.cyan with pulsing alpha
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        val innerRingPaint = Paint().apply {
            color = Color.argb(51, 0, 200, 255)   // cyan @ 20%
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
            isAntiAlias = true
        }
        val dotPaint = Paint().apply {
            color = Color.argb(204, 0, 200, 255)  // cyan @ 80%
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val selectedFillPaint = Paint().apply {
            color = Color.argb(255, 0, 46, 71)   // (0.00, 0.18, 0.28) deep cyan-tinted
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val selectedStrokePaint = Paint().apply {
            color = Color.argb(255, 0, 200, 255) // full cyan
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }

        for ((idx, pos) in slotPositions.withIndex()) {
            if (slotOccupied[idx] == true) continue
            val cx = pos.x; val cy = pos.y
            val isSelected = selectedSlotId == idx

            val octPath = buildOctagonPath(cx, cy, slotRadius)

            canvas.drawPath(octPath, if (isSelected) selectedFillPaint else fillPaint)
            canvas.drawPath(octPath, if (isSelected) selectedStrokePaint else strokePaint)

            // Inner ring
            canvas.drawCircle(cx, cy, innerRingRadius, innerRingPaint)

            // Cardinal corner accent dots (N/S/E/W at 0°, 90°, 180°, 270°)
            for (angle in listOf(0f, 90f, 180f, 270f)) {
                val rad = (angle - 90f) * (PI / 180f).toFloat()  // N = -Y on Canvas
                val dx = cos(rad) * slotRadius
                val dy = sin(rad) * slotRadius
                canvas.drawCircle(cx + dx, cy + dy, dotRadius, dotPaint)
            }
        }
    }

    // Towers with unique shapes per DESIGN_SPEC_BUILD5 Section 2 (PATHRIFT-162)
    private fun drawTowers(canvas: Canvas) {
        for ((slotId, inst) in towerInstances) {
            val cx = inst.position.x
            val cy = inst.position.y

            // Range ring for selected tower
            if (selectedSlotId == slotId) {
                val rangePixels = inst.tower.rangeTiles * GridSystem.TILE_SIZE_DP
                canvas.drawCircle(cx, cy, rangePixels, towerRangeFillPaint)
                canvas.drawCircle(cx, cy, rangePixels, towerRangePaint)
            }

            // Floor shadow ellipse beneath tower
            val shadowPaint = Paint().apply {
                color = Color.argb(89, 0, 0, 0); style = Paint.Style.FILL; isAntiAlias = true
            }
            canvas.drawOval(RectF(cx - 14f, cy + 12f, cx + 14f, cy + 17f), shadowPaint)

            // Rotate canvas to face current target
            val rotationDeg = Math.toDegrees(inst.facingAngle.toDouble()).toFloat() - 90f
            canvas.save()
            canvas.rotate(rotationDeg, cx, cy)

            drawTowerBody(canvas, inst.tower.type, cx, cy)

            canvas.restore()

            // Level badge
            if (inst.level > 1) {
                drawLevelBadge(canvas, cx + 14f, cy + 14f, inst.level)
            }
        }
    }

    private fun drawTowerBody(canvas: Canvas, type: TowerType, cx: Float, cy: Float) {
        val aa = Paint.ANTI_ALIAS_FLAG

        when (type) {
            TowerType.BOLT -> {
                // Hexagon body, radius 14, flat-top
                val hexPath = Path()
                for (i in 0 until 6) {
                    val a = (i * 60f) * (PI / 180f).toFloat()
                    val x = cx + cos(a) * 14f; val y = cy + sin(a) * 14f
                    if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                }
                hexPath.close()
                canvas.drawPath(hexPath, Paint(aa).apply { color = Color.argb(255,0,31,56); style=Paint.Style.FILL })
                canvas.drawPath(hexPath, Paint(aa).apply { color=Color.parseColor("#00C8FF"); style=Paint.Style.STROKE; strokeWidth=1.5f })
                // Barrel up
                canvas.drawRoundRect(RectF(cx-2.5f, cy-22f, cx+2.5f, cy-11f), 2f, 2f,
                    Paint(aa).apply { color=Color.parseColor("#00C8FF"); style=Paint.Style.FILL })
                // Circuit trace lines
                val tracePaint = Paint(aa).apply { color=Color.argb(153,0,200,255); style=Paint.Style.STROKE; strokeWidth=0.75f }
                for (angle in listOf(30f, 90f, 150f)) {
                    val rad = angle * (PI/180f).toFloat()
                    canvas.drawLine(cx, cy, cx + cos(rad)*12f, cy + sin(rad)*12f, tracePaint)
                }
            }
            TowerType.BLAST -> {
                // Circle body, radius 14
                canvas.drawCircle(cx, cy, 14f, Paint(aa).apply { color=Color.argb(255,51,20,0); style=Paint.Style.FILL })
                canvas.drawCircle(cx, cy, 14f, Paint(aa).apply { color=Color.parseColor("#FF7300"); style=Paint.Style.STROKE; strokeWidth=2f })
                // Wide barrel with flared tip
                canvas.drawRect(cx-3f, cy-22f, cx+3f, cy-11f,
                    Paint(aa).apply { color=Color.parseColor("#FF7300"); style=Paint.Style.FILL })
                // Exhaust pipes at 120° intervals from top
                val exhaustPaint = Paint(aa).apply { color=Color.argb(255,38,15,0); style=Paint.Style.FILL }
                val exhaustStroke = Paint(aa).apply { color=Color.parseColor("#FF7300"); style=Paint.Style.STROKE; strokeWidth=1f }
                for (angle in listOf(120f, 240f)) {
                    val rad = (angle - 90f) * (PI/180f).toFloat()
                    val ex = cx + cos(rad)*14f; val ey = cy + sin(rad)*14f
                    canvas.drawRect(ex-2.5f, ey-3.5f, ex+2.5f, ey+3.5f, exhaustPaint)
                    canvas.drawRect(ex-2.5f, ey-3.5f, ex+2.5f, ey+3.5f, exhaustStroke)
                }
            }
            TowerType.FROST -> {
                // Diamond (rhombus) 28×28
                val dPath = Path()
                dPath.moveTo(cx, cy-14f); dPath.lineTo(cx+14f, cy); dPath.lineTo(cx, cy+14f); dPath.lineTo(cx-14f, cy)
                dPath.close()
                canvas.drawPath(dPath, Paint(aa).apply { color=Color.argb(255,15,5,36); style=Paint.Style.FILL })
                canvas.drawPath(dPath, Paint(aa).apply { color=Color.parseColor("#8F2EFF"); style=Paint.Style.STROKE; strokeWidth=1.5f })
                // Barrel
                canvas.drawRect(cx-2f, cy-22f, cx+2f, cy-12f, Paint(aa).apply { color=Color.parseColor("#8F2EFF"); style=Paint.Style.FILL })
                // Crystal spike tips at 4 diamond corners
                val spikePaint = Paint(aa).apply { color=Color.argb(178,179,217,255); style=Paint.Style.FILL }
                for ((dx,dy) in listOf(Pair(0f,-14f),Pair(14f,0f),Pair(0f,14f),Pair(-14f,0f))) {
                    val sp = Path()
                    sp.moveTo(cx+dx-2f, cy+dy); sp.lineTo(cx+dx+2f, cy+dy)
                    sp.lineTo(cx+dx, cy+dy + (if (dy < 0) -6f else if (dy > 0) 6f else 0f) + (if (dx < 0) -6f else if (dx > 0) 6f else 0f))
                    sp.close()
                    canvas.drawPath(sp, spikePaint)
                }
            }
            TowerType.PIERCE -> {
                // Elongated octagon 16×28
                val cut = 5f
                val ep = Path()
                ep.moveTo(cx-8f+cut, cy-14f); ep.lineTo(cx+8f-cut, cy-14f)
                ep.lineTo(cx+8f, cy-14f+cut); ep.lineTo(cx+8f, cy+14f-cut)
                ep.lineTo(cx+8f-cut, cy+14f); ep.lineTo(cx-8f+cut, cy+14f)
                ep.lineTo(cx-8f, cy+14f-cut); ep.lineTo(cx-8f, cy-14f+cut)
                ep.close()
                canvas.drawPath(ep, Paint(aa).apply { color=Color.argb(255,10,36,5); style=Paint.Style.FILL })
                canvas.drawPath(ep, Paint(aa).apply { color=Color.parseColor("#66FF1A"); style=Paint.Style.STROKE; strokeWidth=1.5f })
                // Long barrel
                canvas.drawRect(cx-2f, cy-26f, cx+2f, cy-12f, Paint(aa).apply { color=Color.parseColor("#66FF1A"); style=Paint.Style.FILL })
                // Reticle sight lines
                val reticlePaint = Paint(aa).apply { color=Color.argb(127,102,255,26); style=Paint.Style.STROKE; strokeWidth=0.75f }
                canvas.drawLine(cx-6f, cy-3f, cx+6f, cy-3f, reticlePaint)
                canvas.drawLine(cx-6f, cy+3f, cx+6f, cy+3f, reticlePaint)
            }
            TowerType.CORE -> {
                // Square 26×26
                canvas.drawRect(cx-13f, cy-13f, cx+13f, cy+13f, Paint(aa).apply { color=Color.argb(255,46,13,0); style=Paint.Style.FILL })
                canvas.drawRect(cx-13f, cy-13f, cx+13f, cy+13f, Paint(aa).apply { color=Color.parseColor("#FF590D"); style=Paint.Style.STROKE; strokeWidth=2f })
                // Barrel
                canvas.drawRect(cx-3.5f, cy-21f, cx+3.5f, cy-13f, Paint(aa).apply { color=Color.parseColor("#FF590D"); style=Paint.Style.FILL })
                // Corner rivets
                val rivetFill = Paint(aa).apply { color=Color.argb(255,230,71,10); style=Paint.Style.FILL }
                val rivetStroke = Paint(aa).apply { color=Color.parseColor("#FF590D"); style=Paint.Style.STROKE; strokeWidth=1f }
                for ((rx,ry) in listOf(Pair(-9f,-9f),Pair(9f,-9f),Pair(-9f,9f),Pair(9f,9f))) {
                    canvas.drawCircle(cx+rx, cy+ry, 2.5f, rivetFill)
                    canvas.drawCircle(cx+rx, cy+ry, 2.5f, rivetStroke)
                }
                // Diagonal cross
                val crossPaint = Paint(aa).apply { color=Color.argb(76,255,89,13); style=Paint.Style.STROKE; strokeWidth=0.75f }
                canvas.drawLine(cx-10f, cy-10f, cx+10f, cy+10f, crossPaint)
                canvas.drawLine(cx+10f, cy-10f, cx-10f, cy+10f, crossPaint)
            }
            TowerType.INFERNO -> {
                // Irregular pentagon (flame lean)
                val ip = Path()
                ip.moveTo(cx, cy-14f); ip.lineTo(cx+12f, cy-4f); ip.lineTo(cx+10f, cy+14f)
                ip.lineTo(cx-11f, cy+14f); ip.lineTo(cx-13f, cy-5f)
                ip.close()
                canvas.drawPath(ip, Paint(aa).apply { color=Color.argb(255,51,8,0); style=Paint.Style.FILL })
                canvas.drawPath(ip, Paint(aa).apply { color=Color.parseColor("#FF2E14"); style=Paint.Style.STROKE; strokeWidth=1.75f })
                // Barrel with 3 micro-prong tips
                canvas.drawRect(cx-2.5f, cy-22f, cx+2.5f, cy-11f, Paint(aa).apply { color=Color.parseColor("#FF2E14"); style=Paint.Style.FILL })
                // Flame tips
                val flamePaint = Paint(aa).apply { color=Color.argb(204,255,140,26); style=Paint.Style.FILL }
                for ((fx, fh) in listOf(Triple(cx-5f, cy-14f, 4f), Triple(cx, cy-15f, 6f), Triple(cx+5f, cy-13f, 3f))) {
                    val fp2 = Path(); fp2.moveTo(fx-2f, fh); fp2.lineTo(fx+2f, fh); fp2.lineTo(fx, fh-fh.coerceAtLeast(0f)*0f - 5f); fp2.close()
                    canvas.drawPath(fp2, flamePaint)
                }
            }
            TowerType.TESLA -> {
                // Circle, radius 13
                canvas.drawCircle(cx, cy, 13f, Paint(aa).apply { color=Color.argb(255,5,20,46); style=Paint.Style.FILL })
                canvas.drawCircle(cx, cy, 13f, Paint(aa).apply { color=Color.parseColor("#33A6FF"); style=Paint.Style.STROKE; strokeWidth=1.5f })
                // Barrel
                canvas.drawRect(cx-2.5f, cy-21f, cx+2.5f, cy-13f, Paint(aa).apply { color=Color.parseColor("#33A6FF"); style=Paint.Style.FILL })
                // Arc coil rings
                val arcPaint1 = Paint(aa).apply { color=Color.argb(178,51,166,255); style=Paint.Style.STROKE; strokeWidth=2f }
                val arcPaint2 = Paint(aa).apply { color=Color.argb(102,51,166,255); style=Paint.Style.STROKE; strokeWidth=1f }
                canvas.drawArc(RectF(cx-19f, cy-19f, cx+19f, cy+19f), 210f, 120f, false, arcPaint1)
                canvas.drawArc(RectF(cx-16f, cy-16f, cx+16f, cy+16f), 240f, 60f, false, arcPaint2)
            }
            TowerType.NOVA -> {
                // 6-pointed star outer=14, inner=7
                val starPath = Path()
                for (i in 0 until 12) {
                    val a = (i * 30f - 90f) * (PI / 180f).toFloat()
                    val r = if (i % 2 == 0) 14f else 7f
                    val x = cx + cos(a) * r; val y = cy + sin(a) * r
                    if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                }
                starPath.close()
                canvas.drawPath(starPath, Paint(aa).apply { color=Color.argb(255,46,36,0); style=Paint.Style.FILL })
                canvas.drawPath(starPath, Paint(aa).apply { color=Color.parseColor("#FFD11A"); style=Paint.Style.STROKE; strokeWidth=1.5f })
                // Center lens
                canvas.drawCircle(cx, cy, 4f, Paint(aa).apply { color=Color.argb(153,255,209,26); style=Paint.Style.FILL })
                // Barrel
                canvas.drawRect(cx-2.5f, cy-20f, cx+2.5f, cy-10f, Paint(aa).apply { color=Color.parseColor("#FFD11A"); style=Paint.Style.FILL })
            }
            TowerType.SNIPER -> {
                // Narrow tall rect 10×32, corners clipped 2pt
                val snipPath = Path()
                val sw = 5f; val sh = 16f  // half-width, half-height
                snipPath.moveTo(cx-sw+2f, cy-sh); snipPath.lineTo(cx+sw-2f, cy-sh)
                snipPath.lineTo(cx+sw, cy-sh+2f); snipPath.lineTo(cx+sw, cy+sh-2f)
                snipPath.lineTo(cx+sw-2f, cy+sh); snipPath.lineTo(cx-sw+2f, cy+sh)
                snipPath.lineTo(cx-sw, cy+sh-2f); snipPath.lineTo(cx-sw, cy-sh+2f)
                snipPath.close()
                canvas.drawPath(snipPath, Paint(aa).apply { color=Color.argb(255,15,26,31); style=Paint.Style.FILL })
                canvas.drawPath(snipPath, Paint(aa).apply { color=Color.parseColor("#D9FFFF"); style=Paint.Style.STROKE; strokeWidth=1.25f })
                // Long barrel
                canvas.drawRect(cx-1.5f, cy-sh-16f, cx+1.5f, cy-sh,
                    Paint(aa).apply { color=Color.parseColor("#D9FFFF"); style=Paint.Style.FILL })
                // Scope circle at 30% from top
                val scopeY = cy - sh + sh*0.6f
                canvas.drawCircle(cx, scopeY, 3.5f, Paint(aa).apply { color=Color.argb(64,153,230,255); style=Paint.Style.FILL })
                canvas.drawCircle(cx, scopeY, 3.5f, Paint(aa).apply { color=Color.parseColor("#D9FFFF"); style=Paint.Style.STROKE; strokeWidth=1f })
                canvas.drawLine(cx-3.5f, scopeY, cx+3.5f, scopeY,
                    Paint(aa).apply { color=Color.parseColor("#D9FFFF"); style=Paint.Style.STROKE; strokeWidth=0.5f })
            }
            TowerType.ARTILLERY -> {
                // Wide squat hexagon 30×22 (flat-top)
                val artPath = Path()
                for (i in 0 until 6) {
                    val a = (i * 60f) * (PI / 180f).toFloat()
                    val x = cx + cos(a) * 15f; val y = cy + sin(a) * 11f  // squashed
                    if (i == 0) artPath.moveTo(x, y) else artPath.lineTo(x, y)
                }
                artPath.close()
                canvas.drawPath(artPath, Paint(aa).apply { color=Color.argb(255,36,26,3); style=Paint.Style.FILL })
                canvas.drawPath(artPath, Paint(aa).apply { color=Color.parseColor("#BF941F"); style=Paint.Style.STROKE; strokeWidth=2f })
                // Wide barrel
                canvas.drawRect(cx-4f, cy-22f, cx+4f, cy-9f, Paint(aa).apply { color=Color.parseColor("#BF941F"); style=Paint.Style.FILL })
                // End plate
                canvas.drawRect(cx-4f, cy-22f, cx+4f, cy-19f, Paint(aa).apply { color=Color.parseColor("#BF941F"); style=Paint.Style.FILL })
                // Armor bands
                val bandPaint = Paint(aa).apply { color=Color.argb(102,191,148,31); style=Paint.Style.FILL }
                canvas.drawRect(cx-12f, cy-2f, cx+12f, cy, bandPaint)
                canvas.drawRect(cx-12f, cy+4f, cx+12f, cy+6f, bandPaint)
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
                EnemyType.HEALER   -> drawHealerEnemy(canvas, px, py, enemy)
                EnemyType.PHANTOM  -> drawPhantomEnemy(canvas, px, py, enemy)
            }

            if (scale != 1.0f) {
                canvas.restore()
            }
        }
    }

    private fun drawRunnerEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        // Elongated oval / capsule 10×16, electric blue (DESIGN_SPEC Section 4.1)
        val rw = 5f; val rh = 8f
        canvas.drawOval(RectF(cx-rw, cy-rh, cx+rw, cy+rh),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(255,0,64,179); style=Paint.Style.FILL })
        canvas.drawOval(RectF(cx-rw, cy-rh, cx+rw, cy+rh),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.parseColor("#3399FF"); style=Paint.Style.STROKE; strokeWidth=1.25f })
        // Motion lines trailing behind (below body)
        val motionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(153,77,179,255); style=Paint.Style.STROKE; strokeWidth=0.75f }
        canvas.drawLine(cx-5f, cy+rh+3f, cx+5f, cy+rh+3f, motionPaint)
        canvas.drawLine(cx-7f, cy+rh+6f, cx+7f, cy+rh+6f, motionPaint)
        canvas.drawLine(cx-4f, cy+rh+9f, cx+4f, cy+rh+9f, motionPaint)
        drawHealthBar(canvas, cx, cy, rh, e.currentHp, e.maxHp, barWidth = 20f, barHeight = 3f)
    }

    private fun drawTankEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        // Heavy angular square 20×20, gunmetal (DESIGN_SPEC Section 4.2)
        val half = 10f; val cut = 3f
        val tp = Path()
        tp.moveTo(cx-half+cut, cy-half); tp.lineTo(cx+half-cut, cy-half)
        tp.lineTo(cx+half, cy-half+cut); tp.lineTo(cx+half, cy+half-cut)
        tp.lineTo(cx+half-cut, cy+half); tp.lineTo(cx-half+cut, cy+half)
        tp.lineTo(cx-half, cy+half-cut); tp.lineTo(cx-half, cy-half+cut)
        tp.close()
        canvas.drawPath(tp, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(255,51,46,36); style=Paint.Style.FILL })
        canvas.drawPath(tp, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(255,128,115,89); style=Paint.Style.STROKE; strokeWidth=2f })
        // Armor plate seams
        val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(153,140,128,102); style=Paint.Style.STROKE; strokeWidth=0.75f }
        canvas.drawLine(cx-8f, cy-3f, cx+8f, cy-3f, seamPaint)
        canvas.drawLine(cx-8f, cy+3f, cx+8f, cy+3f, seamPaint)
        drawHealthBar(canvas, cx, cy, half, e.currentHp, e.maxHp, barWidth = 24f, barHeight = 4f)
    }

    private fun drawShieldEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        val r = 9f
        // Body — dark green circle (DESIGN_SPEC Section 4.3)
        val bodyColor = if (e.shieldBroken) Color.argb(255,153,51,0) else Color.argb(255,10,61,31)
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bodyColor; style = Paint.Style.FILL })
        val strokeColor = if (e.shieldBroken) Color.parseColor("#CC3300") else Color.parseColor("#33D966")
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=strokeColor; style=Paint.Style.STROKE; strokeWidth=1.5f })

        // Shield aura hexagon (only if shield active)
        if (!e.shieldBroken && e.shieldHp > 0f) {
            val hexPath = Path()
            for (i in 0 until 6) {
                val a = (i * 60f) * (PI/180f).toFloat()
                val x = cx + cos(a) * 14f; val y = cy + sin(a) * 14f
                if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
            }
            hexPath.close()
            canvas.drawPath(hexPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(38,51,230,102); style=Paint.Style.FILL })
            canvas.drawPath(hexPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(127,51,230,102); style=Paint.Style.STROKE; strokeWidth=1f })
        }
        drawHealthBar(canvas, cx, cy, if (!e.shieldBroken && e.shieldHp > 0f) 14f else r, e.currentHp, e.maxHp, barWidth = 22f, barHeight = 4f)
    }

    private fun drawSwarmEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        // Small irregular hexagon bug body 8×10 (DESIGN_SPEC Section 4.4)
        val hexPath = Path()
        for (i in 0 until 6) {
            val a = (i * 60f - 90f) * (PI/180f).toFloat()
            val x = cx + cos(a) * 4f; val y = cy + sin(a) * 5f
            if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
        }
        hexPath.close()
        canvas.drawPath(hexPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(255,64,56,0); style=Paint.Style.FILL })
        canvas.drawPath(hexPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.parseColor("#FFE11A"); style=Paint.Style.STROKE; strokeWidth=1f })
        // Antennae
        val antPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(178,255,225,26); style=Paint.Style.STROKE; strokeWidth=0.75f }
        canvas.drawLine(cx, cy-5f, cx-3f, cy-9f, antPaint)
        canvas.drawLine(cx, cy-5f, cx+3f, cy-9f, antPaint)
        drawHealthBar(canvas, cx, cy, 5f, e.currentHp, e.maxHp, barWidth = 14f, barHeight = 3f)
    }

    private fun drawGhostEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        // Soft circle radius 10, semi-transparent purple (DESIGN_SPEC Section 4.5)
        val r = 10f
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=Color.argb(166,140,89,204); style=Paint.Style.FILL
        })
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=Color.argb(204,191,140,255); style=Paint.Style.STROKE; strokeWidth=1f
        })
        // Wispy trail
        val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(127,140,89,204); style=Paint.Style.FILL }
        canvas.drawOval(RectF(cx-5f, cy+r, cx+5f, cy+r+6f), trailPaint)
        val trailPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(76,140,89,204); style=Paint.Style.FILL }
        canvas.drawOval(RectF(cx-4f, cy+r+6f, cx+4f, cy+r+10f), trailPaint2)
        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = 22f, barHeight = 4f)
    }

    private fun drawBossEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        val variant = e.bossVariant
        val aa = Paint.ANTI_ALIAS_FLAG
        val hpRatio = if (e.maxHp > 0f) e.currentHp / e.maxHp else 0f

        // Phase color shift for all bosses (DESIGN_SPEC Section 4.8)
        val phaseAlphaBoost = when {
            hpRatio < 0.40f -> (255 * (0.5f + 0.5f * sin(System.currentTimeMillis() * 0.005f).toFloat())).toInt()
            else -> 255
        }

        when (variant) {
            0 -> {
                // Rift Guardian — large circle, radius 18, segmented ring (DESIGN_SPEC 4.8.1)
                canvas.drawCircle(cx, cy, 18f, Paint(aa).apply { color=Color.argb(255,56,10,89); style=Paint.Style.FILL })
                canvas.drawCircle(cx, cy, 18f, Paint(aa).apply { color=Color.parseColor("#B333FF"); style=Paint.Style.STROKE; strokeWidth=2.5f })
                // Outer segmented ring — 8 arc segments with gaps
                val segPaint = Paint(aa).apply { color=Color.argb(153,178,51,255); style=Paint.Style.STROKE; strokeWidth=2f }
                val rot = (System.currentTimeMillis() % 6000L) / 6000f * 360f
                for (i in 0 until 8) {
                    canvas.drawArc(RectF(cx-22f,cy-22f,cx+22f,cy+22f),
                        rot + i*45f, 38f, false, segPaint)
                }
                // Center eye
                canvas.drawCircle(cx, cy, 5f, Paint(aa).apply { color=Color.parseColor("#E64DFF"); style=Paint.Style.FILL })
                // Shell indicator if active
                if (e.bossShellActive) {
                    canvas.drawCircle(cx, cy, 20f, Paint(aa).apply { color=Color.argb(128,255,255,255); style=Paint.Style.STROKE; strokeWidth=3f })
                }
            }
            1 -> {
                // Iron Colossus — heavy square 30×30 (DESIGN_SPEC 4.8.2)
                val half = 15f
                canvas.drawRect(cx-half, cy-half, cx+half, cy+half,
                    Paint(aa).apply { color=Color.argb(255,46,46,51); style=Paint.Style.FILL })
                canvas.drawRect(cx-half, cy-half, cx+half, cy+half,
                    Paint(aa).apply { color=Color.parseColor("#8C8C99"); style=Paint.Style.STROKE; strokeWidth=3f })
                // Armor shell overlay
                val shellStroke = if (e.bossShellActive) Color.argb(255,255,255,255) else Color.argb(102,140,140,153)
                canvas.drawRect(cx-half-3f, cy-half-3f, cx+half+3f, cy+half+3f,
                    Paint(aa).apply { color=shellStroke; style=Paint.Style.STROKE; strokeWidth=1.5f })
                // Corner rivets
                for ((rx,ry) in listOf(Pair(-11f,-11f),Pair(11f,-11f),Pair(-11f,11f),Pair(11f,11f))) {
                    canvas.drawCircle(cx+rx, cy+ry, 3f, Paint(aa).apply { color=Color.argb(255,140,140,153); style=Paint.Style.FILL })
                }
                // Orange reactor core
                val coreAlpha = (153 + (102 * sin(System.currentTimeMillis() * 0.008f).toFloat()).toInt()).coerceIn(100, 255)
                canvas.drawRect(cx-5f, cy-5f, cx+5f, cy+5f,
                    Paint(aa).apply { color=Color.argb(coreAlpha,255,102,0); style=Paint.Style.FILL })
            }
            2 -> {
                // Swarm Queen — hexagon radius 18 (DESIGN_SPEC 4.8.3)
                val hexPath = Path()
                for (i in 0 until 6) {
                    val a = (i * 60f) * (PI/180f).toFloat()
                    val x = cx + cos(a)*18f; val y = cy + sin(a)*18f
                    if (i==0) hexPath.moveTo(x,y) else hexPath.lineTo(x,y)
                }
                hexPath.close()
                canvas.drawPath(hexPath, Paint(aa).apply { color=Color.argb(255,71,36,0); style=Paint.Style.FILL })
                canvas.drawPath(hexPath, Paint(aa).apply { color=Color.parseColor("#FFA619"); style=Paint.Style.STROKE; strokeWidth=2f })
                // Egg sacs
                val sacT = System.currentTimeMillis() % 2400L
                for ((i, angle) in listOf(30f,150f,270f).withIndex()) {
                    val rad = angle * (PI/180f).toFloat()
                    val sx = cx + cos(rad)*18f; val sy = cy + sin(rad)*18f
                    val sacScale = 1f + 0.2f * sin((sacT + i*800L) * 0.003f).toFloat()
                    canvas.save(); canvas.scale(sacScale, sacScale, sx, sy)
                    canvas.drawCircle(sx, sy, 4f, Paint(aa).apply { color=Color.argb(204,204,102,0); style=Paint.Style.FILL })
                    canvas.restore()
                }
            }
            3 -> {
                // Phase Runner — elongated octagon 14×24 (DESIGN_SPEC 4.8.4)
                val phaseAlpha = (127 + (61 * sin(System.currentTimeMillis() * 0.004f).toFloat()).toInt()).coerceIn(80, 242)
                val ep = Path()
                val cut = 4f
                ep.moveTo(cx-7f+cut, cy-12f); ep.lineTo(cx+7f-cut, cy-12f)
                ep.lineTo(cx+7f, cy-12f+cut); ep.lineTo(cx+7f, cy+12f-cut)
                ep.lineTo(cx+7f-cut, cy+12f); ep.lineTo(cx-7f+cut, cy+12f)
                ep.lineTo(cx-7f, cy+12f-cut); ep.lineTo(cx-7f, cy-12f+cut)
                ep.close()
                canvas.drawPath(ep, Paint(aa).apply { color=Color.argb(phaseAlpha,0,71,97); style=Paint.Style.FILL })
                canvas.drawPath(ep, Paint(aa).apply { color=Color.argb(phaseAlpha,0,230,255); style=Paint.Style.STROKE; strokeWidth=2f })
                // Phase trail
                canvas.drawPath(ep, Paint(aa).apply { color=Color.argb(76,0,230,255); style=Paint.Style.FILL })
            }
            4 -> {
                // Void Titan — large circle radius 22 (DESIGN_SPEC 4.8.5)
                canvas.drawCircle(cx, cy, 22f, Paint(aa).apply { color=Color.argb(255,15,5,26); style=Paint.Style.FILL })
                canvas.drawCircle(cx, cy, 22f, Paint(aa).apply { color=Color.parseColor("#591A8C"); style=Paint.Style.STROKE; strokeWidth=3f })
                // Concentric void aura rings
                val ringTime = System.currentTimeMillis()
                for ((ri, rr) in listOf(26f,30f,34f).withIndex()) {
                    val rotA = (ringTime * (0.001f + ri*0.0005f)) % (2*PI).toFloat()
                    canvas.drawArc(RectF(cx-rr,cy-rr,cx+rr,cy+rr),
                        Math.toDegrees(rotA.toDouble()).toFloat(), 120f, false,
                        Paint(aa).apply { color=Color.argb((76-ri*15).coerceAtLeast(20),89,26,140); style=Paint.Style.STROKE; strokeWidth=1f })
                }
                // Singularity core
                canvas.drawCircle(cx, cy, 6f, Paint(aa).apply { color=Color.BLACK; style=Paint.Style.FILL })
            }
        }

        // Boss wide health bar — always on top
        drawHealthBar(canvas, cx, cy, 28f, e.currentHp, e.maxHp, barWidth = 60f, barHeight = 7f)
    }

    private fun drawSplitterEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        // Diamond (rhombus) 18×18, deep amber (DESIGN_SPEC Section 4.6)
        val r = 9f
        val path = Path().apply {
            moveTo(cx, cy-r); lineTo(cx+r, cy); lineTo(cx, cy+r); lineTo(cx-r, cy); close()
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(255,77,36,0); style=Paint.Style.FILL })
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.parseColor("#FF9919"); style=Paint.Style.STROKE; strokeWidth=1.5f })
        // Glowing split seam
        val seamPulse = ((System.currentTimeMillis() % 800L) / 800f)
        val seamAlpha = (153 + (102 * sin(seamPulse * 2 * PI.toFloat())).toInt()).coerceIn(100, 255)
        canvas.drawLine(cx, cy-r, cx, cy+r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=Color.argb(seamAlpha,255,204,51); style=Paint.Style.STROKE; strokeWidth=1.5f
        })
        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = 20f, barHeight = 4f)
    }

    private fun drawJumperEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        // Circle radius 9, dark teal (DESIGN_SPEC Section 4.7)
        val r = 9f
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(255,0,89,97); style=Paint.Style.FILL })
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.parseColor("#1ACCCC"); style=Paint.Style.STROKE; strokeWidth=1.5f })
        // Jump charge ring (faint)
        canvas.drawCircle(cx, cy, r+4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(76,26,204,204); style=Paint.Style.STROKE; strokeWidth=1f })
        // Coil spring legs (zigzag lines below)
        val springPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(204,26,204,204); style=Paint.Style.STROKE; strokeWidth=1f }
        canvas.drawLine(cx-3f, cy+r, cx-1f, cy+r+3f, springPaint)
        canvas.drawLine(cx-1f, cy+r+3f, cx-3f, cy+r+6f, springPaint)
        canvas.drawLine(cx+1f, cy+r, cx+3f, cy+r+3f, springPaint)
        canvas.drawLine(cx+3f, cy+r+3f, cx+1f, cy+r+6f, springPaint)
        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = 20f, barHeight = 4f)
    }

    private fun drawHealerEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        val r = 12f
        val healRadius = r * 6f  // visual aura radius

        // Pulsing aura ring — slow pulse every 2.5s cycle
        val pulseT = ((System.currentTimeMillis() % 2500L) / 2500f)
        val pulseAlpha = (51 + (51 * sin(pulseT * 2 * PI.toFloat())).toInt()).coerceIn(20, 102)
        val auraPaint = Paint(enemyHealerAuraPaint).apply { alpha = pulseAlpha }
        canvas.drawCircle(cx, cy, healRadius, auraPaint)

        // Outer donut ring (hollow circle look)
        canvas.drawCircle(cx, cy, r, enemyHealerStrokePaint)
        // Inner hollow — darker fill
        val innerPaint = Paint().apply {
            color = Color.parseColor("#1A5C38"); style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, r * 0.55f, innerPaint)
        // Outer stroke
        canvas.drawCircle(cx, cy, r, enemyHealerStrokePaint)

        // Fill center with lighter green
        canvas.drawCircle(cx, cy, r * 0.55f, enemyHealerPaint)

        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = 26f, barHeight = 5f)
    }

    private fun drawPhantomEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        // 5-pointed star shape — semi-transparent violet
        val outerR = 12f
        val innerR = 5f
        val path = Path()
        for (i in 0 until 10) {
            val angle = (i * 36f - 90f) * (PI / 180f).toFloat()
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + cos(angle) * r
            val y = cy + sin(angle) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        // Dodge flash: briefly show white
        if (e.dodgeFlashing) {
            val flashPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; alpha = 204 }
            canvas.drawPath(path, flashPaint)
        } else {
            canvas.drawPath(path, enemyPhantomPaint)
            canvas.drawPath(path, enemyPhantomStrokePaint)
        }

        drawHealthBar(canvas, cx, cy, outerR, e.currentHp, e.maxHp, barWidth = 24f, barHeight = 4f)
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
