package com.paradoxo.amadeus.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

object FirebaseSupport {

    fun databaseOrNull(context: Context): FirebaseDatabase? {
        return runCatching {
            val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)
            if (app == null) {
                Log.w(TAG, "Firebase indisponivel nesta instalacao.")
                null
            } else {
                FirebaseDatabase.getInstance(app)
            }
        }.getOrElse { error ->
            Log.w(TAG, "Nao foi possivel inicializar o Firebase.", error)
            null
        }
    }

    private const val TAG = "FirebaseSupport"
}
