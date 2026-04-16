package com.paradoxo.amadeus.modelo

enum class ActionType {
    CLICK,
    TYPE,
    OPEN_APP,
    SCROLL,
    SCROLL_HORIZONTAL,
    IME_ACTION
}

data class ActionIntent(
    val type: ActionType,
    val target: String = "",
    val value: String = ""
)
