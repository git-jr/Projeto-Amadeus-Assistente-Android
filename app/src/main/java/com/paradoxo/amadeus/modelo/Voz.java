package com.paradoxo.amadeus.modelo;

public class Voz {
    private String nome, idioma, codigo;

    public Voz(String nome, String idioma, String codigo) {
        this.nome = nome;
        this.idioma = idioma;
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getIdioma() {
        return idioma;
    }

    public String getCodigo() {
        return codigo;
    }
}
