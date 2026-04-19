package com.paradoxo.amadeus.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.view.View
import android.view.animation.LinearInterpolator

class BorderGlowView(context: Context) : View(context) {

    private val dp = resources.displayMetrics.density
    private val borderWidth = dp * 10f

    private val colors = intArrayOf(
        Color.parseColor("#CC9C27B0"), // roxo
        Color.parseColor("#CC7B2FFF"), // violeta
        Color.parseColor("#CC2979FF"), // azul
        Color.parseColor("#CC00E5FF"), // ciano
        Color.parseColor("#CCFF1744"), // rosa
        Color.parseColor("#CC9C27B0"), // roxo (fecha o loop)
    )

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = borderWidth * 3.5f
        maskFilter = BlurMaskFilter(borderWidth * 2.5f, BlurMaskFilter.Blur.NORMAL)
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
    }

    private var rotationAngle = 0f
    private val matrix = Matrix()
    private var gradient: SweepGradient? = null

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 3500L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { anim ->
            rotationAngle = anim.animatedValue as Float
            invalidate()
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        gradient = SweepGradient(w / 2f, h / 2f, colors, null)
    }

    override fun onDraw(canvas: Canvas) {
        val g = gradient ?: return
        val w = width.toFloat()
        val h = height.toFloat()

        matrix.reset()
        matrix.postRotate(rotationAngle, w / 2f, h / 2f)
        g.setLocalMatrix(matrix)

        val halfBorder = borderWidth / 2f
        val borderRect = RectF(halfBorder, halfBorder, w - halfBorder, h - halfBorder)

        val halfGlow = (borderWidth * 3.5f) / 2f
        val glowRect = RectF(halfGlow, halfGlow, w - halfGlow, h - halfGlow)

        glowPaint.shader = g
        canvas.drawRect(glowRect, glowPaint)

        borderPaint.shader = g
        canvas.drawRect(borderRect, borderPaint)
    }

    fun startGlow() {
        if (!animator.isRunning) animator.start()
    }

    fun stopGlow() {
        animator.cancel()
    }
}
