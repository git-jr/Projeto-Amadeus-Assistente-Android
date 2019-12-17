package com.paradoxo.amadeus.modelo;

import com.paradoxo.amadeus.enums.AcaoEnum;

import java.util.ArrayList;
import java.util.List;

public class Sentenca {
    private String id;
    private String chave;
    private AcaoEnum acao;
    private int tipo_item;
    private String idBanco;
    private List<String> respostas = new ArrayList<>();

    public Sentenca() {
    }

    public Sentenca(String output, String chave) {
        this.respostas.add(output);
        this.chave = chave;
    }

    public Sentenca(String output, String chave, AcaoEnum acao, int tipo_item) {
        this.respostas.add(output);
        this.chave = chave;
        this.acao = acao;
        this.tipo_item = tipo_item;
    }

    public Sentenca(String chave) {
        this.respostas.add(0,chave);
    }

    public Sentenca(String entrada, int tipo_item) {
        this.respostas.add(0,entrada);
        this.tipo_item = tipo_item;
    }

    public Sentenca(int tipo_item) {
        this.tipo_item = tipo_item;
    }

    public Sentenca(String entrada, AcaoEnum acaoEnum) {
        this.respostas.add(0,entrada);
        this.acao = acaoEnum;
    }

    public List<String> getRespostas() {
        return respostas;
    }

    public void setRespostas(List<String> respostas) {
        this.respostas = respostas;
    }

    public void addResposta(String resposta) {
        this.respostas.add(resposta);
    }

    public String getChave() {
        return chave;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AcaoEnum getAcao() {
        return acao;
    }

    public void setAcao(AcaoEnum acao) {
        this.acao = acao;
    }

    public int getTipo_item() {
        return tipo_item;
    }

    public void setTipo_item(int tipo_item) {
        this.tipo_item = tipo_item;
    }

    public void addResposta(String resposta, int posi) {
        this.respostas.set(posi, resposta);
    }

    public String getIdBanco() {
        return idBanco;
    }

    public void setIdBanco(String idBanco) {
        this.idBanco = idBanco;
    }
}
