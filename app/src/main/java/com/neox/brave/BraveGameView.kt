package com.neox.brave

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class BraveGameView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val adaptive = AdaptiveSystem()
    private val combat = AdaptiveCombat()
    private val controller = CompanionController()
    private val game = GameModel()

    private var companion: CompanionProfile? = null
    private var companionAction = CompanionAction.GUARD
    private var message = "NEOX-BRAVE // SECTOR 01"
    private var lastFrameNanos = 0L
    private var leftPressed = false
    private var rightPressed = false

    private val blocks = mutableListOf(
        RectF(500f, 0f, 560f, 0f),
        RectF(570f, 0f, 630f, 0f),
        RectF(640f, 0f, 700f, 0f)
    )
    private val cores = mutableListOf(
        Pair(530f, 0f) to Core.A,
        Pair(600f, 0f) to Core.B,
        Pair(670f, 0f) to Core.A
    )

    init {
        isFocusableInTouchMode = true
        requestFocus()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = if (lastFrameNanos == 0L) 0f else min(0.033f, (now - lastFrameNanos) / 1_000_000_000f)
        lastFrameNanos = now

        val groundY = height * 0.72f
        game.update(dt, groundY)
        game.player.y = groundY - 72f

        if (leftPressed) {
            game.player.x = max(20f, game.player.x - 240f * dt)
            game.player.facing = -1
        }
        if (rightPressed) {
            game.player.x = min(width - 80f, game.player.x + 240f * dt)
            game.player.facing = 1
        }

        val context = CombatContext(
            game.player.x,
            game.player.energy,
            game.nearestEnemy()?.let { abs(it.x - game.player.x) },
            game.projectiles.filter { it.hostile }.minOfOrNull { abs(it.x - game.player.x) },
            game.enemies.count { it.energy > 0f }
        )
        companion?.let {
            companionAction = combat.decide(it, context)
            if (controller.state.x == 0f) {
                controller.state.x = game.player.x + 72f
            }
            controller.update(
                dt = dt,
                groundY = groundY,
                player = game.player,
                enemies = game.enemies,
                projectiles = game.projectiles,
                profile = it,
                action = companionAction,
                worldWidth = width.toFloat()
            )
        }

        blocks.forEach {
            it.top = groundY - 110f
            it.bottom = groundY - 50f
        }
        cores.forEach { it.first.second = groundY - 145f }

        drawWorld(canvas, groundY)
        drawHud(canvas)
        postInvalidateOnAnimation()
    }

    private fun drawWorld(canvas: Canvas, groundY: Float) {
        canvas.drawColor(android.graphics.Color.rgb(9, 12, 20))
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.rgb(24, 31, 48)
        canvas.drawRect(0f, groundY, width.toFloat(), height.toFloat(), paint)

        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(game.player.x, game.player.y, game.player.x + 42f, game.player.y + 72f, paint)

        paint.color = android.graphics.Color.rgb(80, 180, 255)
        blocks.forEach { canvas.drawRect(it, paint) }

        cores.forEach { (position, core) ->
            paint.color = if (core == Core.A) android.graphics.Color.rgb(255,120,80) else android.graphics.Color.rgb(100,255,180)
            canvas.drawCircle(position.first, position.second, 16f, paint)
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 18f
            canvas.drawText(core.name, position.first - 6f, position.second + 6f, paint)
        }

        game.enemies.filter { it.energy > 0f }.forEach {
            paint.color = android.graphics.Color.rgb(220,70,90)
            canvas.drawRect(it.x, groundY - 92f, it.x + 42f, groundY - 20f, paint)
        }

        paint.color = android.graphics.Color.YELLOW
        game.projectiles.filter { it.hostile }.forEach {
            canvas.drawCircle(it.x, it.y, 7f, paint)
        }

        companion?.let {
            val cx = controller.state.x
            val cy = controller.state.y
            paint.color = android.graphics.Color.rgb(210,210,255)
            canvas.drawCircle(cx, cy, 22f, paint)
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 12f
            canvas.drawText(it.signature, cx - 18f, cy + 4f, paint)
        }
    }

    private fun drawHud(canvas: Canvas) {
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 24f
        canvas.drawText(message, 32f, 42f, paint)
        paint.textSize = 18f
        canvas.drawText("CORES: " + adaptive.pending().joinToString(""), 32f, 72f, paint)
        canvas.drawText("ENERGY: " + game.player.energy.toInt(), 32f, 98f, paint)
        companion?.let {
            canvas.drawText("COMPANION " + it.signature + " // " + companionAction.name, 32f, 124f, paint)
        }
        paint.textSize = 16f
        canvas.drawText("TOUCH: LEFT/RIGHT MOVE · CENTER COLLECT", 32f, height - 22f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                leftPressed = event.x < width * 0.33f
                rightPressed = event.x > width * 0.66f
                if (event.actionMasked == MotionEvent.ACTION_DOWN && event.x in (width * 0.33f)..(width * 0.66f)) {
                    collectNearestCore()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                leftPressed = false
                rightPressed = false
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> leftPressed = true
            KeyEvent.KEYCODE_DPAD_RIGHT -> rightPressed = true
            KeyEvent.KEYCODE_BUTTON_A -> collectNearestCore()
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> leftPressed = false
            KeyEvent.KEYCODE_DPAD_RIGHT -> rightPressed = false
            else -> return super.onKeyUp(keyCode, event)
        }
        return true
    }

    private fun collectNearestCore() {
        if (cores.isEmpty()) return
        val nearest = cores.minByOrNull { abs(it.first.first - game.player.x) } ?: return
        if (abs(nearest.first.first - game.player.x) > 120f) {
            message = "MOVE CLOSER"
            return
        }
        cores.remove(nearest)
        val result = adaptive.collect(nearest.second)
        message = if (result == null) {
            "CORE " + nearest.second.name + " ACQUIRED"
        } else {
            companion = result
            controller.state.x = game.player.x + if (game.player.facing > 0) 72f else -72f
            "COMPANION " + result.signature + " ONLINE"
        }
    }
}