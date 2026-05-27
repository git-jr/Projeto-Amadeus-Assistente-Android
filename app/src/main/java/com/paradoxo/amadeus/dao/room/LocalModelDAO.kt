package com.paradoxo.amadeus.dao.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.paradoxo.amadeus.dao.room.entities.LocalModelEntity
import com.paradoxo.amadeus.dao.room.entities.ModelStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalModelDAO {

    @Query("SELECT * FROM local_model ORDER BY displayName ASC")
    fun getAll(): Flow<List<LocalModelEntity>>

    @Query("SELECT * FROM local_model WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): LocalModelEntity?

    @Query("SELECT * FROM local_model WHERE modelId = :modelId LIMIT 1")
    suspend fun findByModelId(modelId: String): LocalModelEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(model: LocalModelEntity): Long

    @Delete
    suspend fun delete(model: LocalModelEntity)

    @Query("UPDATE local_model SET status = :status, filePath = :filePath WHERE modelId = :modelId")
    suspend fun updateStatus(modelId: String, status: ModelStatus, filePath: String?)

    @Query("UPDATE local_model SET status = :status WHERE modelId = :modelId")
    suspend fun updateStatusOnly(modelId: String, status: ModelStatus)

    @Query("UPDATE local_model SET downloadedBytes = :bytes, downloadManagerId = :dmId WHERE modelId = :modelId")
    suspend fun updateProgress(modelId: String, bytes: Long, dmId: Long)

    @Query("UPDATE local_model SET isActive = CASE WHEN modelId = :modelId THEN 1 ELSE 0 END")
    suspend fun setActive(modelId: String)

    @Query("UPDATE local_model SET isActive = 0 WHERE modelId = :modelId")
    suspend fun deactivate(modelId: String)
}
