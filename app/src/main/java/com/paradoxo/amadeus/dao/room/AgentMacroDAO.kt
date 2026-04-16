package com.paradoxo.amadeus.dao.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paradoxo.amadeus.dao.room.entities.AgentMacroEntity

@Dao
interface AgentMacroDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(macro: AgentMacroEntity): Long

    @Query("SELECT * FROM agent_macro WHERE goalKey = :goalKey AND isConfirmed = 1 LIMIT 1")
    suspend fun findConfirmedByGoalKey(goalKey: String): AgentMacroEntity?

    @Query("SELECT * FROM agent_macro WHERE goalKey = :goalKey LIMIT 1")
    suspend fun findByGoalKey(goalKey: String): AgentMacroEntity?

    @Query("UPDATE agent_macro SET executionCount = :count, isConfirmed = :confirmed, lastUsedAt = :ts WHERE id = :id")
    suspend fun updateCounts(id: Int, count: Int, confirmed: Boolean, ts: Long)

    @Query("UPDATE agent_macro SET successCount = :count, lastUsedAt = :ts WHERE id = :id")
    suspend fun updateSuccessCount(id: Int, count: Int, ts: Long)

    @Query("UPDATE agent_macro SET failureCount = :count, isConfirmed = :confirmed WHERE id = :id")
    suspend fun updateFailureCount(id: Int, count: Int, confirmed: Boolean)

    @Update
    suspend fun update(macro: AgentMacroEntity): Int

    @Delete
    suspend fun delete(macro: AgentMacroEntity)

    @Query("SELECT * FROM agent_macro WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): AgentMacroEntity?

    @Query("SELECT * FROM agent_macro ORDER BY lastUsedAt DESC")
    suspend fun listAll(): List<AgentMacroEntity>
}
