package com.paradoxo.amadeus.ui.litertmodelmanager

import com.paradoxo.amadeus.dao.room.entities.LocalModelEntity
import com.paradoxo.amadeus.dao.room.entities.ModelStatus

data class LiteRTModelManagerUiState(
    val models: List<LocalModelEntity> = emptyList(),
    val hfToken: String = ""
) {
    val activeModel: LocalModelEntity? get() = models.firstOrNull { it.isActive }
    val totalUsedBytes: Long get() = models.filter { it.status == ModelStatus.DOWNLOADED }.sumOf { it.sizeBytes }
}
