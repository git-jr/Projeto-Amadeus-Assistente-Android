package com.paradoxo.amadeus.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Banco {
    private int id;
    private String nome;
    private String idAutor;
    private long dtAtualizado;
    private String dtAtualizadoExibicao;
    private boolean baixado;
    private String tamanho;
    private String urlDownlaod;

    public Banco() {
    }

    public Banco(String dtAtualizadoExibicao, String idAutor, String nome,   String tamanho, String urlDownlaod) {
        this.nome = nome;
        this.idAutor = idAutor;
        this.dtAtualizadoExibicao = dtAtualizadoExibicao;
        this.tamanho = tamanho;
        this.urlDownlaod = urlDownlaod;
    }

    public Banco(int id, String nome, String idAutor, long dtAtualizado, String dtAtualizadoExibicao, boolean baixado, String tamanho, String urlDownlaod) {
        this.id = id;
        this.nome = nome;
        this.idAutor = idAutor;
        this.dtAtualizado = dtAtualizado;
        this.dtAtualizadoExibicao = dtAtualizadoExibicao;
        this.baixado = baixado;
        this.tamanho = tamanho;
        this.urlDownlaod = urlDownlaod;
    }

    public Banco(int id, String nome, String idAutor, long dtAtualizado, boolean baixado, String tamanho) {
        this.id = id;
        this.idAutor = idAutor;
        this.nome = nome;
        this.dtAtualizado = dtAtualizado;
        this.baixado = baixado;
        this.tamanho = tamanho;
    }


    public Banco(String nome, String tamanho, String idAutor, String dtAtualizadoExibicao, boolean baixado) {
        this.nome = nome;
        this.tamanho = tamanho;
        this.idAutor = idAutor;
        this.dtAtualizadoExibicao = dtAtualizadoExibicao;
        this.baixado = baixado;
    }

    public Banco(String nome, String tamanho, String idAutor, String dtAtualizadoExibicao, boolean baixado, String urlDownlaod) {
        this.nome = nome;
        this.tamanho = tamanho;
        this.idAutor = idAutor;
        this.dtAtualizadoExibicao = dtAtualizadoExibicao;
        this.baixado = baixado;
        this.urlDownlaod = urlDownlaod;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public long getDtAtualizado() {
        return dtAtualizado;
    }

    public void setDtAtualizado(long dtAtualizado) {
        this.dtAtualizado = dtAtualizado;
    }

    public String getIdAutor() {
        return idAutor;
    }

    public void setIdAutor(String idAutor) {
        this.idAutor = idAutor;
    }

    public boolean getBaixado() {
        return baixado;
    }

    public void setBaixado(boolean baixado) {
        this.baixado = baixado;
    }

    public String getTamanho() {
        return tamanho;
    }

    public void setTamanho(String tamanho) {
        this.tamanho = tamanho;
    }

    public String getDtAtualizadoExibicao() {
        return dtAtualizadoExibicao;
    }

    public void setDtAtualizadoExibicao(String dtAtualizadoExibicao) {
        this.dtAtualizadoExibicao = dtAtualizadoExibicao;
    }

    public String getUrlDownlaod() {
        return urlDownlaod;
    }

    public void setUrlDownlaod(String urlDownlaod) {
        this.urlDownlaod = urlDownlaod;
    }
}
