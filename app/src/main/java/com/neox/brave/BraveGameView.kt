package com.neox.brave

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class BraveGameView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val adaptive = AdaptiveSystem()

    private var playerX = 180f
    private var playerY = 420f
    private var companion: CompanionProfile? = null
    private var message = "COLLECT 3 CORES"

    private val blocks = mutableListOf(
        RectF(500f, 370f, 560f, 430f),
        RectF(570f, 370f, 630f, 430f),
        RectF(640f, 370f, 700f, 430f)
    )

    private val cores = mutableListOf(
        Pair(530f, 330f) to Core.A,
        Pair(600f, 330f) to Core.B,
        Pair(670f, 330f) to Core.A
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(android.graphics.Color.rgb(9, 12, 20))

        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.rgb(24, 31, 48)
        canvas.drawRect(0f, height * 0.72f, width.toFloat(), height.toFloat(), paint)

        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(playerX, playerY, playerX + 42f, playerY + 72f, paint)

        paint.color = android.graphics.Color.rgb(80, 180, 255)
        for (block in blocks) canvas.drawRect(block, paint)

        for ((position, core) in cores) {
            paint.color = if (core == Core.A) {
                android.graphics.Color.rgb(255, 120, 80)
            } else {
                android.graphics.Color.rgb(100, 255, 180)
            }
            canvas.drawCircle(position.first, position.second, 16f, paint)
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 18f
            canvas.drawText(core.name, position.first - 6f, position.second + 6f, paint)
        }

        companion?.let {
            val cx = playerX + 60f
            val cy = playerY - 30f
            paint.color = android.graphics.Color.rgb(210, 210, 255)
            canvas.drawCircle(cx, cy, 22f, paint)
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 14f
            canvas.drawText(it.signature, cx - 12f, cy + 5f, paint)
        }

        paint.color = android.graphics.Color.WHITE
        paint.textSize = 24f
        canvas.drawText(message, 32f, 42f, paint)

        val pending = adaptive.pending().joinToString("")
        canvas.drawText("CORES: " + pending, 32f, 74f, paint)

        paint.textSize = 18f
        canvas.drawText("TAP LEFT/RIGHT TO MOVE · TAP CENTER TO COLLECT", 32f, height - 28f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN && event.action != MotionEvent.ACTION_MOVE) {
            return true
        }

        val x = event.x

        if (x < width * 0.33f) {
            playerX = max(20f, playerX - 12f)
        } else if (x > width * 0.66f) {
            playerX = min(width - 80f, playerX + 12f)
        } else {
            collectNearestCore()
        }

        invalidate()
        return true
    }

    private fun collectNearestCore() {
        if (cores.isEmpty()) return

        val nearest = cores.minByOrNull {
            kotlin.math.abs(it.first.first - playerX)
        } ?: return

        if (kotlin.math.abs(nearest.first.first - playerX) > 120f) {
            message = "MOVE CLOSER"
            return
        }

        cores.remove(nearest)
        val result = adaptive.collect(nearest.second)

        message = if (result == null) {
            "CORE " + nearest.second.name + " ACQUIRED"
        } else {
            companion = result
            "COMPANION " + result.signature + " ONLINE"
        }
    }
}