package com.paradoxo.amadeus.dao

import android.content.ContentValues
import android.content.Context
import com.paradoxo.amadeus.modelo.Autor
import com.paradoxo.amadeus.modelo.Mensagem

class MensagemDAO(private val context: Context) {
    private val bdGateway = BDGateway.getInstance(context)

    companion object {
        private const val TABELA_MENSAGEM = "mensagem"
    }

    fun inserirMensagemImportada(conteudo: String, autor: Int) {
        val cv = ContentValues().apply {
            put("conteudo", conteudo)
            put("fk_autor", autor)
        }
        bdGateway.database.insert(TABELA_MENSAGEM, null, cv)
    }

    fun inserirRespostaImportada(pergunta: Int, resposta: Int) {
        val cv = ContentValues().apply { put("fk_resposta", resposta) }
        bdGateway.database.update(TABELA_MENSAGEM, cv, "id=?", arrayOf(pergunta.toString()))
    }

    fun listarRespostasCompleto(): List<Mensagem> {
        val mensagens = mutableListOf<Mensagem>()
        val cursor = bdGateway.database.rawQuery(
            "SELECT msg2.*, msg1.conteudo conteudo_resposta FROM mensagem msg1, mensagem msg2 WHERE msg2.fk_resposta = msg1.id;",
            null
        )
        while (cursor.moveToNext()) {
            val objMsg = Mensagem().apply {
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                conteudo = cursor.getString(cursor.getColumnIndexOrThrow("conteudo"))
                idResposta = cursor.getInt(cursor.getColumnIndexOrThrow("fk_resposta"))
                conteudo_resposta = cursor.getString(cursor.getColumnIndexOrThrow("conteudo_resposta"))
                val objAutor = Autor(id = cursor.getInt(cursor.getColumnIndexOrThrow("fk_autor")))
                autor = AutorDAO(context).buscar(objAutor)
            }
            mensagens.add(objMsg)
        }
        cursor.close()
        return mensagens
    }

    fun verificarExistencia(mensagem: Mensagem): Boolean {
        val cursor = bdGateway.database.rawQuery("SELECT * FROM mensagem", null)
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }

    fun deletarTodasMensagens() {
        bdGateway.database.delete(TABELA_MENSAGEM, null, null)
        bdGateway.database.execSQL("delete from sqlite_sequence where name='mensagem'")
    }
}
