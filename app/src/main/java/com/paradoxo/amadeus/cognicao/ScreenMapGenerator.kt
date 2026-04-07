package com.paradoxo.amadeus.cognicao

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.paradoxo.amadeus.modelo.UiElement
import java.io.File
import java.io.FileOutputStream

class ScreenMapGenerator(private val context: Context) {

    private val scale = 0.5f

    private val paintClickable = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC00E676")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val paintEditable = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC00B0FF")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.MONOSPACE
        style = Paint.Style.FILL
    }

    private val paintLabelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC000000")
        style = Paint.Style.FILL
    }

    private val statusBarHeight: Int by lazy {
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
    }

    fun generate(
        elements: List<UiElement>,
        screenW: Int,
        screenH: Int,
        sessionId: String,
        stepNumber: Int
    ): Bitmap {
        val interactive = elements.filter {
            (it.isClickable || it.isEditable) && it.labelLegivel.isNotBlank()
        }

        val bmpW = (screenW * scale).toInt()
        val bmpH = (screenH * scale).toInt()
        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#111827"))

        interactive.forEachIndexed { i, el ->
            val b = el.bounds
            val left = b.left * scale
            val top = (b.top - statusBarHeight) * scale
            val right = b.right * scale
            val bottom = (b.bottom - statusBarHeight) * scale

            if (left >= right || top >= bottom) return@forEachIndexed

            val paint = if (el.isEditable) paintEditable else paintClickable
            canvas.drawRect(left, top, right, bottom, paint)

            val label = "[$i]"
            val labelW = paintLabel.measureText(label)
            val labelH = paintLabel.textSize
            canvas.drawRect(left, top, left + labelW + 6, top + labelH + 4, paintLabelBg)
            canvas.drawText(label, left + 3, top + labelH, paintLabel)
        }

        saveToDisk(bitmap, sessionId, stepNumber)
        return bitmap
    }

    private fun saveToDisk(bitmap: Bitmap, sessionId: String, stepNumber: Int) {
        try {
            val dir = File(context.cacheDir, "agent_debug/$sessionId")
            dir.mkdirs()
            val file = File(dir, "step_$stepNumber.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        } catch (_: Exception) { }
    }
}
