package com.pathrift.anonve.android.game

import android.graphics.PointF
import kotlin.math.sqrt
import kotlin.math.pow

enum class PathLayer { GROUND, BRIDGE }

/**
 * PathSystem — 18 layouts (12 Z-shaped + 6 crossing/complex), continuous waypoint path,
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

    // Total layout count: 12 Z-based + 6 crossing = 18
    val layoutCount: Int get() = layoutParams.size + 6

    var currentLayoutIndex: Int = 0
        private set

    var waypoints: List<PointF> = emptyList()
        private set

    var slotPositions: List<PointF> = emptyList()
        private set

    var waypointLayers: List<PathLayer> = emptyList()
        private set

    val bridgeSegmentCount: Int get() = waypointLayers.count { it == PathLayer.BRIDGE }

    fun layerAt(index: Int): PathLayer =
        if (index < waypointLayers.size) waypointLayers[index] else PathLayer.GROUND

    // Layer definitions for crossing layouts (crossIdx 0-5)
    private val crossingLayerDefs: List<List<PathLayer>> = listOf(
        listOf(PathLayer.GROUND, PathLayer.GROUND, PathLayer.BRIDGE, PathLayer.GROUND, PathLayer.GROUND),
        listOf(PathLayer.GROUND, PathLayer.GROUND, PathLayer.BRIDGE, PathLayer.BRIDGE, PathLayer.GROUND, PathLayer.GROUND),
        listOf(PathLayer.GROUND, PathLayer.GROUND, PathLayer.GROUND, PathLayer.BRIDGE, PathLayer.BRIDGE, PathLayer.GROUND),
        listOf(PathLayer.GROUND, PathLayer.GROUND, PathLayer.BRIDGE, PathLayer.GROUND, PathLayer.BRIDGE, PathLayer.GROUND),
        listOf(PathLayer.GROUND, PathLayer.BRIDGE, PathLayer.BRIDGE, PathLayer.GROUND, PathLayer.GROUND, PathLayer.GROUND),
        listOf(PathLayer.GROUND, PathLayer.GROUND, PathLayer.BRIDGE, PathLayer.GROUND)
    )

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
        val W = screenWidth
        val H = screenHeight
        val totalLayouts = layoutParams.size + 6  // 18 total
        val idx = if (layoutIndex < 0) (0 until totalLayouts).random() else layoutIndex
        currentLayoutIndex = idx

        if (idx < layoutParams.size) {
            // Z-shaped layouts (indices 0–11)
            val p = layoutParams[idx]
            val y1 = H * p[0]
            val y2 = H * p[1]
            val y3 = H * p[2]
            val xL = W * p[3]
            val xR = W * p[4]

            waypoints = listOf(
                PointF(-10f, y1),
                PointF(xR, y1),
                PointF(xR, y2),
                PointF(xL, y2),
                PointF(xL, y3),
                PointF(W + 10f, y3)
            )

            // All Z-shaped layouts are ground-only
            waypointLayers = List(waypoints.size) { PathLayer.GROUND }

            val rawSlots = computeSlots(y1, y2, y3, xL, xR, W, H, currentWave)
            val filteredSlots = rawSlots.filter { isSlotClearOfPath(it, waypoints, clearance = 36f) }
            slotPositions = guaranteePathCoverage(filteredSlots, waypoints, currentWave)
        } else {
            // Crossing/complex layouts (indices 12–17)
            val crossIdx = idx - layoutParams.size
            waypoints = buildCrossingLayout(crossIdx, W, H)

            // Assign layer definitions for crossing layouts
            waypointLayers = if (crossIdx >= 0 && crossIdx < crossingLayerDefs.size) {
                val layerDef = crossingLayerDefs[crossIdx]
                // Pad or trim to match waypoints size
                List(waypoints.size) { i -> if (i < layerDef.size) layerDef[i] else PathLayer.GROUND }
            } else {
                List(waypoints.size) { PathLayer.GROUND }
            }

            val rawSlots = computeSlotsForCrossing(W, H, currentWave)
            val filteredSlots = rawSlots.filter { isSlotClearOfPath(it, waypoints, clearance = 36f) }
            slotPositions = guaranteePathCoverage(filteredSlots, waypoints, currentWave)
        }
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

    // ---- Path-clearance helpers ----

    private fun distanceFromPointToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val abx = bx - ax; val aby = by - ay
        val apx = px - ax; val apy = py - ay
        val lenSq = abx * abx + aby * aby
        if (lenSq == 0f) return sqrt(apx * apx + apy * apy)
        val t = ((apx * abx + apy * aby) / lenSq).coerceIn(0f, 1f)
        val cx = ax + t * abx; val cy = ay + t * aby
        return sqrt((px - cx).pow(2) + (py - cy).pow(2))
    }

    private fun isSlotClearOfPath(slot: PointF, waypoints: List<PointF>, clearance: Float = 36f): Boolean {
        for (i in 1 until waypoints.size) {
            val dist = distanceFromPointToSegment(
                slot.x, slot.y,
                waypoints[i-1].x, waypoints[i-1].y,
                waypoints[i].x, waypoints[i].y
            )
            if (dist < clearance) return false
        }
        return true
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

    // ---- Guarantee path coverage: entry / mid / exit segments each have at least one slot ----

    private fun guaranteePathCoverage(
        slots: List<PointF>,
        wps: List<PointF>,
        currentWave: Int
    ): List<PointF> {
        if (wps.size < 2) return slots

        val result = slots.toMutableList()

        // Divide waypoints into 3 segments
        val segSize = (wps.size - 1) / 3
        val segments = listOf(
            wps.subList(0, segSize + 1),                   // entry
            wps.subList(segSize, 2 * segSize + 1),         // mid
            wps.subList(2 * segSize, wps.size)             // exit
        )

        for (segment in segments) {
            if (segment.size < 1) continue
            val midIndex = segment.size / 2
            val segMid = segment[midIndex]

            // Check if any existing slot is within 120px of this segment midpoint
            val hasCoverage = result.any { slot ->
                val dx = slot.x - segMid.x
                val dy = slot.y - segMid.y
                sqrt((dx * dx + dy * dy).toDouble()) < 120.0
            }

            if (!hasCoverage) {
                // Add guaranteed slot offset 80px perpendicular from segment midpoint
                val dir = if (midIndex + 1 < segment.size) {
                    val next = segment[midIndex + 1]
                    PointF(next.x - segMid.x, next.y - segMid.y)
                } else if (midIndex > 0) {
                    val prev = segment[midIndex - 1]
                    PointF(segMid.x - prev.x, segMid.y - prev.y)
                } else {
                    PointF(1f, 0f)
                }
                val len = sqrt((dir.x * dir.x + dir.y * dir.y).toDouble()).toFloat()
                val safeDen = if (len > 0f) len else 1f
                val perp = PointF(-dir.y / safeDen * 80f, dir.x / safeDen * 80f)
                result.add(PointF(segMid.x + perp.x, segMid.y + perp.y))
            }
        }

        val maxSlots = when {
            currentWave < 5  -> 6
            currentWave < 10 -> 8
            currentWave < 15 -> 10
            else             -> 12
        }
        return result.take(maxSlots)
    }

    // ---- Crossing / complex path layouts (indices 12–17) ----

    private fun buildCrossingLayout(crossIdx: Int, W: Float, H: Float): List<PointF> {
        return when (crossIdx) {
            0 -> listOf(
                // Cross-0: S-curve (smooth)
                PointF(-10f, H * 0.5f),
                PointF(W * 0.25f, H * 0.2f),
                PointF(W * 0.5f, H * 0.5f),
                PointF(W * 0.75f, H * 0.8f),
                PointF(W + 10f, H * 0.5f)
            )
            1 -> listOf(
                // Cross-1: Diamond/X approach
                PointF(-10f, H * 0.5f),
                PointF(W * 0.35f, H * 0.15f),
                PointF(W * 0.65f, H * 0.5f),
                PointF(W * 0.35f, H * 0.85f),
                PointF(W * 0.5f, H * 0.5f),
                PointF(W + 10f, H * 0.5f)
            )
            2 -> listOf(
                // Cross-2: Double zigzag
                PointF(-10f, H * 0.15f),
                PointF(W * 0.3f, H * 0.15f),
                PointF(W * 0.3f, H * 0.85f),
                PointF(W * 0.7f, H * 0.85f),
                PointF(W * 0.7f, H * 0.15f),
                PointF(W + 10f, H * 0.15f)
            )
            3 -> listOf(
                // Cross-3: Spiral approach
                PointF(-10f, H * 0.75f),
                PointF(W * 0.5f, H * 0.75f),
                PointF(W * 0.5f, H * 0.25f),
                PointF(W * 0.2f, H * 0.25f),
                PointF(W * 0.7f, H * 0.6f),
                PointF(W + 10f, H * 0.6f)
            )
            4 -> listOf(
                // Cross-4: W-shape
                PointF(-10f, H * 0.5f),
                PointF(W * 0.2f, H * 0.15f),
                PointF(W * 0.4f, H * 0.55f),
                PointF(W * 0.6f, H * 0.15f),
                PointF(W * 0.8f, H * 0.55f),
                PointF(W + 10f, H * 0.5f)
            )
            else -> listOf(
                // Cross-5: Long diagonal
                PointF(-10f, H * 0.2f),
                PointF(W * 0.6f, H * 0.2f),
                PointF(W * 0.4f, H * 0.8f),
                PointF(W + 10f, H * 0.8f)
            )
        }
    }

    /** Generate candidate slots spread across the screen for crossing layouts. */
    private fun computeSlotsForCrossing(W: Float, H: Float, currentWave: Int): List<PointF> {
        val edge = 30f
        val minSep = 56f

        val candidates = listOf(
            PointF(W * 0.12f, H * 0.35f),
            PointF(W * 0.12f, H * 0.65f),
            PointF(W * 0.28f, H * 0.20f),
            PointF(W * 0.28f, H * 0.50f),
            PointF(W * 0.28f, H * 0.80f),
            PointF(W * 0.45f, H * 0.35f),
            PointF(W * 0.45f, H * 0.65f),
            PointF(W * 0.60f, H * 0.20f),
            PointF(W * 0.60f, H * 0.50f),
            PointF(W * 0.60f, H * 0.80f),
            PointF(W * 0.78f, H * 0.35f),
            PointF(W * 0.78f, H * 0.65f),
            PointF(W * 0.88f, H * 0.50f),
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
