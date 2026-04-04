package com.paradoxo.amadeus.dao.room

import android.content.Context
import com.google.gson.Gson
import com.paradoxo.amadeus.modelo.Entidade
import com.paradoxo.amadeus.modelo.Sentenca

object JsonAssetLoader {
    private val gson = Gson()

    fun carregarSentencas(context: Context): List<Sentenca> =
        lerArquivo(context, "SentencasPadrao.json")
            ?.let { gson.fromJson(it, Array<Sentenca>::class.java).toList() }
            ?: emptyList()

    fun carregarSentencasHistorico(context: Context): List<Sentenca> =
        lerArquivo(context, "SentencasHistoricoPadrao.json")
            ?.let { gson.fromJson(it, Array<Sentenca>::class.java).toList() }
            ?: emptyList()

    fun carregarEntidades(context: Context): List<Entidade> =
        lerArquivo(context, "EntidadesPadrao.json")
            ?.let { gson.fromJson(it, Array<Entidade>::class.java).toList() }
            ?: emptyList()

    private fun lerArquivo(context: Context, arquivo: String): String? = try {
        context.assets.open(arquivo).use { stream ->
            val buffer = ByteArray(stream.available())
            stream.read(buffer)
            String(buffer, Charsets.UTF_8)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
