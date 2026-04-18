package com.iidx.controller

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class TurntableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var rotationAngle = 0f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C1C1C"); style = Paint.Style.FILL
    }
    private val labelRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333"); style = Paint.Style.FILL
    }
    private val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#555555"); style = Paint.Style.STROKE; strokeWidth = 2.5f
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DD2222"); style = Paint.Style.FILL
    }
    private val spindlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888"); style = Paint.Style.FILL
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC"); style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#666666")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1f
    }

    fun rotateDelta(angleDegrees: Float) {
        rotationAngle = (rotationAngle + angleDegrees) % 360f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val cx = width  / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) * 0.90f

        // Outer disc
        canvas.drawCircle(cx, cy, radius, bgPaint)

        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)

        // Vinyl grooves
        groovePaint.color = Color.argb(50, 80, 80, 80)
        for (r in 3..10) {
            val rr = radius * 0.30f + radius * 0.60f * r / 10f
            canvas.drawCircle(cx, cy, rr, groovePaint)
        }

        // Label ring
        canvas.drawCircle(cx, cy, radius * 0.42f, labelRingPaint)

        // Stripe ticks on label
        for (i in 0 until 16) {
            val angle = Math.toRadians(i * 22.5)
            val x1 = cx + (radius * 0.28f * cos(angle)).toFloat()
            val y1 = cy + (radius * 0.28f * sin(angle)).toFloat()
            val x2 = cx + (radius * 0.40f * cos(angle)).toFloat()
            val y2 = cy + (radius * 0.40f * sin(angle)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, stripePaint)
        }

        // Position marker
        canvas.drawCircle(cx + radius * 0.30f, cy, radius * 0.04f, markerPaint)

        canvas.restore()

        // Spindle (static, doesn't rotate)
        canvas.drawCircle(cx, cy, radius * 0.06f, centerPaint)
        canvas.drawCircle(cx, cy, radius * 0.025f, spindlePaint)

        // Label text
        val textSize = maxOf(radius * 0.10f, 8f)
        textPaint.textSize = textSize
        canvas.drawText("TURNTABLE", cx, cy + radius * 0.62f, textPaint)
    }
}
