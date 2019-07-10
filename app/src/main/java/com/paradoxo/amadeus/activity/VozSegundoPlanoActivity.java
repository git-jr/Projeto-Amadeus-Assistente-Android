package com.paradoxo.amadeus.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.modelo.Mensagem;
import com.paradoxo.amadeus.service.EscutadaoraService;
import com.paradoxo.amadeus.util.Chatbot;

import java.util.Locale;

public class VozSegundoPlanoActivity extends Activity {

    private TextToSpeech textToSpeech;
    private Chatbot chatbot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = this.getIntent();
        setContentView(R.layout.activity_voz_segundo_plano);

        chatbot = new Chatbot(this);

        String textoOuvido = intent.getStringExtra("textoOuvido");

        BuscaInteracao buscaInteracao = new BuscaInteracao();
        buscaInteracao.setEntradaUsuario(textoOuvido);
        buscaInteracao.execute();


    }

    public void iniciarEscutadoraService() {
        setPrefBool("segundoPlanoAtivo", true);
        Intent intent = new Intent(this, EscutadaoraService.class);
        startService(intent);
        VozSegundoPlanoActivity.this.finish();

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

                            if (!texto.equals(getString(R.string.finalizando_segundo_plano)))
                                iniciarEscutadoraService();

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

    private void setPrefBool(String nomeShared, boolean valor) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putBoolean(nomeShared, valor);
        mEditor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        interromperFalaIA();
    }

    private void interromperFalaIA() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
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
                    Log.e("Resposta ao texto dito", resposta.getConteudo());
                }
            }, 500);

        }
    }
}
