package com.paradoxo.amadeus.util;

import android.content.Context;

import com.paradoxo.amadeus.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Classificador {

    public String normalizar(String entrada) {
        entrada = entrada.toLowerCase();
        entrada = entrada.trim();
        // Retira os espaços do fim e do começo para aumentar o pontencial de busca das respostas gravadas
        return entrada;
    }

    private void carregarRespostasPositivasENegativas(Context context) {
        // Usar o conceito dessa função na próxima fase de cognção da IA

        List<String> respostasNegativas = new ArrayList<>();
        List<String> respostasPositivas = new ArrayList<>();
        respostasPositivas.addAll(Arrays.asList(context.getResources().getStringArray(R.array.respostas_positivas)));
        respostasNegativas.addAll(Arrays.asList(context.getResources().getStringArray(R.array.respostas_negativas)));
    }

}

