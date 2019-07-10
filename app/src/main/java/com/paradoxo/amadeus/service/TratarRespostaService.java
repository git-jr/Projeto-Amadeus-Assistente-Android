package com.paradoxo.amadeus.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;

import androidx.annotation.Nullable;

import com.paradoxo.amadeus.R;

import java.util.Locale;

public class TratarRespostaService extends Service {
    private TextToSpeech textToSpeech;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        falarTexto("No dia mais claro, na noite mais escura, nenhum mal escapará a minha visão! Que aqueles que adoram o poder do mal, temam o meu poder a luz do lanterna vede");

        return super.onStartCommand(intent, flags, startId);
    }

    private void falarTexto(final String texto) {

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.getDefault());

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Lingua não suportada");
                    } else {
                        String estiloDeVozPadrao = getPrefString("estiloDeVozPadrao");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            textToSpeech.setVoice(new Voice(estiloDeVozPadrao, Locale.getDefault(), 1, 1, false, null));
                            textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null);

                            //if (!texto.equals(getString(R.string.finalizando_segundo_plano)))
                            //iniciarEscutadoraService();

                        }
                    }
                }
            }
        });
        Log.e("Texto Segundo Plano", "Sucesso " + texto);

    }

    private String getPrefString(String nomeShared) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        return sharedPreferences.getString(nomeShared, "");
    }
}
