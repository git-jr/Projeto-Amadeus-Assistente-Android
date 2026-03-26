package com.paradoxo.amadeus.modelo;

import com.paradoxo.amadeus.enums.AcaoEnum;

import java.util.List;

public class AcaoJava {
    AcaoEnum acaoEnum;
    List<String> gatilhos;

    public AcaoJava(AcaoEnum acao) {
        this.acaoEnum = acao;
    }

    public AcaoJava(AcaoEnum acaoEnum, List<String> gatilhos) {
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
