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
import kotlin.math.min

/**
 * GameRenderer — Canvas-based SurfaceView renderer. iOS parity for all 6 enemy types,
 * level badges, range circles, boss variants, Rift Shift flash, and path rendering.
 */
class GameRenderer(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    // ---- Density helpers — converts dp/sp values to physical pixels ----
    private val density: Float = context.resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun sp(v: Float): Float = v * density

    // ---- Game state (updated from Compose thread, read from RenderThread) ----
    @Volatile var enemies: List<EnemyInstance> = emptyList()
    @Volatile var towerInstances: Map<Int, TowerInstance> = emptyMap()
    @Volatile var slotPositions: List<PointF> = emptyList()
    @Volatile var slotOccupied: Map<Int, Boolean> = emptyMap()
    @Volatile var selectedSlotId: Int? = null
    @Volatile var riftShiftActive: Boolean = false

    // ---- Projectile system — simple fire effect, iOS parity ----
    data class Projectile(
        val id: Long,
        val fromX: Float,
        val fromY: Float,
        val toX: Float,
        val toY: Float,
        val progress: Float,      // 0f → 1f; removed when ≥ 1f
        val type: TowerType,
        val createdAt: Long = System.currentTimeMillis()
    )

    @Volatile var projectiles: List<Projectile> = emptyList()

    // ---- Paints ----
    private val backgroundPaint = Paint().apply { color = Color.parseColor("#0A0A0F") }

    // Grid tiles (checkerboard) — iOS parity: gridColor1=(0.07,0.07,0.10)=#121219, gridColor2=(0.09,0.09,0.13)=#171721
    private val gridPaint1 = Paint().apply { color = Color.argb(255, 18, 18, 25); style = Paint.Style.FILL }
    private val gridPaint2 = Paint().apply { color = Color.argb(255, 23, 23, 33); style = Paint.Style.FILL }
    private val gridBorderPaint = Paint().apply {
        color = Color.argb(77, 38, 38, 51)   // iOS: #262633 @ alpha 0.30 (77/255)
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
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

    // Rift Shift overlay — alpha kept minimal (8) since Compose RiftShiftOverlay handles the main visual
    // Keeping a very subtle renderer flash avoids double-darkening while preserving edge blending
    private val riftFlashPaint = Paint().apply {
        color = Color.parseColor("#8C4FFF")
        style = Paint.Style.FILL
        alpha = 8
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

    /** Apply density-scaled sizes to all paints after density is available. */
    private fun initPaintSizes() {
        // Path paints
        pathStrokePaint.strokeWidth = dp(3f)
        pathEdgeInnerPaint.strokeWidth = dp(2f)
        pathEdgeOuterPaint.strokeWidth = dp(1f)
        pathJointStrokePaint.strokeWidth = dp(1.5f)

        // Slot paints
        slotBorderPaint.strokeWidth = dp(1.5f)

        // Tower range ring
        towerRangePaint.strokeWidth = dp(2f)

        // Level badge / indicator text
        badgeTextPaint.textSize = sp(12f)
        badgeBorderPaint.strokeWidth = dp(1f)
        indicatorTextPaint.textSize = sp(13f)

        // Enemy paints with stroke
        enemyShieldRingPaint.strokeWidth = dp(3f)
        enemyHealerStrokePaint.strokeWidth = dp(2f)
        enemyPhantomStrokePaint.strokeWidth = dp(1.5f)

        // Boss stroke paints
        bossStrokePaints[0].strokeWidth = dp(3f)
        bossStrokePaints[1].strokeWidth = dp(3f)
        bossStrokePaints[2].strokeWidth = dp(3f)
        bossStrokePaints[3].strokeWidth = dp(2f)
        bossStrokePaints[4].strokeWidth = dp(4f)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        initPaintSizes()
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
        drawProjectiles(canvas)

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

    // Path fill and edge paints — DESIGN_SPEC_BUILD5_3 Section 2 (Build 5.3)
    private val pathCorridorFillPaint = Paint().apply {
        color = Color.argb(255, 56, 50, 79)   // (0.22, 0.20, 0.32) — violet-slate, clearly distinct from grid
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    // Inner glow line (each edge): 2pt width, alpha=0x8C≈55%
    private val pathEdgeInnerPaint = Paint().apply {
        color = Color.argb(0x8C, 0, 200, 255)   // (0.00, 0.78, 1.00, 0.55) — primary cyan glow
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    // Outer glow line (each edge): 1pt width, alpha=0x40≈25%
    private val pathEdgeOuterPaint = Paint().apply {
        color = Color.argb(0x40, 0, 200, 255)   // (0.00, 0.78, 1.00, 0.25) — soft falloff
        style = Paint.Style.STROKE
        strokeWidth = 1.0f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val pathTexturePaint = Paint().apply {
        color = Color.argb(31, 255, 255, 255)   // (1.0, 1.0, 1.0, 0.12) — near-white tick marks
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
        isAntiAlias = true
    }
    // Waypoint joint stroke — alpha 50%, blue accent
    private val pathJointStrokePaint = Paint().apply {
        color = Color.argb(0x80, 0, 158, 255)   // (0.00, 0.62, 1.00, 0.50)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
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

    // Render the path as filled corridor with dual-edge glow lines and texture (Build 5.3 path visibility spec)
    private fun drawPath(canvas: Canvas, W: Float, H: Float) {
        val wps = PathSystem.waypoints
        if (wps.size < 2) return
        val halfWidth = dp(8.5f)  // 17dp corridor = 8.5dp each side, scaled to pixels

        // All segments: violet-slate fill + 2-line glow per edge (inner 2pt@55%, outer 1pt@25%)
        for (i in 1 until wps.size) {
            val from = wps[i - 1]; val to = wps[i]
            val seg = buildCorridorSegment(from, to, halfWidth)
            canvas.drawPath(seg, pathCorridorFillPaint)

            val dx = to.x - from.x; val dy = to.y - from.y
            val len = sqrt(dx * dx + dy * dy)
            if (len > 0f) {
                val px = -dy / len * halfWidth; val py = dx / len * halfWidth

                // Left edge — inner glow (2pt) + outer glow (1pt)
                canvas.drawLine(from.x + px, from.y + py, to.x + px, to.y + py, pathEdgeInnerPaint)
                canvas.drawLine(from.x + px, from.y + py, to.x + px, to.y + py, pathEdgeOuterPaint)

                // Right edge — inner glow (2pt) + outer glow (1pt)
                canvas.drawLine(from.x - px, from.y - py, to.x - px, to.y - py, pathEdgeInnerPaint)
                canvas.drawLine(from.x - px, from.y - py, to.x - px, to.y - py, pathEdgeOuterPaint)

                // Texture tick marks — perpendicular, every 8pt along length (optional, near-white 12%)
                val steps = (len / 8f).toInt().coerceAtMost(24)
                for (s in 0..steps) {
                    val t = if (steps > 0) s.toFloat() / steps else 0f
                    val mx = from.x + dx * t; val my = from.y + dy * t
                    val tickHalf = halfWidth * 0.5f
                    val nx = -dy / len * tickHalf; val ny = dx / len * tickHalf
                    canvas.drawLine(mx + nx, my + ny, mx - nx, my - ny, pathTexturePaint)
                }
            }
        }

        // Waypoint joints (circular caps) — fill with path color, stroke with blue accent (Build 5.3)
        val jointFillPaint = Paint(pathCorridorFillPaint)
        for (i in wps.indices) {
            canvas.drawCircle(wps[i].x, wps[i].y, halfWidth, jointFillPaint)
            canvas.drawCircle(wps[i].x, wps[i].y, halfWidth, pathJointStrokePaint)
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
        paint.strokeWidth = dp(1.5f)
        paint.color = android.graphics.Color.argb((230 * (0.25f + 0.75f * (1f - pulse))).toInt().coerceIn(0, 255), 0, 200, 255)
        canvas.drawCircle(pos.x, pos.y, dp(13f) + dp(6f) * pulse, paint)

        // Inner ring — neon blue steady
        paint.strokeWidth = dp(1.5f)
        paint.color = android.graphics.Color.argb(178, 0, 200, 255)
        canvas.drawCircle(pos.x, pos.y, dp(7f), paint)

        // Right-pointing chevron arrow
        val ap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(255, 0, 200, 255)
            style = Paint.Style.STROKE
            strokeWidth = dp(2f)
            strokeCap = Paint.Cap.ROUND
        }
        val path = android.graphics.Path().apply {
            moveTo(pos.x + dp(4f), pos.y)
            lineTo(pos.x - dp(2f), pos.y - dp(5f))
            moveTo(pos.x + dp(4f), pos.y)
            lineTo(pos.x - dp(2f), pos.y + dp(5f))
        }
        canvas.drawPath(path, ap)
    }

    private fun drawExitIndicator(canvas: Canvas, pos: PointF) {
        val t = ((System.currentTimeMillis() % 800L) / 800f)
        val pulse = (0.5f + 0.5f * kotlin.math.sin(t * 2 * Math.PI)).toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 6 dashed arc segments — red danger ring
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(2.5f)
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = android.graphics.Color.argb(217, 255, 43, 84)
        val r = dp(13f)
        val oval = RectF(pos.x - r, pos.y - r, pos.x + r, pos.y + r)
        val dashCount = 6
        val sweep = 360f / dashCount
        for (i in 0 until dashCount) {
            canvas.drawArc(oval, i * sweep, sweep * 0.55f, false, paint)
        }

        // Pulsing core dot — danger
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.argb((89 + (89 * pulse).toInt()).coerceIn(0, 255), 255, 43, 84)
        canvas.drawCircle(pos.x, pos.y, dp(5f), paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(1.5f)
        paint.color = android.graphics.Color.argb(204, 255, 43, 84)
        canvas.drawCircle(pos.x, pos.y, dp(5f), paint)

        // Right-pointing chevron arrow — red
        val ap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(255, 255, 43, 84)
            style = Paint.Style.STROKE
            strokeWidth = dp(2f)
            strokeCap = Paint.Cap.ROUND
        }
        val path = android.graphics.Path().apply {
            moveTo(pos.x + dp(4f), pos.y)
            lineTo(pos.x - dp(2f), pos.y - dp(5f))
            moveTo(pos.x + dp(4f), pos.y)
            lineTo(pos.x - dp(2f), pos.y + dp(5f))
        }
        canvas.drawPath(path, ap)
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

    // Tower slot backgrounds — iOS parity: 32×32pt rounded square, cornerRadius 5, cross 13×2 + 2×13, 4 corner dots at 15pt offset (45°/135°/225°/315°)
    private fun drawTowerSlots(canvas: Canvas) {
        val slotSize = dp(32f)          // iOS: 32pt square
        val cornerR = dp(5f)            // iOS: cornerRadius 5
        val halfSize = slotSize / 2f
        val crossW = dp(13f); val crossH = dp(2f)
        val dotR = dp(1.5f)
        val dotOffset = dp(15f)         // iOS: 15pt from center

        val pulseT = ((System.currentTimeMillis() % 2400L) / 2400f).toFloat()
        // pulse alpha 0.40 → 0.65 (sin wave), matching iOS: alpha 0.40 → 0.65, 1.2s cycle
        val pulseAlpha = (0.40f + 0.25f * sin(pulseT * 2 * PI.toFloat())).toFloat().coerceIn(0.40f, 0.65f)

        for ((idx, pos) in slotPositions.withIndex()) {
            if (slotOccupied[idx] == true) continue
            val cx = pos.x; val cy = pos.y
            val isSelected = selectedSlotId == idx
            val aa = Paint.ANTI_ALIAS_FLAG

            // Background fill — iOS: (0.05, 0.09, 0.14) = argb(255,13,23,36) normal; (0,0.18,0.28)=argb(255,0,46,71) selected
            val fillColor = if (isSelected) Color.argb(255, 0, 46, 71) else Color.argb(255, 13, 23, 36)
            val fillPaint = Paint(aa).apply { color = fillColor; style = Paint.Style.FILL }
            val rect = RectF(cx - halfSize, cy - halfSize, cx + halfSize, cy + halfSize)
            canvas.drawRoundRect(rect, cornerR, cornerR, fillPaint)

            // Border stroke — iOS: (0.0, 0.78, 1.0, 0.55) pulsing; selected: full cyan, 2dp
            val strokeAlpha = if (isSelected) 1.0f else pulseAlpha
            val strokeWidth = if (isSelected) dp(2f) else dp(1f)
            val strokePaint = Paint(aa).apply {
                color = Color.argb((strokeAlpha * 255).toInt().coerceIn(0, 255), 0, 200, 255)
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
            }
            canvas.drawRoundRect(rect, cornerR, cornerR, strokePaint)

            // Inner cross — iOS: H-line 13×2, V-line 2×13, color (0.0,0.78,1.0,0.20) = alpha 51
            val crossPaint = Paint(aa).apply {
                color = Color.argb(51, 0, 200, 255)  // alpha 0.20
                style = Paint.Style.FILL
            }
            // H line
            canvas.drawRect(cx - crossW / 2, cy - crossH / 2, cx + crossW / 2, cy + crossH / 2, crossPaint)
            // V line
            canvas.drawRect(cx - crossH / 2, cy - crossW / 2, cx + crossH / 2, cy + crossW / 2, crossPaint)

            // 4 corner dots at 45°/135°/225°/315° — iOS: radius 1.5pt, color (0.0,0.78,1.0,0.80) = alpha 204
            val dotPaint = Paint(aa).apply {
                color = Color.argb(204, 0, 200, 255)  // alpha 0.80
                style = Paint.Style.FILL
            }
            for (angleDeg in listOf(45f, 135f, 225f, 315f)) {
                val rad = (angleDeg * PI.toFloat() / 180f)
                val dx2 = cos(rad) * dotOffset
                val dy2 = sin(rad) * dotOffset
                canvas.drawCircle(cx + dx2, cy + dy2, dotR, dotPaint)
            }
        }
    }

    // Towers with unique shapes per DESIGN_SPEC_BUILD5 Section 2 (PATHRIFT-162)
    private fun drawTowers(canvas: Canvas) {
        // density field is already available from the class-level dp() helper
        for ((slotId, inst) in towerInstances) {
            val cx = inst.position.x
            val cy = inst.position.y

            // Range ring for selected tower — convert dp range to pixels
            if (selectedSlotId == slotId) {
                val rangePixels = inst.tower.rangeTiles * GridSystem.TILE_SIZE_DP * density
                canvas.drawCircle(cx, cy, rangePixels, towerRangeFillPaint)
                canvas.drawCircle(cx, cy, rangePixels, towerRangePaint)
            }

            // Tower body glow — faint colored aura circle beneath the tower (iOS SpriteKit glow parity)
            val towerGlowColor = towerTypeGlowColor(inst.tower.type)
            val glowPaint = Paint().apply {
                color = towerGlowColor; style = Paint.Style.FILL; isAntiAlias = true; alpha = 51  // 20% alpha
            }
            canvas.drawCircle(cx, cy, dp(18f), glowPaint)

            // Floor shadow ellipse beneath tower
            val shadowPaint = Paint().apply {
                color = Color.argb(89, 0, 0, 0); style = Paint.Style.FILL; isAntiAlias = true
            }
            canvas.drawOval(RectF(cx - dp(14f), cy + dp(12f), cx + dp(14f), cy + dp(17f)), shadowPaint)

            // Rotate canvas to face current target
            val rotationDeg = Math.toDegrees(inst.facingAngle.toDouble()).toFloat() + 90f
            canvas.save()
            canvas.rotate(rotationDeg, cx, cy)

            drawTowerBody(canvas, inst.tower.type, cx, cy)

            canvas.restore()

            // Level badge
            if (inst.level > 1) {
                drawLevelBadge(canvas, cx + dp(14f), cy + dp(14f), inst.level)
            }
        }
    }

    private fun drawTowerBody(canvas: Canvas, type: TowerType, cx: Float, cy: Float) {
        val aa = Paint.ANTI_ALIAS_FLAG
        // Build 5.3: all tower visual dimensions scaled ×0.70. Body max radius 16→11, barrel width 4→3.

        when (type) {
            TowerType.BOLT -> {
                // Hexagon body, flat-top — iOS: radius=11pt, strokeWidth=1.5
                val hexPath = Path()
                for (i in 0 until 6) {
                    val a = (i * 60f) * (PI / 180f).toFloat()
                    val x = cx + cos(a) * dp(11f); val y = cy + sin(a) * dp(11f)
                    if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                }
                hexPath.close()
                canvas.drawPath(hexPath, Paint(aa).apply { color = Color.argb(255,0,31,56); style=Paint.Style.FILL })
                canvas.drawPath(hexPath, Paint(aa).apply { color=Color.parseColor("#00C8FF"); style=Paint.Style.STROKE; strokeWidth=dp(1.5f) })
                // Barrel up — iOS: 3×8pt at y=13 (pointing up)
                canvas.drawRoundRect(RectF(cx-dp(1.5f), cy-dp(17f), cx+dp(1.5f), cy-dp(9f)), dp(1.5f), dp(1.5f),
                    Paint(aa).apply { color=Color.parseColor("#00C8FF"); style=Paint.Style.FILL })
                // Circuit trace lines — iOS: 3 diagonals at π/6, π/2, 5π/6 (30°, 90°, 150°)
                val tracePaint = Paint(aa).apply { color=Color.argb(153,0,200,255); style=Paint.Style.STROKE; strokeWidth=dp(0.75f) }
                for (angle in listOf(30f, 90f, 150f)) {
                    val rad = angle * (PI/180f).toFloat()
                    canvas.drawLine(cx, cy, cx + cos(rad)*dp(9f), cy + sin(rad)*dp(9f), tracePaint)
                }
            }
            TowerType.BLAST -> {
                // Circle body — iOS: radius=11pt, strokeWidth=1.5, fill=(0.20,0.08,0.00), stroke=(1.00,0.45,0.00)
                canvas.drawCircle(cx, cy, dp(11f), Paint(aa).apply { color=Color.argb(255,51,20,0); style=Paint.Style.FILL })
                canvas.drawCircle(cx, cy, dp(11f), Paint(aa).apply { color=Color.parseColor("#FF7300"); style=Paint.Style.STROKE; strokeWidth=dp(1.5f) })
                // Barrel — iOS: 3×8pt at y=13 (pointing up)
                canvas.drawRoundRect(RectF(cx-dp(1.5f), cy-dp(17f), cx+dp(1.5f), cy-dp(9f)), dp(1f), dp(1f),
                    Paint(aa).apply { color=Color.parseColor("#FF7300"); style=Paint.Style.FILL })
                // Flared tip — iOS: 7×2pt at y=17
                canvas.drawRect(cx-dp(3.5f), cy-dp(19.5f), cx+dp(3.5f), cy-dp(18.5f),
                    Paint(aa).apply { color=Color.argb(204,255,115,0); style=Paint.Style.FILL })
                // 3 exhaust pipe stubs at 120° intervals — iOS: 3 angles (7π/6, 11π/6, π/6)
                val exhaustAngles = listOf(210f, 330f, 30f)   // π*7/6, π*11/6, π/6 in degrees
                val exhaustPaint = Paint(aa).apply { color=Color.argb(255,38,15,0); style=Paint.Style.FILL }
                val exhaustStroke = Paint(aa).apply { color=Color.parseColor("#FF7300"); style=Paint.Style.STROKE; strokeWidth=dp(0.5f) }
                for (angle in exhaustAngles) {
                    val rad = angle * (PI/180f).toFloat()
                    val ex = cx + cos(rad)*dp(11f); val ey = cy + sin(rad)*dp(11f)
                    canvas.drawRoundRect(RectF(ex-dp(2f), ey-dp(3f), ex+dp(2f), ey+dp(3f)), dp(1f), dp(1f), exhaustPaint)
                    canvas.drawRoundRect(RectF(ex-dp(2f), ey-dp(3f), ex+dp(2f), ey+dp(3f)), dp(1f), dp(1f), exhaustStroke)
                }
            }
            TowerType.FROST -> {
                // Diamond (rhombus) 20×20dp (was 28×28, ×0.70)
                val dPath = Path()
                dPath.moveTo(cx, cy-dp(10f)); dPath.lineTo(cx+dp(10f), cy); dPath.lineTo(cx, cy+dp(10f)); dPath.lineTo(cx-dp(10f), cy)
                dPath.close()
                canvas.drawPath(dPath, Paint(aa).apply { color=Color.argb(255,15,5,36); style=Paint.Style.FILL })
                canvas.drawPath(dPath, Paint(aa).apply { color=Color.parseColor("#8F2EFF"); style=Paint.Style.STROKE; strokeWidth=dp(1.0f) })
                // Barrel — 1.5dp wide (was 2dp), 7dp long (was 10dp)
                canvas.drawRect(cx-dp(1.5f), cy-dp(15.5f), cx+dp(1.5f), cy-dp(8.5f), Paint(aa).apply { color=Color.parseColor("#8F2EFF"); style=Paint.Style.FILL })
                // Crystal spike tips at 4 diamond corners
                val spikePaint = Paint(aa).apply { color=Color.argb(178,179,217,255); style=Paint.Style.FILL }
                for ((dx2,dy2) in listOf(Pair(0f,-dp(10f)),Pair(dp(10f),0f),Pair(0f,dp(10f)),Pair(-dp(10f),0f))) {
                    val sp = Path()
                    sp.moveTo(cx+dx2-dp(1.5f), cy+dy2); sp.lineTo(cx+dx2+dp(1.5f), cy+dy2)
                    sp.lineTo(cx+dx2, cy+dy2 + (if (dy2 < 0) -dp(4f) else if (dy2 > 0) dp(4f) else 0f) + (if (dx2 < 0) -dp(4f) else if (dx2 > 0) dp(4f) else 0f))
                    sp.close()
                    canvas.drawPath(sp, spikePaint)
                }
            }
            TowerType.PIERCE -> {
                // Elongated octagon — iOS: w=5.5, h=9, cut=3.5, fill=(0.04,0.14,0.02), stroke=(0.40,1.00,0.10)
                val cut = dp(3.5f)
                val w = dp(5.5f); val h = dp(9f)
                val ep = Path()
                ep.moveTo(cx-w+cut, cy-h); ep.lineTo(cx+w-cut, cy-h)
                ep.lineTo(cx+w, cy-h+cut); ep.lineTo(cx+w, cy+h-cut)
                ep.lineTo(cx+w-cut, cy+h); ep.lineTo(cx-w+cut, cy+h)
                ep.lineTo(cx-w, cy+h-cut); ep.lineTo(cx-w, cy-h+cut)
                ep.close()
                canvas.drawPath(ep, Paint(aa).apply { color=Color.argb(255,10,36,5); style=Paint.Style.FILL })
                canvas.drawPath(ep, Paint(aa).apply { color=Color.parseColor("#66FF1A"); style=Paint.Style.STROKE; strokeWidth=dp(1.0f) })
                // Barrel — iOS: 3×10pt at y=13 (pointing up)
                canvas.drawRoundRect(RectF(cx-dp(1.5f), cy-dp(19f), cx+dp(1.5f), cy-dp(9f)), dp(1f), dp(1f),
                    Paint(aa).apply { color=Color.parseColor("#66FF1A"); style=Paint.Style.FILL })
                // Reticle sight lines — iOS: 2 rects at y=±1.5
                val reticlePaint = Paint(aa).apply { color=Color.argb(127,102,255,26); style=Paint.Style.STROKE; strokeWidth=dp(0.75f) }
                canvas.drawLine(cx-dp(4f), cy-dp(1.5f), cx+dp(4f), cy-dp(1.5f), reticlePaint)
                canvas.drawLine(cx-dp(4f), cy+dp(1.5f), cx+dp(4f), cy+dp(1.5f), reticlePaint)
            }
            TowerType.CORE -> {
                // Square 18×18 — iOS: fill=(0.18,0.05,0.00), stroke=(1.00,0.35,0.05), strokeWidth=1.5
                canvas.drawRect(cx-dp(9f), cy-dp(9f), cx+dp(9f), cy+dp(9f), Paint(aa).apply { color=Color.argb(255,46,13,0); style=Paint.Style.FILL })
                canvas.drawRect(cx-dp(9f), cy-dp(9f), cx+dp(9f), cy+dp(9f), Paint(aa).apply { color=Color.parseColor("#FF590D"); style=Paint.Style.STROKE; strokeWidth=dp(1.5f) })
                // Barrel — iOS: 5×7pt at y=11 (pointing up), cornerRadius=1
                canvas.drawRoundRect(RectF(cx-dp(2.5f), cy-dp(16f), cx+dp(2.5f), cy-dp(9f)), dp(1f), dp(1f),
                    Paint(aa).apply { color=Color.parseColor("#FF590D"); style=Paint.Style.FILL })
                // Corner rivets
                val rivetFill = Paint(aa).apply { color=Color.argb(255,230,71,10); style=Paint.Style.FILL }
                val rivetStroke = Paint(aa).apply { color=Color.parseColor("#FF590D"); style=Paint.Style.STROKE; strokeWidth=dp(1f) }
                for ((rx,ry) in listOf(Pair(-dp(6f),-dp(6f)),Pair(dp(6f),-dp(6f)),Pair(-dp(6f),dp(6f)),Pair(dp(6f),dp(6f)))) {
                    canvas.drawCircle(cx+rx, cy+ry, dp(1.5f), rivetFill)
                    canvas.drawCircle(cx+rx, cy+ry, dp(1.5f), rivetStroke)
                }
                // Diagonal cross
                val crossPaint = Paint(aa).apply { color=Color.argb(76,255,89,13); style=Paint.Style.STROKE; strokeWidth=dp(0.75f) }
                canvas.drawLine(cx-dp(7f), cy-dp(7f), cx+dp(7f), cy+dp(7f), crossPaint)
                canvas.drawLine(cx+dp(7f), cy-dp(7f), cx-dp(7f), cy+dp(7f), crossPaint)
            }
            TowerType.INFERNO -> {
                // Irregular pentagon (flame lean) ~0.70× scale
                val ip = Path()
                ip.moveTo(cx, cy-dp(10f)); ip.lineTo(cx+dp(8.5f), cy-dp(3f)); ip.lineTo(cx+dp(7f), cy+dp(10f))
                ip.lineTo(cx-dp(7.5f), cy+dp(10f)); ip.lineTo(cx-dp(9f), cy-dp(3.5f))
                ip.close()
                canvas.drawPath(ip, Paint(aa).apply { color=Color.argb(255,51,8,0); style=Paint.Style.FILL })
                canvas.drawPath(ip, Paint(aa).apply { color=Color.parseColor("#FF2E14"); style=Paint.Style.STROKE; strokeWidth=dp(1.25f) })
                // Barrel — 3dp wide (was 5dp), 7.5dp long (was 11dp)
                canvas.drawRect(cx-dp(1.5f), cy-dp(15.5f), cx+dp(1.5f), cy-dp(8f), Paint(aa).apply { color=Color.parseColor("#FF2E14"); style=Paint.Style.FILL })
                // Flame tips
                val flamePaint = Paint(aa).apply { color=Color.argb(204,255,140,26); style=Paint.Style.FILL }
                data class FlamePoint(val fx: Float, val fy: Float)
                for (fp in listOf(FlamePoint(cx-dp(3.5f), cy-dp(10f)), FlamePoint(cx, cy-dp(10.5f)), FlamePoint(cx+dp(3.5f), cy-dp(9f)))) {
                    val fp2 = Path(); fp2.moveTo(fp.fx-dp(1.5f), fp.fy); fp2.lineTo(fp.fx+dp(1.5f), fp.fy); fp2.lineTo(fp.fx, fp.fy - dp(3.5f)); fp2.close()
                    canvas.drawPath(fp2, flamePaint)
                }
            }
            TowerType.TESLA -> {
                // Circle, radius 9dp (was 13, ×0.70)
                canvas.drawCircle(cx, cy, dp(9f), Paint(aa).apply { color=Color.argb(255,5,20,46); style=Paint.Style.FILL })
                canvas.drawCircle(cx, cy, dp(9f), Paint(aa).apply { color=Color.parseColor("#33A6FF"); style=Paint.Style.STROKE; strokeWidth=dp(1.0f) })
                // Barrel — 3dp wide (was 5dp), 5.5dp long (was 8dp)
                canvas.drawRect(cx-dp(1.5f), cy-dp(14.5f), cx+dp(1.5f), cy-dp(9f), Paint(aa).apply { color=Color.parseColor("#33A6FF"); style=Paint.Style.FILL })
                // Arc coil rings
                val arcPaint1 = Paint(aa).apply { color=Color.argb(178,51,166,255); style=Paint.Style.STROKE; strokeWidth=dp(1.5f) }
                val arcPaint2 = Paint(aa).apply { color=Color.argb(102,51,166,255); style=Paint.Style.STROKE; strokeWidth=dp(0.75f) }
                canvas.drawArc(RectF(cx-dp(13f), cy-dp(13f), cx+dp(13f), cy+dp(13f)), 210f, 120f, false, arcPaint1)
                canvas.drawArc(RectF(cx-dp(11f), cy-dp(11f), cx+dp(11f), cy+dp(11f)), 240f, 60f, false, arcPaint2)
            }
            TowerType.NOVA -> {
                // 6-pointed star outer=10dp, inner=5dp (was 14/7, ×0.70)
                val starPath = Path()
                for (i in 0 until 12) {
                    val a = (i * 30f - 90f) * (PI / 180f).toFloat()
                    val r = if (i % 2 == 0) dp(10f) else dp(5f)
                    val x = cx + cos(a) * r; val y = cy + sin(a) * r
                    if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                }
                starPath.close()
                canvas.drawPath(starPath, Paint(aa).apply { color=Color.argb(255,46,36,0); style=Paint.Style.FILL })
                canvas.drawPath(starPath, Paint(aa).apply { color=Color.parseColor("#FFD11A"); style=Paint.Style.STROKE; strokeWidth=dp(1.0f) })
                // Center lens
                canvas.drawCircle(cx, cy, dp(3f), Paint(aa).apply { color=Color.argb(153,255,209,26); style=Paint.Style.FILL })
                // Barrel — 3dp wide (was 5dp), 7dp long (was 10dp)
                canvas.drawRect(cx-dp(1.5f), cy-dp(14f), cx+dp(1.5f), cy-dp(7f), Paint(aa).apply { color=Color.parseColor("#FFD11A"); style=Paint.Style.FILL })
            }
            TowerType.SNIPER -> {
                // SNIPER: hexagonal base radius 10dp (was 14) + octagon turret radius 7dp (was 10) + long barrel 3dp×15dp (was 4dp×22dp)
                val hexBasePath = Path()
                for (i in 0 until 6) {
                    val a = (i * 60f) * (PI / 180f).toFloat()
                    val x = cx + cos(a) * dp(10f); val y = cy + sin(a) * dp(10f)
                    if (i == 0) hexBasePath.moveTo(x, y) else hexBasePath.lineTo(x, y)
                }
                hexBasePath.close()
                canvas.drawPath(hexBasePath, Paint(aa).apply { color=Color.argb(255,10,20,26); style=Paint.Style.FILL })
                canvas.drawPath(hexBasePath, Paint(aa).apply { color=Color.parseColor("#66CCCC"); style=Paint.Style.STROKE; strokeWidth=dp(1.0f) })

                // Octagon turret — radius 7dp
                val octTurretPath = Path()
                for (i in 0 until 8) {
                    val a = (i * 45f - 22.5f) * (PI / 180f).toFloat()
                    val x = cx + cos(a) * dp(7f); val y = cy + sin(a) * dp(7f)
                    if (i == 0) octTurretPath.moveTo(x, y) else octTurretPath.lineTo(x, y)
                }
                octTurretPath.close()
                canvas.drawPath(octTurretPath, Paint(aa).apply { color=Color.argb(255,15,26,31); style=Paint.Style.FILL })
                canvas.drawPath(octTurretPath, Paint(aa).apply { color=Color.parseColor("#D9FFFF"); style=Paint.Style.STROKE; strokeWidth=dp(0.9f) })

                // Long barrel — 3dp wide × 15dp long, pointing up (Y-)
                canvas.drawRoundRect(RectF(cx-dp(1.5f), cy-dp(22.5f), cx+dp(1.5f), cy-dp(7f)), dp(1.5f), dp(1.5f),
                    Paint(aa).apply { color=Color.parseColor("#D9FFFF"); style=Paint.Style.FILL })

                // Scope circle on turret body
                val scopeY = cy - dp(1.5f)
                canvas.drawCircle(cx, scopeY, dp(3f), Paint(aa).apply { color=Color.argb(76,153,230,255); style=Paint.Style.FILL })
                canvas.drawCircle(cx, scopeY, dp(3f), Paint(aa).apply { color=Color.parseColor("#D9FFFF"); style=Paint.Style.STROKE; strokeWidth=dp(0.75f) })
                canvas.drawLine(cx-dp(3f), scopeY, cx+dp(3f), scopeY,
                    Paint(aa).apply { color=Color.parseColor("#D9FFFF"); style=Paint.Style.STROKE; strokeWidth=dp(0.6f) })
                canvas.drawLine(cx, scopeY-dp(3f), cx, scopeY+dp(3f),
                    Paint(aa).apply { color=Color.parseColor("#D9FFFF"); style=Paint.Style.STROKE; strokeWidth=dp(0.6f) })
            }
            TowerType.ARTILLERY -> {
                // Wide squat hexagon ~21×15dp (×0.70 of 30×22, flat-top)
                val artPath = Path()
                for (i in 0 until 6) {
                    val a = (i * 60f) * (PI / 180f).toFloat()
                    val x = cx + cos(a) * dp(10.5f); val y = cy + sin(a) * dp(7.5f)  // squashed
                    if (i == 0) artPath.moveTo(x, y) else artPath.lineTo(x, y)
                }
                artPath.close()
                canvas.drawPath(artPath, Paint(aa).apply { color=Color.argb(255,36,26,3); style=Paint.Style.FILL })
                canvas.drawPath(artPath, Paint(aa).apply { color=Color.parseColor("#BF941F"); style=Paint.Style.STROKE; strokeWidth=dp(1.5f) })
                // Barrel — iOS: 5×9pt at y=(hh+4), end plate 7×2pt at y=(hh+8)
                canvas.drawRoundRect(RectF(cx-dp(2.5f), cy-dp(16f), cx+dp(2.5f), cy-dp(7f)), dp(1f), dp(1f),
                    Paint(aa).apply { color=Color.parseColor("#BF941F"); style=Paint.Style.FILL })
                // End plate — iOS: 7×2pt at y=hh+8
                canvas.drawRect(cx-dp(3.5f), cy-dp(16.5f), cx+dp(3.5f), cy-dp(15f), Paint(aa).apply { color=Color.parseColor("#BF941F"); style=Paint.Style.FILL })
                // Armor bands
                val bandPaint = Paint(aa).apply { color=Color.argb(102,191,148,31); style=Paint.Style.FILL }
                canvas.drawRect(cx-dp(8.5f), cy-dp(1.5f), cx+dp(8.5f), cy, bandPaint)
                canvas.drawRect(cx-dp(8.5f), cy+dp(3f), cx+dp(8.5f), cy+dp(4.5f), bandPaint)
            }
        }
    }

    private fun drawLevelBadge(canvas: Canvas, cx: Float, cy: Float, level: Int) {
        val r = dp(9f)
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
            textSize = sp(11f)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(level.toString(), cx, cy + dp(4f), tp)
    }

    /** Returns the ARGB glow color for a tower type — used for the body glow circle. */
    private fun towerTypeGlowColor(type: TowerType): Int = when (type) {
        TowerType.BOLT      -> Color.parseColor("#00C8FF")
        TowerType.BLAST     -> Color.parseColor("#FF6B00")
        TowerType.FROST     -> Color.parseColor("#8B4FFF")
        TowerType.PIERCE    -> Color.parseColor("#CCFF00")
        TowerType.CORE      -> Color.parseColor("#FF4400")
        TowerType.INFERNO   -> Color.parseColor("#FF2200")
        TowerType.TESLA     -> Color.parseColor("#00AAFF")
        TowerType.NOVA      -> Color.parseColor("#FFD700")
        TowerType.SNIPER    -> Color.parseColor("#66FFFF")
        TowerType.ARTILLERY -> Color.parseColor("#CC8800")
    }

    /** Returns the ARGB aura color for an enemy type — drawn as a faint glow before the body. */
    private fun enemyTypeAuraColor(type: EnemyType): Int = when (type) {
        EnemyType.RUNNER   -> Color.parseColor("#3399FF")
        EnemyType.TANK     -> Color.parseColor("#808090")
        EnemyType.BOSS     -> Color.parseColor("#FF2D55")
        EnemyType.SHIELD   -> Color.parseColor("#33D966")
        EnemyType.SWARM    -> Color.parseColor("#FFE11A")
        EnemyType.GHOST    -> Color.parseColor("#BF8BFF")
        EnemyType.SPLITTER -> Color.parseColor("#FF9919")
        EnemyType.JUMPER   -> Color.parseColor("#1ACCCC")
        EnemyType.HEALER   -> Color.parseColor("#2ECC71")
        EnemyType.PHANTOM  -> Color.parseColor("#8B00FF")
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

            // Enemy aura glow — faint colored halo before drawing body (iOS parity ~10% alpha)
            val enemyAuraPaint = Paint().apply {
                color = enemyTypeAuraColor(enemy.type)
                style = Paint.Style.FILL
                isAntiAlias = true
                alpha = 28  // ~11% alpha
            }
            canvas.drawCircle(px, py, dp(10f), enemyAuraPaint)

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
        // Elongated oval / capsule ~7×11dp (×0.70 of 10×16), electric blue (Build 5.3 scale)
        val rw = dp(3.5f); val rh = dp(5.5f)
        canvas.drawOval(RectF(cx-rw, cy-rh, cx+rw, cy+rh),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(255,0,64,179); style=Paint.Style.FILL })
        canvas.drawOval(RectF(cx-rw, cy-rh, cx+rw, cy+rh),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.parseColor("#3399FF"); style=Paint.Style.STROKE; strokeWidth=dp(0.9f) })
        // Motion lines trailing behind (below body)
        val motionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(153,77,179,255); style=Paint.Style.STROKE; strokeWidth=dp(0.75f) }
        canvas.drawLine(cx-dp(3.5f), cy+rh+dp(2f), cx+dp(3.5f), cy+rh+dp(2f), motionPaint)
        canvas.drawLine(cx-dp(5f), cy+rh+dp(4f), cx+dp(5f), cy+rh+dp(4f), motionPaint)
        canvas.drawLine(cx-dp(3f), cy+rh+dp(6f), cx+dp(3f), cy+rh+dp(6f), motionPaint)
        drawHealthBar(canvas, cx, cy, rh, e.currentHp, e.maxHp, barWidth = dp(22f), barHeight = dp(3f))
    }

    private fun drawTankEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        // Heavy angular square 14×14dp (×0.70 of 20×20), gunmetal (Build 5.3 scale)
        val half = dp(7f); val cut = dp(2f)
        val tp = Path()
        tp.moveTo(cx-half+cut, cy-half); tp.lineTo(cx+half-cut, cy-half)
        tp.lineTo(cx+half, cy-half+cut); tp.lineTo(cx+half, cy+half-cut)
        tp.lineTo(cx+half-cut, cy+half); tp.lineTo(cx-half+cut, cy+half)
        tp.lineTo(cx-half, cy+half-cut); tp.lineTo(cx-half, cy-half+cut)
        tp.close()
        canvas.drawPath(tp, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(255,51,46,36); style=Paint.Style.FILL })
        canvas.drawPath(tp, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(255,128,115,89); style=Paint.Style.STROKE; strokeWidth=dp(1.5f) })
        // Armor plate seams
        val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(153,140,128,102); style=Paint.Style.STROKE; strokeWidth=dp(0.75f) }
        canvas.drawLine(cx-dp(5.5f), cy-dp(2f), cx+dp(5.5f), cy-dp(2f), seamPaint)
        canvas.drawLine(cx-dp(5.5f), cy+dp(2f), cx+dp(5.5f), cy+dp(2f), seamPaint)
        drawHealthBar(canvas, cx, cy, half, e.currentHp, e.maxHp, barWidth = dp(22f), barHeight = dp(3f))
    }

    private fun drawShieldEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        val r = dp(6f)  // was 9f, ×0.70 (Build 5.3 scale), now density-scaled
        // Body — dark green circle
        val bodyColor = if (e.shieldBroken) Color.argb(255,153,51,0) else Color.argb(255,10,61,31)
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bodyColor; style = Paint.Style.FILL })
        val strokeColor = if (e.shieldBroken) Color.parseColor("#CC3300") else Color.parseColor("#33D966")
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=strokeColor; style=Paint.Style.STROKE; strokeWidth=dp(1.0f) })

        // Shield aura hexagon (only if shield active) — radius 10dp (was 14, ×0.70)
        val shieldHexRadius = dp(10f)
        if (!e.shieldBroken && e.shieldHp > 0f) {
            val hexPath = Path()
            for (i in 0 until 6) {
                val a = (i * 60f) * (PI/180f).toFloat()
                val x = cx + cos(a) * shieldHexRadius; val y = cy + sin(a) * shieldHexRadius
                if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
            }
            hexPath.close()
            canvas.drawPath(hexPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(38,51,230,102); style=Paint.Style.FILL })
            canvas.drawPath(hexPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(127,51,230,102); style=Paint.Style.STROKE; strokeWidth=dp(1f) })
        }
        drawHealthBar(canvas, cx, cy, if (!e.shieldBroken && e.shieldHp > 0f) shieldHexRadius else r, e.currentHp, e.maxHp, barWidth = dp(22f), barHeight = dp(3f))
    }

    private fun drawSwarmEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        // Small irregular hexagon bug body ~6×7dp (×0.70 of 8×10, Build 5.3 scale)
        val hexPath = Path()
        for (i in 0 until 6) {
            val a = (i * 60f - 90f) * (PI/180f).toFloat()
            val x = cx + cos(a) * dp(3f); val y = cy + sin(a) * dp(3.5f)
            if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
        }
        hexPath.close()
        canvas.drawPath(hexPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(255,64,56,0); style=Paint.Style.FILL })
        canvas.drawPath(hexPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.parseColor("#FFE11A"); style=Paint.Style.STROKE; strokeWidth=dp(0.75f) })
        // Antennae
        val antPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(178,255,225,26); style=Paint.Style.STROKE; strokeWidth=dp(0.75f) }
        canvas.drawLine(cx, cy-dp(3.5f), cx-dp(2f), cy-dp(6.5f), antPaint)
        canvas.drawLine(cx, cy-dp(3.5f), cx+dp(2f), cy-dp(6.5f), antPaint)
        drawHealthBar(canvas, cx, cy, dp(3.5f), e.currentHp, e.maxHp, barWidth = dp(14f), barHeight = dp(3f))
    }

    private fun drawGhostEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        // Soft circle radius 6dp (×0.70 of ~9pt, Build 5.3 scale), semi-transparent purple
        val r = dp(6f)
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=Color.argb(166,140,89,204); style=Paint.Style.FILL
        })
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=Color.argb(204,191,140,255); style=Paint.Style.STROKE; strokeWidth=dp(0.75f)
        })
        // Wispy trail
        val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(127,140,89,204); style=Paint.Style.FILL }
        canvas.drawOval(RectF(cx-dp(3.5f), cy+r, cx+dp(3.5f), cy+r+dp(4f)), trailPaint)
        val trailPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(76,140,89,204); style=Paint.Style.FILL }
        canvas.drawOval(RectF(cx-dp(2.5f), cy+r+dp(4f), cx+dp(2.5f), cy+r+dp(7f)), trailPaint2)
        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = dp(22f), barHeight = dp(3f))
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
                // Rift Guardian — large circle radius 13dp (×0.70 of 18, Build 5.3), segmented ring
                canvas.drawCircle(cx, cy, dp(13f), Paint(aa).apply { color=Color.argb(255,56,10,89); style=Paint.Style.FILL })
                canvas.drawCircle(cx, cy, dp(13f), Paint(aa).apply { color=Color.parseColor("#B333FF"); style=Paint.Style.STROKE; strokeWidth=dp(2.0f) })
                // Outer segmented ring — 8 arc segments with gaps
                val segPaint = Paint(aa).apply { color=Color.argb(153,178,51,255); style=Paint.Style.STROKE; strokeWidth=dp(1.5f) }
                val rot = (System.currentTimeMillis() % 6000L) / 6000f * 360f
                for (i in 0 until 8) {
                    canvas.drawArc(RectF(cx-dp(16f),cy-dp(16f),cx+dp(16f),cy+dp(16f)),
                        rot + i*45f, 38f, false, segPaint)
                }
                // Center eye
                canvas.drawCircle(cx, cy, dp(3.5f), Paint(aa).apply { color=Color.parseColor("#E64DFF"); style=Paint.Style.FILL })
                // Shell indicator if active
                if (e.bossShellActive) {
                    canvas.drawCircle(cx, cy, dp(15f), Paint(aa).apply { color=Color.argb(128,255,255,255); style=Paint.Style.STROKE; strokeWidth=dp(2.5f) })
                }
            }
            1 -> {
                // Iron Colossus — heavy square 21×21dp (×0.70 of 30×30, Build 5.3)
                val half = dp(10.5f)
                canvas.drawRect(cx-half, cy-half, cx+half, cy+half,
                    Paint(aa).apply { color=Color.argb(255,46,46,51); style=Paint.Style.FILL })
                canvas.drawRect(cx-half, cy-half, cx+half, cy+half,
                    Paint(aa).apply { color=Color.parseColor("#8C8C99"); style=Paint.Style.STROKE; strokeWidth=dp(2.5f) })
                // Armor shell overlay
                val shellStroke = if (e.bossShellActive) Color.argb(255,255,255,255) else Color.argb(102,140,140,153)
                canvas.drawRect(cx-half-dp(2f), cy-half-dp(2f), cx+half+dp(2f), cy+half+dp(2f),
                    Paint(aa).apply { color=shellStroke; style=Paint.Style.STROKE; strokeWidth=dp(1.0f) })
                // Corner rivets
                for ((rx,ry) in listOf(Pair(-dp(7.5f),-dp(7.5f)),Pair(dp(7.5f),-dp(7.5f)),Pair(-dp(7.5f),dp(7.5f)),Pair(dp(7.5f),dp(7.5f)))) {
                    canvas.drawCircle(cx+rx, cy+ry, dp(2.0f), Paint(aa).apply { color=Color.argb(255,140,140,153); style=Paint.Style.FILL })
                }
                // Orange reactor core
                val coreAlpha = (153 + (102 * sin(System.currentTimeMillis() * 0.008f).toFloat()).toInt()).coerceIn(100, 255)
                canvas.drawRect(cx-dp(3.5f), cy-dp(3.5f), cx+dp(3.5f), cy+dp(3.5f),
                    Paint(aa).apply { color=Color.argb(coreAlpha,255,102,0); style=Paint.Style.FILL })
            }
            2 -> {
                // Swarm Queen — hexagon radius 13dp (×0.70 of 18, Build 5.3)
                val hexPath = Path()
                for (i in 0 until 6) {
                    val a = (i * 60f) * (PI/180f).toFloat()
                    val x = cx + cos(a)*dp(13f); val y = cy + sin(a)*dp(13f)
                    if (i==0) hexPath.moveTo(x,y) else hexPath.lineTo(x,y)
                }
                hexPath.close()
                canvas.drawPath(hexPath, Paint(aa).apply { color=Color.argb(255,71,36,0); style=Paint.Style.FILL })
                canvas.drawPath(hexPath, Paint(aa).apply { color=Color.parseColor("#FFA619"); style=Paint.Style.STROKE; strokeWidth=dp(1.5f) })
                // Egg sacs
                val sacT = System.currentTimeMillis() % 2400L
                for ((i, angle) in listOf(30f,150f,270f).withIndex()) {
                    val rad = angle * (PI/180f).toFloat()
                    val sx = cx + cos(rad)*dp(13f); val sy = cy + sin(rad)*dp(13f)
                    val sacScale = 1f + 0.2f * sin((sacT + i*800L) * 0.003f).toFloat()
                    canvas.save(); canvas.scale(sacScale, sacScale, sx, sy)
                    canvas.drawCircle(sx, sy, dp(3f), Paint(aa).apply { color=Color.argb(204,204,102,0); style=Paint.Style.FILL })
                    canvas.restore()
                }
            }
            3 -> {
                // Phase Runner — elongated octagon ~10×17dp (×0.70 of 14×24, Build 5.3)
                val phaseAlpha = (127 + (61 * sin(System.currentTimeMillis() * 0.004f).toFloat()).toInt()).coerceIn(80, 242)
                val ep = Path()
                val cut = dp(3f)
                ep.moveTo(cx-dp(5f)+cut, cy-dp(8.5f)); ep.lineTo(cx+dp(5f)-cut, cy-dp(8.5f))
                ep.lineTo(cx+dp(5f), cy-dp(8.5f)+cut); ep.lineTo(cx+dp(5f), cy+dp(8.5f)-cut)
                ep.lineTo(cx+dp(5f)-cut, cy+dp(8.5f)); ep.lineTo(cx-dp(5f)+cut, cy+dp(8.5f))
                ep.lineTo(cx-dp(5f), cy+dp(8.5f)-cut); ep.lineTo(cx-dp(5f), cy-dp(8.5f)+cut)
                ep.close()
                canvas.drawPath(ep, Paint(aa).apply { color=Color.argb(phaseAlpha,0,71,97); style=Paint.Style.FILL })
                canvas.drawPath(ep, Paint(aa).apply { color=Color.argb(phaseAlpha,0,230,255); style=Paint.Style.STROKE; strokeWidth=dp(1.5f) })
                // Phase trail
                canvas.drawPath(ep, Paint(aa).apply { color=Color.argb(76,0,230,255); style=Paint.Style.FILL })
            }
            4 -> {
                // Void Titan — large circle radius 15dp (×0.70 of 22, explicit override per spec, Build 5.3)
                canvas.drawCircle(cx, cy, dp(15f), Paint(aa).apply { color=Color.argb(255,15,5,26); style=Paint.Style.FILL })
                canvas.drawCircle(cx, cy, dp(15f), Paint(aa).apply { color=Color.parseColor("#591A8C"); style=Paint.Style.STROKE; strokeWidth=dp(2.5f) })
                // Concentric void aura rings
                val ringTime = System.currentTimeMillis()
                for ((ri, rr) in listOf(dp(18f),dp(21f),dp(24f)).withIndex()) {
                    val rotA = (ringTime * (0.001f + ri*0.0005f)) % (2*PI).toFloat()
                    canvas.drawArc(RectF(cx-rr,cy-rr,cx+rr,cy+rr),
                        Math.toDegrees(rotA.toDouble()).toFloat(), 120f, false,
                        Paint(aa).apply { color=Color.argb((76-ri*15).coerceAtLeast(20),89,26,140); style=Paint.Style.STROKE; strokeWidth=dp(1f) })
                }
                // Singularity core
                canvas.drawCircle(cx, cy, dp(4.5f), Paint(aa).apply { color=Color.BLACK; style=Paint.Style.FILL })
            }
        }

        // Boss wide health bar — 40×5dp (was 60×7, ×0.70, Build 5.3)
        drawHealthBar(canvas, cx, cy, dp(20f), e.currentHp, e.maxHp, barWidth = dp(40f), barHeight = dp(5f))
    }

    private fun drawSplitterEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        // Diamond (rhombus) ~13×13dp (×0.70 of 18×18, Build 5.3), deep amber
        val r = dp(6.5f)
        val path = Path().apply {
            moveTo(cx, cy-r); lineTo(cx+r, cy); lineTo(cx, cy+r); lineTo(cx-r, cy); close()
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(255,77,36,0); style=Paint.Style.FILL })
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.parseColor("#FF9919"); style=Paint.Style.STROKE; strokeWidth=dp(1.0f) })
        // Glowing split seam
        val seamPulse = ((System.currentTimeMillis() % 800L) / 800f)
        val seamAlpha = (153 + (102 * sin(seamPulse * 2 * PI.toFloat())).toInt()).coerceIn(100, 255)
        canvas.drawLine(cx, cy-r, cx, cy+r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=Color.argb(seamAlpha,255,204,51); style=Paint.Style.STROKE; strokeWidth=dp(1.0f)
        })
        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = dp(20f), barHeight = dp(3f))
    }

    private fun drawJumperEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        // Circle radius 6dp (×0.70 of 9, Build 5.3), dark teal
        val r = dp(6f)
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(255,0,89,97); style=Paint.Style.FILL })
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.parseColor("#1ACCCC"); style=Paint.Style.STROKE; strokeWidth=dp(1.0f) })
        // Jump charge ring (faint)
        canvas.drawCircle(cx, cy, r+dp(3f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(76,26,204,204); style=Paint.Style.STROKE; strokeWidth=dp(0.75f) })
        // Coil spring legs (zigzag lines below)
        val springPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.argb(204,26,204,204); style=Paint.Style.STROKE; strokeWidth=dp(0.75f) }
        canvas.drawLine(cx-dp(2f), cy+r, cx-dp(0.5f), cy+r+dp(2f), springPaint)
        canvas.drawLine(cx-dp(0.5f), cy+r+dp(2f), cx-dp(2f), cy+r+dp(4f), springPaint)
        canvas.drawLine(cx+dp(0.5f), cy+r, cx+dp(2f), cy+r+dp(2f), springPaint)
        canvas.drawLine(cx+dp(2f), cy+r+dp(2f), cx+dp(0.5f), cy+r+dp(4f), springPaint)
        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = dp(20f), barHeight = dp(3f))
    }

    private fun drawHealerEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        val r = dp(8.5f)  // was 12f, ×0.70 (Build 5.3), now density-scaled
        val healRadius = r * 5f  // visual aura radius — relative to r, already scaled

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

        drawHealthBar(canvas, cx, cy, r, e.currentHp, e.maxHp, barWidth = dp(22f), barHeight = dp(3f))
    }

    private fun drawPhantomEnemy(canvas: Canvas, cx: Float, cy: Float, e: EnemyInstance) {
        // 5-pointed star shape — semi-transparent violet (×0.70, Build 5.3)
        val outerR = dp(8.5f)  // was 12f, now density-scaled
        val innerR = dp(3.5f)  // was 5f, now density-scaled
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

        drawHealthBar(canvas, cx, cy, outerR, e.currentHp, e.maxHp, barWidth = dp(22f), barHeight = dp(3f))
    }

    private fun drawHealthBar(
        canvas: Canvas,
        cx: Float, cy: Float,
        enemyRadius: Float,
        currentHp: Float, maxHp: Float,
        barWidth: Float, barHeight: Float
    ) {
        val barLeft = cx - barWidth / 2f
        val barTop = cy - enemyRadius - barHeight - dp(4f)

        canvas.drawRect(barLeft, barTop, barLeft + barWidth, barTop + barHeight, hpBgPaint)

        val ratio = if (maxHp > 0f) (currentHp / maxHp).coerceIn(0f, 1f) else 0f
        val fgPaint = when {
            ratio > 0.6f -> hpGreenPaint
            ratio > 0.3f -> hpYellowPaint
            else         -> hpRedPaint
        }
        canvas.drawRect(barLeft, barTop, barLeft + barWidth * ratio, barTop + barHeight, fgPaint)
    }

    /**
     * Draw all active projectiles — iOS parity: type-specific line/circle effects.
     * Each projectile lives for ~200ms; progress 0→1 drives alpha/position/size.
     */
    private fun drawProjectiles(canvas: Canvas) {
        val now = System.currentTimeMillis()
        val snapshotProj = projectiles
        val alive = mutableListOf<Projectile>()
        for (p in snapshotProj) {
            val age = now - p.createdAt
            val lifetimeMs = if (isAoeType(p.type)) 280L else 180L
            if (age >= lifetimeMs) continue   // expired — drop
            alive.add(p)
            val t = age.toFloat() / lifetimeMs   // 0→1 over lifetime

            when {
                // AoE types: expanding ring at target position — Blast, Nova, Artillery, Inferno
                isAoeType(p.type) -> {
                    val maxRadius = dp(28f)
                    val radius = maxRadius * t
                    val alpha = ((1f - t) * 220).toInt().coerceIn(0, 255)
                    val aoeColor = aoeProjectileColor(p.type)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = aoeColor
                        style = Paint.Style.STROKE
                        strokeWidth = dp(2f)
                        this.alpha = alpha
                    }
                    canvas.drawCircle(p.toX, p.toY, radius, paint)
                    // Inner fill pulse
                    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = aoeColor
                        style = Paint.Style.FILL
                        this.alpha = ((1f - t) * 40).toInt().coerceIn(0, 255)
                    }
                    canvas.drawCircle(p.toX, p.toY, radius * 0.5f, fillPaint)
                }

                // Frost: short blue line + freeze ring at target
                p.type == TowerType.FROST -> {
                    val alpha = ((1f - t) * 220).toInt().coerceIn(0, 255)
                    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(alpha, 143, 79, 255)
                        style = Paint.Style.STROKE
                        strokeWidth = dp(2f)
                        strokeCap = Paint.Cap.ROUND
                    }
                    // Draw bullet travelling from → to
                    val bx = p.fromX + (p.toX - p.fromX) * t
                    val by = p.fromY + (p.toY - p.fromY) * t
                    canvas.drawLine(p.fromX, p.fromY, bx, by, linePaint)
                    // Freeze ring at target
                    val fr = dp(12f) * t
                    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb((alpha * 0.6f).toInt(), 143, 79, 255)
                        style = Paint.Style.STROKE
                        strokeWidth = dp(1.5f)
                    }
                    canvas.drawCircle(p.toX, p.toY, fr, ringPaint)
                }

                // Tesla: jagged lightning line (multi-segment with jitter)
                p.type == TowerType.TESLA -> {
                    val alpha = ((1f - t) * 255).toInt().coerceIn(0, 255)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(alpha, 51, 166, 255)
                        style = Paint.Style.STROKE
                        strokeWidth = dp(1.5f)
                        strokeCap = Paint.Cap.ROUND
                    }
                    val segments = 5
                    val path = android.graphics.Path()
                    path.moveTo(p.fromX, p.fromY)
                    for (s in 1..segments) {
                        val st = s.toFloat() / segments
                        val mx = p.fromX + (p.toX - p.fromX) * st
                        val my = p.fromY + (p.toY - p.fromY) * st
                        val jitter = dp(6f) * (0.5f - (s % 2).toFloat()) * (1f - t)
                        path.lineTo(mx + jitter, my - jitter)
                    }
                    canvas.drawPath(path, paint)
                }

                // Sniper: long fast beam — one bright line drawn fully
                p.type == TowerType.SNIPER -> {
                    val alpha = ((1f - t) * 255).toInt().coerceIn(0, 255)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(alpha, 153, 255, 255)
                        style = Paint.Style.STROKE
                        strokeWidth = dp(1.5f)
                        strokeCap = Paint.Cap.ROUND
                    }
                    canvas.drawLine(p.fromX, p.fromY, p.toX, p.toY, paint)
                }

                // Default line projectile: bullet travels from → to
                else -> {
                    val alpha = ((1f - t) * 220).toInt().coerceIn(0, 255)
                    val projColor = lineProjectileColor(p.type)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = projColor
                        style = Paint.Style.STROKE
                        strokeWidth = dp(2f)
                        strokeCap = Paint.Cap.ROUND
                        this.alpha = alpha
                    }
                    val bx = p.fromX + (p.toX - p.fromX) * t
                    val by = p.fromY + (p.toY - p.fromY) * t
                    // Trail from bullet back to origin (fading)
                    val trailLen = min(1f, t * 3f)
                    val tx = bx - (p.toX - p.fromX) * 0.3f * trailLen
                    val ty = by - (p.toY - p.fromY) * 0.3f * trailLen
                    canvas.drawLine(tx, ty, bx, by, paint)
                    // Bright tip dot
                    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = projColor
                        style = Paint.Style.FILL
                        this.alpha = alpha
                    }
                    canvas.drawCircle(bx, by, dp(2.5f), dotPaint)
                }
            }
        }
        // Update projectile list (drop expired ones)
        projectiles = alive
    }

    private fun isAoeType(type: TowerType): Boolean = when (type) {
        TowerType.BLAST, TowerType.NOVA, TowerType.ARTILLERY, TowerType.INFERNO -> true
        else -> false
    }

    private fun aoeProjectileColor(type: TowerType): Int = when (type) {
        TowerType.BLAST     -> android.graphics.Color.parseColor("#FF7300")
        TowerType.NOVA      -> android.graphics.Color.parseColor("#FFD11A")
        TowerType.ARTILLERY -> android.graphics.Color.parseColor("#BF941F")
        TowerType.INFERNO   -> android.graphics.Color.parseColor("#FF2E14")
        else                -> android.graphics.Color.WHITE
    }

    private fun lineProjectileColor(type: TowerType): Int = when (type) {
        TowerType.BOLT      -> android.graphics.Color.parseColor("#00C8FF")
        TowerType.PIERCE    -> android.graphics.Color.parseColor("#66FF1A")
        TowerType.CORE      -> android.graphics.Color.parseColor("#FF590D")
        else                -> android.graphics.Color.WHITE
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
