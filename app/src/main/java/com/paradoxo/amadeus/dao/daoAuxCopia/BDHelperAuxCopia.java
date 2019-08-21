package com.paradoxo.amadeus.dao.daoAuxCopia;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

public class BDHelperAuxCopia extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.disableWriteAheadLogging();
        }
    }

    public BDHelperAuxCopia(Context context, String nomeOutrobanco) {
        super(context, nomeOutrobanco, null, DATABASE_VERSION);
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

}
