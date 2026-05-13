package com.pathrift.anonve.android.game

import android.graphics.PointF
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * PathSystem — iOS parity. 12 Z-shaped layouts, continuous waypoint path,
 * algorithmic slot placement that never overlaps the path.
 *
 * Call [buildLayout] once per layout change (game start / Rift Shift).
 * Use [positionAt] to convert normalized progress [0,1] → screen PointF.
 */
object PathSystem {

    // 12 layout parameter sets: (y1%, y2%, y3%, xL%, xR%) — fractions of screen H/W
    private val layoutParams: List<List<Float>> = listOf(
        listOf(0.20f, 0.50f, 0.78f, 0.26f, 0.74f),   // 0: standard forward Z
        listOf(0.78f, 0.50f, 0.22f, 0.28f, 0.72f),   // 1: reverse Z
        listOf(0.12f, 0.50f, 0.88f, 0.18f, 0.82f),   // 2: wide Z
        listOf(0.28f, 0.50f, 0.72f, 0.30f, 0.70f),   // 3: tight centre
        listOf(0.20f, 0.53f, 0.84f, 0.15f, 0.60f),   // 4: left-heavy
        listOf(0.16f, 0.50f, 0.84f, 0.40f, 0.85f),   // 5: right-heavy
        listOf(0.15f, 0.40f, 0.78f, 0.25f, 0.75f),   // 6: upper double
        listOf(0.22f, 0.60f, 0.85f, 0.26f, 0.74f),   // 7: lower double
        listOf(0.82f, 0.48f, 0.16f, 0.22f, 0.78f),   // 8: wide reverse
        listOf(0.20f, 0.44f, 0.80f, 0.35f, 0.65f),   // 9: compressed mid
        listOf(0.18f, 0.58f, 0.82f, 0.24f, 0.76f),   // 10: lower compressed
        listOf(0.75f, 0.44f, 0.17f, 0.32f, 0.68f),   // 11: narrow reverse
    )

    val layoutCount: Int get() = layoutParams.size

    var currentLayoutIndex: Int = 0
        private set

    var waypoints: List<PointF> = emptyList()
        private set

    var slotPositions: List<PointF> = emptyList()
        private set

    /**
     * Build the path and slot layout for the given screen dimensions.
     * @param layoutIndex -1 = random pick
     */
    fun buildLayout(
        screenWidth: Float,
        screenHeight: Float,
        currentWave: Int = 0,
        layoutIndex: Int = -1
    ) {
        val idx = if (layoutIndex < 0) (0 until layoutParams.size).random() else layoutIndex
        currentLayoutIndex = idx
        val p = layoutParams[idx]
        val y1 = screenHeight * p[0]
        val y2 = screenHeight * p[1]
        val y3 = screenHeight * p[2]
        val xL = screenWidth * p[3]
        val xR = screenWidth * p[4]

        waypoints = listOf(
            PointF(-10f, y1),
            PointF(xR, y1),
            PointF(xR, y2),
            PointF(xL, y2),
            PointF(xL, y3),
            PointF(screenWidth + 10f, y3)
        )

        slotPositions = computeSlots(y1, y2, y3, xL, xR, screenWidth, screenHeight, currentWave)
    }

    fun totalPathLength(): Float {
        var len = 0f
        for (i in 1 until waypoints.size) {
            val dx = waypoints[i].x - waypoints[i - 1].x
            val dy = waypoints[i].y - waypoints[i - 1].y
            len += sqrt(dx * dx + dy * dy)
        }
        return len
    }

    /** Convert normalized progress [0,1] to screen position. */
    fun positionAt(progress: Float): PointF {
        val total = totalPathLength()
        val target = progress * total
        var acc = 0f
        for (i in 1 until waypoints.size) {
            val from = waypoints[i - 1]
            val to = waypoints[i]
            val dx = to.x - from.x
            val dy = to.y - from.y
            val seg = sqrt(dx * dx + dy * dy)
            if (acc + seg >= target) {
                val t = if (seg > 0f) (target - acc) / seg else 0f
                return PointF(from.x + dx * t, from.y + dy * t)
            }
            acc += seg
        }
        return waypoints.lastOrNull() ?: PointF(0f, 0f)
    }

    // ---- Slot computation (mirrors iOS GameScene.computeSlots) ----

    private fun computeSlots(
        y1: Float, y2: Float, y3: Float,
        xL: Float, xR: Float,
        W: Float, H: Float,
        currentWave: Int
    ): List<PointF> {
        val vGap = 88f
        val hGap = 68f
        val edge = 30f
        val minSep = 56f

        val fwd = y1 < y2
        val seg1Side = if (fwd) y1 + vGap else y1 - vGap
        val seg3Side = if (fwd) y2 - vGap else y2 + vGap
        val seg5Side = if (fwd) y3 - vGap else y3 + vGap
        val srx = minOf(xR + hGap, W - edge)

        val candidates = listOf(
            PointF(W * 0.14f, seg1Side),
            PointF(W * 0.40f, seg1Side),
            PointF(minOf(W * 0.64f, xR - hGap - 8f), seg1Side),
            PointF(srx, y1 + (y2 - y1) * 0.27f),
            PointF(srx, y1 + (y2 - y1) * 0.73f),
            PointF(maxOf(W * 0.36f, xL + hGap + 12f), seg3Side),
            PointF(minOf(W * 0.62f, xR - hGap - 12f), seg3Side),
            PointF(xL + hGap, y2 + (y3 - y2) * 0.33f),
            PointF(xL + hGap, y2 + (y3 - y2) * 0.68f),
            PointF(srx, y2 + (y3 - y2) * 0.46f),
            PointF(maxOf(xL + hGap + 6f, W * 0.20f), seg5Side),
            PointF((xL + W) * 0.5f + 8f, seg5Side),
            PointF(minOf(W * 0.80f, W - edge), seg5Side),
        )

        val result = mutableListOf<PointF>()
        for (c in candidates) {
            if (c.x < edge || c.x > W - edge || c.y < edge || c.y > H - edge) continue
            val tooClose = result.any { e ->
                val dx = c.x - e.x
                val dy = c.y - e.y
                sqrt(dx * dx + dy * dy) < minSep
            }
            if (!tooClose) result.add(c)
        }

        val maxSlots = when {
            currentWave < 5  -> 6
            currentWave < 10 -> 8
            currentWave < 15 -> 10
            else             -> 12
        }
        return result.take(maxSlots)
    }
}
