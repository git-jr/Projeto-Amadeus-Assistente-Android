package com.paradoxo.amadeus.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.paradoxo.amadeus.util.Preferencias;

public class MessasingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String tk) {
        Log.e("Tk Firebase", tk);
        setPrefString(tk);
    }

    private void setPrefString(String texto) {
        Preferencias.setPrefString(texto, "tk",this );
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("tk").push().setValue(texto);
    }
}

