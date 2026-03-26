package com.paradoxo.amadeus.firebase

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.paradoxo.amadeus.util.Preferencias

class MessasingService : FirebaseMessagingService() {
    override fun onNewToken(tk: String) {
        Log.e("Tk Firebase", tk)
        Preferencias.setPrefString(tk, "tk", this)
        FirebaseDatabase.getInstance().reference.child("tk").push().setValue(tk)
    }
}
