package com.paradoxo.amadeus.modelo

enum class ActionType {
    CLICK,
    TYPE,
    OPEN_APP,
    SCROLL
}

data class ActionIntent(
    val type: ActionType,
    val target: String = "",
    val value: String = ""
)
