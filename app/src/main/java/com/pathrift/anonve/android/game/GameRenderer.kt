package com.pathrift.anonve.android.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.pathrift.anonve.android.game.enemies.EnemyInstance
import com.pathrift.anonve.android.game.enemies.EnemyType
import com.pathrift.anonve.android.game.towers.Tower
import com.pathrift.anonve.android.game.towers.TowerType

/**
 * Custom SurfaceView that handles all game rendering via Android Canvas API.
 * Renders the grid, towers, enemies, health bars, and path highlights at ~60fps.
 */
class GameRenderer(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    // ---- Paints ----
    private val backgroundPaint = Paint().apply { color = Color.parseColor("#0A0A0F") }
    private val gridEmptyPaint = Paint().apply {
        color = Color.parseColor("#12121A")
        style = Paint.Style.FILL
    }
    private val gridBorderPaint = Paint().apply {
        color = Color.parseColor("#1E1E2E")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val pathPaint = Paint().apply {
        color = Color.parseColor("#1A1A28")
        style = Paint.Style.FILL
    }
    private val towerBoltPaint = Paint().apply { color = Color.parseColor("#00C8FF") }
    private val towerBlastPaint = Paint().apply { color = Color.parseColor("#FF6B00") }
    private val towerFrostPaint = Paint().apply { color = Color.parseColor("#8B4FFF") }
    private val towerRangePaint = Paint().apply {
        color = Color.parseColor("#FFFFFF")
        alpha = 30
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val enemyRunnerPaint = Paint().apply { color = Color.parseColor("#FF2D55") }
    private val enemyTankPaint = Paint().apply { color = Color.parseColor("#FF6B00") }
    private val healthBarBgPaint = Paint().apply { color = Color.parseColor("#333333") }
    private val healthBarFgPaint = Paint().apply { color = Color.parseColor("#30D158") }
    private val healthBarLowPaint = Paint().apply { color = Color.parseColor("#FF2D55") }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        isAntiAlias = true
    }

    // ---- Game State (updated externally from Compose) ----
    var gridTiles: List<GridTile> = emptyList()
    var towers: Map<TileCoordinate, Tower> = emptyMap()
    var enemies: List<EnemyInstance> = emptyList()
    var selectedTile: TileCoordinate? = null

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

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Handle orientation/size changes if needed
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        renderThread?.join()
        renderThread = null
    }

    private fun drawFrame(canvas: Canvas) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()

        canvas.drawRect(0f, 0f, w, h, backgroundPaint)

        val tileW = w / GridSystem.COLS
        val tileH = h / GridSystem.ROWS

        // Grid tiles
        gridTiles.forEach { tile ->
            val left = tile.coordinate.col * tileW
            val top = tile.coordinate.row * tileH
            val rect = RectF(left, top, left + tileW, top + tileH)

            val fillPaint = when (tile.type) {
                TileType.PATH -> pathPaint
                else -> gridEmptyPaint
            }
            canvas.drawRect(rect, fillPaint)
            canvas.drawRect(rect, gridBorderPaint)

            if (tile.coordinate == selectedTile && tile.type == TileType.EMPTY) {
                val highlightPaint = Paint().apply {
                    color = Color.parseColor("#00C8FF")
                    alpha = 60
                    style = Paint.Style.FILL
                }
                canvas.drawRect(rect, highlightPaint)
            }
        }

        // Towers
        towers.forEach { (coord, tower) ->
            val cx = coord.col * tileW + tileW / 2
            val cy = coord.row * tileH + tileH / 2
            val radius = tileW.coerceAtMost(tileH) * 0.35f

            val rangeRadius = tower.rangeTiles * tileW
            canvas.drawCircle(cx, cy, rangeRadius, towerRangePaint)

            val towerPaint = when (tower.type) {
                TowerType.BOLT -> towerBoltPaint
                TowerType.BLAST -> towerBlastPaint
                TowerType.FROST -> towerFrostPaint
            }

            val path = android.graphics.Path().apply {
                moveTo(cx, cy - radius)
                lineTo(cx + radius, cy)
                lineTo(cx, cy + radius)
                lineTo(cx - radius, cy)
                close()
            }
            canvas.drawPath(path, towerPaint)

            val label = tower.type.name.first().toString()
            textPaint.textSize = radius * 0.9f
            val textWidth = textPaint.measureText(label)
            canvas.drawText(label, cx - textWidth / 2, cy + radius * 0.3f, textPaint)
        }

        // Enemies
        enemies.forEach { enemy ->
            val px = enemy.pathProgress * tileW
            val py = enemy.pathRow * tileH + tileH / 2

            val (paint, radius) = when (enemy.type) {
                EnemyType.RUNNER -> enemyRunnerPaint to tileW * 0.25f
                EnemyType.TANK -> enemyTankPaint to tileW * 0.38f
            }

            canvas.drawCircle(px, py, radius, paint)

            val barW = radius * 2.5f
            val barH = 6f
            val barLeft = px - barW / 2
            val barTop = py - radius - barH - 4f

            canvas.drawRect(barLeft, barTop, barLeft + barW, barTop + barH, healthBarBgPaint)
            val hpRatio = enemy.currentHp.toFloat() / enemy.maxHp.toFloat()
            val fgPaint = if (hpRatio > 0.4f) healthBarFgPaint else healthBarLowPaint
            canvas.drawRect(barLeft, barTop, barLeft + barW * hpRatio, barTop + barH, fgPaint)
        }
    }

    inner class RenderThread(private val surfaceHolder: SurfaceHolder) : Thread("PathriftRenderThread") {
        override fun run() {
            while (isRunning) {
                val canvas = surfaceHolder.lockCanvas() ?: continue
                try {
                    synchronized(surfaceHolder) {
                        drawFrame(canvas)
                    }
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
                sleep(16L)
            }
        }
    }
}
