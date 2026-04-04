package com.paradoxo.amadeus.dao.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "mensagem",
    foreignKeys = [
        ForeignKey(
            entity = AutorEntity::class,
            parentColumns = ["id"],
            childColumns = ["fk_autor"]
        ),
        ForeignKey(
            entity = MensagemEntity::class,
            parentColumns = ["id"],
            childColumns = ["fk_resposta"]
        )
    ]
)
data class MensagemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conteudo: String?,
    val fk_autor: Int?,
    @ColumnInfo(name = "dt")
    val dt: Double?,
    val fk_resposta: Int?
)
