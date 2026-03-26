/*
 * Created by Carlos Mion On 4 aug 2017
 * Link original http://www.decom.ufop.br/imobilis/tutorial-android-speech-to-text-sem-o-pop-up/
 * Modified by Junior Obom 27 jun 2019
 */

package com.paradoxo.amadeus.util.voz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.paradoxo.amadeus.modelo.EventoMensagem
import org.greenrobot.eventbus.EventBus
import java.util.Locale

class VozParaTexto(context: Context) : RecognitionListener {
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(this@VozParaTexto)
    }
    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
    }

    val backgroundVoiceListener = BackgroundVoiceListener()

    companion object {
        const val ERRO_CORRESPONDENCIA = "Sem correspondência"
        const val ERRO_AO_TENTAR_ESCUTAR = " ao tentar escutar"
        const val ERRO_TEMPO_ESGOTADO = "Tempo para conexão esgotado"
        const val ERRO_AO_ESCUTAR = "Ocorreu um erro ao textar escutar: "
        const val ERRO_PERMISSOES_INSUFICIENTES = "Permissões insuficientes"
        const val ERRO_TEMPO_PARA_FALA_ESGOTADO = "Tempo para fala esgotado"
        const val ERRO_RECONHECEDOR_DE_VOZ_OCUPADO = "Reconhecedor de voz ocupado"
    }

    override fun onReadyForSpeech(bundle: Bundle) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(v: Float) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(i: Int, bundle: Bundle) {}
    override fun onBufferReceived(bytes: ByteArray) {}

    override fun onPartialResults(partialResults: Bundle) {
        val matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val textoEscutado = matches?.joinToString("") ?: ""
        Log.e("Texto parcial", textoEscutado)
        Log.e("TAG", "Texto esctuado: $textoEscutado")
        EventBus.getDefault().post(EventoMensagem(textoEscutado))
    }

    override fun onError(error: Int) {
        val nomeErro = when (error) {
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> ERRO_TEMPO_ESGOTADO
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> ERRO_TEMPO_PARA_FALA_ESGOTADO
            SpeechRecognizer.ERROR_NO_MATCH -> ERRO_CORRESPONDENCIA
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> ERRO_RECONHECEDOR_DE_VOZ_OCUPADO
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> ERRO_PERMISSOES_INSUFICIENTES
            else -> "Erro: $error$ERRO_AO_TENTAR_ESCUTAR"
        }
        Log.e("TAG", "Erro: $error $nomeErro")
        EventBus.getDefault().post(EventoMensagem(ERRO_AO_ESCUTAR, error, nomeErro))
    }

    override fun onResults(bundle: Bundle) {
        Log.e("Resultados da escuta", "Terminou de gravar")
        val resultados = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val textoEscutado = resultados?.get(0) ?: ""
        EventBus.getDefault().post(EventoMensagem(textoEscutado, jaTerminou = true))
    }

    inner class BackgroundVoiceListener {
        fun run() {
            try {
                speechRecognizer.startListening(recognizerIntent)
                Log.e("TAG", "Escutando")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun interrupt() {
            try {
                speechRecognizer.destroy()
                Log.e("TAG", "Parou de escutar")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
