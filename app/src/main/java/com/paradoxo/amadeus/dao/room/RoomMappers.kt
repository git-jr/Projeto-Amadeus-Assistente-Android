package com.paradoxo.amadeus.dao.room

import com.google.gson.Gson
import com.paradoxo.amadeus.dao.room.entities.*
import com.paradoxo.amadeus.enums.AcaoEnum
import com.paradoxo.amadeus.modelo.Autor
import com.paradoxo.amadeus.modelo.Entidade
import com.paradoxo.amadeus.modelo.Sentenca

private val gson = Gson()

// ── Sentenca ──────────────────────────────────────────────────────────────────

fun Sentenca.toEntity(): SentencaEntity = SentencaEntity(
    id = id?.toIntOrNull() ?: 0,
    chave = chave,
    respostas = gson.toJson(respostas),
    acao = gson.toJson(acao),
    tipo_item = tipo_item,
    idBanco = idBanco
)

fun SentencaEntity.toModel(): Sentenca = Sentenca().also {
    it.id = id.toString()
    it.chave = chave
    it.respostas = respostas
        ?.let { r -> runCatching { gson.fromJson(r, Array<String>::class.java).toMutableList() }.getOrNull() }
        ?: mutableListOf()
    it.acao = acao?.let { a -> runCatching { gson.fromJson(a, AcaoEnum::class.java) }.getOrNull() }
    it.tipo_item = tipo_item ?: 0
    it.idBanco = idBanco
}

fun Sentenca.toHistoricoEntity(): HistoricoSentencaEntity = HistoricoSentencaEntity(
    id = id?.toIntOrNull() ?: 0,
    chave = chave,
    respostas = gson.toJson(respostas),
    acao = gson.toJson(acao),
    tipo_item = tipo_item
)

fun HistoricoSentencaEntity.toModel(): Sentenca = Sentenca().also {
    it.id = id.toString()
    it.chave = chave
    it.respostas = respostas
        ?.let { r -> runCatching { gson.fromJson(r, Array<String>::class.java).toMutableList() }.getOrNull() }
        ?: mutableListOf()
    it.acao = acao?.let { a -> runCatching { gson.fromJson(a, AcaoEnum::class.java) }.getOrNull() }
    it.tipo_item = tipo_item ?: 0
}

// ── Entidade ──────────────────────────────────────────────────────────────────

fun Entidade.toEntity(): EntidadeEntity = EntidadeEntity(
    id = id?.toIntOrNull() ?: 0,
    nome = nome,
    significados = gson.toJson(significado),
    sinonimos = gson.toJson(sinonimos),
    atributos = gson.toJson(atributos),
    idBanco = idBanco
)

fun EntidadeEntity.toModel(): Entidade = Entidade(nome = nome).also {
    it.id = id.toString()
    it.significado = significados?.let { s ->
        runCatching { gson.fromJson(s, Array<String>::class.java).toList() }.getOrNull()
    }
    it.sinonimos = sinonimos?.let { s ->
        runCatching { gson.fromJson(s, Array<String>::class.java).toList() }.getOrNull()
    }
    it.atributos = atributos?.let { a ->
        runCatching { gson.fromJson(a, Array<Entidade>::class.java).toList() }.getOrNull()
    }
    it.idBanco = idBanco
}

// ── Autor ─────────────────────────────────────────────────────────────────────

fun Autor.toEntity(): AutorEntity = AutorEntity(id = id, nome = nome)
fun AutorEntity.toModel(): Autor = Autor(id = id, nome = nome ?: "")
