package com.paradoxo.amadeus.modelo;

public class EventoMensagemJava {
    private int codErro;
    private String nomeErro;
    private String mensagem;
    private boolean jaTerminou;

    public EventoMensagemJava(String mensagem) {
        this.mensagem = mensagem;
    }

    public EventoMensagemJava(String mensagem, int codErro, String nomeErro) {
        this.mensagem = mensagem;
        this.codErro = codErro;
        this.nomeErro = nomeErro;
    }

    public EventoMensagemJava(String mensagem, boolean jaTerminou) {
        this.mensagem = mensagem;
        this.jaTerminou = jaTerminou;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public int getCodErro() {
        return codErro;
    }

    public void setCodErro(int codErro) {
        this.codErro = codErro;
    }

    public String getNomeErro() {
        return nomeErro;
    }

    public void setNomeErro(String nomeErro) {
        this.nomeErro = nomeErro;
    }

    public boolean getJaTerminou() {
        return jaTerminou;
    }

    public void setJaTerminou(boolean jaTerminou) {
        this.jaTerminou = jaTerminou;
    }
}
