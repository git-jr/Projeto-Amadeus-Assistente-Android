package com.paradoxo.amadeus.modelo

data class AgentStepResult(
    val done: Boolean,
    val reason: String = "",
    val actions: List<ActionIntent> = emptyList()
)
