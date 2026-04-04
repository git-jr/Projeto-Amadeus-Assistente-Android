package com.paradoxo.amadeus.dao.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// significados e sinonimos são JSON de List<String>
// atributos era INTEGER no schema original mas armazenava JSON — corrigido para TEXT na Migration 4→5
@Entity(tableName = "entidade")
data class EntidadeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String?,
    val significados: String?,
    val sinonimos: String?,
    val atributos: String?,
    val idBanco: String?
)
