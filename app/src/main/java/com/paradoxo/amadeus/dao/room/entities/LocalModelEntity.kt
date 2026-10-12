package com.paradoxo.amadeus.dao.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ModelStatus { NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, ERROR }

@Entity(
    tableName = "local_model",
    indices = [Index(value = ["modelId"], unique = true)]
)
data class LocalModelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val modelId: String,
    val displayName: String,
    val description: String,
    val sizeBytes: Long,
    val downloadedBytes: Long = 0L,
    val status: ModelStatus = ModelStatus.NOT_DOWNLOADED,
    val isActive: Boolean = false,
    val filePath: String? = null,
    val downloadManagerId: Long = -1L
)
