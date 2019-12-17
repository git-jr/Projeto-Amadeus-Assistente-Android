package com.paradoxo.amadeus.dao;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paradoxo.amadeus.modelo.Acao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AcaoDAO {
    public static final String ARQ_LISTA_ACOES_JSON = "ListaAcoes.json";
    Context context;

    public AcaoDAO(Context context) {
        this.context = context;
    }

    public List<Acao> getAcoes() {
        String json;
        InputStream inputStream;
        try {
            inputStream = Objects.requireNonNull(context).getAssets().open(ARQ_LISTA_ACOES_JSON);
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

        return Arrays.asList(gson.fromJson(json, Acao[].class));
    }

    public Long getQuantidadeTotal() {
        String json;
        InputStream inputStream;
        try {
            inputStream = Objects.requireNonNull(context).getAssets().open(ARQ_LISTA_ACOES_JSON);
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

        return (long) Arrays.asList(gson.fromJson(json, Acao[].class)).size();
    }
}
