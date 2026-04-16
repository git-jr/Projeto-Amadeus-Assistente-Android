package com.paradoxo.amadeus.modelo

enum class ActionType {
    CLICK,
    TYPE,
    OPEN_APP,
    SCROLL,
    SCROLL_HORIZONTAL,
    IME_ACTION,
    BACK,
    HOME,
    RECENTS,
    NOTIFICATIONS,
    QUICK_SETTINGS
}

data class ActionIntent(
    val type: ActionType,
    val target: String = "",
    val value: String = ""
)
