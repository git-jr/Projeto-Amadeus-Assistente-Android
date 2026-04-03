package com.paradoxo.amadeus.dao.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// respostas e acao são JSON serializado — convertidos via Converters.kt
@Entity(tableName = "sentenca")
data class SentencaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chave: String?,
    val respostas: String?,
    val acao: String?,
    val tipo_item: Int?,
    val idBanco: String?
)
