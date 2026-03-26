package com.paradoxo.amadeus.dao

import android.content.Context
import com.google.gson.GsonBuilder
import com.paradoxo.amadeus.modelo.Acao

class AcaoDAO(private val context: Context) {
    companion object {
        const val ARQ_LISTA_ACOES_JSON = "ListaAcoes.json"
    }

    private fun lerJson(): String? {
        return try {
            val inputStream = context.assets.open(ARQ_LISTA_ACOES_JSON)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAcoes(): List<Acao>? {
        val json = lerJson() ?: return null
        return GsonBuilder().create().fromJson(json, Array<Acao>::class.java).toList()
    }

    val quantidadeTotal: Long
        get() = getAcoes()?.size?.toLong() ?: 0L
}
