package com.paradoxo.amadeus.modelo

data class EventoMensagem(
    var mensagem: String,
    var codErro: Int = 0,
    var nomeErro: String? = null,
    var jaTerminou: Boolean = false
)
