package com.paradoxo.amadeus.util.voz

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.util.Preferencias
import java.util.Locale

class TextoParaVoz(val context: Context) {
    private var estiloDeVozPadrao: String
    private var textToSpeech: TextToSpeech? = null
    private val respostasNaoEncontradas: List<String>

    companion object {
        const val PREF_VOZ_ATIVA = "voz_ativa"
        const val ESTILO_DE_VOZ_PADRAO = "estiloDeVozPadrao"
        const val PREF_FALAR_RESPOSTA_NAO_ENCONTRADA = "falar_resposta_nao_encontrada"
    }

    init {
        estiloDeVozPadrao = Preferencias.getPrefString(ESTILO_DE_VOZ_PADRAO, context).ifEmpty { "default" }
        respostasNaoEncontradas = context.resources.getStringArray(R.array.resposta_nao_localizada).toList()
    }

    fun configurarFalaIA(texto: String) {
        if (!Preferencias.getPrefBool(PREF_VOZ_ATIVA, context, false)) return
        if (Preferencias.getPrefBool(PREF_FALAR_RESPOSTA_NAO_ENCONTRADA, context, false)) {
            if (respostasNaoEncontradas.contains(texto)) return
        }

        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Lingua não suportada")
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        textToSpeech?.setVoice(Voice(estiloDeVozPadrao, Locale.getDefault(), 1, 1, false, null))
                        Log.e("TTS", "Lingua suportada")
                    }
                }
                falar(texto)
            }
        }
    }

    private fun falar(texto: String) {
        val utteranceId = hashCode().toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            textToSpeech?.speak(texto, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    fun interromperFalaIA() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
