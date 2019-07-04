/*
 * Created by Carlos Mion On 4 aug 2017
 * Link original http://www.decom.ufop.br/imobilis/tutorial-android-speech-to-text-sem-o-pop-up/
 * Modified by Junior Obom 27 jun 2019
 */

package com.paradoxo.amadeus.util;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.paradoxo.amadeus.activity.MainActivity;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechToText implements RecognitionListener {

    private boolean ouvindo = false;
    private Intent recognizerIntent;
    private String textoEscutado = "";
    private SpeechRecognizer speechRecognizer;
    public final BackgroundVoiceListener backgroundVoiceListener;

    public SpeechToText(Context context){
        backgroundVoiceListener = new BackgroundVoiceListener();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(this);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,context.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

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
        Log.e("TAG", "onRmsChanged: " + v);

    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        textoEscutado = "";
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if(matches!=null)
            for (String result : matches)
                textoEscutado += result;
        Log.e("TAG","text: " + textoEscutado);

        MainActivity.setEditTextMsgUsu(textoEscutado);
        // Talvez usar biblioteca EventBus seja o ideal para o futuro aqui
        setOuvindo(false);
    }

    @Override
    public void onEndOfSpeech() {
        setOuvindo(false);
    }

    @Override
    public void onError(int i) {
        Log.e("Erro aou ouvir","textoEscutado: " + textoEscutado);
    }

    @Override
    public void onResults(Bundle bundle) {
        Log.e("Resultados da escuta","Terminou de gravar");
    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {

    }

    public class BackgroundVoiceListener{

        public void run(){
            try {
                if(!isOuvindo()){
                    setOuvindo(true);
                    speechRecognizer.startListening(recognizerIntent);
                    Log.e("TAG","Escutando");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void interrupt() {
            try {
                speechRecognizer.stopListening();
                Log.e("TAG","Parou de escutar");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
