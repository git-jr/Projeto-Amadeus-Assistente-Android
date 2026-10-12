package com.paradoxo.amadeus.modelo

data class GoalAnalyticsEvent(
    val goalText: String = "",
    val timestamp: Long = 0L,
    val versionApp: String = ""
)
