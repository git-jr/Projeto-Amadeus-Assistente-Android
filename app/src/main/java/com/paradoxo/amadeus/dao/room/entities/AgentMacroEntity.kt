package com.paradoxo.amadeus.dao.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_macro")
data class AgentMacroEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalKey: String,
    val actionsJson: String,
    val executionCount: Int = 1,
    val isConfirmed: Boolean = false,
    val lastUsedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val successCount: Int = 0,
    val failureCount: Int = 0
)
