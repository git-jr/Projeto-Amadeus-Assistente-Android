package com.paradoxo.amadeus.dao.room

import androidx.room.*
import com.paradoxo.amadeus.dao.room.entities.MensagemEntity

data class MensagemComResposta(
    val id: Int,
    val conteudo: String?,
    val fk_autor: Int?,
    val dt: Double?,
    val fk_resposta: Int?,
    val conteudo_resposta: String?
)

@Dao
interface MensagemRoomDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(mensagem: MensagemEntity): Long

    // Atualiza apenas fk_resposta de uma mensagem já inserida (par pergunta→resposta)
    @Query("UPDATE mensagem SET fk_resposta = :resposta WHERE id = :pergunta")
    suspend fun inserirRespostaImportada(pergunta: Int, resposta: Int)

    @Query("""
        SELECT msg2.id, msg2.conteudo, msg2.fk_autor, msg2.dt, msg2.fk_resposta,
               msg1.conteudo AS conteudo_resposta
        FROM mensagem msg2
        JOIN mensagem msg1 ON msg2.fk_resposta = msg1.id
        ORDER BY msg2.id ASC
    """)
    suspend fun listarRespostasCompleto(): List<MensagemComResposta>

    @Query("SELECT COUNT(*) FROM mensagem")
    suspend fun contarMensagens(): Long

    @Query("DELETE FROM mensagem")
    suspend fun deletarTodas()
}
