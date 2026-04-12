package com.paradoxo.amadeus.firebase

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.paradoxo.amadeus.util.Preferencias

class MessasingService : FirebaseMessagingService() {
    override fun onNewToken(tk: String) {
        Log.e("Tk Firebase", tk)
        Preferencias.setPrefString(tk, "tk", this)
        FirebaseSupport.databaseOrNull(this)
            ?.reference
            ?.child("tk")
            ?.push()
            ?.setValue(tk)
    }
}
