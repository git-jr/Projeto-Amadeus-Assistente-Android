package com.paradoxo.amadeus.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.ActionType
import com.paradoxo.amadeus.modelo.UiElement
import com.paradoxo.amadeus.util.GerenciaApp
import com.paradoxo.amadeus.view.TouchFeedbackView

class AmadeusAccessibilityService : AccessibilityService() {

    private val feedbackWindowManager: WindowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }
    private var touchFeedbackView: TouchFeedbackView? = null
    private val actionHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    fun cancelPendingActions() {
        actionHandler.removeCallbacksAndMessages(null)
    }

    // ── Action execution ──────────────────────────────────────────────────────

    fun executeActions(
        actions: List<ActionIntent>,
        onFeedback: (String) -> Unit,
        onDone: () -> Unit
    ) {
        actionHandler.removeCallbacksAndMessages(null)
        val handler = actionHandler
        var cumulativeDelay = 500L

        actions.forEachIndexed { index, action ->
            val thisDelay = cumulativeDelay
            cumulativeDelay += when (action.type) {
                ActionType.WAIT -> action.value.toLongOrNull() ?: 1000L
                else -> 1400L
            }
            handler.postDelayed({
                when (action.type) {
                    ActionType.CLICK -> {
                        val target = action.target
                        onFeedback("Clicando em: $target")
                        findAndClick(target) { }
                    }

                    ActionType.CLICK_COORDS -> {
                        val parts = action.value.split(",")
                        val x = parts.getOrNull(0)?.trim()?.toFloatOrNull()
                        val y = parts.getOrNull(1)?.trim()?.toFloatOrNull()
                        if (x != null && y != null) {
                            val label = action.target.ifBlank { "(${x.toInt()},${y.toInt()})" }
                            onFeedback("Tocando em: $label")
                            val bounds = Rect(x.toInt() - 20, y.toInt() - 20, x.toInt() + 20, y.toInt() + 20)
                            showTouchFeedback(bounds, ActionType.CLICK)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    tapAtCoordinates(x, y)
                                }, 300)
                            }
                        }
                    }

                    ActionType.TYPE -> {
                        onFeedback("Digitando: ${action.value}")
                        typeText(action.value)
                    }

                    ActionType.OPEN_APP -> {
                        onFeedback("Abrindo: ${action.target}")
                        GerenciaApp.encontrarApp(action.target, applicationContext)
                    }

                    ActionType.SCROLL -> {
                        val direction = action.value.lowercase().trim()
                        val label = if (direction == "up") "para cima" else "para baixo"
                        onFeedback("Rolando tela $label")
                        scrollScreen(direction)
                    }

                    ActionType.SCROLL_HORIZONTAL -> {
                        val direction = action.value.lowercase().trim()
                        val label = if (direction == "left") "para esquerda" else "para direita"
                        onFeedback("Rolando $label")
                        scrollHorizontal(direction)
                    }

                    ActionType.IME_ACTION -> {
                        onFeedback("Confirmando...")
                        performImeAction()
                    }

                    ActionType.BACK -> {
                        onFeedback("Voltando...")
                        // delay para o feedback aparecer antes da ação
                        showGlobalActionFeedback(ActionType.BACK)
                        Handler(Looper.getMainLooper()).postDelayed({
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }, 300)
                    }

                    ActionType.HOME -> {
                        onFeedback("Indo para home...")
                        // delay para o feedback aparecer antes da ação
                        showGlobalActionFeedback(ActionType.HOME)
                        Handler(Looper.getMainLooper()).postDelayed({
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        }, 300)
                    }

                    ActionType.RECENTS -> {
                        onFeedback("Abrindo recentes...")
                        // delay para o feedback aparecer antes da ação
                        showGlobalActionFeedback(ActionType.RECENTS)
                        Handler(Looper.getMainLooper()).postDelayed({
                            performGlobalAction(GLOBAL_ACTION_RECENTS)
                        }, 300)
                    }

                    ActionType.NOTIFICATIONS -> {
                        onFeedback("Abrindo notificações...")
                        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                    }

                    ActionType.QUICK_SETTINGS -> {
                        onFeedback("Abrindo configurações rápidas...")
                        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                    }

                    ActionType.WAIT -> {
                        val ms = action.value.toLongOrNull() ?: 1000L
                        onFeedback("Aguardando ${ms}ms...")
                    }
                }
                if (index == actions.lastIndex) {
                    handler.postDelayed({ onDone() }, 700)
                }
            }, thisDelay)
        }
    }

    // ── Touch feedback overlay ────────────────────────────────────────────────

    private fun showTouchFeedback(bounds: Rect, type: ActionType) {
        val view = buildFeedbackView()
        view.show(bounds, type)
        mountFeedbackView(view)
    }

    private fun showSwipeFeedback(fromX: Float, fromY: Float, toX: Float, toY: Float, type: ActionType) {
        val view = buildFeedbackView()
        view.showSwipe(fromX, fromY, toX, toY, type)
        mountFeedbackView(view)
    }

    private fun showGlobalActionFeedback(type: ActionType) {
        val view = buildFeedbackView()
        view.showGlobalAction(type)
        mountFeedbackView(view)
    }

    private fun buildFeedbackView(): TouchFeedbackView {
        removeTouchFeedback()
        return TouchFeedbackView(this).also { it.onDone = { removeTouchFeedback() } }
    }

    private fun mountFeedbackView(view: TouchFeedbackView) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        try {
            feedbackWindowManager.addView(view, params)
            touchFeedbackView = view
        } catch (_: Exception) {
        }
    }

    private fun removeTouchFeedback() {
        touchFeedbackView?.let {
            try {
                feedbackWindowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        touchFeedbackView = null
    }

    // ── Screen elements ───────────────────────────────────────────────────────

    fun getVisibleElements(): List<UiElement> {
        val result = mutableListOf<UiElement>()
        windows.mapNotNull { it.root }.forEach { root -> extractNodes(root, result) }
        return result
    }

    private fun extractNodes(node: AccessibilityNodeInfo?, into: MutableList<UiElement>) {
        if (node == null) return
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            into.add(
                UiElement(
                    text = node.text?.toString(),
                    contentDescription = node.contentDescription?.toString(),
                    className = node.className?.toString(),
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    bounds = bounds
                )
            )
        }
        for (i in 0 until node.childCount) extractNodes(node.getChild(i), into)
    }

    // ── Click ─────────────────────────────────────────────────────────────────

    fun clickOnText(targetText: String, onResult: (found: Boolean) -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            findAndClick(targetText, onResult)
        }, 400)
    }

    private fun findAndClick(targetText: String, onResult: (Boolean) -> Unit) {
        val nodes = windows
            .mapNotNull { it.root }
            .flatMap { root -> root.findAccessibilityNodeInfosByText(targetText) ?: emptyList() }

        val node = nodes.firstOrNull {
            it.text?.toString()
                ?.lowercase() == targetText.lowercase() || it.contentDescription?.toString()
                ?.lowercase() == targetText.lowercase()
        }
        if (node == null) {
            onResult(false)
            return
        }

        val clickableNode = if (node.isClickable) node else findClickableParent(node)
        if (clickableNode != null) {
            val bounds = Rect()
            clickableNode.getBoundsInScreen(bounds)
            // delay para o feedback aparecer antes do clique
            showTouchFeedback(bounds, ActionType.CLICK)
            Handler(Looper.getMainLooper()).postDelayed({
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                onResult(true)
            }, 300)
        } else {
            onResult(false)
        }
    }

    private fun findClickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current = node.parent
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    // ── Type ──────────────────────────────────────────────────────────────────

    private fun typeText(value: String) {
        val node = findFirstEditableNode() ?: return
        // ACTION_CLICK antes de setar texto garante que o campo tem foco e o teclado está visível
        // — necessário para que IME_ACTION funcione depois
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        // delay para o feedback aparecer antes da digitação
        showTouchFeedback(bounds, ActionType.TYPE)
        Handler(Looper.getMainLooper()).postDelayed({
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }, 300)
    }

    private fun findFirstEditableNode(): AccessibilityNodeInfo? {
        val roots = windows.mapNotNull { it.root }
        for (root in roots) {
            val node = findEditableInTree(root)
            if (node != null) return node
        }
        return null
    }

    private fun findEditableInTree(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val found = findEditableInTree(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    // ── IME action ────────────────────────────────────────────────────────────

    private fun performImeAction() {
        val focusedNode = windows.mapNotNull { it.root }
            .firstNotNullOfOrNull { root -> root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }
        val editableNode = focusedNode
            ?: windows.mapNotNull { it.root }.firstNotNullOfOrNull { findEditableInTree(it) }

        if (editableNode != null) {
            val bounds = Rect()
            editableNode.getBoundsInScreen(bounds)
            showTouchFeedback(bounds, ActionType.IME_ACTION)
        }

        // delay para o feedback aparecer antes da confirmação
        Handler(Looper.getMainLooper()).postDelayed({
            // Estratégia 1: ACTION_IME_ENTER (API 30+)
            // Aciona diretamente a ação do teclado (busca, enviar, ir, etc.) no campo focado.
            if (editableNode != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val ok = editableNode.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
                if (ok) return@postDelayed
            }

            // Estratégia 2: setar texto atual + "\n"
            // Fallback para ROMs que não implementam ACTION_IME_ENTER via acessibilidade.
            // Funciona em campos de busca single-line que tratam newline como submit.
            if (editableNode != null) {
                val current = editableNode.text?.toString() ?: ""
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "$current\n")
                }
                val ok = editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                if (ok) return@postDelayed
            }

            // Estratégia 3: tap por coordenadas estimadas no canto inf-dir da janela do teclado (API 24+)
            // Último recurso para teclados (ex: Samsung) que não expõem teclas como nós de acessibilidade.
            // Layout típico: 4 linhas de teclas, botão de ação sempre na última linha, coluna mais à direita.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return@postDelayed
            val imeWindow = windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
                ?: return@postDelayed
            val imeBounds = Rect()
            imeWindow.getBoundsInScreen(imeBounds)

            val keyHeight = imeBounds.height() / 4f
            val keyWidth = imeBounds.width() / 10f
            val x = imeBounds.right - keyWidth / 2f
            val y = imeBounds.bottom - keyHeight / 2f
            tapAtCoordinates(x, y)
        }, 300)
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.N)
    private fun tapAtCoordinates(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y); lineTo(x + 1f, y + 1f) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 500)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    // ── Scroll ────────────────────────────────────────────────────────────────

    private fun scrollScreen(direction: String) {
        // Estratégia 1: gesto de swipe — funciona em qualquer app independente da árvore de acessibilidade
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val w = resources.displayMetrics.widthPixels
            val h = resources.displayMetrics.heightPixels
            val x = w / 2f
            if (direction == "up") {
                val fromY = h * 0.3f
                // delay para o feedback aparecer antes do scroll
                showSwipeFeedback(x, fromY, x, h * 0.7f, ActionType.SCROLL)
                Handler(Looper.getMainLooper()).postDelayed({
                    swipe(x, fromY, x, h * 0.7f) // swipe para baixo = rola para cima
                }, 300)
            } else {
                val fromY = h * 0.7f
                // delay para o feedback aparecer antes do scroll
                showSwipeFeedback(x, fromY, x, h * 0.3f, ActionType.SCROLL)
                Handler(Looper.getMainLooper()).postDelayed({
                    swipe(x, fromY, x, h * 0.3f) // swipe para cima = rola para baixo
                }, 300)
            }
            return
        }

        // Estratégia 2: ACTION_SCROLL no nó rolável — fallback para API < 24 sem suporte a gestos
        val action = if (direction == "up") {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
        findScrollableNode()?.performAction(action)
    }

    private fun scrollHorizontal(direction: String) {
        // Estratégia 1: gesto de swipe horizontal
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val w = resources.displayMetrics.widthPixels
            val h = resources.displayMetrics.heightPixels
            val y = h / 2f
            if (direction == "left") {
                val fromX = w * 0.7f
                // delay para o feedback aparecer antes do scroll
                showSwipeFeedback(fromX, y, w * 0.3f, y, ActionType.SCROLL_HORIZONTAL)
                Handler(Looper.getMainLooper()).postDelayed({
                    swipe(fromX, y, w * 0.3f, y) // swipe para esquerda = avança (ex: próximo item)
                }, 300)
            } else {
                val fromX = w * 0.3f
                // delay para o feedback aparecer antes do scroll
                showSwipeFeedback(fromX, y, w * 0.7f, y, ActionType.SCROLL_HORIZONTAL)
                Handler(Looper.getMainLooper()).postDelayed({
                    swipe(fromX, y, w * 0.7f, y) // swipe para direita = volta (ex: item anterior)
                }, 300)
            }
            return
        }

        // Estratégia 2: ACTION_SCROLL no nó rolável — fallback para API < 24
        val action = if (direction == "left") {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        findScrollableNode()?.performAction(action)
    }

    private fun findScrollableNode(): AccessibilityNodeInfo? {
        for (root in windows.mapNotNull { it.root }) {
            val found = findScrollableInTreeDeep(root)
            if (found != null) return found
        }
        return null
    }

    private fun findScrollableInTreeDeep(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        var deepest: AccessibilityNodeInfo? = null
        for (i in 0 until node.childCount) {
            val found = findScrollableInTreeDeep(node.getChild(i))
            if (found != null) deepest = found  // não para — continua buscando mais fundo
        }
        if (deepest != null) return deepest
        return if (node.isScrollable) node else null
    }


    private fun findScrollableInTreeFirst(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findScrollableInTreeFirst(node.getChild(i))
            if (found != null) return found
        }
        return null
    }


    @androidx.annotation.RequiresApi(Build.VERSION_CODES.N)
    private fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(toX, toY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 300)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg != packageName) currentForegroundPackage = pkg
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        removeTouchFeedback()
        instance = null
    }

    companion object {
        private var instance: AmadeusAccessibilityService? = null
        var currentForegroundPackage: String = ""

        fun isActive(context: Context): Boolean {
            val serviceName =
                "${context.packageName}/${AmadeusAccessibilityService::class.java.canonicalName}"
            val activeServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return activeServices.split(':').any { it.equals(serviceName, ignoreCase = true) }
        }

        fun openSettings(context: Context) {
            val componentName =
                "${context.packageName}/${AmadeusAccessibilityService::class.java.canonicalName}"

            // Deep-link direto para o toggle do serviço (funciona na maioria dos ROMs)
            val deepLink = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                val args = android.os.Bundle().apply {
                    putString(":settings:fragment_args_key", componentName)
                }
                putExtra(":settings:show_fragment_args", args)
                putExtra(":settings:fragment_args_key", componentName)
            }

            try {
                context.startActivity(deepLink)
            } catch (e: Exception) {
                Log.w("openSettings", "Erro ao abrir as configurações de acessibilidade: ${e.message}")
                // Fallback para a lista genérica de acessibilidade
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
        }

        fun clickOnText(targetText: String, onResult: (found: Boolean) -> Unit): Boolean {
            val service = instance ?: return false
            service.clickOnText(targetText, onResult)
            return true
        }

        fun cancelPendingActions() {
            instance?.cancelPendingActions()
        }

        fun executeActions(
            actions: List<ActionIntent>,
            onFeedback: (String) -> Unit,
            onDone: () -> Unit
        ): Boolean {
            val service = instance ?: return false
            service.executeActions(actions, onFeedback, onDone)
            Log.w("AgenteLog", "AgenteLog Açoes sendo sendo executadas: \n$actions")
            return true
        }

        fun getVisibleElements(): List<UiElement> = instance?.getVisibleElements() ?: emptyList()

        fun performImeAction(): Boolean {
            val service = instance ?: return false
            service.performImeAction()
            return true
        }
    }
}
