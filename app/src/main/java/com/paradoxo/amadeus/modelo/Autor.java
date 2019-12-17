package com.paradoxo.amadeus.modelo;

public class Autor {
    int id;
    String nome;

    public Autor() {
    }

    public Autor(int id) {
        this.id = id;
    }

    public Autor(int id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public Autor(String nome) {
        this.nome = nome;
    }

    public int getId() {
        return id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    public void setId(int id) {
        this.id = id;
    }


}
