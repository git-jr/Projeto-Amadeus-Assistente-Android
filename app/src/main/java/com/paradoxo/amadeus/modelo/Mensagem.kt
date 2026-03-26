package com.paradoxo.amadeus.modelo

import com.paradoxo.amadeus.enums.AcaoEnum
import java.util.Date

class Mensagem {
    var id: Int = 0
    var data: Date? = null
    var autor: Autor? = null
    var acao: AcaoEnum? = null
    var idResposta: Int = 0
    var conteudo: String? = null
    var conteudo_resposta: String? = null
    var progresso: Boolean = false

    constructor()

    constructor(id: Int, conteudo: String, autor: Autor, data: Date, idResposta: Int) {
        this.id = id
        this.conteudo = conteudo
        this.autor = autor
        this.data = data
        this.idResposta = idResposta
    }

    constructor(id: Int, conteudo: String, autor: Autor, data: Date) {
        this.id = id
        this.conteudo = conteudo
        this.autor = autor
        this.data = data
    }

    constructor(conteudo: String, autor: Autor) {
        this.conteudo = conteudo
        this.autor = autor
    }

    constructor(id: Int) {
        this.id = id
    }

    constructor(conteudo: String, conteudo_resposta: String) {
        this.conteudo = conteudo
        this.conteudo_resposta = conteudo_resposta
    }

    constructor(progresso: Boolean, autor: Autor) {
        this.progresso = progresso
        this.autor = autor
    }

    fun iAEhAutor(): Boolean = autor?.id == 1

    fun temResposta(): Boolean = idResposta > 0

    fun ehUmaResposta(): Boolean = id > 0
}
