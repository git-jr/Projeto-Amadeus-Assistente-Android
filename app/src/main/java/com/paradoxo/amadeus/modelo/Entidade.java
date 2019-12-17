package com.paradoxo.amadeus.modelo;

import java.util.List;

public class Entidade {
    String id;
    Acao acao;
    String nome;
    String idBanco;
    List<String> sinonimos;
    List<Entidade> atributos;
    List<String> significado;

    public Entidade() {
    }

    public Entidade(String nome) {
        this.nome = nome;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public List<String> getSinonimos() {
        return sinonimos;
    }

    public void setSinonimos(List<String> sinonimos) {
        this.sinonimos = sinonimos;
    }

    public List<Entidade> getAtributos() {
        return atributos;
    }

    public void setAtributos(List<Entidade> atributos) {
        this.atributos = atributos;
    }

    public List<String> getSignificado() {
        return significado;
    }

    public void setSignificado(List<String> significado) {
        this.significado = significado;
    }

    public Acao getAcao() {
        return acao;
    }

    public void setAcao(Acao acao) {
        this.acao = acao;
    }

    public String getIdBanco() {
        return idBanco;
    }

    public void setIdBanco(String idBanco) {
        this.idBanco = idBanco;
    }
}
