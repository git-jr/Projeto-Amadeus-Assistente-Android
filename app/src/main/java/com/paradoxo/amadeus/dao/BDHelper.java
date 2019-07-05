package com.paradoxo.amadeus.dao;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static android.content.Context.MODE_PRIVATE;

public class BDHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    public BDHelper(Context context) {
        super(context, getNomeBancoEmUso(context), null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CRIAR_TABELA_AUTOR = "CREATE TABLE autor( id INTEGER PRIMARY KEY, nome VARCHAR);";
        String CRIAR_TABELA_MENSAGEM = "CREATE TABLE mensagem (id INTEGER PRIMARY KEY AUTOINCREMENT, conteudo VARCHAR, fk_autor integer, dt REAL DEFAULT (datetime('now', 'localtime')), fk_resposta INTEGER, foreign key (fk_autor) REFERENCES autor (id), foreign key (fk_resposta) references mensagem (id));";

        db.execSQL(CRIAR_TABELA_AUTOR);
        db.execSQL(CRIAR_TABELA_MENSAGEM);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private static String getNomeBancoEmUso(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        return sharedPreferences.getString("bdAtual", "Amadeus") + ".db";
        // "Amadeus.db" é o banco padrão
    }

}