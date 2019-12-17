package com.paradoxo.amadeus.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paradoxo.amadeus.enums.AcaoEnum;
import com.paradoxo.amadeus.modelo.Entidade;
import com.paradoxo.amadeus.modelo.Sentenca;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class EntidadeDAO {
    private Gson gson;
    private Context context;
    private String tabelaEmUso;
    private static BDGateway bdGateway;

    public static final String SINONIMOS = "sinonimos";
    public static final String ATRIBUTOS = "atributos";
    public static final String SIGNIFICADOS = "significados";
    public static final String ARQ_SENTENCAS_PADRAO_JSON = "SentencasPadrao.json";
    public static final String ARQ_ENTIDADES_PADRAO_JSON = "EntidadesPadrao.json";

    public EntidadeDAO(Context context) {
        this.context = context;
        bdGateway = BDGateway.getInstance(this.context);
        tabelaEmUso = "entidade";
        gson = new GsonBuilder().create();
    }

    public void inserir(Entidade entidade) {
        ContentValues contentValues = getContentValues(entidade);
        bdGateway.getDatabase().insert(tabelaEmUso, null, contentValues);
    }

    public Entidade buscaPorChave(String nome) {
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM " + tabelaEmUso + " WHERE nome = ?", new String[]{nome});
        cursor.moveToNext();
        Entidade objEntidade = new Entidade();

        if (cursor.getCount() > 0) {
            objEntidade.setId(cursor.getString(cursor.getColumnIndex("id")));
            objEntidade.setNome(cursor.getString(cursor.getColumnIndex("nome")));

            String[] significados = gson.fromJson(cursor.getString(cursor.getColumnIndex(SIGNIFICADOS)), String[].class);
            if (significados != null)
                objEntidade.setSignificado(Arrays.asList(significados));

            String[] sinonimos = gson.fromJson(cursor.getString(cursor.getColumnIndex(SINONIMOS)), String[].class);
            if (sinonimos != null)
                objEntidade.setSinonimos(Arrays.asList(sinonimos));

            Entidade[] atributos = gson.fromJson(cursor.getString(cursor.getColumnIndex(ATRIBUTOS)), Entidade[].class);
            if (atributos != null)
                objEntidade.setAtributos(Arrays.asList(atributos));

        }

        cursor.close();
        return objEntidade;
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

    public List<Entidade> buscaPorChaveLista(String chave, long limiteItensCarregar) {
        List<Entidade> entidades = new ArrayList<>();
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM " + tabelaEmUso + " WHERE nome LIKE ? LIMIT ?", new String[]{chave + "%", String.valueOf(limiteItensCarregar)});

        while (cursor.moveToNext()) {
            Entidade objEntidade = new Entidade();
            objEntidade.setId(cursor.getString(cursor.getColumnIndex("id")));
            objEntidade.setNome(cursor.getString(cursor.getColumnIndex("nome")));

            String[] significados = gson.fromJson(cursor.getString(cursor.getColumnIndex(SIGNIFICADOS)), String[].class);
            if (significados != null)
                objEntidade.setSignificado(Arrays.asList(significados));

            String[] sinonimos = gson.fromJson(cursor.getString(cursor.getColumnIndex(SINONIMOS)), String[].class);
            if (sinonimos != null)
                objEntidade.setSinonimos(Arrays.asList(sinonimos));

            Entidade[] atributos = gson.fromJson(cursor.getString(cursor.getColumnIndex(ATRIBUTOS)), Entidade[].class);
            if (atributos != null)
                objEntidade.setAtributos(Arrays.asList(atributos));

            entidades.add(objEntidade);
        }

        cursor.close();
        return entidades;
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

    public List<Entidade> listar(long limiteItensCarregar) {
        List<Entidade> entidades = new ArrayList<>();
        Cursor cursor = bdGateway.getDatabase().rawQuery("SELECT * FROM " + tabelaEmUso + " LIMIT ?", new String[]{String.valueOf(limiteItensCarregar)});

        while (cursor.moveToNext()) {
            Entidade objEntidade = new Entidade();
            objEntidade.setId(cursor.getString(cursor.getColumnIndex("id")));
            objEntidade.setNome(cursor.getString(cursor.getColumnIndex("nome")));

            String[] significados = gson.fromJson(cursor.getString(cursor.getColumnIndex(SIGNIFICADOS)), String[].class);
            if (significados != null)
                objEntidade.setSignificado(Arrays.asList(significados));

            String[] sinonimos = gson.fromJson(cursor.getString(cursor.getColumnIndex(SINONIMOS)), String[].class);
            if (sinonimos != null)
                objEntidade.setSinonimos(Arrays.asList(sinonimos));

            Entidade[] atributos = gson.fromJson(cursor.getString(cursor.getColumnIndex(ATRIBUTOS)), Entidade[].class);
            if (atributos != null)
                objEntidade.setAtributos(Arrays.asList(atributos));

            entidades.add(objEntidade);
        }

        cursor.close();
        return entidades;
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

    public void alterarSentenca(Entidade entidade) {
        ContentValues contentValues = getContentValues(entidade);
        bdGateway.getDatabase().update(tabelaEmUso, contentValues, "id=?", new String[]{entidade.getId() + ""});
    }

    private ContentValues getContentValues(Entidade entidade) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("nome", entidade.getNome());
        contentValues.put(SIGNIFICADOS, gson.toJson(entidade.getSignificado()));
        contentValues.put(SINONIMOS, gson.toJson(entidade.getSinonimos()));
        contentValues.put(ATRIBUTOS, gson.toJson(entidade.getAtributos()));
        return contentValues;
    }

    public void excluir(Entidade entidade) {
        bdGateway.getDatabase().delete(tabelaEmUso, "id=?", new String[]{entidade.getId() + ""});
    }

    public void listarDadosTeste() {
        Sentenca s1 = new Sentenca();
        s1.setChave("Quando eu estou");
        s1.setRespostas(Arrays.asList("No presente é claro, ainda não temos uma máquina do tempo", "A pergunta não é quando, e sim onde, não espera"));
        s1.setAcao(AcaoEnum.SEM_ACAO);
        s1.setTipo_item(1);

        Sentenca s2 = new Sentenca();
        s2.setChave("Jogue uma moeda");
        s2.setRespostas(Arrays.asList("Você tirou cara", "Você tirou coroa"));
        s2.setAcao(AcaoEnum.SEM_ACAO);
        s2.setTipo_item(1);

        List<Sentenca> sentencas = Arrays.asList(s1, s2);


        Gson gson = new GsonBuilder().create();
        String s = gson.toJson(sentencas);
    }

    public long getQuantidadeTotal() {
        return DatabaseUtils.queryNumEntries(bdGateway.getDatabase(), tabelaEmUso);
    }

    public List<Sentenca> getSentencasJson() {
        String json = null;
        InputStream inputStream = null;
        try {
            inputStream = Objects.requireNonNull(context).getAssets().open(ARQ_SENTENCAS_PADRAO_JSON);
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

    public List<Entidade> getEntidadesPadraoJson() {
        String json = null;
        InputStream inputStream = null;
        try {
            inputStream = Objects.requireNonNull(context).getAssets().open(ARQ_ENTIDADES_PADRAO_JSON);
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

        return Arrays.asList(gson.fromJson(json, Entidade[].class));
    }
}
