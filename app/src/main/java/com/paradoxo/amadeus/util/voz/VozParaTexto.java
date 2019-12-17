/*
 * Created by Carlos Mion On 4 aug 2017
 * Link original http://www.decom.ufop.br/imobilis/tutorial-android-speech-to-text-sem-o-pop-up/
 * Modified by Junior Obom 27 jun 2019
 */

package com.paradoxo.amadeus.util.voz;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.paradoxo.amadeus.modelo.EventoMensagem;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Locale;

public class VozParaTexto implements RecognitionListener {
    Intent recognizerIntent;
    SpeechRecognizer speechRecognizer;
    public final BackgroundVoiceListener backgroundVoiceListener;

    public static final String ERRO_CORRESPONDENCIA = "Sem correspondência";
    public static final String ERRO_AO_TENTAR_ESCUTAR = " ao tentar escutar";
    public static final String ERRO_TEMPO_ESGOTADO = "Tempo para conexão esgotado";
    public static final String ERRO_AO_ESCUTAR = "Ocorreu um erro ao textar escutar: ";
    public static final String ERRO_PERMISSOES_INSUFICIENTES = "Permissões insuficientes";
    public static final String ERRO_TEMPO_PARA_FALA_ESGOTADO = "Tempo para fala esgotado";
    public static final String ERRO_RECONHECEDOR_DE_VOZ_OCUPADO = "Reconhecedor de voz ocupado";

    public VozParaTexto(Context context) {
        backgroundVoiceListener = new BackgroundVoiceListener();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(this);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

/*
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000);
        Essa flag parece não sutir efeito se implementada assim, uma possível solução seja criar uma
        classe extentendo RecognizerIntent e fazer a modificação diretamente, por hora a
        solução atual basta e isso pode ser revisto no futuro
 */

    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onRmsChanged(float v) {
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        StringBuilder textoEscutado = new StringBuilder();
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if (matches != null)
            for (String result : matches)
                textoEscutado.append(result);
                Log.e("Texto parcial", String.valueOf(textoEscutado));

        Log.e("TAG", "Texto esctuado: " + textoEscutado);

        EventBus.getDefault().post(new EventoMensagem(textoEscutado.toString()));
    }

    @Override
    public void onEndOfSpeech() {
    }

    @Override
    public void onError(int error) {
        String nomeErro = "Erro: " + error + ERRO_AO_TENTAR_ESCUTAR;
        switch (error) {
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                nomeErro = ERRO_TEMPO_ESGOTADO;
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                nomeErro = ERRO_TEMPO_PARA_FALA_ESGOTADO;
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                nomeErro = ERRO_CORRESPONDENCIA;
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                nomeErro = ERRO_RECONHECEDOR_DE_VOZ_OCUPADO;
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                nomeErro = ERRO_PERMISSOES_INSUFICIENTES;
                break;

        }
        Log.e("TAG", "Erro: " + error + " " + nomeErro);

        EventBus.getDefault().post(new EventoMensagem(ERRO_AO_ESCUTAR, error, nomeErro));
    }

    @Override
    public void onResults(Bundle bundle) {
        Log.e("Resultados da escuta", "Terminou de gravar");

        ArrayList<String> resultados = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String textoEscutado = resultados!=null?  resultados.get(0) : "";
        EventBus.getDefault().post(new EventoMensagem(textoEscutado,true));
    }

    @Override
    public void onEvent(int i, Bundle bundle) {
    }

    @Override
    public void onBufferReceived(byte[] bytes) {
    }

    public class BackgroundVoiceListener {
        public void run() {
            try {
                speechRecognizer.startListening(recognizerIntent);
                Log.e("TAG", "Escutando");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void interrupt() {
            try {
                //speechRecognizer.stopListening();
                speechRecognizer.destroy();
                Log.e("TAG", "Parou de escutar");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
