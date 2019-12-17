package com.paradoxo.amadeus.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.paradoxo.amadeus.modelo.Autor;
import com.paradoxo.amadeus.modelo.Mensagem;

import java.util.ArrayList;
import java.util.List;

public class MensagemDAO {
    private Context context;
    private static BDGateway bdGateway;

    private static final String TABELA_MENSAGEM = "mensagem";

    public MensagemDAO(Context context) {
        this.context = context;
        bdGateway = BDGateway.getInstance(this.context);
    }

    public void inserirMensagemImportada(String conteudo, int autor) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("conteudo", conteudo);
        contentValues.put("fk_autor", autor);

        bdGateway.getDatabase().insert(TABELA_MENSAGEM, null, contentValues);
    }

    public void inserirRespostaImportada(int pergunta, int resposta) {
        ContentValues contentValues = getContentValuesHelper(resposta);
        bdGateway.getDatabase().update(TABELA_MENSAGEM, contentValues, "id=?", new String[]{pergunta + ""});
    }

    public List<Mensagem> listarRespostasCompleto() {
        List<Mensagem> mensagens = new ArrayList<>();
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT msg2.*, msg1.conteudo conteudo_resposta FROM mensagem msg1, mensagem msg2  WHERE msg2.fk_resposta = msg1.id;", null);

        while (cursor.moveToNext()) {
            Mensagem objMsg = getMensagemHelper(cursor);
            objMsg.setConteudo_resposta(cursor.getString(cursor.getColumnIndex("conteudo_resposta")));
            setAutorHelper(cursor, objMsg);
            mensagens.add(objMsg);
        }

        cursor.close();
        return mensagens;
    }

    public boolean verificarExistencia(Mensagem mensagem) {
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM mensagem", null);
        cursor.moveToNext();

        if (cursor.getCount() > 0) {
            return true;
        }

        cursor.close();
        return false;
    }

    public void deletarTodasMensagens() {
        bdGateway.getDatabase().delete(TABELA_MENSAGEM, null, null);
        bdGateway.getDatabase().execSQL("delete from sqlite_sequence where name='mensagem'");
    }

    private Mensagem getMensagemHelper(Cursor cursor) {
        Mensagem objMsg = new Mensagem();
        setMensagemHelper(cursor, objMsg);
        return objMsg;
    }

    private void setAutorHelper(Cursor cursor, Mensagem objMsg) {
        AutorDAO objAutorDAO = new AutorDAO(context);
        Autor objAutor = new Autor();
        objAutor.setId(cursor.getInt(cursor.getColumnIndex("fk_autor")));
        objMsg.setAutor(objAutorDAO.buscar(objAutor));
    }

    private void setMensagemHelper(Cursor cursor, Mensagem objMsg) {
        objMsg.setId(cursor.getInt(cursor.getColumnIndex("id")));
        objMsg.setConteudo(cursor.getString(cursor.getColumnIndex("conteudo")));
        objMsg.setIdResposta(cursor.getInt(cursor.getColumnIndex("fk_resposta")));
    }

    private static ContentValues getContentValuesHelper(int resposta) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("fk_resposta", resposta);
        return contentValues;
    }
}
