package com.paradoxo.amadeus.util.voz;


import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;

import com.paradoxo.amadeus.R;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.paradoxo.amadeus.util.Preferencias.getPrefBool;
import static com.paradoxo.amadeus.util.Preferencias.getPrefString;

public class TextoParaVoz {
    final Context context;
    String estiloDeVozPadrao;
    TextToSpeech textToSpeech;
    List<String> respostasNaoEncontradas;

    public static final String PREF_VOZ_ATIVA = "voz_ativa";
    public static final String ESTILO_DE_VOZ_PADRAO = "estiloDeVozPadrao";
    public static final String PREF_FALAR_RESPOSTA_NAO_ENCONTRADA = "falar_resposta_nao_encontrada";


    public TextoParaVoz(Context context) {
        this.context = context;
        estiloDeVozPadrao = getPrefString(ESTILO_DE_VOZ_PADRAO, context);
        estiloDeVozPadrao = estiloDeVozPadrao.isEmpty() ? "default" : estiloDeVozPadrao;
        respostasNaoEncontradas = Arrays.asList(context.getResources().getStringArray(R.array.resposta_nao_localizada));
    }

    public void configurarFalaIA(String texto) {

        if (!getPrefBool(PREF_VOZ_ATIVA, context, false)) return;

        if (getPrefBool(PREF_FALAR_RESPOSTA_NAO_ENCONTRADA, context, false)) {
            for (String resposta : respostasNaoEncontradas) {
                if (resposta.equals(texto)) return;
            }

        }

        textToSpeech = new android.speech.tts.TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Lingua não suportada");
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        textToSpeech.setVoice(new Voice(estiloDeVozPadrao, Locale.getDefault(), 1, 1, false, null));
                        Log.e("TTS", "Lingua suportada");
                    }
                }
                falar(texto);
            }
        });
    }

    private void falar(String texto) {
        String utteranceId = this.hashCode() + "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        } else {
            textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void interromperFalaIA() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
