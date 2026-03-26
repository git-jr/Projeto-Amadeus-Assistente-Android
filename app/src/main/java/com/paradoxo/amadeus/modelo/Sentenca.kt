package com.paradoxo.amadeus.modelo

import com.paradoxo.amadeus.enums.AcaoEnum

class Sentenca {
    var id: String? = null
    var chave: String? = null
    var acao: AcaoEnum? = null
    var tipo_item: Int = 0
    var idBanco: String? = null
    var respostas: MutableList<String> = mutableListOf()

    constructor()

    constructor(output: String, chave: String) {
        respostas.add(output)
        this.chave = chave
    }

    constructor(output: String, chave: String, acao: AcaoEnum, tipo_item: Int) {
        respostas.add(output)
        this.chave = chave
        this.acao = acao
        this.tipo_item = tipo_item
    }

    constructor(chave: String) {
        respostas.add(0, chave)
    }

    constructor(entrada: String, tipo_item: Int) {
        respostas.add(0, entrada)
        this.tipo_item = tipo_item
    }

    constructor(tipo_item: Int) {
        this.tipo_item = tipo_item
    }

    constructor(entrada: String, acaoEnum: AcaoEnum) {
        respostas.add(0, entrada)
        this.acao = acaoEnum
    }

    fun addResposta(resposta: String) {
        respostas.add(resposta)
    }

    fun addResposta(resposta: String, posi: Int) {
        respostas[posi] = resposta
    }
}
