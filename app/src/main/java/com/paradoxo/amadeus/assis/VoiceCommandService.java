package com.paradoxo.amadeus.assis;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.voice.VoiceInteractionService;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.paradoxo.amadeus.activity.MainActivity;

import java.util.Locale;

/**
 * Serviço de interação por voz. A API pública {@code AlwaysOnHotwordDetector} foi removida do
 * SDK de compilação em versões recentes; o fluxo de hotword foi desativado para permitir build.
 * Não se passa 1 mës da minha vida de Dev sem que a Google dificulte meu trabalho, vou arrumar depois
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class VoiceCommandService extends VoiceInteractionService {
    private static final String TAG = "VoiceCommandService";
    Locale locale = new Locale("pt-BR");
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;

    @Override
    public void onCreate() {
        Log.e(TAG, "Entered on create");
        super.onCreate();
    }

    @Override
    public void onReady() {
        super.onReady();
        Log.e(TAG, "onReady: " + this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle args = new Bundle();
        args.putParcelable("intent", new Intent(this, MainActivity.class));
        stopSelf(startId);
        return START_NOT_STICKY;
    }
}
