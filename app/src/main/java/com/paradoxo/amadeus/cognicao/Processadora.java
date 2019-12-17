package com.paradoxo.amadeus.cognicao;

import android.app.Activity;

import com.paradoxo.amadeus.modelo.Entidade;
import com.paradoxo.amadeus.modelo.Sentenca;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import static com.paradoxo.amadeus.util.Preferencias.getPrefString;

public class Processadora {
    Activity activity;
    Acionadora acionadora;

    public static final String PREF_NOME_IA = "nomeIa";

    public Processadora(Activity activity) {
        this.activity = activity;
        acionadora = new Acionadora(activity);
        EventBus.getDefault().register(this);
    }


    public void processarEntrada(String entrada){
        entrada = entrada.toLowerCase();
        entrada = entrada.replace("?", "").replace("!", "");
        entrada = entrada.replace(getPrefString(PREF_NOME_IA, activity),"");
        entrada = entrada.trim();

        acionadora.isAcao(entrada);
    }

    @Subscribe
    public void lidarResultadoProcessamento(Entidade entidade){
        Sentenca sentenca = new Sentenca("Via Event");
        EventBus.getDefault().post(sentenca);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        EventBus.getDefault().unregister(this);
    }
}
