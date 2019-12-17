package com.paradoxo.amadeus.modelo;

import android.os.Parcel;

import com.paradoxo.amadeus.enums.AcaoEnum;

import java.util.Date;

public class Mensagem {
    private int id;
    private Date data;
    private Autor autor;
    private AcaoEnum acao;
    private int idResposta;
    private String conteudo;
    private String conteudo_resposta;
    private boolean progresso = false;

    public Mensagem() {
    }

    public boolean iAEhAutor() { // A IA sempre terá o ID "0"
        return this.autor.getId() == 1;
    }

    public Mensagem(int id, String conteudo, Autor autor, Date data, int idResposta) {
        this.id = id;
        this.conteudo = conteudo;
        this.autor = autor;
        this.data = data;
        this.idResposta = idResposta;
    }

    public Mensagem(int id, String conteudo, Autor autor, Date data) {
        this.id = id;
        this.conteudo = conteudo;
        this.autor = autor;
        this.data = data;
    }

    public Mensagem(String conteudo, Autor autor) {
        this.conteudo = conteudo;
        this.autor = autor;
    }

    protected Mensagem(Parcel in) {
        id = in.readInt();
        conteudo = in.readString();
    }

    public Mensagem(int id){
        this.id = id;
    }

    public Mensagem(String conteudo, String conteudo_resposta) {
        this.conteudo = conteudo;
        this.conteudo_resposta = conteudo_resposta;
    }

    public Mensagem(boolean progresso, Autor autor) {
        this.progresso = progresso;
        this.autor = autor;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public Autor getAutor() {
        return autor;
    }

    public void setAutor(Autor autor) {
        this.autor = autor;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }

    public int getIdResposta() {
        return idResposta;
    }

    public void setIdResposta(int idResposta) {
        this.idResposta = idResposta;
    }

    public String getConteudo_resposta() {
        return conteudo_resposta;
    }

    public void setConteudo_resposta(String conteudo_resposta) {
        this.conteudo_resposta = conteudo_resposta;
    }

    public boolean getProgresso() {
        return progresso;
    }

    public void setProgresso(boolean progresso) {
        this.progresso = progresso;
    }

    public boolean temResposta() {
        return this.getIdResposta() > 0;
    }

    public boolean ehUmaResposta() {
        return this.getId() > 0;
    }

    public void setAcao(AcaoEnum acao) {
        this.acao = acao;
    }

    public AcaoEnum getAcao() {
        return acao;
    }
}
