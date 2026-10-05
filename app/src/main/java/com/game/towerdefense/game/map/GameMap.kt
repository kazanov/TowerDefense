package com.game.towerdefense.game.map

import com.game.towerdefense.domain.model.GridPoint
import com.game.towerdefense.domain.model.LevelConfig
import java.util.Random
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/** Размер клетки в «игровых пикселях». Все дальности из ТЗ (150 px и т.д.) заданы в этих единицах. */
const val CELL = 100f

class Decoration(val x: Float, val y: Float, val kind: Int, val size: Float, val color: Int)

class GameMap(level: LevelConfig) {
    val cols: Int = level.cols
    val rows: Int = level.rows
    val width: Float = cols * CELL
    val height: Float = rows * CELL

    val waypointsX: FloatArray
    val waypointsY: FloatArray
    private val segmentLength: FloatArray
    val pathLength: Float

    private val pathCells = BooleanArray(cols * rows)
    private val blockedCells = BooleanArray(cols * rows)

    val obstacles: List<GridPoint>
    val decorations: List<Decoration>

    val spawnX: Float
    val spawnY: Float
    val baseX: Float
    val baseY: Float

    init {
        val pts = level.path
        waypointsX = FloatArray(pts.size) { pts[it].x * CELL + CELL / 2f }
        waypointsY = FloatArray(pts.size) { pts[it].y * CELL + CELL / 2f }
        segmentLength = FloatArray(pts.size - 1) { i ->
            hypot(waypointsX[i + 1] - waypointsX[i], waypointsY[i + 1] - waypointsY[i])
        }
        var total = 0f
        for (len in segmentLength) total += len
        pathLength = total

        // Растеризуем маршрут в клетки (на них нельзя строить).
        for (i in 0 until pts.size - 1) {
            val a = pts[i]
            val b = pts[i + 1]
            val steps = max(abs(b.x - a.x), abs(b.y - a.y))
            for (s in 0..steps) {
                val x = if (steps == 0) a.x else a.x + ((b.x - a.x) * s.toFloat() / steps).roundToInt()
                val y = if (steps == 0) a.y else a.y + ((b.y - a.y) * s.toFloat() / steps).roundToInt()
                if (isInside(x, y)) pathCells[y * cols + x] = true
            }
        }

        obstacles = level.obstacles.filter { isInside(it.x, it.y) && !pathCells[it.y * cols + it.x] }
        for (o in obstacles) blockedCells[o.y * cols + o.x] = true

        spawnX = waypointsX.first().coerceIn(CELL / 2f, width - CELL / 2f)
        spawnY = waypointsY.first().coerceIn(CELL / 2f, height - CELL / 2f)
        baseX = waypointsX.last()
        baseY = waypointsY.last()

        decorations = buildDecorations(level.index)
    }

    fun isInside(col: Int, row: Int): Boolean = col in 0 until cols && row in 0 until rows

    fun isPath(col: Int, row: Int): Boolean = isInside(col, row) && pathCells[row * cols + col]

    /** Можно ли в принципе строить на клетке (без учёта уже стоящих башен). */
    fun isBuildable(col: Int, row: Int): Boolean =
        isInside(col, row) && !pathCells[row * cols + col] && !blockedCells[row * cols + col]

    /** Позиция на маршруте по пройденной дистанции. Результат пишется в [out] (x, y), без аллокаций. */
    fun positionAt(distance: Float, out: FloatArray) {
        var d = if (distance < 0f) 0f else distance
        val last = segmentLength.size - 1
        for (i in segmentLength.indices) {
            val len = segmentLength[i]
            if (d <= len || i == last) {
                val t = if (len > 0f) (d / len).coerceIn(0f, 1f) else 1f
                out[0] = waypointsX[i] + (waypointsX[i + 1] - waypointsX[i]) * t
                out[1] = waypointsY[i] + (waypointsY[i + 1] - waypointsY[i]) * t
                return
            }
            d -= len
        }
        out[0] = waypointsX[0]
        out[1] = waypointsY[0]
    }

    private fun buildDecorations(seed: Int): List<Decoration> {
        val rnd = Random(seed * 7919L + 17L)
        val colors = intArrayOf(
            0xFFFFF176.toInt(), 0xFFF48FB1.toInt(), 0xFFFFFFFF.toInt(), 0xFF90CAF9.toInt(), 0xFFFFAB91.toInt(),
        )
        val list = ArrayList<Decoration>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                if (pathCells[idx] || blockedCells[idx]) continue
                repeat(rnd.nextInt(3)) {
                    list.add(
                        Decoration(
                            x = c * CELL + 15f + rnd.nextFloat() * 70f,
                            y = r * CELL + 15f + rnd.nextFloat() * 70f,
                            kind = rnd.nextInt(3),
                            size = 3f + rnd.nextFloat() * 3f,
                            color = colors[rnd.nextInt(colors.size)],
                        )
                    )
                }
            }
        }
        return list
    }
}
