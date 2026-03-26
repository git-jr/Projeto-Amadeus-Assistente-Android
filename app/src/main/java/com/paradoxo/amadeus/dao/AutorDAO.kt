package com.paradoxo.amadeus.dao

import android.content.ContentValues
import android.content.Context
import com.paradoxo.amadeus.modelo.Autor

class AutorDAO(context: Context) {
    private val bdGateway = BDGateway.getInstance(context)

    companion object {
        private const val TABELA_AUTOR = "Autor"
    }

    fun inserirAutor(autor: Autor): Long {
        val cv = ContentValues().apply { put("nome", autor.nome) }
        return bdGateway.database.insert(TABELA_AUTOR, null, cv)
    }

    fun alterar(autor: Autor) {
        val cv = ContentValues().apply { put("nome", autor.nome) }
        bdGateway.database.update(TABELA_AUTOR, cv, "id=?", arrayOf(autor.id.toString()))
    }

    fun listar(): List<Autor> {
        val autores = mutableListOf<Autor>()
        val cursor = bdGateway.database.rawQuery("SELECT * FROM autor", null)
        while (cursor.moveToNext()) {
            autores.add(Autor(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"))
            ))
        }
        cursor.close()
        return autores
    }

    fun buscar(autor: Autor): Autor {
        val cursor = bdGateway.database.rawQuery("SELECT * FROM autor where id = ${autor.id}", null)
        cursor.moveToNext()
        if (cursor.count > 0) {
            autor.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            autor.nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"))
        }
        cursor.close()
        return autor
    }

    fun deletarTodosAutores() {
        bdGateway.database.delete(TABELA_AUTOR, null, null)
        bdGateway.database.execSQL("delete from sqlite_sequence where name='autor'")
    }
}
