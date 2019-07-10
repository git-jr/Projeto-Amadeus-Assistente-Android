/*
 * Created by Carlos Mion On 4 aug 2017
 * Link original http://www.decom.ufop.br/imobilis/tutorial-android-speech-to-text-sem-o-pop-up/
 * Modified by Junior Obom 27 jun 2019
 */

package com.paradoxo.amadeus.util;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.paradoxo.amadeus.activity.VozSegundoPlanoActivity;
import com.paradoxo.amadeus.service.TratarRespostaService;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechToTextSegundoPlano implements RecognitionListener {

    private final Context context;
    private boolean chaveDetecada;
    private final String nomeChave;
    private Intent recognizerIntent;
    private boolean ouvindo = false;
    private AudioManager audioManager;
    private SpeechRecognizer speechRecognizer;
    private final String oQueFoiOuvidoAteAgora = null;
    public final BackgroundVoiceListener backgroundVoiceListener;

    public SpeechToTextSegundoPlano(Context context, String nomeChave) {
        this.nomeChave = nomeChave;
        this.context = context;

        backgroundVoiceListener = new BackgroundVoiceListener();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(this);

        int silencion = 9000;
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, silencion);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silencion);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);


        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);


    }

    private boolean isOuvindo() {
        return ouvindo;
    }

    private void setOuvindo(boolean ouvindo) {
        this.ouvindo = ouvindo;
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        setOuvindo(false);
    }

    @Override
    public void onBeginningOfSpeech() {
        setOuvindo(true);
    }

    @Override
    public void onRmsChanged(float v) {
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if (matches != null)
            for (String result : matches)
                if (result.contains(nomeChave)) {
                    Log.e("Chave detectada", nomeChave);
                    //this.backgroundVoiceListener.interrupt();
                    chaveDetecada = true;
                } else {
                    Log.e("Texto ouvido", "Palavra comum");
                }

        setOuvindo(false);
    }

    @Override
    public void onEndOfSpeech() {
        setOuvindo(false);
    }

    @Override
    public void onError(int i) {
        Log.e("Não escutei nada", "Mas ainda estou ouvindo");
        this.backgroundVoiceListener.run();
    }

    @Override
    public void onResults(Bundle bundle) {
        Log.e("Resultados da escuta", "Terminou de gravar");
        if (!chaveDetecada) {
            this.backgroundVoiceListener.run();
        } else {
            String resultado = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0).toLowerCase();

            Log.e("Gravação", "Interrompendo a gravação");
            Log.e("TextoOuvido Integral", resultado);

            if (!resultado.replace(nomeChave, "").trim().isEmpty()) {
                // Se a palavra chave estiver acompanhada de um texto vamos lidar com ela, se não reniciar a audição

                try {
                    String resultadoTratado = resultado.substring(resultado.indexOf(nomeChave) + nomeChave.length() + 1).toLowerCase().trim();
                    Log.e("TextoOuvido tratado", resultadoTratado);

/*
                    Intent intent = new Intent(context, VozSegundoPlanoActivity.class);
                    intent.putExtra("textoOuvido", resultadoTratado);
                    context.startActivity(intent);
*/

                    Intent intent2 = new Intent(context, TratarRespostaService.class);
                    intent2.putExtra("textoOuvido", resultadoTratado);
                    context.startService(intent2);


                } catch (Exception e) {
                    this.backgroundVoiceListener.run();
                }

            } else {
                this.backgroundVoiceListener.run();
            }


        }
    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {

    }

    public class BackgroundVoiceListener {

        public void run() {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            // Muta o volume da música para veitar o som do beep da APi do Google
            try {
                if (!isOuvindo()) {
                    setOuvindo(true);
                    speechRecognizer.startListening(recognizerIntent);
                    Log.e("TAG", "Escutando");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
                }
            }, 500);
            // Desmuta
        }

        public void interrupt() {
            try {
                speechRecognizer.stopListening();
                Log.e("TAG", "Parou de escutar");
                setOuvindo(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
