package com.paradoxo.amadeus.dao.room

import androidx.room.*
import com.paradoxo.amadeus.dao.room.entities.AutorEntity

@Dao
interface AutorRoomDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(autor: AutorEntity): Long

    @Update
    suspend fun alterar(autor: AutorEntity)

    @Query("SELECT * FROM autor")
    suspend fun listar(): List<AutorEntity>

    @Query("SELECT * FROM autor WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): AutorEntity?

    @Query("DELETE FROM autor")
    suspend fun deletarTodos()
}
