package com.paradoxo.amadeus.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.ActionType
import kotlin.math.abs

class MacroRecorder(private val service: AccessibilityService) {

    var isRecording = false
        private set
    private val recordedActions = mutableListOf<ActionIntent>()
    private val lastScrollY = mutableMapOf<Int, Int>()
    private val lastScrollX = mutableMapOf<Int, Int>()
    private var pendingTypeText: String? = null
    private var lastRecordedPackage: String = ""
    var onActionRecorded: ((count: Int) -> Unit)? = null

    fun startRecording() {
        isRecording = true
        recordedActions.clear()
        lastScrollY.clear()
        lastScrollX.clear()
        pendingTypeText = null
        lastRecordedPackage = ""
    }

    fun stopRecording(): List<ActionIntent> {
        commitPendingType()
        isRecording = false
        return recordedActions.toList()
    }

    fun captureEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg == service.packageName) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                commitPendingType()
                val source = event.source ?: return
                if (isImeWindow(source.windowId)) return
                val text = source.text?.toString()?.takeIf { it.isNotBlank() }
                    ?: source.contentDescription?.toString()?.takeIf { it.isNotBlank() }
                if (text != null) {
                    addAction(ActionIntent(ActionType.CLICK, target = text))
                } else {
                    val bounds = Rect()
                    source.getBoundsInScreen(bounds)
                    if (!bounds.isEmpty) {
                        addAction(ActionIntent(
                            ActionType.CLICK_COORDS,
                            value = "${bounds.centerX()},${bounds.centerY()}"
                        ))
                    }
                }
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                pendingTypeText = event.text?.firstOrNull()?.toString() ?: ""
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                commitPendingType()
                val nodeId = event.source?.windowId ?: return
                val scrollY = event.scrollY
                val scrollX = event.scrollX
                val prevY = lastScrollY[nodeId]
                val prevX = lastScrollX[nodeId]
                if (scrollY >= 0) lastScrollY[nodeId] = scrollY
                if (scrollX >= 0) lastScrollX[nodeId] = scrollX
                if (prevY == null || (scrollY < 0 && scrollX < 0)) return
                val deltaY = if (scrollY >= 0 && prevY >= 0) scrollY - prevY else 0
                val deltaX = if (scrollX >= 0 && prevX != null && prevX >= 0) scrollX - prevX else 0
                if (deltaY == 0 && deltaX == 0) return
                if (abs(deltaX) > abs(deltaY)) {
                    val direction = if (deltaX > 0) "left" else "right"
                    val last = recordedActions.lastOrNull()
                    if (last?.type != ActionType.SCROLL_HORIZONTAL || last.value != direction) {
                        addAction(ActionIntent(ActionType.SCROLL_HORIZONTAL, value = direction))
                    }
                } else {
                    val direction = if (deltaY > 0) "down" else "up"
                    val last = recordedActions.lastOrNull()
                    if (last?.type != ActionType.SCROLL || last.value != direction) {
                        addAction(ActionIntent(ActionType.SCROLL, value = direction))
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                commitPendingType()
                if (pkg != lastRecordedPackage) {
                    val launchIntent = service.packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        lastRecordedPackage = pkg
                        val appName = try {
                            val info = service.packageManager.getApplicationInfo(pkg, 0)
                            service.packageManager.getApplicationLabel(info).toString()
                        } catch (_: Exception) { pkg }
                        val last = recordedActions.lastOrNull()
                        if (last?.type != ActionType.OPEN_APP || last.target != appName) {
                            addAction(ActionIntent(ActionType.OPEN_APP, target = appName))
                        }
                    }
                }
            }
        }
    }

    private fun isImeWindow(windowId: Int): Boolean = try {
        service.windows.any { it.id == windowId && it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
    } catch (_: Exception) { false }

    private fun commitPendingType() {
        val text = pendingTypeText?.takeIf { it.isNotBlank() } ?: return
        pendingTypeText = null
        addAction(ActionIntent(ActionType.TYPE, value = text))
    }

    private fun addAction(action: ActionIntent) {
        recordedActions.add(action)
        onActionRecorded?.invoke(recordedActions.size)
    }
}
