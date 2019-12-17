package com.paradoxo.amadeus.util.voz;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class EscutadaoraService extends Service {
    VozParaTexto vozParaTexto;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        vozParaTexto = new VozParaTexto(getApplicationContext());
        vozParaTexto.backgroundVoiceListener.run();

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
        vozParaTexto.backgroundVoiceListener.interrupt();
    }


}
