package com.paradoxo.amadeus.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.paradoxo.amadeus.modelo.UiElement

class ScreenHighlightView(context: Context) : View(context) {

    var elements: List<UiElement> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val strokeWidth = resources.displayMetrics.density * 2.5f

    private val paintClickable = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC00E676") // green
        style = Paint.Style.STROKE
        strokeWidth = this@ScreenHighlightView.strokeWidth
    }

    private val paintEditable = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC00B0FF") // cyan-blue
        style = Paint.Style.STROKE
        strokeWidth = this@ScreenHighlightView.strokeWidth
    }

    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99FFFFFF") // semi-transparent white
        style = Paint.Style.STROKE
        strokeWidth = this@ScreenHighlightView.strokeWidth * 0.7f
    }

    // getBoundsInScreen() retorna coordenadas absolutas (y=0 = topo físico da tela,
    // incluindo a status bar). O View do overlay começa ABAIXO da status bar, então
    // subtraímos a altura da status bar para alinhar corretamente.
    private val statusBarHeight: Int by lazy {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resId > 0) resources.getDimensionPixelSize(resId) else 0
    }

    override fun onDraw(canvas: Canvas) {
        val offsetY = statusBarHeight.toFloat()

        for (el in elements) {
            val b = el.bounds
            if (b.isEmpty) continue

            val paint = when {
                el.isEditable -> paintEditable
                el.isClickable -> paintClickable
                el.labelLegivel.isNotBlank() -> paintText
                else -> continue
            }

            canvas.drawRect(b.left.toFloat(), b.top - offsetY, b.right.toFloat(), b.bottom - offsetY, paint)
        }
    }
}
