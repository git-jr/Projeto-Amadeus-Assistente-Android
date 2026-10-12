package com.paradoxo.amadeus.util

import android.net.Uri
import androidx.core.net.toUri

object Validadora {
    fun validarTexto(texto: String): Boolean = texto.trim().isNotEmpty()

    /** Retorna o link como [Uri] só se for https com host; caso contrário, null. */
    fun uriHttpsValida(link: String?): Uri? {
        val uri = link?.trim()?.takeIf { it.isNotEmpty() }?.toUri() ?: return null
        return uri.takeIf { it.scheme.equals("https", ignoreCase = true) && !it.host.isNullOrBlank() }
    }
}
