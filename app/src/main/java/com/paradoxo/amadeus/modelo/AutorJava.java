package com.paradoxo.amadeus.modelo;

public class AutorJava {
    int id;
    String nome;

    public AutorJava() {
    }

    public AutorJava(int id) {
        this.id = id;
    }

    public AutorJava(int id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public AutorJava(String nome) {
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
