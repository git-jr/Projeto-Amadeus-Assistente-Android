package com.paradoxo.amadeus.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.paradoxo.amadeus.modelo.Autor;

import java.util.ArrayList;
import java.util.List;

public class AutorDAO {
    
    private static BDGateway bdGateway;

    private static final String TABELA_AUTOR = "Autor";
    
    public AutorDAO(Context context) {
        bdGateway = BDGateway.getInstance(context);
    }

    public void alterar(Autor autor) {
        ContentValues cv = new ContentValues();
        cv.put("nome", autor.getNome());
        bdGateway.getDatabase().update(TABELA_AUTOR, cv, "id=?", new String[]{autor.getId() + ""});
    }

    public List<Autor> listar() {
        List<Autor> autores = new ArrayList<>();
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM autor", null);
        while (cursor.moveToNext()) {
            Autor objAutor = new Autor();
            objAutor.setId(cursor.getInt(cursor.getColumnIndex("id")));
            objAutor.setNome(cursor.getString(cursor.getColumnIndex("nome")));
            autores.add(objAutor);
        }
        cursor.close();
        return autores;
    }

    public Autor buscar(Autor autor) {
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM autor where id =" + autor.getId(), null);
        cursor.moveToNext();

        if (cursor.getCount() > 0) {
            autor.setId(cursor.getInt(cursor.getColumnIndex("id")));
            autor.setNome(cursor.getString(cursor.getColumnIndex("nome")));
        }
        cursor.close();
        return autor;
    }

}
