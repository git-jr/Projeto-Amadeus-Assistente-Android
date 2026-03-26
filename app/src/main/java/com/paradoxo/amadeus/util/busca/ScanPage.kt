package com.paradoxo.amadeus.util.busca

import org.jsoup.Jsoup
import java.net.URLEncoder

object ScanPage {
    const val SELETOR_CSS_SITE_SINONIMO = ".sinonimo"
    const val SITE_SINONIMO = "https://www.sinonimos.com.br/%s/"
    const val SITE_SIGNIFICADO = "https://www.dicio.com.br/pesquisa.php?q=%s/"
    const val SELETOR_CSS_SITE_SIGNIFICADO =
        ".cl+ span, span:nth-child(4) , span:nth-child(6), span:nth-child(8) , span:nth-child(10) , span:nth-child(12)"

    @Throws(Exception::class)
    fun obterSignificado(chave: String): ArrayList<String> {
        val url = SITE_SIGNIFICADO.format(URLEncoder.encode(chave, "UTF-8"))
        return Jsoup.connect(url).get()
            .select(SELETOR_CSS_SITE_SIGNIFICADO)
            .mapTo(ArrayList()) { it.text() }
    }

    @Throws(Exception::class)
    fun obterSinonimo(chave: String): ArrayList<String> {
        val url = SITE_SINONIMO.format(URLEncoder.encode(chave, "ISO-8859-1"))
        return Jsoup.connect(url).get()
            .select(SELETOR_CSS_SITE_SINONIMO)
            .mapTo(ArrayList()) { it.text() }
    }
}
