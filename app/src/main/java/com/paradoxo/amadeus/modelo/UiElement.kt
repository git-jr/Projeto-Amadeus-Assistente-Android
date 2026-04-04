package com.paradoxo.amadeus.modelo

import android.graphics.Rect

data class UiElement(
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val bounds: Rect
) {
    val labelLegivel: String get() = text?.takeIf { it.isNotBlank() }
        ?: contentDescription?.takeIf { it.isNotBlank() }
        ?: ""
}
