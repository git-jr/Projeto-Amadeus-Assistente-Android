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

    public long inserirMensagem(Mensagem mensagem) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("conteudo", mensagem.getConteudo());
        contentValues.put("fk_autor", mensagem.getAutor().getId());

        return bdGateway.getDatabase().insert(TABELA_MENSAGEM, null, contentValues);
    }

    public static long inserirMensagemImportada(String conteudo, int autor) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("conteudo", conteudo);
        contentValues.put("fk_autor", autor);

        long id = bdGateway.getDatabase().insert(TABELA_MENSAGEM, null, contentValues);

        return id;
    }

    public static void inserirRespostaImportada(int pergunta, int resposta) {
        ContentValues contentValues = getContentValuesHelper(resposta);
        bdGateway.getDatabase().update(TABELA_MENSAGEM, contentValues, "id=?", new String[]{pergunta + ""});
    }

    public void inserirResposta(Mensagem mensagem) {
        // Faz o update da mensagem passada como parâmetro para que ela aponte para uma outra mensagem que servirá como resposta
        ContentValues contentValues = getContentValuesHelper(mensagem.getIdResposta());
        bdGateway.getDatabase().update(TABELA_MENSAGEM, contentValues, "id=?", new String[]{mensagem.getId() + ""});
    }

    public Mensagem listarUltimaMensagem() {
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * from mensagem order by id desc limit 1", null);
        cursor.moveToNext();

        Mensagem mensagem = getMensagemHelper(cursor);
        setAutorHelper(cursor, mensagem);

        cursor.close();
        return mensagem;
    }

    public List<Mensagem> listarUltimasInseridas() {
        List<Mensagem> mensagens = new ArrayList<>();
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM mensagem WHERE id>2002", null);

        while (cursor.moveToNext()) {
            Mensagem objMsg = getMensagemHelper(cursor);
            setAutorHelper(cursor, objMsg);
            mensagens.add(objMsg);
        }

        cursor.close();
        return mensagens;
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

    public void alterar(Mensagem mensagem) {
        ContentValues cv = new ContentValues();
        cv.put("conteudo", mensagem.getConteudo());
        cv.put("fk_autor", mensagem.getAutor().getId());
        cv.put("fk_resposta", mensagem.getIdResposta());

        bdGateway.getDatabase().update(TABELA_MENSAGEM, cv, "id=?", new String[]{mensagem.getId() + ""});
    }

    public Mensagem buscaPorID(Mensagem mensagem) {
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM mensagem WHERE id = ?", new String[]{mensagem.getIdResposta() + ""});
        cursor.moveToNext();

        Mensagem objMsg = new Mensagem();
        if (cursor.getCount() > 0) {
            setMensagemHelper(cursor, objMsg);
            setAutorHelper(cursor, objMsg);
        }

        cursor.close();
        return objMsg;
    }

    public Mensagem buscaPorConteudo(Mensagem mensagem, boolean buscaParaEdicao) {
        Cursor cursor;

        if (buscaParaEdicao) {
            cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM mensagem WHERE conteudo = ?", new String[]{mensagem.getConteudo()});
        } else {
            cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM mensagem WHERE conteudo = ? and NOT fk_resposta ISNULL", new String[]{mensagem.getConteudo()});
        }
        cursor.moveToNext();

        Mensagem mensagemBuscada = new Mensagem();
        if (cursor.getCount() > 0) {
            setMensagemHelper(cursor, mensagemBuscada);
            setAutorHelper(cursor, mensagemBuscada);
        }

        cursor.close();
        return mensagemBuscada;
    }

    public List<Mensagem> listarParcial(String textoParcial, boolean ehUmaPergunta) {
        List<Mensagem> mensagens = new ArrayList<>();
        Cursor cursor;

        if (ehUmaPergunta) {
            cursor = bdGateway.getDatabase().rawQuery("SELECT msg2.*, msg1.conteudo conteudo_resposta FROM mensagem msg1, mensagem msg2  WHERE msg2.fk_resposta = msg1.id AND msg2.conteudo LIKE ? LIMIT 10;", new String[]{textoParcial + "%"});
        } else {
            cursor = bdGateway.getDatabase().rawQuery("SELECT msg2.*, msg1.conteudo conteudo_resposta FROM mensagem msg1, mensagem msg2  WHERE msg2.fk_resposta = msg1.id AND conteudo_resposta LIKE ? LIMIT 10;", new String[]{textoParcial + "%"});
        }

        while (cursor.moveToNext()) {
            Mensagem mensagem = getMensagemHelper(cursor);
            mensagem.setConteudo_resposta(cursor.getString(cursor.getColumnIndex("conteudo_resposta")));
            setAutorHelper(cursor, mensagem);
            mensagens.add(mensagem);
        }

        cursor.close();
        return mensagens;
    }

    public void excluirResposta(Mensagem mensagem) {
        ContentValues cv = getContentValuesHelper(0);
        bdGateway.getDatabase().update(TABELA_MENSAGEM, cv, "id=?", new String[]{mensagem.getId() + ""});

    }

    public static void deletarTodasMensagens() {
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
