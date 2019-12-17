package com.paradoxo.amadeus.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BDHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 4;

    private String TABELA_SENTENCA = "CREATE TABLE sentenca( id INTEGER PRIMARY KEY, chave VARCHAR, respostas VARCHAR, acao VARCHAR, tipo_item INTEGER, idBanco VARCHAR);";
    private String TABELA_HISTORICIO_SENTENCA = "CREATE TABLE historico_sentenca( id INTEGER PRIMARY KEY, chave VARCHAR, respostas VARCHAR, acao VARCHAR, tipo_item INTEGER);";
    private String TABELA_ENTIDADE = "CREATE TABLE entidade( id INTEGER PRIMARY KEY, nome VARCHAR, significados VARCHAR, sinonimos VARCHAR, atributos INTEGER, idBanco VARCHAR);";

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }

    public BDHelper(Context context) {
        super(context, "Amadeus.db", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CRIAR_TABELA_AUTOR = "CREATE TABLE autor( id INTEGER PRIMARY KEY, nome VARCHAR);";
        String CRIAR_TABELA_MENSAGEM = "CREATE TABLE mensagem (id INTEGER PRIMARY KEY AUTOINCREMENT, conteudo VARCHAR, fk_autor integer, dt REAL DEFAULT (datetime('now', 'localtime')), fk_resposta INTEGER, foreign key (fk_autor) REFERENCES autor (id), foreign key (fk_resposta) references mensagem (id));";

        db.execSQL(CRIAR_TABELA_AUTOR);
        db.execSQL(CRIAR_TABELA_MENSAGEM);

        db.execSQL(TABELA_SENTENCA);
        db.execSQL(TABELA_HISTORICIO_SENTENCA);
        db.execSQL(TABELA_ENTIDADE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 2) {
            db.execSQL(TABELA_SENTENCA);
            db.execSQL(TABELA_HISTORICIO_SENTENCA);
        }

        if(oldVersion < 3){
            db.execSQL(TABELA_ENTIDADE);
        }

        if(oldVersion < 4){
            String ADICONAR_COLUNA_IDBANCO_SENTENCA = "ALTER TABLE SENTENCA ADD COLUMN idBanco VARCHAR;";
            String ADICONAR_COLUNA_IDBANCO_ENTIDADE = "ALTER TABLE ENTIDADE ADD COLUMN idBanco VARCHAR;";

            db.execSQL(ADICONAR_COLUNA_IDBANCO_SENTENCA);
            db.execSQL(ADICONAR_COLUNA_IDBANCO_ENTIDADE);
        }
    }
}
