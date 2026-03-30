package com.paradoxo.amadeus.dao.room

import androidx.room.*
import com.paradoxo.amadeus.dao.room.entities.EntidadeEntity

@Dao
interface EntidadeRoomDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(entidade: EntidadeEntity): Long

    @Update
    suspend fun alterar(entidade: EntidadeEntity)

    @Delete
    suspend fun excluir(entidade: EntidadeEntity)

    @Query("SELECT * FROM entidade WHERE nome = :nome LIMIT 1")
    suspend fun buscaPorChave(nome: String): EntidadeEntity?

    @Query("SELECT * FROM entidade WHERE id = :id LIMIT 1")
    suspend fun buscaPorId(id: Int): EntidadeEntity?

    @Query("SELECT * FROM entidade WHERE nome LIKE :chave || '%' LIMIT :limite")
    suspend fun buscaPorChaveLista(chave: String, limite: Long): List<EntidadeEntity>

    @Query("SELECT * FROM entidade")
    suspend fun listar(): List<EntidadeEntity>

    @Query("SELECT * FROM entidade LIMIT :limite")
    suspend fun listarComLimite(limite: Long): List<EntidadeEntity>

    @Query("SELECT * FROM entidade WHERE id > :idInicio")
    suspend fun listarAPartirDe(idInicio: Long): List<EntidadeEntity>

    @Query("SELECT COUNT(*) FROM entidade")
    suspend fun getQuantidadeTotal(): Long
}
