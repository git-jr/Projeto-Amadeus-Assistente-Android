package com.paradoxo.amadeus.dao.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "historico_sentenca")
data class HistoricoSentencaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chave: String?,
    val respostas: String?,
    val acao: String?,
    val tipo_item: Int?
)
