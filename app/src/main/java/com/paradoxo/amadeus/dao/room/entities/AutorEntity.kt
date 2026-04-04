package com.paradoxo.amadeus.dao.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "autor")
data class AutorEntity(
    @PrimaryKey val id: Int,
    val nome: String?
)
