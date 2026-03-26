package com.paradoxo.amadeus.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import com.google.gson.GsonBuilder
import com.paradoxo.amadeus.enums.AcaoEnum
import com.paradoxo.amadeus.modelo.Entidade
import com.paradoxo.amadeus.modelo.Sentenca

class EntidadeDAO(private val context: Context) {
    private val gson = GsonBuilder().create()
    private val bdGateway = BDGateway.getInstance(context)
    private val tabelaEmUso = "entidade"

    companion object {
        const val SINONIMOS = "sinonimos"
        const val ATRIBUTOS = "atributos"
        const val SIGNIFICADOS = "significados"
        const val ARQ_SENTENCAS_PADRAO_JSON = "SentencasPadrao.json"
        const val ARQ_ENTIDADES_PADRAO_JSON = "EntidadesPadrao.json"
    }

    fun inserir(entidade: Entidade) {
        bdGateway.database.insert(tabelaEmUso, null, toContentValues(entidade))
    }

    fun buscaPorChave(nome: String): Entidade {
        val cursor = bdGateway.database.rawQuery("SELECT * FROM $tabelaEmUso WHERE nome = ?", arrayOf(nome))
        cursor.moveToNext()
        val obj = Entidade()
        if (cursor.count > 0) obj.fromCursor(cursor)
        cursor.close()
        return obj
    }

    fun buscaPorId(id: String): Sentenca {
        val cursor = bdGateway.database.rawQuery("SELECT * FROM $tabelaEmUso WHERE id = ?", arrayOf(id))
        cursor.moveToNext()
        val obj = Sentenca()
        if (cursor.count > 0) {
            obj.id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            obj.chave = cursor.getString(cursor.getColumnIndexOrThrow("chave"))
            obj.respostas = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow("respostas")), Array<String>::class.java).toMutableList()
            obj.acao = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow("acao")), AcaoEnum::class.java)
            obj.tipo_item = cursor.getInt(cursor.getColumnIndexOrThrow("tipo_item"))
        }
        cursor.close()
        return obj
    }

    fun buscaPorChaveLista(chave: String, limiteItensCarregar: Long): List<Entidade> {
        val entidades = mutableListOf<Entidade>()
        val cursor = bdGateway.database.rawQuery(
            "SELECT * FROM $tabelaEmUso WHERE nome LIKE ? LIMIT ?",
            arrayOf("$chave%", limiteItensCarregar.toString())
        )
        while (cursor.moveToNext()) entidades.add(Entidade().also { it.fromCursor(cursor) })
        cursor.close()
        return entidades
    }

    fun listar(): List<Sentenca> {
        val sentencas = mutableListOf<Sentenca>()
        val cursor = bdGateway.database.rawQuery("SELECT * FROM $tabelaEmUso", null)
        while (cursor.moveToNext()) {
            val obj = Sentenca()
            obj.id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            obj.chave = cursor.getString(cursor.getColumnIndexOrThrow("chave"))
            obj.respostas = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow("respostas")), Array<String>::class.java).toMutableList()
            obj.acao = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow("acao")), AcaoEnum::class.java)
            obj.tipo_item = cursor.getInt(cursor.getColumnIndexOrThrow("tipo_item"))
            sentencas.add(obj)
        }
        cursor.close()
        return sentencas
    }

    fun listar(limiteItensCarregar: Long): List<Entidade> {
        val entidades = mutableListOf<Entidade>()
        val cursor = bdGateway.database.rawQuery("SELECT * FROM $tabelaEmUso LIMIT ?", arrayOf(limiteItensCarregar.toString()))
        while (cursor.moveToNext()) entidades.add(Entidade().also { it.fromCursor(cursor) })
        cursor.close()
        return entidades
    }

    fun listarAPartirDe(idInicio: Long): List<Sentenca> {
        val sentencas = mutableListOf<Sentenca>()
        val cursor = bdGateway.database.rawQuery("SELECT * FROM $tabelaEmUso WHERE id > ?", arrayOf(idInicio.toString()))
        while (cursor.moveToNext()) {
            val obj = Sentenca()
            obj.id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            obj.chave = cursor.getString(cursor.getColumnIndexOrThrow("chave"))
            obj.respostas = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow("respostas")), Array<String>::class.java).toMutableList()
            obj.acao = gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow("acao")), AcaoEnum::class.java)
            obj.tipo_item = cursor.getInt(cursor.getColumnIndexOrThrow("tipo_item"))
            sentencas.add(obj)
        }
        cursor.close()
        return sentencas
    }

    fun alterarSentenca(entidade: Entidade) {
        bdGateway.database.update(tabelaEmUso, toContentValues(entidade), "id=?", arrayOf(entidade.id.toString()))
    }

    fun excluir(entidade: Entidade) {
        bdGateway.database.delete(tabelaEmUso, "id=?", arrayOf(entidade.id.toString()))
    }

    val quantidadeTotal: Long
        get() = DatabaseUtils.queryNumEntries(bdGateway.database, tabelaEmUso)

    fun getSentencasJson(): List<Sentenca>? = lerArquivoJson(ARQ_SENTENCAS_PADRAO_JSON)?.let {
        gson.fromJson(it, Array<Sentenca>::class.java).toList()
    }

    val entidadesPadraoJson: List<Entidade>
        get() = lerArquivoJson(ARQ_ENTIDADES_PADRAO_JSON)?.let {
            gson.fromJson(it, Array<Entidade>::class.java).toList()
        } ?: emptyList()

    private fun toContentValues(entidade: Entidade) = ContentValues().apply {
        put("nome", entidade.nome)
        put(SIGNIFICADOS, gson.toJson(entidade.significado))
        put(SINONIMOS, gson.toJson(entidade.sinonimos))
        put(ATRIBUTOS, gson.toJson(entidade.atributos))
    }

    private fun Entidade.fromCursor(cursor: Cursor) {
        id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
        nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"))
        gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(SIGNIFICADOS)), Array<String>::class.java)
            ?.let { significado = it.toList() }
        gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(SINONIMOS)), Array<String>::class.java)
            ?.let { sinonimos = it.toList() }
        gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(ATRIBUTOS)), Array<Entidade>::class.java)
            ?.let { atributos = it.toList() }
    }

    private fun lerArquivoJson(arquivo: String): String? {
        return try {
            val inputStream = context.assets.open(arquivo)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
