package com.paradoxo.amadeus.modelo;

import com.paradoxo.amadeus.enums.AcaoEnum;

import java.util.List;

public class Acao {
    AcaoEnum acaoEnum;
    List<String> gatilhos;

    public Acao(AcaoEnum acao) {
        this.acaoEnum = acao;
    }

    public Acao(AcaoEnum acaoEnum, List<String> gatilhos) {
        this.acaoEnum = acaoEnum;
        this.gatilhos = gatilhos;
    }

    public AcaoEnum getAcaoEnum() {
        return acaoEnum;
    }

    public void setAcaoEnum(AcaoEnum acaoEnum) {
        this.acaoEnum = acaoEnum;
    }

    public List<String> getGatilhos() {
        return gatilhos;
    }

    public void setGatilhos(List<String> gatilhos) {
        this.gatilhos = gatilhos;
    }
}
