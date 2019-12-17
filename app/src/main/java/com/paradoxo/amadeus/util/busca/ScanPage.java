package com.paradoxo.amadeus.util.busca;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class ScanPage {
    public static final String SELETOR_CSS_SITE_SINONIMO = ".sinonimo";
    public static final String SITE_SINONIMO = "https://www.sinonimos.com.br/%s/";
    public static final String SITE_SIGNIFICADO = "https://www.dicio.com.br/pesquisa.php?q=%s/";
    public static final String SELETOR_CSS_SITE_SIGNIFICADO = ".cl+ span, span:nth-child(4) , span:nth-child(6), span:nth-child(8) , span:nth-child(10) , span:nth-child(12)";

    public static void main(String[] args) {

    }

    public static ArrayList<String> obterSignificado(String chave) throws IOException {
        ArrayList<String> listaSignificados = new ArrayList<>();

        String url = String.format(SITE_SIGNIFICADO, URLEncoder.encode(chave, "UTF-8"));

        Document document = Jsoup.connect(url).get();
        Elements sinonimos = document.select(SELETOR_CSS_SITE_SIGNIFICADO);

        for (Element sinonimo : sinonimos) {
            listaSignificados.add(sinonimo.text());
        }

        return listaSignificados;
    }

    public static ArrayList<String> obterSinonimo(String chave) throws IOException {
        ArrayList<String> listaSinonimos = new ArrayList<>();

        String url = String.format(SITE_SINONIMO, URLEncoder.encode(chave, "ISO-8859-1"));

        Document document = Jsoup.connect(url).get();
        Elements sinonimos = document.select(SELETOR_CSS_SITE_SINONIMO);

        for (Element sinonimo : sinonimos) {
            listaSinonimos.add(sinonimo.text());
        }

        return listaSinonimos;
    }

}