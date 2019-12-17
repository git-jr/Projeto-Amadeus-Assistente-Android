package com.paradoxo.amadeus.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paradoxo.amadeus.enums.AcaoEnum;
import com.paradoxo.amadeus.modelo.Sentenca;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SentencaDAO {
    private Gson gson;
    private Context context;
    private String tabelaEmUso;
    private static BDGateway bdGateway;

    public static final String PREF_SENTENCAS_PADRAO_JSON = "SentencasPadrao.json";
    public static final String PREF_SENTENCAS_HISTORICO_PADRAO_JSON = "SentencasHistoricoPadrao.json";

    public SentencaDAO(Context context, boolean usarTabelaHistorico) {
        this.context = context;
        bdGateway = BDGateway.getInstance(this.context);

        if (usarTabelaHistorico) {
            tabelaEmUso = "historico_sentenca";
        } else {
            tabelaEmUso = "sentenca";
        }

        gson = new GsonBuilder().create();
    }

    public long inserir(Sentenca sentenca) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("chave", sentenca.getChave());
        contentValues.put("respostas", gson.toJson(sentenca.getRespostas()));
        contentValues.put("acao", gson.toJson(sentenca.getAcao()));
        contentValues.put("tipo_item", gson.toJson(sentenca.getTipo_item()));

        return bdGateway.getDatabase().insert(tabelaEmUso, null, contentValues);
    }

    public long inserirHistorico(Sentenca sentenca) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("chave", sentenca.getChave());
        contentValues.put("respostas", gson.toJson(sentenca.getRespostas()));
        contentValues.put("acao", gson.toJson(sentenca.getAcao()));
        contentValues.put("tipo_item", gson.toJson(sentenca.getTipo_item()));

        return bdGateway.getDatabase().insert(tabelaEmUso, null, contentValues);
    }

    public Sentenca buscaPorChave(String chave) {
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM " + tabelaEmUso + " WHERE chave LIKE ?", new String[]{chave});
        cursor.moveToNext();
        Sentenca objSentenca = new Sentenca();

        if (cursor.getCount() > 0) {
            objSentenca.setId(cursor.getString(cursor.getColumnIndex("id")));
            objSentenca.setChave(cursor.getString(cursor.getColumnIndex("chave")));
            String[] respostas = gson.fromJson(cursor.getString(cursor.getColumnIndex("respostas")), String[].class);
            objSentenca.setRespostas(Arrays.asList(respostas));
            objSentenca.setAcao(gson.fromJson(cursor.getString(cursor.getColumnIndex("acao")), AcaoEnum.class));
            objSentenca.setTipo_item(cursor.getInt(cursor.getColumnIndex("tipo_item")));
        }

        cursor.close();
        return objSentenca;
    }

    public Sentenca buscaPorId(String id) {
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM " + tabelaEmUso + " WHERE id = ?", new String[]{id});
        cursor.moveToNext();
        Sentenca objSentenca = new Sentenca();

        if (cursor.getCount() > 0) {
            objSentenca.setId(cursor.getString(cursor.getColumnIndex("id")));
            objSentenca.setChave(cursor.getString(cursor.getColumnIndex("chave")));
            String[] respostas = gson.fromJson(cursor.getString(cursor.getColumnIndex("respostas")), String[].class);
            objSentenca.setRespostas(Arrays.asList(respostas));
            objSentenca.setAcao(gson.fromJson(cursor.getString(cursor.getColumnIndex("acao")), AcaoEnum.class));
            objSentenca.setTipo_item(cursor.getInt(cursor.getColumnIndex("tipo_item")));
        }

        cursor.close();
        return objSentenca;
    }

    public List<Sentenca> buscaPorChaveLista(String chave, long limiteItensCarregar) {
        List<Sentenca> sentencas = new ArrayList<>();
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM " + tabelaEmUso + " WHERE chave LIKE ? LIMIT ?", new String[]{chave + "%", String.valueOf(limiteItensCarregar)});

        while (cursor.moveToNext()) {
            Sentenca objSentenca = new Sentenca();
            objSentenca.setId(cursor.getString(cursor.getColumnIndex("id")));
            objSentenca.setChave(cursor.getString(cursor.getColumnIndex("chave")));
            String[] respostas = gson.fromJson(cursor.getString(cursor.getColumnIndex("respostas")), String[].class);
            objSentenca.setRespostas(Arrays.asList(respostas));
            objSentenca.setAcao(gson.fromJson(cursor.getString(cursor.getColumnIndex("acao")), AcaoEnum.class));
            objSentenca.setTipo_item(cursor.getInt(cursor.getColumnIndex("tipo_item")));

            sentencas.add(objSentenca);
        }

        cursor.close();
        return sentencas;
    }

    public List<Sentenca> listar() {
        List<Sentenca> sentencas = new ArrayList<>();
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM " + tabelaEmUso, null);

        while (cursor.moveToNext()) {
            Sentenca objSentenca = new Sentenca();
            objSentenca.setId(cursor.getString(cursor.getColumnIndex("id")));
            objSentenca.setChave(cursor.getString(cursor.getColumnIndex("chave")));
            String[] respostas = gson.fromJson(cursor.getString(cursor.getColumnIndex("respostas")), String[].class);
            objSentenca.setRespostas(Arrays.asList(respostas));
            objSentenca.setAcao(gson.fromJson(cursor.getString(cursor.getColumnIndex("acao")), AcaoEnum.class));
            objSentenca.setTipo_item(cursor.getInt(cursor.getColumnIndex("tipo_item")));

            sentencas.add(objSentenca);
        }

        cursor.close();
        return sentencas;
    }

    public List<Sentenca> listar(long limiteItensCarregar) {
        List<Sentenca> sentencas = new ArrayList<>();
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM " + tabelaEmUso + " LIMIT ?", new String[]{String.valueOf(limiteItensCarregar)});

        while (cursor.moveToNext()) {
            Sentenca objSentenca = new Sentenca();
            objSentenca.setId(cursor.getString(cursor.getColumnIndex("id")));
            objSentenca.setChave(cursor.getString(cursor.getColumnIndex("chave")));
            String[] respostas = gson.fromJson(cursor.getString(cursor.getColumnIndex("respostas")), String[].class);
            objSentenca.setRespostas(Arrays.asList(respostas));
            objSentenca.setAcao(gson.fromJson(cursor.getString(cursor.getColumnIndex("acao")), AcaoEnum.class));
            objSentenca.setTipo_item(cursor.getInt(cursor.getColumnIndex("tipo_item")));

            sentencas.add(objSentenca);
        }

        cursor.close();
        return sentencas;
    }

    public List<Sentenca> listarAPartirDe(long idInicio) {
        List<Sentenca> sentencas = new ArrayList<>();
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM " + tabelaEmUso + " WHERE id > ?", new String[]{String.valueOf(idInicio)});

        while (cursor.moveToNext()) {
            Sentenca objSentenca = new Sentenca();
            objSentenca.setId(cursor.getString(cursor.getColumnIndex("id")));
            objSentenca.setChave(cursor.getString(cursor.getColumnIndex("chave")));
            String[] respostas = gson.fromJson(cursor.getString(cursor.getColumnIndex("respostas")), String[].class);
            objSentenca.setRespostas(Arrays.asList(respostas));
            objSentenca.setAcao(gson.fromJson(cursor.getString(cursor.getColumnIndex("acao")), AcaoEnum.class));
            objSentenca.setTipo_item(cursor.getInt(cursor.getColumnIndex("tipo_item")));

            sentencas.add(objSentenca);
        }

        cursor.close();
        return sentencas;
    }

    public void alterarSentenca(Sentenca sentenca) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("chave", sentenca.getChave());
        contentValues.put("respostas", gson.toJson(sentenca.getRespostas()));
        contentValues.put("acao", gson.toJson(sentenca.getAcao()));
        contentValues.put("tipo_item", gson.toJson(sentenca.getTipo_item()));

        bdGateway.getDatabase().update(tabelaEmUso, contentValues, "id=?", new String[]{sentenca.getId() + ""});
    }

    public void excluir(Sentenca sentenca) {
        bdGateway.getDatabase().delete(tabelaEmUso, "id=?", new String[]{sentenca.getId() + ""});
    }

    public long getQuantidadeTotal() {
        return DatabaseUtils.queryNumEntries( bdGateway.getDatabase(),tabelaEmUso);
    }

    public List<Sentenca> getSentencasPadraoJson() {
        String json = null;
        InputStream inputStream = null;
        try {
            inputStream = Objects.requireNonNull(context).getAssets().open(PREF_SENTENCAS_PADRAO_JSON);
            int tamanho = inputStream.available();
            byte[] buffer = new byte[tamanho];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Gson gson = new GsonBuilder().create();

        return Arrays.asList(gson.fromJson(json, Sentenca[].class));
    }

    public List<Sentenca> getSentencasHistoricoPadraoJson() {
        String json = null;
        InputStream inputStream = null;
        try {
            inputStream = Objects.requireNonNull(context).getAssets().open(PREF_SENTENCAS_HISTORICO_PADRAO_JSON);
            int tamanho = inputStream.available();
            byte[] buffer = new byte[tamanho];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Gson gson = new GsonBuilder().create();

        return Arrays.asList(gson.fromJson(json, Sentenca[].class));
    }
}
