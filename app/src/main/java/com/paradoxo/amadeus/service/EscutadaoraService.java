package com.paradoxo.amadeus.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.paradoxo.amadeus.util.SpeechToTextSegundoPlano;

public class EscutadaoraService extends Service {
    SpeechToTextSegundoPlano speechToText;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("Script", "OnCreate é chamado apeans uma vez");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("Script", "onStartCommand é chamado sempre que um serviço é chamado de novo");

        String nomeIA = intent.getStringExtra("nomeIA");

        speechToText = new SpeechToTextSegundoPlano(getApplicationContext(), nomeIA);
        speechToText.backgroundVoiceListener.run();

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
