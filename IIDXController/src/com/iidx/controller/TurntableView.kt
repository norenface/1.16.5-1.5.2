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
        color = Color.parseColor("#1C1C1C")
        style = Paint.Style.FILL
    }

    private val labelRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.FILL
    }

    private val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#444444")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DD2222")
        style = Paint.Style.FILL
    }

    private val spindle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#666666")
        textSize = 24f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC")
        style = Paint.Style.FILL
    }

    fun rotateDelta(angleDegrees: Float) {
        rotationAngle += angleDegrees
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) * 0.92f

        // Outer record
        canvas.drawCircle(cx, cy, radius, bgPaint)

        // Rotating content
        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)

        // Grooves (concentric rings)
        for (r in 3..10) {
            val rr = radius * 0.3f + (radius * 0.6f * r / 10f)
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(60, 80, 80, 80)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawCircle(cx, cy, rr, ringPaint)
        }

        // Label ring
        canvas.drawCircle(cx, cy, radius * 0.42f, labelRingPaint)

        // Stripe markers on label
        for (i in 0 until 16) {
            val angle = Math.toRadians((i * 22.5).toDouble())
            val x1 = cx + (radius * 0.28f * cos(angle)).toFloat()
            val y1 = cy + (radius * 0.28f * sin(angle)).toFloat()
            val x2 = cx + (radius * 0.40f * cos(angle)).toFloat()
            val y2 = cy + (radius * 0.40f * sin(angle)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, stripePaint)
        }

        // Position marker dot
        canvas.drawCircle(
            cx + radius * 0.30f,
            cy,
            radius * 0.04f,
            markerPaint
        )

        canvas.restore()

        // Center spindle (doesn't rotate)
        canvas.drawCircle(cx, cy, radius * 0.06f, centerPaint)
        canvas.drawCircle(cx, cy, radius * 0.025f, spindle)

        // Label text
        textPaint.textSize = radius * 0.12f
        canvas.drawText("TURNTABLE", cx, cy + radius * 0.60f, textPaint)
    }
}
