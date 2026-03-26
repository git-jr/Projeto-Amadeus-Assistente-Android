package com.paradoxo.amadeus.dao

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BDHelper(context: Context) : SQLiteOpenHelper(context, "Amadeus.db", null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_VERSION = 4
    }

    private val TABELA_SENTENCA = "CREATE TABLE sentenca( id INTEGER PRIMARY KEY, chave VARCHAR, respostas VARCHAR, acao VARCHAR, tipo_item INTEGER, idBanco VARCHAR);"
    private val TABELA_HISTORICIO_SENTENCA = "CREATE TABLE historico_sentenca( id INTEGER PRIMARY KEY, chave VARCHAR, respostas VARCHAR, acao VARCHAR, tipo_item INTEGER);"
    private val TABELA_ENTIDADE = "CREATE TABLE entidade( id INTEGER PRIMARY KEY, nome VARCHAR, significados VARCHAR, sinonimos VARCHAR, atributos INTEGER, idBanco VARCHAR);"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE autor( id INTEGER PRIMARY KEY, nome VARCHAR);")
        db.execSQL("CREATE TABLE mensagem (id INTEGER PRIMARY KEY AUTOINCREMENT, conteudo VARCHAR, fk_autor integer, dt REAL DEFAULT (datetime('now', 'localtime')), fk_resposta INTEGER, foreign key (fk_autor) REFERENCES autor (id), foreign key (fk_resposta) references mensagem (id));")
        db.execSQL(TABELA_SENTENCA)
        db.execSQL(TABELA_HISTORICIO_SENTENCA)
        db.execSQL(TABELA_ENTIDADE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(TABELA_SENTENCA)
            db.execSQL(TABELA_HISTORICIO_SENTENCA)
        }
        if (oldVersion < 3) db.execSQL(TABELA_ENTIDADE)
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE SENTENCA ADD COLUMN idBanco VARCHAR;")
            db.execSQL("ALTER TABLE ENTIDADE ADD COLUMN idBanco VARCHAR;")
        }
    }
}
