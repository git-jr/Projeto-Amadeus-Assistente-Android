package com.paradoxo.amadeus.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.ActionType
import com.paradoxo.amadeus.modelo.UiElement
import com.paradoxo.amadeus.util.GerenciaApp

class AmadeusAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    // ── Action execution ──────────────────────────────────────────────────────

    fun executeActions(
        actions: List<ActionIntent>,
        onFeedback: (String) -> Unit,
        onDone: () -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())
        val initialDelay = 500L
        val delayPerAction = 1000L

        actions.forEachIndexed { index, action ->
            handler.postDelayed({
                when (action.type) {
                    ActionType.CLICK -> {
                        val target = action.target
                        onFeedback("Clicando em: $target")
                        findAndClick(target) { }
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
                        onFeedback("Rolando tela")
                    }
                }
                if (index == actions.lastIndex) {
                    handler.postDelayed({ onDone() }, 700)
                }
            }, initialDelay + index * delayPerAction)
        }
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
            clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            onResult(true)
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
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
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

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    companion object {
        private var instance: AmadeusAccessibilityService? = null

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
            val componentName = "${context.packageName}/${AmadeusAccessibilityService::class.java.canonicalName}"

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
            } catch (_: Exception) {
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

        fun executeActions(
            actions: List<ActionIntent>,
            onFeedback: (String) -> Unit,
            onDone: () -> Unit
        ): Boolean {
            val service = instance ?: return false
            service.executeActions(actions, onFeedback, onDone)
            return true
        }

        fun getVisibleElements(): List<UiElement> = instance?.getVisibleElements() ?: emptyList()
    }
}
