package com.paradoxo.amadeus.dao.room

import androidx.room.*
import com.paradoxo.amadeus.dao.room.entities.HistoricoSentencaEntity
import com.paradoxo.amadeus.dao.room.entities.SentencaEntity

@Dao
interface SentencaRoomDAO {

    // ── sentenca ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(sentenca: SentencaEntity): Long

    @Update
    suspend fun alterar(sentenca: SentencaEntity)

    @Delete
    suspend fun excluir(sentenca: SentencaEntity)

    @Query("SELECT * FROM sentenca WHERE chave LIKE :chave || '%' LIMIT 1")
    suspend fun buscaPorChave(chave: String): SentencaEntity?

    @Query("SELECT * FROM sentenca WHERE id = :id LIMIT 1")
    suspend fun buscaPorId(id: Int): SentencaEntity?

    @Query("SELECT * FROM sentenca WHERE chave LIKE :chave || '%' LIMIT :limite")
    suspend fun buscaPorChaveLista(chave: String, limite: Long): List<SentencaEntity>

    @Query("SELECT * FROM sentenca")
    suspend fun listar(): List<SentencaEntity>

    @Query("SELECT * FROM sentenca LIMIT :limite")
    suspend fun listarComLimite(limite: Long): List<SentencaEntity>

    @Query("SELECT * FROM sentenca WHERE id > :idInicio")
    suspend fun listarAPartirDe(idInicio: Long): List<SentencaEntity>

    @Query("SELECT COUNT(*) FROM sentenca")
    suspend fun getQuantidadeTotal(): Long

    // ── historico_sentenca ────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirHistorico(sentenca: HistoricoSentencaEntity): Long

    @Delete
    suspend fun excluirHistorico(sentenca: HistoricoSentencaEntity)

    @Query("SELECT * FROM historico_sentenca WHERE chave LIKE :chave || '%' LIMIT 1")
    suspend fun buscaHistoricoPorChave(chave: String): HistoricoSentencaEntity?

    @Query("SELECT * FROM historico_sentenca")
    suspend fun listarHistorico(): List<HistoricoSentencaEntity>

    @Query("SELECT COUNT(*) FROM historico_sentenca")
    suspend fun getQuantidadeTotalHistorico(): Long
}
