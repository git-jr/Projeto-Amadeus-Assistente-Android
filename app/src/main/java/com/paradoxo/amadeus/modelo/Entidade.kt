package com.paradoxo.amadeus.modelo

class Entidade(var nome: String? = null) {
    var id: String? = null
    var acao: Acao? = null
    var idBanco: String? = null
    var sinonimos: List<String>? = null
    var atributos: List<Entidade>? = null
    var significado: List<String>? = null
}
