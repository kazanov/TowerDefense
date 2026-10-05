package com.game.towerdefense.presentation.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.game.towerdefense.domain.model.GridPoint
import com.game.towerdefense.game.enemies.Enemy
import com.game.towerdefense.game.engine.GameEngine
import com.game.towerdefense.game.entities.EffectType
import com.game.towerdefense.game.map.CELL
import com.game.towerdefense.game.map.GameMap
import com.game.towerdefense.game.towers.Tower
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val OutsideColor = Color(0xFF1B2E1A)
private val GrassA = Color(0xFF6DAA4F)
private val GrassB = Color(0xFF66A14A)
private val RoadEdge = Color(0xFFB08A55)
private val Road = Color(0xFFD9B77E)
private val RoadDetail = Color(0x33FFFFFF)
private val Stone = Color(0xFFB0BEC5)
private val StoneDark = Color(0xFF78909C)
private val Shadow = Color(0x40000000)
private val Gold = Color(0xFFFFD54F)
private val SelectFill = Color(0x5576FF03)
private val RangeFill = Color(0x22FFFFFF)
private val RangeStroke = Color(0x99FFFFFF)

private val Stroke3 = Stroke(width = 3f)
private val Stroke4 = Stroke(width = 4f)
private val Stroke6 = Stroke(width = 6f)

/** Переводит координаты экрана в игровые: общий масштаб с центрированием поля. */
class MapTransform(val scale: Float, val offsetX: Float, val offsetY: Float) {
    companion object {
        fun compute(viewW: Float, viewH: Float, map: GameMap): MapTransform {
            val s = min(viewW / map.width, viewH / map.height)
            return MapTransform(s, (viewW - map.width * s) / 2f, (viewH - map.height * s) / 2f)
        }
    }
}

fun DrawScope.drawGame(engine: GameEngine, selectedCell: GridPoint?, selectedTower: Tower?) {
    val map = engine.map
    drawRect(OutsideColor)
    val t = MapTransform.compute(size.width, size.height, map)
    withTransform({
        translate(t.offsetX, t.offsetY)
        scale(t.scale, t.scale, pivot = Offset.Zero)
    }) {
        clipRect(0f, 0f, map.width, map.height) {
            val time = engine.time
            drawTerrain(map)
            drawRoad(map)
            drawDecorations(map)
            drawObstacles(map)
            drawPortal(map, time)
            drawBase(map, engine.baseHp.toFloat() / engine.maxBaseHp.coerceAtLeast(1))
            if (selectedCell != null) drawCellSelection(selectedCell, time)
            for (i in engine.towers.indices) drawTower(engine.towers[i], time)
            if (selectedTower != null) drawRange(selectedTower)
            for (i in engine.enemies.indices) drawEnemy(engine.enemies[i])
            drawProjectiles(engine)
            drawEffects(engine)
        }
    }
}

private fun DrawScope.drawTerrain(map: GameMap) {
    for (r in 0 until map.rows) {
        for (c in 0 until map.cols) {
            drawRect(
                color = if ((r + c) % 2 == 0) GrassA else GrassB,
                topLeft = Offset(c * CELL, r * CELL),
                size = Size(CELL + 1f, CELL + 1f),
            )
        }
    }
}

private fun DrawScope.drawRoad(map: GameMap) {
    val xs = map.waypointsX
    val ys = map.waypointsY
    for (i in 0 until xs.size - 1) {
        drawLine(RoadEdge, Offset(xs[i], ys[i]), Offset(xs[i + 1], ys[i + 1]), CELL * 0.88f, StrokeCap.Square)
    }
    for (i in 0 until xs.size - 1) {
        drawLine(Road, Offset(xs[i], ys[i]), Offset(xs[i + 1], ys[i + 1]), CELL * 0.72f, StrokeCap.Square)
    }
    for (i in 0 until xs.size - 1) {
        drawLine(RoadDetail, Offset(xs[i], ys[i]), Offset(xs[i + 1], ys[i + 1]), 4f, StrokeCap.Round)
    }
}

private fun DrawScope.drawDecorations(map: GameMap) {
    for (d in map.decorations) {
        when (d.kind) {
            0 -> { // цветок
                drawCircle(Color(d.color), d.size, Offset(d.x, d.y))
                drawCircle(Gold, d.size * 0.4f, Offset(d.x, d.y))
            }
            1 -> { // пучок травы
                val c = Color(0xFF4E8A36)
                drawLine(c, Offset(d.x, d.y), Offset(d.x - 4f, d.y - 10f), 2.5f)
                drawLine(c, Offset(d.x, d.y), Offset(d.x, d.y - 12f), 2.5f)
                drawLine(c, Offset(d.x, d.y), Offset(d.x + 4f, d.y - 10f), 2.5f)
            }
            else -> drawCircle(Color(0xFF8D9A87), d.size, Offset(d.x, d.y)) // камешек
        }
    }
}

private fun DrawScope.drawObstacles(map: GameMap) {
    for (o in map.obstacles) {
        val cx = o.x * CELL + CELL / 2f
        val cy = o.y * CELL + CELL / 2f
        drawOval(Shadow, Offset(cx - 34f, cy + 14f), Size(68f, 24f))
        when ((o.x * 7 + o.y * 3) % 3) {
            0 -> { // дерево
                drawRect(Color(0xFF6D4C41), Offset(cx - 6f, cy), Size(12f, 28f))
                drawCircle(Color(0xFF2E7D32), 30f, Offset(cx, cy - 8f))
                drawCircle(Color(0xFF388E3C), 20f, Offset(cx - 10f, cy - 16f))
                drawCircle(Color(0xFF43A047), 10f, Offset(cx - 14f, cy - 20f))
            }
            1 -> { // скала
                drawCircle(Color(0xFF616161), 32f, Offset(cx, cy))
                drawCircle(Color(0xFF8D8D8D), 22f, Offset(cx - 6f, cy - 6f))
                drawCircle(Color(0xFFA6A6A6), 9f, Offset(cx - 12f, cy - 12f))
            }
            else -> { // куст
                drawCircle(Color(0xFF33691E), 22f, Offset(cx - 14f, cy + 4f))
                drawCircle(Color(0xFF558B2F), 24f, Offset(cx + 10f, cy))
                drawCircle(Color(0xFFE53935), 4f, Offset(cx + 6f, cy - 8f))
                drawCircle(Color(0xFFE53935), 4f, Offset(cx - 14f, cy))
            }
        }
    }
}

private fun DrawScope.drawPortal(map: GameMap, time: Float) {
    val c = Offset(map.spawnX, map.spawnY)
    drawCircle(Color(0xFF311B92), 38f, c)
    drawCircle(Color(0xFF6A1B9A), 28f, c)
    val pulse = 24f + 8f * sin(time * 4f)
    drawCircle(Color(0xFFCE93D8), pulse, c, alpha = 0.8f, style = Stroke4)
}

private fun DrawScope.drawBase(map: GameMap, hpRatio: Float) {
    val bx = map.baseX
    val by = map.baseY
    drawOval(Shadow, Offset(bx - 44f, by + 22f), Size(88f, 22f))
    drawRect(StoneDark, Offset(bx - 40f, by - 32f), Size(80f, 66f))
    drawRect(Stone, Offset(bx - 34f, by - 26f), Size(68f, 54f))
    for (i in 0 until 4) {
        drawRect(StoneDark, Offset(bx - 40f + i * 22.6f, by - 44f), Size(12f, 14f))
    }
    drawRoundRect(Color(0xFF4E342E), Offset(bx - 11f, by + 4f), Size(22f, 30f), CornerRadius(10f, 10f))
    drawLine(Color(0xFF424242), Offset(bx, by - 44f), Offset(bx, by - 76f), 3f)
    val flagColor = if (hpRatio > 0.5f) Color(0xFFE53935) else if (hpRatio > 0.25f) Color(0xFFFB8C00) else Color(0xFF757575)
    drawRect(flagColor, Offset(bx + 1f, by - 76f), Size(24f, 15f))
}

private fun DrawScope.drawCellSelection(cell: GridPoint, time: Float) {
    val topLeft = Offset(cell.x * CELL + 3f, cell.y * CELL + 3f)
    val sz = Size(CELL - 6f, CELL - 6f)
    drawRect(SelectFill, topLeft, sz)
    drawRect(Color.White, topLeft, sz, alpha = 0.6f + 0.4f * sin(time * 6f), style = Stroke4)
}

private fun DrawScope.drawRange(t: Tower) {
    val c = Offset(t.x, t.y)
    drawCircle(RangeFill, t.stats.range, c)
    drawCircle(RangeStroke, t.stats.range, c, style = Stroke3)
}

private fun DrawScope.drawTower(t: Tower, time: Float) {
    val c = Offset(t.x, t.y)
    val deg = (t.angle * 180f / PI.toFloat())
    val fire = if (t.fireAnim > 0f) t.fireAnim / Tower.FIRE_ANIM else 0f
    val recoil = fire * 6f
    val color = Color(t.config.color)

    drawRoundRect(Shadow, Offset(t.x - 36f, t.y - 28f), Size(76f, 72f), CornerRadius(12f, 12f))
    drawRoundRect(StoneDark, Offset(t.x - 38f, t.y - 38f), Size(76f, 76f), CornerRadius(12f, 12f))
    drawRoundRect(Stone, Offset(t.x - 33f, t.y - 33f), Size(66f, 66f), CornerRadius(10f, 10f))

    when (t.config.id) {
        "cannon" -> {
            drawCircle(color, 28f, c)
            rotate(deg, c) {
                drawRoundRect(Color(0xFF263238), Offset(t.x - 4f - recoil, t.y - 8f), Size(44f, 16f), CornerRadius(4f, 4f))
            }
            drawCircle(Color(0xFF37474F), 14f, c)
        }
        "ice" -> {
            drawCircle(color, 27f, c)
            rotate(time * 40f, c) {
                drawRect(Color(0xFFE1F5FE), Offset(t.x - 14f, t.y - 14f), Size(28f, 28f))
            }
            rotate(time * 40f + 45f, c) {
                drawRect(Color(0xCCB3E5FC), Offset(t.x - 14f, t.y - 14f), Size(28f, 28f))
            }
            drawCircle(Color.White, 6f, c)
        }
        "magic" -> {
            drawCircle(color, 27f, c)
            val pulse = 1f + 0.15f * sin(time * 5f) + fire * 0.4f
            drawCircle(Color(0x66E1BEE7), 20f * pulse, c)
            drawCircle(Color(0xFFF3E5F5), 10f, c)
            for (k in 0 until 3) {
                val a = time * 2f + k * 2.094f
                drawCircle(Color(0xFFE1BEE7), 4f, Offset(t.x + cos(a) * 22f, t.y + sin(a) * 22f))
            }
        }
        else -> { // archer и любые новые башни по умолчанию
            drawCircle(color, 26f, c)
            drawCircle(Color(0xFFA1887F), 17f, c)
            rotate(deg, c) {
                drawLine(Color(0xFF3E2723), Offset(t.x + 8f, t.y - 20f), Offset(t.x + 8f, t.y + 20f), 5f, StrokeCap.Round)
                drawLine(Color(0xFF5D4037), Offset(t.x - 12f - recoil, t.y), Offset(t.x + 30f - recoil, t.y), 4f, StrokeCap.Round)
            }
        }
    }

    if (fire > 0f) drawCircle(Color.White, 34f, c, alpha = 0.35f * fire, style = Stroke4)

    // Индикатор уровня
    for (i in 0..t.level) {
        drawCircle(Color(0xFF5D4037), 6f, Offset(t.x - 12f + i * 12f, t.y + 31f))
        drawCircle(Gold, 4.5f, Offset(t.x - 12f + i * 12f, t.y + 31f))
    }
}

private fun DrawScope.drawEnemy(e: Enemy) {
    if (!e.alive) return
    val cfg = e.config
    val r = cfg.radius
    val bob = sin(e.animTime * 10f * cfg.speed) * 2.5f
    val x = e.x
    val y = e.y + bob

    drawOval(Shadow, Offset(x - r, e.y + r * 0.55f), Size(r * 2f, r * 0.6f))
    val body = if (e.hitFlash > 0f) Color.White else Color(cfg.color)
    drawCircle(body, r, Offset(x, y))
    drawCircle(Color(0x33000000), r, Offset(x, y), style = Stroke3)

    when (cfg.id) {
        "armored" -> {
            drawCircle(Color(0xFF455A64), r - 2f, Offset(x, y), style = Stroke6)
            drawRect(Color(0xFF455A64), Offset(x - r * 0.7f, y - r * 0.25f), Size(r * 1.4f, r * 0.2f))
        }
        "boss" -> {
            val crownY = y - r - 4f
            for (k in -1..1) {
                drawCircle(Gold, 7f, Offset(x + k * 14f, crownY - if (k == 0) 6f else 0f))
            }
            drawRect(Gold, Offset(x - 20f, crownY), Size(40f, 8f))
        }
        "heavy" -> drawCircle(Color(0x55000000), r * 0.55f, Offset(x, y + r * 0.2f))
    }

    // Глаза
    val eo = r * 0.35f
    val er = r * 0.22f
    drawCircle(Color.White, er, Offset(x - eo, y - r * 0.2f))
    drawCircle(Color.White, er, Offset(x + eo, y - r * 0.2f))
    drawCircle(Color.Black, er * 0.5f, Offset(x - eo + 1f, y - r * 0.2f))
    drawCircle(Color.Black, er * 0.5f, Offset(x + eo + 1f, y - r * 0.2f))

    if (e.isSlowed) drawCircle(Color(0xCC81D4FA), r + 5f, Offset(x, y), style = Stroke4)

    if (e.hp < e.maxHp) {
        val ratio = (e.hp / e.maxHp).coerceIn(0f, 1f)
        val w = r * 2f
        val top = y - r - (if (cfg.id == "boss") 26f else 12f)
        drawRect(Color(0xCC000000), Offset(x - w / 2f - 1f, top - 1f), Size(w + 2f, 8f))
        val hpColor = when {
            ratio > 0.6f -> Color(0xFF66BB6A)
            ratio > 0.3f -> Color(0xFFFFCA28)
            else -> Color(0xFFEF5350)
        }
        drawRect(hpColor, Offset(x - w / 2f, top), Size(w * ratio, 6f))
    }
}

private fun DrawScope.drawProjectiles(engine: GameEngine) {
    for (i in engine.projectiles.indices) {
        val p = engine.projectiles[i]
        val c = Offset(p.x, p.y)
        val cfg = p.tower
        when {
            cfg.splashRadius > 0f -> {
                drawCircle(Color(0xFF212121), 9f, c)
                drawCircle(Color(0xFF616161), 3f, Offset(p.x - 3f, p.y - 3f))
            }
            cfg.slowFactor > 0f -> {
                drawCircle(Color(0x8880DEEA), 11f, c)
                drawCircle(Color.White, 5f, c)
            }
            cfg.armorPenetration > 0f -> {
                drawCircle(Color(0x88CE93D8), 13f, c)
                drawCircle(Color(0xFFF3E5F5), 6f, c)
            }
            else -> drawLine(
                Color(0xFF4E342E),
                Offset(p.x - p.dirX * 18f, p.y - p.dirY * 18f),
                c,
                3.5f,
                StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawEffects(engine: GameEngine) {
    for (i in engine.effects.indices) {
        val fx = engine.effects[i]
        val p = fx.progress
        val c = Offset(fx.x, fx.y)
        val color = Color(fx.color)
        when (fx.type) {
            EffectType.HIT -> drawCircle(color, fx.radius * (0.4f + p), c, alpha = (1f - p) * 0.8f, style = Stroke3)
            EffectType.EXPLOSION -> {
                drawCircle(color, fx.radius * (0.3f + 0.7f * p), c, alpha = (1f - p) * 0.7f)
                drawCircle(Color(0xFFFFEB3B), fx.radius * 0.5f * (1f - p), c, alpha = 1f - p)
            }
            EffectType.FREEZE -> {
                drawCircle(color, fx.radius * (0.5f + 0.5f * p), c, alpha = 1f - p, style = Stroke4)
                for (k in 0 until 6) {
                    val a = k * (PI.toFloat() / 3f)
                    val d = fx.radius * (0.4f + 0.6f * p)
                    drawCircle(Color.White, 3f, Offset(fx.x + cos(a) * d, fx.y + sin(a) * d), alpha = 1f - p)
                }
            }
            EffectType.MAGIC -> drawCircle(color, fx.radius * (0.3f + 0.7f * p), c, alpha = (1f - p) * 0.8f)
            EffectType.DEATH -> {
                drawCircle(color, fx.radius * (1f - 0.5f * p), c, alpha = (1f - p) * 0.6f)
                for (k in 0 until 6) {
                    val a = k * (PI.toFloat() / 3f) + 0.3f
                    val d = fx.radius * (0.5f + 1.2f * p)
                    drawCircle(color, 4f * (1f - p) + 1f, Offset(fx.x + cos(a) * d, fx.y + sin(a) * d), alpha = 1f - p)
                }
            }
            EffectType.BUILD -> drawCircle(color, fx.radius * (0.5f + p), c, alpha = 1f - p, style = Stroke6)
        }
    }
}
