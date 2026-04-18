package com.paradoxo.amadeus.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.graphics.toColorInt
import com.paradoxo.amadeus.modelo.ActionType

class TouchFeedbackView(context: Context) : View(context) {

    private val dp = resources.displayMetrics.density

    // getBoundsInScreen() returns absolute screen coordinates (y=0 = top of physical screen,
    // including the status bar). The overlay view starts below the status bar, so we subtract
    // status bar height to keep the ripple aligned with the touched element.
    private val statusBarHeight: Int by lazy {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resId > 0) resources.getDimensionPixelSize(resId) else 0
    }

    // ── Cores ──────────────────────────────────────────────────────────────────

    private val colorClick       = "#ff002b".toColorInt()
    private val colorType        = "#29B6F6".toColorInt()
    private val colorScroll      = "#EEEEEE".toColorInt()
    private val colorBack        = Color.WHITE
    private val colorBackHalo    = Color.RED
    private val colorHome        = Color.WHITE
    private val colorRecents     = Color.WHITE

    // ── Alfas (0–255) ──────────────────────────────────────────────────────────

    private val alphaRippleHalo   = 0.25f   // halo do ripple de clique/tipo
    private val alphaSwipeTrail   = 0.35f   // trilha da animação de scroll
    private val alphaSwipeOrigin  = 0.50f   // ponto de origem que some no scroll
    private val alphaBackHalo     = 0.50f   // halo vermelho atrás da seta de voltar
    private val alphaHomeFill     = 0.50f   // preenchimento da casa (home)
    private val alphaCardBack     = 0.10f   // preenchimento do card de trás (recentes)
    private val alphaCardBackRing = 0.55f   // borda do card de trás (recentes)
    private val alphaCardFront    = 0.20f   // preenchimento do card da frente (recentes)
    private val alphaCardLine     = 0.40f   // linha de título dentro do card (recentes)

    // ── Paints ─────────────────────────────────────────────────────────────────

    private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val paintRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp * 3f
    }
    private val paintTrail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp * 4f
        strokeCap = Paint.Cap.ROUND
    }
    private val paintArrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp * 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // ── Estado ─────────────────────────────────────────────────────────────────

    private var cx = 0f
    private var cy = 0f
    private var animFraction = 0f
    private var actionType = ActionType.CLICK

    private var isSwipe = false
    private var swipeFromX = 0f
    private var swipeFromY = 0f
    private var swipeToX = 0f
    private var swipeToY = 0f

    private var isGlobalAction = false

    var onDone: (() -> Unit)? = null

    // ── API pública ────────────────────────────────────────────────────────────

    fun show(rect: Rect, type: ActionType) {
        isSwipe = false
        isGlobalAction = false
        cx = rect.exactCenterX()
        cy = rect.exactCenterY() - statusBarHeight
        actionType = type
        startRippleAnim()
    }

    fun showAt(x: Float, y: Float, type: ActionType) {
        isSwipe = false
        isGlobalAction = false
        cx = x
        cy = y - statusBarHeight
        actionType = type
        startRippleAnim()
    }

    fun showSwipe(fromX: Float, fromY: Float, toX: Float, toY: Float, type: ActionType) {
        isSwipe = true
        isGlobalAction = false
        swipeFromX = fromX
        swipeFromY = fromY - statusBarHeight
        swipeToX = toX
        swipeToY = toY - statusBarHeight
        actionType = type
        startSwipeAnim()
    }

    fun showGlobalAction(type: ActionType) {
        isSwipe = false
        isGlobalAction = true
        actionType = type
        startGlobalActionAnim()
    }

    // ── Animações ──────────────────────────────────────────────────────────────

    private fun startRippleAnim() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator(1.4f)
            addUpdateListener { animFraction = it.animatedFraction; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { onDone?.invoke() }
            })
            start()
        }
    }

    private fun startSwipeAnim() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            interpolator = LinearInterpolator()
            addUpdateListener { animFraction = it.animatedFraction; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { onDone?.invoke() }
            })
            start()
        }
    }

    private fun startGlobalActionAnim() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator(1.2f)
            addUpdateListener { animFraction = it.animatedFraction; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { onDone?.invoke() }
            })
            start()
        }
    }

    // ── Draw ───────────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        if (animFraction <= 0f) return
        when {
            isGlobalAction -> drawGlobalAction(canvas)
            isSwipe -> drawSwipe(canvas)
            else -> drawRipple(canvas)
        }
    }

    private fun drawRipple(canvas: Canvas) {
        val t = animFraction
        val maxR = dp * 54f
        val r = maxR * t
        val alpha = ((1f - t) * 255f).toInt().coerceIn(0, 255)
        val color = colorForType()

        paintFill.color = color
        paintFill.alpha = (alpha * alphaRippleHalo).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx, cy, r, paintFill)

        paintRing.color = color
        paintRing.alpha = alpha
        canvas.drawCircle(cx, cy, r, paintRing)

        val dotR = (dp * 12f * (1f - t * 0.7f)).coerceAtLeast(0f)
        paintFill.alpha = alpha
        canvas.drawCircle(cx, cy, dotR, paintFill)
    }

    private fun drawSwipe(canvas: Canvas) {
        val t = animFraction
        val currentX = swipeFromX + (swipeToX - swipeFromX) * t
        val currentY = swipeFromY + (swipeToY - swipeFromY) * t
        val alpha = when {
            t < 0.15f -> ((t / 0.15f) * 255f).toInt()
            t > 0.8f  -> (((1f - t) / 0.2f) * 255f).toInt()
            else      -> 255
        }.coerceIn(0, 255)
        val color = colorForType()

        paintTrail.color = color
        paintTrail.alpha = (alpha * alphaSwipeTrail).toInt().coerceIn(0, 255)
        canvas.drawLine(swipeFromX, swipeFromY, currentX, currentY, paintTrail)

        paintRing.color = color
        paintRing.alpha = alpha
        canvas.drawCircle(currentX, currentY, dp * 22f, paintRing)

        paintFill.color = color
        paintFill.alpha = alpha
        canvas.drawCircle(currentX, currentY, dp * 12f, paintFill)

        val originAlpha = ((1f - t) * alpha * alphaSwipeOrigin).toInt().coerceIn(0, 255)
        paintFill.alpha = originAlpha
        canvas.drawCircle(swipeFromX, swipeFromY, dp * 8f, paintFill)
    }

    private fun drawGlobalAction(canvas: Canvas) {
        when (actionType) {
            ActionType.BACK    -> drawBack(canvas)
            ActionType.HOME    -> drawHome(canvas)
            ActionType.RECENTS -> drawRecents(canvas)
            else               -> Unit
        }
    }

    // Seta < que entra da borda esquerda e desliza para dentro, simulando o gesto de back
    private fun drawBack(canvas: Canvas) {
        val t = animFraction
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = (resources.displayMetrics.heightPixels - statusBarHeight).toFloat()

        val alpha = when {
            t < 0.3f -> ((t / 0.3f) * 255f).toInt()
            t > 0.7f -> (((1f - t) / 0.3f) * 255f).toInt()
            else     -> 255
        }.coerceIn(0, 255)

        val slideX  = screenW * 0.15f * t
        val centerY = screenH * 0.5f
        val arrowHalf = dp * 32f
        val arrowTip  = dp * 28f

        val path = Path().apply {
            moveTo(slideX + arrowTip, centerY - arrowHalf)
            lineTo(slideX, centerY)
            lineTo(slideX + arrowTip, centerY + arrowHalf)
        }

        paintFill.color = colorBackHalo
        paintFill.alpha = (alpha * alphaBackHalo).toInt()
        canvas.drawCircle(slideX + arrowTip * 0.5f, centerY, arrowHalf * 1.4f, paintFill)

        paintArrow.color = colorBack
        paintArrow.alpha = alpha
        canvas.drawPath(path, paintArrow)
    }

    // Casa que escala no centro da tela — representa ir para home
    private fun drawHome(canvas: Canvas) {
        val t = animFraction
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = (resources.displayMetrics.heightPixels - statusBarHeight).toFloat()
        val cx = screenW / 2f
        val cy = screenH / 2f

        val alpha = when {
            t < 0.25f -> ((t / 0.25f) * 255f).toInt()
            t > 0.75f -> (((1f - t) / 0.25f) * 255f).toInt()
            else      -> 255
        }.coerceIn(0, 255)

        val scale = if (t < 0.4f) t / 0.4f else 1f
        val s = dp * 44f * scale

        val roofPath = Path().apply {
            moveTo(cx, cy - s)
            lineTo(cx - s, cy - s * 0.1f)
            lineTo(cx + s, cy - s * 0.1f)
            close()
        }
        val bL = cx - s * 0.7f;  val bT = cy - s * 0.1f
        val bR = cx + s * 0.7f;  val bB = cy + s * 0.9f
        val dW = s * 0.35f;      val dH = s * 0.5f
        val dL = cx - dW / 2f;   val dT = bB - dH

        paintFill.color = colorHome
        paintFill.alpha = (alpha * alphaHomeFill).toInt()
        canvas.drawPath(roofPath, paintFill)
        canvas.drawRect(bL, bT, bR, bB, paintFill)

        paintArrow.color = colorHome
        paintArrow.alpha = alpha
        paintArrow.strokeWidth = dp * 4f
        canvas.drawPath(roofPath, paintArrow)
        paintArrow.strokeWidth = dp * 3f
        canvas.drawRect(bL, bT, bR, bB, paintArrow)
        canvas.drawRect(dL, dT, dL + dW, bB, paintArrow)
        paintArrow.strokeWidth = dp * 5f  // restaura default
    }

    // Dois cards empilhados que se abrem — representa recentes
    private fun drawRecents(canvas: Canvas) {
        val t = animFraction
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = (resources.displayMetrics.heightPixels - statusBarHeight).toFloat()
        val cx = screenW / 2f
        val cy = screenH / 2f

        val alpha = when {
            t < 0.2f  -> ((t / 0.2f) * 255f).toInt()
            t > 0.75f -> (((1f - t) / 0.25f) * 255f).toInt()
            else      -> 255
        }.coerceIn(0, 255)

        val scale = if (t < 0.35f) t / 0.35f else 1f
        val cardW = dp * 72f * scale
        val cardH = dp * 104f * scale
        val cornerR = dp * 8f * scale

        val spread = dp * 18f * (minOf(t, 0.5f) / 0.5f) * scale

        val c2L = cx - cardW / 2f - spread * 0.5f
        val c2T = cy - cardH / 2f - spread
        val c1L = cx - cardW / 2f + spread * 0.4f
        val c1T = cy - cardH / 2f + spread * 0.3f

        paintFill.color = colorRecents
        paintRing.color = colorRecents

        // Card de trás
        paintFill.alpha = (alpha * alphaCardBack).toInt()
        canvas.drawRoundRect(c2L, c2T, c2L + cardW, c2T + cardH, cornerR, cornerR, paintFill)
        paintRing.strokeWidth = dp * 2.5f
        paintRing.alpha = (alpha * alphaCardBackRing).toInt()
        canvas.drawRoundRect(c2L, c2T, c2L + cardW, c2T + cardH, cornerR, cornerR, paintRing)

        // Card da frente
        paintFill.alpha = (alpha * alphaCardFront).toInt()
        canvas.drawRoundRect(c1L, c1T, c1L + cardW, c1T + cardH, cornerR, cornerR, paintFill)
        paintRing.strokeWidth = dp * 3f
        paintRing.alpha = alpha
        canvas.drawRoundRect(c1L, c1T, c1L + cardW, c1T + cardH, cornerR, cornerR, paintRing)

        // Linha de título dentro do card da frente
        val lineY = c1T + cardH * 0.18f
        paintRing.strokeWidth = dp * 2.5f
        paintRing.alpha = (alpha * alphaCardLine).toInt()
        canvas.drawLine(c1L + dp * 10f, lineY, c1L + cardW - dp * 10f, lineY, paintRing)
        paintRing.strokeWidth = dp * 3f  // restaura default
    }

    private fun colorForType() = when (actionType) {
        ActionType.CLICK                                    -> colorClick
        ActionType.TYPE, ActionType.IME_ACTION              -> colorType
        ActionType.SCROLL, ActionType.SCROLL_HORIZONTAL     -> colorScroll
        else                                                -> colorClick
    }
}
