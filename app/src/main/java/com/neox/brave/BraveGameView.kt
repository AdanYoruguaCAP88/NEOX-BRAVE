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

        val observation = game.observe()

        companion?.let {
            companionAction = combat.decide(it, observation.toCombatContext())
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
        canvas.drawColor(android.graphics.Color.rgb(238, 241, 246))

        // Clean 2D arena: the character art is intentionally built from simple
        // vector shapes so the visual language survives without external assets.
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.rgb(205, 211, 220)
        canvas.drawRect(0f, groundY, width.toFloat(), height.toFloat(), paint)

        paint.color = android.graphics.Color.rgb(170, 178, 190)
        canvas.drawRect(0f, groundY, width.toFloat(), groundY + 3f, paint)

        blocks.forEach {
            paint.color = android.graphics.Color.rgb(62, 87, 120)
            canvas.drawRoundRect(it, 10f, 10f, paint)
        }

        cores.forEach { (position, core) ->
            paint.color = if (core == Core.A) {
                android.graphics.Color.rgb(255, 170, 45)
            } else {
                android.graphics.Color.rgb(70, 190, 255)
            }
            canvas.drawCircle(position.first, position.second, 16f, paint)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 18f
            canvas.drawText(core.name, position.first - 6f, position.second + 6f, paint)
        }

        // Enemies retain the same readable silhouette, but use the game's new
        // white/navy/gold visual grammar.
        game.enemies.filter { it.energy > 0f }.forEach {
            drawEnemy(canvas, it.x, groundY - 18f)
        }

        paint.color = android.graphics.Color.rgb(255, 170, 45)
        game.projectiles.filter { it.hostile }.forEach {
            canvas.drawCircle(it.x, it.y, 7f, paint)
        }

        // Main fighter: right-facing side profile, armored white/navy/gold.
        drawBrave(canvas, game.player.x, game.player.y, game.player.facing, 1.0f)

        companion?.let {
            drawBrave(
                canvas,
                controller.state.x,
                controller.state.y + 4f,
                game.player.facing,
                0.62f
            )
        }
    }

    private fun drawBrave(
        canvas: Canvas,
        x: Float,
        feetY: Float,
        facing: Int,
        scale: Float
    ) {
        canvas.save()
        canvas.translate(x, feetY)
        canvas.scale(if (facing >= 0) scale else -scale, scale)

        val white = android.graphics.Color.rgb(245, 247, 250)
        val navy = android.graphics.Color.rgb(18, 34, 64)
        val gold = android.graphics.Color.rgb(255, 171, 42)
        val visor = android.graphics.Color.rgb(238, 133, 43)
        val dark = android.graphics.Color.rgb(45, 52, 65)

        // Rear leg first: gives the silhouette a clear 90-degree combat stance.
        paint.color = navy
        canvas.drawRoundRect(RectF(-20f, -58f, 4f, -4f), 7f, 7f, paint)
        paint.color = white
        canvas.drawRoundRect(RectF(-17f, -54f, 1f, -8f), 5f, 5f, paint)
        paint.color = gold
        canvas.drawCircle(-8f, -48f, 4f, paint)

        // Forward leg.
        paint.color = navy
        canvas.drawRoundRect(RectF(10f, -64f, 34f, -4f), 7f, 7f, paint)
        paint.color = white
        canvas.drawRoundRect(RectF(13f, -60f, 31f, -10f), 5f, 5f, paint)
        paint.color = gold
        canvas.drawCircle(22f, -48f, 4f, paint)

        // Boots.
        paint.color = navy
        canvas.drawRoundRect(RectF(-23f, -10f, 7f, 2f), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(28f, -10f, 58f, 2f), 6f, 6f, paint)
        paint.color = white
        canvas.drawRoundRect(RectF(-18f, -8f, 4f, 0f), 4f, 4f, paint)
        canvas.drawRoundRect(RectF(33f, -8f, 54f, 0f), 4f, 4f, paint)

        // Torso.
        paint.color = navy
        canvas.drawRoundRect(RectF(-22f, -116f, 31f, -56f), 12f, 12f, paint)
        paint.color = white
        canvas.drawRoundRect(RectF(-16f, -111f, 25f, -61f), 9f, 9f, paint)

        // Chest solar emblem.
        paint.color = gold
        canvas.drawCircle(9f, -91f, 10f, paint)
        paint.color = white
        canvas.drawCircle(9f, -91f, 5f, paint)

        // Shoulder armor.
        paint.color = white
        canvas.drawOval(RectF(-34f, -113f, -4f, -91f), paint)
        paint.color = gold
        canvas.drawCircle(-21f, -102f, 5f, paint)

        // Rear arm and forward punching arm.
        paint.color = navy
        canvas.drawRoundRect(RectF(-39f, -99f, -20f, -55f), 8f, 8f, paint)
        paint.color = white
        canvas.drawRoundRect(RectF(-35f, -96f, -23f, -60f), 6f, 6f, paint)

        paint.color = navy
        canvas.drawRoundRect(RectF(20f, -101f, 42f, -67f), 8f, 8f, paint)
        paint.color = white
        canvas.drawRoundRect(RectF(23f, -98f, 39f, -70f), 6f, 6f, paint)

        // Raised fist.
        paint.color = navy
        canvas.drawRoundRect(RectF(38f, -104f, 55f, -87f), 7f, 7f, paint)
        paint.color = gold
        canvas.drawCircle(49f, -101f, 2.5f, paint)
        canvas.drawCircle(53f, -98f, 2.5f, paint)

        // Neck.
        paint.color = dark
        canvas.drawRect(0f, -122f, 14f, -112f, paint)

        // Helmet, strictly side-profile silhouette.
        paint.color = navy
        canvas.drawOval(RectF(-17f, -151f, 28f, -113f), paint)
        paint.color = white
        canvas.drawOval(RectF(-12f, -147f, 23f, -117f), paint)

        // Side visor only — never a front-facing pair of eyes.
        paint.color = visor
        canvas.drawRoundRect(RectF(13f, -141f, 34f, -128f), 5f, 5f, paint)

        // Helmet side disk and gold fins.
        paint.color = white
        canvas.drawCircle(-5f, -132f, 9f, paint)
        paint.color = navy
        canvas.drawCircle(-5f, -132f, 5f, paint)
        paint.color = gold
        val fin = android.graphics.Path()
        fin.moveTo(-2f, -148f)
        fin.lineTo(5f, -166f)
        fin.lineTo(9f, -147f)
        fin.close()
        canvas.drawPath(fin, paint)

        // Belt.
        paint.color = dark
        canvas.drawRoundRect(RectF(-21f, -64f, 32f, -56f), 3f, 3f, paint)
        paint.color = gold
        canvas.drawRect(RectF(3f, -64f, 9f, -56f), paint)

        canvas.restore()
    }

    private fun drawEnemy(canvas: Canvas, x: Float, feetY: Float) {
        paint.color = android.graphics.Color.rgb(31, 45, 70)
        canvas.drawRoundRect(RectF(x, feetY - 62f, x + 40f, feetY), 8f, 8f, paint)
        paint.color = android.graphics.Color.rgb(210, 219, 230)
        canvas.drawRoundRect(RectF(x + 7f, feetY - 54f, x + 33f, feetY - 14f), 6f, 6f, paint)
        paint.color = android.graphics.Color.rgb(225, 95, 65)
        canvas.drawCircle(x + 29f, feetY - 44f, 4f, paint)
        paint.color = android.graphics.Color.rgb(255, 171, 42)
        canvas.drawRect(x + 4f, feetY - 8f, x + 36f, feetY - 4f, paint)
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