package com.paradoxo.amadeus.ui.demoactions

data class DemoActionsUiState(
    val serviceActive: Boolean = false,
    val runningId: String? = null,
    val countdown: Int = 0,
    val statusText: String = "",
    val readResult: String = ""
)
