package com.paradoxo.amadeus.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.modelo.Mensagem;
import com.paradoxo.amadeus.util.Chatbot;

import java.util.Locale;

public class SegundoPlanoActivity extends AppCompatActivity {

    private TextToSpeech textToSpeech;
    private Chatbot chatbot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = this.getIntent();
        setContentView(R.layout.activity_segundo_plano);

        chatbot = new Chatbot(this);

        String textoOuvido = intent.getStringExtra("textoOuvido");

        BuscaInteracao buscaInteracao = new BuscaInteracao();
        buscaInteracao.setEntradaUsuario(textoOuvido);
        buscaInteracao.execute();


    }


    private void configurarFalaIA(final String texto) {
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
                            falar(texto);
                        }
                    }

                }
            }
        });
    }

    public void falar(String texto) {
        textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null);
    }

    private String getPrefString(String nomeShared) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        return sharedPreferences.getString(nomeShared, "");
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
            configurarFalaIA(resposta.getConteudo());
            Log.e("Resposta ao texto dito", resposta.getConteudo());

        }
    }
}
