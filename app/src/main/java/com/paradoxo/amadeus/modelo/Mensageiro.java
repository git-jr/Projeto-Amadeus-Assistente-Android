package com.paradoxo.amadeus.modelo;

import java.util.ArrayList;
import java.util.List;

public class Mensageiro {

    public List<Mensagem> mensagem = new ArrayList<>();
    private boolean status;

    public Mensageiro(Boolean status) {
        status = status;
    }

    public Mensageiro(List<Mensagem> mensagem, boolean status) {
        this.mensagem = mensagem;
        this.status = status;
    }

    public Mensageiro() {

    }

    public List<Mensagem> getMensagem() {
        return mensagem;
    }

    public void setMensagem(List<Mensagem> mensagem) {
        this.mensagem = mensagem;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
