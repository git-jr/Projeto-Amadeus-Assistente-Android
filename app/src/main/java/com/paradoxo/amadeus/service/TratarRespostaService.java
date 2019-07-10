package com.paradoxo.amadeus.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;

import androidx.annotation.Nullable;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.activity.VozSegundoPlanoActivity;
import com.paradoxo.amadeus.modelo.Mensagem;
import com.paradoxo.amadeus.util.Chatbot;

import java.util.Locale;

public class TratarRespostaService extends Service {

    private TextToSpeech textToSpeech;
    private Chatbot chatbot;

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

        chatbot = new Chatbot(this);

        String textoOuvido = intent.getStringExtra("textoOuvido");

        BuscaInteracao buscaInteracao = new BuscaInteracao();
        buscaInteracao.setEntradaUsuario(textoOuvido);
        buscaInteracao.execute();

        return super.onStartCommand(intent, flags, startId);
    }

    public void iniciarEscutadoraService() {
        setPrefBool("segundoPlanoAtivo", true);

        while (true) {
            if (!textToSpeech.isSpeaking()) {
                Intent intent = new Intent(this, EscutadaoraService.class);
                startService(intent);
                stopSelf();
                break;
            }
        }
    }


    private void falarTexto(final String texto) {

        try {
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

                                Log.e("Resposta ao texto dito", texto);

                                if (!texto.equals(getString(R.string.finalizando_segundo_plano)))
                                    iniciarEscutadoraService();

                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            falarTexto(texto);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        interromperFalaIA();
    }

    private void interromperFalaIA() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    private String getPrefString(String nomeShared) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        return sharedPreferences.getString(nomeShared, "");
    }

    private void setPrefBool(String nomeShared, boolean valor) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putBoolean(nomeShared, valor);
        mEditor.apply();
    }

    @SuppressLint("StaticFieldLeak")
    public class BuscaInteracao extends AsyncTask<Void, Void, Void> {
        private String entradaUsuario;
        private Mensagem resposta;

        private void setEntradaUsuario(String entradaUsuario) {
            this.entradaUsuario = entradaUsuario;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            resposta = chatbot.gerarRespoosta(entradaUsuario);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    falarTexto(resposta.getConteudo());
                }
            }, 500);

        }
    }
}
