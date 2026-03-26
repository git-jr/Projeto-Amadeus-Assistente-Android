package com.paradoxo.amadeus.modelo

import com.paradoxo.amadeus.enums.AcaoEnum

data class Acao(
    var acaoEnum: AcaoEnum,
    var gatilhos: List<String>? = null
)
