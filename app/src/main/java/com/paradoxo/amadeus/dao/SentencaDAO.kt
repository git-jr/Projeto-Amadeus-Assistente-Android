package com.paradoxo.amadeus.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import com.google.gson.GsonBuilder
import com.paradoxo.amadeus.enums.AcaoEnum
import com.paradoxo.amadeus.modelo.Sentenca

class SentencaDAO(private val context: Context, usarTabelaHistorico: Boolean) {
    private val gson = GsonBuilder().create()
    private val bdGateway = BDGateway.getInstance(context)
    private val tabelaEmUso = if (usarTabelaHistorico) "historico_sentenca" else "sentenca"

    companion object {
        const val PREF_SENTENCAS_PADRAO_JSON = "SentencasPadrao.json"
        const val PREF_SENTENCAS_HISTORICO_PADRAO_JSON = "SentencasHistoricoPadrao.json"
    }

    fun inserir(sentenca: Sentenca): Long {
        return bdGateway.database.insert(tabelaEmUso, null, toContentValues(sentenca))
    }

    fun inserirHistorico(sentenca: Sentenca): Long = inserir(sentenca)

    fun buscaPorChave(chave: String): Sentenca {
        val cursor = bdGateway.database.rawQuery("SELECT * FROM $tabelaEmUso WHERE chave LIKE ?", arrayOf(chave))
        cursor.moveToNext()
        val obj = Sentenca()
        if (cursor.count > 0) obj.fromCursor(cursor)
        cursor.close()
        return obj
    }

    fun buscaPorId(id: String): Sentenca {
        val cursor = bdGateway.database.rawQuery("SELECT * FROM $tabelaEmUso WHERE id = ?", arrayOf(id))
        cursor.moveToNext()
        val obj = Sentenca()
        if (cursor.count > 0) obj.fromCursor(cursor)
        cursor.close()
        return obj
    }

    fun buscaPorChaveLista(chave: String, limiteItensCarregar: Long): List<Sentenca> {
        val sentencas = mutableListOf<Sentenca>()
        val cursor = bdGateway.database.rawQuery(
            "SELECT * FROM $tabelaEmUso WHERE chave LIKE ? LIMIT ?",
            arrayOf("$chave%", limiteItensCarregar.toString())
        )
        while (cursor.moveToNext()) sentencas.add(Sentenca().also { it.fromCursor(cursor) })
        cursor.close()
        return sentencas
    }

    fun listar(): List<Sentenca> {
        val sentencas = mutableListOf<Sentenca>()
        val cursor = bdGateway.database.rawQuery("SELECT * FROM $tabelaEmUso", null)
        while (cursor.moveToNext()) sentencas.add(Sentenca().also { it.fromCursor(cursor) })
        cursor.close()
        return sentencas
    }

    fun listar(limiteItensCarregar: Long): List<Sentenca> {
        val sentencas = mutableListOf<Sentenca>()
        val cursor = bdGateway.database.rawQuery("SELECT * FROM $tabelaEmUso LIMIT ?", arrayOf(limiteItensCarregar.toString()))
        while (cursor.moveToNext()) sentencas.add(Sentenca().also { it.fromCursor(cursor) })
        cursor.close()
        return sentencas
    }

    fun listarAPartirDe(idInicio: Long): List<Sentenca> {
        val sentencas = mutableListOf<Sentenca>()
        val cursor = bdGateway.database.rawQuery("SELECT * FROM $tabelaEmUso WHERE id > ?", arrayOf(idInicio.toString()))
        while (cursor.moveToNext()) sentencas.add(Sentenca().also { it.fromCursor(cursor) })
        cursor.close()
        return sentencas
    }

    fun alterarSentenca(sentenca: Sentenca) {
        bdGateway.database.update(tabelaEmUso, toContentValues(sentenca), "id=?", arrayOf(sentenca.id.toString()))
    }

    fun excluir(sentenca: Sentenca) {
        bdGateway.database.delete(tabelaEmUso, "id=?", arrayOf(sentenca.id.toString()))
    }

    val quantidadeTotal: Long
        get() = DatabaseUtils.queryNumEntries(bdGateway.database, tabelaEmUso)

    val sentencasPadraoJson: List<Sentenca>
        get() = lerSentencasJson(PREF_SENTENCAS_PADRAO_JSON) ?: emptyList()

    val sentencasHistoricoPadraoJson: List<Sentenca>
        get() = lerSentencasJson(PREF_SENTENCAS_HISTORICO_PADRAO_JSON) ?: emptyList()

    private fun toContentValues(sentenca: Sentenca) = ContentValues().apply {
        put("chave", sentenca.chave)
        put("respostas", gson.toJson(sentenca.respostas))
        put("acao", gson.toJson(sentenca.acao))
        put("tipo_item", gson.toJson(sentenca.tipo_item))
    }

    private fun Sentenca.fromCursor(cursor: Cursor) {
        id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
        chave = cursor.getString(cursor.getColumnIndexOrThrow("chave"))
        respostas = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow("respostas")), Array<String>::class.java).toMutableList()
        acao = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow("acao")), AcaoEnum::class.java)
        tipo_item = cursor.getInt(cursor.getColumnIndexOrThrow("tipo_item"))
    }

    private fun lerSentencasJson(arquivo: String): List<Sentenca>? {
        return try {
            val inputStream = context.assets.open(arquivo)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            gson.fromJson(String(buffer, Charsets.UTF_8), Array<Sentenca>::class.java).toList()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
