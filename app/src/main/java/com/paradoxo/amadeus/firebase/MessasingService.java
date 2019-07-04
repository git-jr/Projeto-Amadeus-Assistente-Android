package com.paradoxo.amadeus.firebase;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;

public class MessasingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String tk) {
        Log.e("Token Firebase", tk);
        setPrefString(tk);
    }

    private void setPrefString(String texto) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putString("tk", texto);
        mEditor.apply();
    }

}

