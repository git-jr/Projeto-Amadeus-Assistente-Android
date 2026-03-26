package com.paradoxo.amadeus.util

import android.content.Context
import android.widget.Toast

object Toasts {
    fun meuToast(texto: String, context: Context) =
        Toast.makeText(context, texto, Toast.LENGTH_SHORT).show()

    fun meuToastLong(texto: String, context: Context) =
        Toast.makeText(context, texto, Toast.LENGTH_LONG).show()
}
