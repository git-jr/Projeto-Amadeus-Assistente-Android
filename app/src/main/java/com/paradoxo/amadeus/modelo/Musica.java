package com.paradoxo.amadeus.modelo;

public class Musica {
    String nome, caminho, artista, album;

    public Musica(String nome, String caminho, String artista, String album) {
        this.nome = nome;
        this.caminho = caminho;
        this.artista = artista;
        this.album = album;
    }

    public Musica() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCaminho() {
        return caminho;
    }

    public void setCaminho(String caminho) {
        this.caminho = caminho;
    }

    public String getArtista() {
        return artista;
    }

    public void setArtista(String artista) {
        this.artista = artista;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }
}
