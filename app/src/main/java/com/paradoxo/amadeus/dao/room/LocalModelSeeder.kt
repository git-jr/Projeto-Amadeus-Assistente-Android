package com.paradoxo.amadeus.dao.room

import com.paradoxo.amadeus.cognicao.localmodel.SupportedModels
import com.paradoxo.amadeus.dao.room.entities.LocalModelEntity

object LocalModelSeeder {

    suspend fun seed(db: AmadeusDatabase) {
        val dao = db.localModelDAO()
        for (descriptor in SupportedModels.CATALOG) {
            dao.insert(
                LocalModelEntity(
                    modelId = descriptor.modelId,
                    displayName = descriptor.displayName,
                    description = descriptor.description,
                    sizeBytes = descriptor.sizeBytes
                )
            )
        }
    }
}
